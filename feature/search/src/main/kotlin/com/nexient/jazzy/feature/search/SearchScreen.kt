package com.nexient.jazzy.feature.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.core.model.TitleRef
import com.nexient.jazzy.feature.search.ui.PersonScreen
import com.nexient.jazzy.feature.search.ui.SearchCallbacks
import com.nexient.jazzy.feature.search.ui.SearchPhoneScreen
import com.nexient.jazzy.feature.search.ui.SearchTvScreen
import com.nexient.jazzy.feature.search.ui.rememberVoiceSearch

/**
 * Search (spec §5.4): one box over `/search/multi` with filter chips, recent and popular searches,
 * and voice input. Opening a result records the query in the profile's recent searches.
 */
@Composable
fun SearchRoute(
    onOpenTitle: (TitleRef) -> Unit,
    onOpenPerson: (personId: Int, name: String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val results = viewModel.results.collectAsLazyPagingItems()
    val voice = rememberVoiceSearch(viewModel::onQueryChange)
    val callbacks = SearchCallbacks(
        onQueryChange = viewModel::onQueryChange,
        onClear = viewModel::clearQuery,
        onFilter = viewModel::selectFilter,
        onOpenTitle = { viewModel.onResultOpened(); onOpenTitle(it.ref) },
        onOpenPerson = { viewModel.onResultOpened(); onOpenPerson(it.id, it.name) },
        onOpenPopular = { onOpenTitle(it.ref) },
        onRecent = viewModel::useRecent,
        onRemoveRecent = viewModel::removeRecent,
        onClearRecent = viewModel::clearRecent,
        onRetryPopular = viewModel::retryPopular,
        onVoice = voice,
    )
    if (JazzyTheme.isTv) {
        SearchTvScreen(viewModel.query, state, results, callbacks)
    } else {
        SearchPhoneScreen(viewModel.query, state, results, callbacks)
    }
}

/** A person's filmography grid (SR-1), opened from a person search result. */
@Composable
fun PersonRoute(
    personId: Int,
    name: String,
    onOpenTitle: (TitleRef) -> Unit,
    onBack: () -> Unit,
    viewModel: PersonViewModel = hiltViewModel(key = "person:$personId"),
) {
    LaunchedEffect(personId) { viewModel.load(personId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    PersonScreen(name, state, onOpenTitle = { onOpenTitle(it.ref) }, onRetry = viewModel::retry, onBack = onBack)
}
