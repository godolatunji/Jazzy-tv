package com.nexient.jazzy.feature.library.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nexient.jazzy.core.designsystem.component.JazzyChip
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.feature.library.LibraryNav
import com.nexient.jazzy.feature.library.LibraryTab
import com.nexient.jazzy.feature.library.LibraryUiState
import com.nexient.jazzy.feature.library.LibraryViewModel

/**
 * 10-foot Library: tab chips beside the title (initial focus on the selected tab), then the grid
 * or history list. Card and entry menus open with long-press OK or the remote's Menu key.
 */
@Composable
internal fun LibraryTvScreen(state: LibraryUiState, viewModel: LibraryViewModel, nav: LibraryNav) {
    val spacing = JazzyTheme.spacing
    val tabFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { tabFocus.requestFocus() } }

    Column(Modifier.fillMaxSize().padding(top = spacing.verticalSafe)) {
        Row(
            Modifier.padding(horizontal = spacing.gutter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Library",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(end = 24.dp).semantics { heading() },
            )
            LibraryTab.entries.forEachIndexed { index, tab ->
                JazzyChip(
                    tab.label,
                    selected = index == nav.tab,
                    onClick = { nav.onTab(index) },
                    modifier = if (index == nav.tab) Modifier.focusRequester(tabFocus) else Modifier,
                )
            }
        }
        LibraryTabContent(
            state, viewModel, nav,
            PaddingValues(start = spacing.gutter, end = spacing.gutter, top = 20.dp, bottom = spacing.verticalSafe + 24.dp),
        )
    }
}
