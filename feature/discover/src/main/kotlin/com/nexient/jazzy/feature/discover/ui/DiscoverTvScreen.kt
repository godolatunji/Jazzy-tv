package com.nexient.jazzy.feature.discover.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.nexient.jazzy.core.designsystem.component.OfflineBanner
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.core.model.TitleSummary
import com.nexient.jazzy.feature.discover.DiscoverUiState
import com.nexient.jazzy.feature.discover.R

/**
 * 10-foot Discover: a scrollable filter column on the left (D-pad Right moves into the grid) and
 * the results grid on the right. Initial focus is on the first filter (the media type).
 */
@Composable
internal fun DiscoverTvScreen(
    state: DiscoverUiState,
    items: LazyPagingItems<TitleSummary>,
    callbacks: DiscoverCallbacks,
    onOpenTitle: (TitleSummary) -> Unit,
) {
    val spacing = JazzyTheme.spacing
    val gridState = rememberLazyGridState()
    val filtersFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { filtersFocus.requestFocus() } }
    ScrollToTopOnFilterChange(state, gridState)

    Row(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .width(380.dp)
                .fillMaxHeight()
                .background(JazzyColors.Surface.copy(alpha = 0.6f))
                .focusRequester(filtersFocus)
                .focusGroup()
                .verticalScroll(rememberScrollState())
                .padding(start = spacing.gutter, end = 24.dp, top = spacing.verticalSafe, bottom = spacing.verticalSafe),
        ) {
            Text(stringResource(R.string.discover_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 16.dp).semantics { heading() })
            if (state.restricted) RestrictedNote(Modifier.padding(bottom = 12.dp))
            FilterPanel(state = state, callbacks = callbacks, includeTypeAndSort = true)
        }
        Column(Modifier.weight(1f).fillMaxHeight()) {
            if (state.offline) OfflineBanner(Modifier.padding(top = spacing.verticalSafe))
            DiscoverResultsGrid(
                items = items,
                state = state,
                onOpenTitle = onOpenTitle,
                onReset = callbacks.onReset,
                contentPadding = PaddingValues(start = 24.dp, end = spacing.gutter, top = spacing.verticalSafe, bottom = spacing.verticalSafe + 24.dp),
                gridState = gridState,
            )
        }
    }
}
