package com.nexient.jazzy.feature.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.nexient.jazzy.core.data.NetworkMonitor
import com.nexient.jazzy.core.data.catalog.CatalogRepository
import com.nexient.jazzy.core.data.catalog.HomeTab
import com.nexient.jazzy.core.data.catalog.SearchFilter
import com.nexient.jazzy.core.data.catalog.SearchRepository
import com.nexient.jazzy.core.data.catalog.tmdbRails
import com.nexient.jazzy.core.data.library.LibraryRepository
import com.nexient.jazzy.core.data.profile.ProfileRepository
import com.nexient.jazzy.core.model.Profile
import com.nexient.jazzy.core.model.SearchResult
import com.nexient.jazzy.core.model.TitleRef
import com.nexient.jazzy.core.model.TitleSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Minimum query length before a search runs (SR-1). */
internal const val MIN_QUERY_LENGTH = 2

/** Debounce after the last keystroke (SR-1). */
internal const val SEARCH_DEBOUNCE_MS = 300L

data class PopularSearches(
    val items: List<TitleSummary> = emptyList(),
    val loading: Boolean = true,
    val failed: Boolean = false,
)

data class SearchUiState(
    val profile: Profile? = null,
    val filter: SearchFilter = SearchFilter.ALL,
    /** The (trimmed, debounced) query the results currently belong to; blank while idle. */
    val activeQuery: String = "",
    val recent: List<String> = emptyList(),
    val popular: PopularSearches = PopularSearches(),
    val myList: Set<TitleRef> = emptySet(),
    val liked: Set<TitleRef> = emptySet(),
    val offline: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    profiles: ProfileRepository,
    private val search: SearchRepository,
    private val catalog: CatalogRepository,
    library: LibraryRepository,
    network: NetworkMonitor,
) : ViewModel() {

    /**
     * The text in the search box. Kept as Compose state (not a StateFlow) so the text field is
     * updated synchronously and the cursor never jumps; results follow it through [activeQuery].
     */
    var query by mutableStateOf("")
        private set

    private val filter = MutableStateFlow(SearchFilter.ALL)
    private val popular = MutableStateFlow(PopularSearches())
    private val profile = profiles.activeProfile.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Trimmed query after the 300 ms debounce; blank when it's shorter than 2 characters. */
    private val activeQuery: StateFlow<String> = snapshotFlow { query.trim() }
        .debounce { if (it.length < MIN_QUERY_LENGTH) 0L else SEARCH_DEBOUNCE_MS }
        .map { if (it.length < MIN_QUERY_LENGTH) "" else it }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val results: Flow<PagingData<SearchResult>> = combine(activeQuery, filter, profile.filterNotNull()) { q, f, p -> Triple(q, f, p) }
        .distinctUntilChanged()
        .flatMapLatest { (q, f, p) -> if (q.isEmpty()) flowOf(PagingData.empty()) else search.search(q, f, p) }
        .cachedIn(viewModelScope)

    private val recent: Flow<List<String>> = profile.filterNotNull()
        .flatMapLatest { search.recentSearches(it.id) }
        .onStart { emit(emptyList()) }

    private val keys: Flow<Pair<Set<TitleRef>, Set<TitleRef>>> = profile.filterNotNull()
        .flatMapLatest { p -> combine(library.myListKeys(p.id), library.likedKeys(p.id)) { a, b -> a to b } }
        .onStart { emit(emptySet<TitleRef>() to emptySet()) }

    val state: StateFlow<SearchUiState> = combine(
        combine(profile, filter, activeQuery) { p, f, q -> Triple(p, f, q) },
        recent,
        popular,
        keys,
        network.status.onStart { emit(network.current()) },
    ) { (p, f, q), rec, pop, (myList, liked), net ->
        SearchUiState(
            profile = p,
            filter = f,
            activeQuery = q,
            recent = rec,
            popular = pop,
            myList = myList,
            liked = liked,
            offline = !net.online,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    init {
        // "Popular searches" = the first page of Trending, reloaded when the profile changes (SR-3).
        viewModelScope.launch {
            profile.filterNotNull().map { it.id to it.language }.distinctUntilChanged().collect { loadPopular() }
        }
    }

    fun onQueryChange(value: String) {
        query = value
    }

    fun clearQuery() {
        query = ""
    }

    fun selectFilter(value: SearchFilter) {
        filter.value = value
    }

    /** Tapping a recent search fills the box and searches immediately. */
    fun useRecent(value: String) {
        query = value
    }

    /** SR-3: record the query when the user opens one of its results. */
    fun onResultOpened() {
        val p = profile.value ?: return
        val q = query.trim().ifEmpty { activeQuery.value }
        if (q.length < MIN_QUERY_LENGTH) return
        viewModelScope.launch { runCatching { search.recordSearch(p.id, q) } }
    }

    fun removeRecent(value: String) {
        val p = profile.value ?: return
        viewModelScope.launch { search.removeRecent(p.id, value) }
    }

    fun clearRecent() {
        val p = profile.value ?: return
        viewModelScope.launch { search.clearRecent(p.id) }
    }

    fun retryPopular() {
        viewModelScope.launch { loadPopular() }
    }

    private suspend fun loadPopular() {
        val p = profile.value ?: return
        popular.value = popular.value.copy(loading = true, failed = false)
        popular.value = runCatching { catalog.loadRailPage(tmdbRails(HomeTab.ALL).first(), 1, p).items }
            .fold(
                onSuccess = { PopularSearches(it.take(POPULAR_COUNT), loading = false, failed = false) },
                onFailure = { PopularSearches(popular.value.items, loading = false, failed = true) },
            )
    }

    private companion object {
        const val POPULAR_COUNT = 18
    }
}
