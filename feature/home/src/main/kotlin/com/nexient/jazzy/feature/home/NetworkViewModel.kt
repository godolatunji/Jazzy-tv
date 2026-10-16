package com.nexient.jazzy.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.nexient.jazzy.core.data.NetworkMonitor
import com.nexient.jazzy.core.data.catalog.CatalogRepository
import com.nexient.jazzy.core.data.catalog.fullPageRail
import com.nexient.jazzy.core.data.library.LibraryRepository
import com.nexient.jazzy.core.data.profile.ProfileRepository
import com.nexient.jazzy.core.designsystem.text.UiText
import com.nexient.jazzy.core.model.TitleRef
import com.nexient.jazzy.core.model.TitleSummary
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

data class NetworkUiState(
    val title: UiText,
    val myList: Set<TitleRef> = emptySet(),
    val liked: Set<TitleRef> = emptySet(),
    val offline: Boolean = false,
)

/**
 * A network's page (e.g. Netflix Originals): the Home rail's query, paged until TMDB runs out, so
 * scrolling down keeps loading more titles.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = NetworkViewModel.Factory::class)
class NetworkViewModel @AssistedInject constructor(
    @Assisted railKey: String,
    profiles: ProfileRepository,
    catalog: CatalogRepository,
    library: LibraryRepository,
    network: NetworkMonitor,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(railKey: String): NetworkViewModel
    }

    private val rail = fullPageRail(railKey)
    private val title: UiText = rail?.uiTitle() ?: UiText.Res(R.string.home_network_fallback_title)

    // Only what changes the query restarts paging; other profile edits keep the scroll position.
    private val profile = profiles.activeProfile.filterNotNull().distinctUntilChangedBy { Triple(it.id, it.maturity, it.language) }

    val titles: Flow<PagingData<TitleSummary>> = profile
        .flatMapLatest { p -> rail?.let { catalog.pager(it, p) } ?: flowOf(PagingData.empty()) }
        .cachedIn(viewModelScope)

    private val keys: Flow<Pair<Set<TitleRef>, Set<TitleRef>>> = profile
        .flatMapLatest { p -> combine(library.myListKeys(p.id), library.likedKeys(p.id)) { a, b -> a to b } }
        .onStart { emit(emptySet<TitleRef>() to emptySet()) }

    val state: StateFlow<NetworkUiState> = combine(keys, network.status) { (myList, liked), net ->
        NetworkUiState(title, myList, liked, offline = !net.online)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NetworkUiState(title))
}
