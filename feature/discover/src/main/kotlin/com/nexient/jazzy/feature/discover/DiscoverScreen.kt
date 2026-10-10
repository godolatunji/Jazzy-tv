package com.nexient.jazzy.feature.discover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.core.model.TitleRef
import com.nexient.jazzy.feature.discover.ui.DiscoverCallbacks
import com.nexient.jazzy.feature.discover.ui.DiscoverPhoneScreen
import com.nexient.jazzy.feature.discover.ui.DiscoverTvScreen

/** Discover (spec §5.5): TMDB `/discover/{movie|tv}` with filters, sort and an infinite grid. */
@Composable
fun DiscoverRoute(
    onOpenTitle: (TitleRef) -> Unit,
    viewModel: DiscoverViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val items = viewModel.results.collectAsLazyPagingItems()
    val callbacks = remember(viewModel) {
        DiscoverCallbacks(
            onMediaType = viewModel::setMediaType,
            onToggleGenre = viewModel::toggleGenre,
            onClearGenres = viewModel::clearGenres,
            onYears = viewModel::setYears,
            onMinRating = viewModel::setMinRating,
            onMinVotes = viewModel::setMinVotes,
            onLanguage = viewModel::setLanguage,
            onRuntime = viewModel::setRuntime,
            onSort = viewModel::setSort,
            onReset = viewModel::resetFilters,
            onRetryGenres = viewModel::retryGenres,
            currentYear = viewModel.currentYear(),
        )
    }
    if (JazzyTheme.isTv) {
        DiscoverTvScreen(state, items, callbacks, onOpenTitle = { onOpenTitle(it.ref) })
    } else {
        DiscoverPhoneScreen(state, items, callbacks, onOpenTitle = { onOpenTitle(it.ref) })
    }
}
