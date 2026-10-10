package com.nexient.jazzy.feature.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.nexient.jazzy.core.common.Clock
import com.nexient.jazzy.core.data.NetworkMonitor
import com.nexient.jazzy.core.data.catalog.CatalogRepository
import com.nexient.jazzy.core.data.catalog.TitleRepository
import com.nexient.jazzy.core.data.library.LibraryRepository
import com.nexient.jazzy.core.data.profile.ProfileRepository
import com.nexient.jazzy.core.model.Genre
import com.nexient.jazzy.core.model.MaturityLevel
import com.nexient.jazzy.core.model.MediaType
import com.nexient.jazzy.core.model.Profile
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class GenreState(val genres: List<Genre> = emptyList(), val loading: Boolean = true, val failed: Boolean = false)

data class DiscoverUiState(
    val profile: Profile? = null,
    val filters: DiscoverFilters = DiscoverFilters(),
    val genres: GenreState = GenreState(),
    /** Restricted (kids) profiles get maturity-limited queries from the repository; some filters are ignored. */
    val restricted: Boolean = false,
    val myList: Set<TitleRef> = emptySet(),
    val liked: Set<TitleRef> = emptySet(),
    val offline: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class DiscoverViewModel @Inject constructor(
    profiles: ProfileRepository,
    catalog: CatalogRepository,
    private val titles: TitleRepository,
    library: LibraryRepository,
    network: NetworkMonitor,
    private val clock: Clock,
) : ViewModel() {
    private val filters = MutableStateFlow(DiscoverFilters())
    private val genreRetry = MutableStateFlow(0)
    private val profile = profiles.activeProfile.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** One pager per filter set; quick chip changes are coalesced so we don't page TMDB for every tap. */
    val results: Flow<PagingData<TitleSummary>> = combine(filters.debounce(FILTER_DEBOUNCE_MS), profile.filterNotNull()) { f, p -> f to p }
        .distinctUntilChanged()
        .flatMapLatest { (f, p) -> catalog.pager(f.toRail(clock.today()), p) }
        .cachedIn(viewModelScope)

    /** Genre list for the current media type in the profile's language (spec §5.5). */
    private val genres: Flow<GenreState> = combine(
        profile.filterNotNull().map { it.language }.distinctUntilChanged(),
        filters.map { it.mediaType }.distinctUntilChanged(),
        genreRetry,
    ) { language, type, _ -> language to type }
        .flatMapLatest { (language, type) -> genreFlow(type, language) }
        .onStart { emit(GenreState()) }

    private val keys: Flow<Pair<Set<TitleRef>, Set<TitleRef>>> = profile.filterNotNull()
        .flatMapLatest { p -> combine(library.myListKeys(p.id), library.likedKeys(p.id)) { a, b -> a to b } }
        .onStart { emit(emptySet<TitleRef>() to emptySet()) }

    val state: StateFlow<DiscoverUiState> = combine(
        profile,
        filters,
        genres,
        keys,
        network.status.onStart { emit(network.current()) },
    ) { p, f, g, (myList, liked), net ->
        DiscoverUiState(
            profile = p,
            filters = f,
            genres = g,
            restricted = p != null && p.maturity != MaturityLevel.ALL,
            myList = myList,
            liked = liked,
            offline = !net.online,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiscoverUiState())

    private fun genreFlow(type: MediaType, language: String): Flow<GenreState> = flow {
        emit(GenreState(loading = true))
        val result = runCatching { titles.genres(type, language) }
        result.onSuccess { list ->
            // Genre IDs differ between movies and TV; drop selections the new list doesn't have.
            val valid = list.map { it.id }.toSet()
            filters.update { f -> if (f.mediaType == type) f.copy(genreIds = f.genreIds intersect valid) else f }
            emit(GenreState(list.sortedBy { it.name }, loading = false))
        }.onFailure { emit(GenreState(loading = false, failed = true)) }
    }

    fun setMediaType(type: MediaType) = filters.update { it.withMediaType(type) }

    fun toggleGenre(id: Int) = filters.update { f ->
        f.copy(genreIds = if (id in f.genreIds) f.genreIds - id else f.genreIds + id)
    }

    fun clearGenres() = filters.update { it.copy(genreIds = emptySet()) }

    fun setYears(from: Int?, to: Int?) = filters.update { it.withYears(from, to) }

    fun setMinRating(value: Int?) = filters.update { it.copy(minRating = value) }

    fun setMinVotes(value: Int) = filters.update { it.copy(minVotes = value) }

    fun setLanguage(code: String?) = filters.update { it.copy(language = code) }

    fun setRuntime(range: RuntimeRange?) = filters.update {
        if (range == null || it.hasRuntime(range)) it.copy(runtimeMin = null, runtimeMax = null)
        else it.copy(runtimeMin = range.min, runtimeMax = range.max)
    }

    fun setSort(sort: DiscoverSort) = filters.update { if (sort.supports(it.mediaType)) it.copy(sort = sort) else it }

    /** Clears every filter but keeps the media type and sort. */
    fun resetFilters() = filters.update { DiscoverFilters(mediaType = it.mediaType, sort = it.sort) }

    fun retryGenres() = genreRetry.update { it + 1 }

    fun currentYear(): Int = clock.today().year

    private companion object {
        const val FILTER_DEBOUNCE_MS = 250L
    }
}
