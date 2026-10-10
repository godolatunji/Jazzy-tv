package com.nexient.jazzy.feature.discover.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.nexient.jazzy.core.designsystem.component.JazzyChip
import com.nexient.jazzy.core.designsystem.component.OfflineBanner
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.core.model.TitleSummary
import com.nexient.jazzy.feature.discover.DiscoverUiState

/** Phone: type + sort + "Filters" chips over the grid; the full filter set lives in a bottom sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DiscoverPhoneScreen(
    state: DiscoverUiState,
    items: LazyPagingItems<TitleSummary>,
    callbacks: DiscoverCallbacks,
    onOpenTitle: (TitleSummary) -> Unit,
) {
    val gutter = JazzyTheme.spacing.gutter
    var showFilters by rememberSaveable { mutableStateOf(false) }
    val gridState = rememberLazyGridState()

    ScrollToTopOnFilterChange(state, gridState)

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
        Text(
            "Discover",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = gutter, vertical = 12.dp).semantics { heading() },
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = gutter),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MediaTypeChips(state.filters.mediaType, callbacks.onMediaType)
            SortDropdownChip(state.filters, callbacks.onSort)
            val count = state.filters.activeCount
            JazzyChip(
                if (count > 0) "Filters · $count" else "Filters",
                selected = count > 0,
                onClick = { showFilters = true },
                leadingIcon = Icons.Filled.Tune,
            )
        }
        if (state.restricted) RestrictedNote(Modifier.padding(horizontal = gutter, vertical = 4.dp))
        if (state.offline) OfflineBanner(Modifier.padding(top = 8.dp))
        DiscoverResultsGrid(
            items = items,
            state = state,
            onOpenTitle = onOpenTitle,
            onReset = callbacks.onReset,
            contentPadding = PaddingValues(start = gutter, end = gutter, top = 12.dp, bottom = 24.dp),
            gridState = gridState,
        )
    }

    if (showFilters) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { showFilters = false }, sheetState = sheetState, containerColor = JazzyColors.SurfaceRaised) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = gutter), verticalAlignment = Alignment.CenterVertically) {
                    Text("Filters", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).semantics { heading() })
                    if (state.filters.activeCount > 0) TextButton(onClick = callbacks.onReset) { Text("Reset") }
                }
                FilterPanel(
                    state = state,
                    callbacks = callbacks,
                    includeTypeAndSort = false,
                    modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = gutter, vertical = 12.dp),
                )
                Button(
                    onClick = { showFilters = false },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = gutter, vertical = 12.dp),
                ) { Text("Show results") }
            }
        }
    }
}

/** A new filter set starts at the top of the results; returning to the screen keeps the position. */
@Composable
internal fun ScrollToTopOnFilterChange(state: DiscoverUiState, gridState: LazyGridState) {
    var applied by remember { mutableStateOf(state.filters) }
    LaunchedEffect(state.filters) {
        if (state.filters != applied) {
            applied = state.filters
            gridState.scrollToItem(0)
        }
    }
}

@Composable
internal fun RestrictedNote(modifier: Modifier = Modifier) {
    Text(
        "Results follow this profile's maturity setting.",
        style = MaterialTheme.typography.bodySmall,
        color = JazzyColors.TextTertiary,
        modifier = modifier,
    )
}
