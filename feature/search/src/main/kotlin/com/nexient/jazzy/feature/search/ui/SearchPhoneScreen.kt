package com.nexient.jazzy.feature.search.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.nexient.jazzy.core.designsystem.component.OfflineBanner
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.core.model.SearchResult
import com.nexient.jazzy.feature.search.SearchUiState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/** Phone: the system keyboard plus a mic button inside the search box (SR-2). */
@Composable
internal fun SearchPhoneScreen(
    query: String,
    state: SearchUiState,
    results: LazyPagingItems<SearchResult>,
    callbacks: SearchCallbacks,
) {
    val gutter = JazzyTheme.spacing.gutter
    val gridState = rememberLazyGridState()
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Scrolling the results dismisses the keyboard so the grid is fully visible.
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.isScrollInProgress }.distinctUntilChanged().filter { it }.collect {
            keyboard?.hide()
            focusManager.clearFocus()
        }
    }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
        SearchField(
            query = query,
            callbacks = callbacks,
            modifier = Modifier.fillMaxWidth().padding(horizontal = gutter, vertical = 8.dp),
        )
        if (!isIdle(query)) {
            SearchFilterRow(state.filter, callbacks.onFilter, Modifier.padding(horizontal = gutter).padding(bottom = 8.dp))
        }
        if (state.offline) OfflineBanner()
        SearchBody(
            query = query,
            state = state,
            results = results,
            callbacks = callbacks,
            contentPadding = PaddingValues(start = gutter, end = gutter, top = 8.dp, bottom = 24.dp),
            gridState = gridState,
        )
    }
}
