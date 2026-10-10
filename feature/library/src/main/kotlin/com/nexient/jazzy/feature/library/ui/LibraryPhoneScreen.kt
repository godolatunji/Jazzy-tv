package com.nexient.jazzy.feature.library.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.feature.library.LibraryNav
import com.nexient.jazzy.feature.library.LibraryTab
import com.nexient.jazzy.feature.library.LibraryUiState
import com.nexient.jazzy.feature.library.LibraryViewModel

/** Phone: a title, three tabs (My List · Liked · History) and the selected tab's content. */
@Composable
internal fun LibraryPhoneScreen(state: LibraryUiState, viewModel: LibraryViewModel, nav: LibraryNav) {
    val gutter = JazzyTheme.spacing.gutter
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
        Text(
            "Library",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = gutter, vertical = 12.dp).semantics { heading() },
        )
        PrimaryTabRow(selectedTabIndex = nav.tab, containerColor = Color.Transparent, contentColor = JazzyColors.OnBackground) {
            LibraryTab.entries.forEachIndexed { index, tab ->
                Tab(
                    selected = index == nav.tab,
                    onClick = { nav.onTab(index) },
                    text = { Text(tab.label) },
                    unselectedContentColor = JazzyColors.TextSecondary,
                )
            }
        }
        LibraryTabContent(state, viewModel, nav, PaddingValues(start = gutter, end = gutter, top = 12.dp, bottom = 24.dp))
    }
}

/** The selected tab's content; shared by phone and TV. */
@Composable
internal fun LibraryTabContent(state: LibraryUiState, viewModel: LibraryViewModel, nav: LibraryNav, contentPadding: PaddingValues) {
    when (LibraryTab.entries[nav.tab]) {
        LibraryTab.MY_LIST -> CollectionGrid(state.myList, state, viewModel, onOpen = { nav.openTitle(it.ref) }, contentPadding = contentPadding)
        LibraryTab.LIKED -> CollectionGrid(state.liked, state, viewModel, onOpen = { nav.openTitle(it.ref) }, contentPadding = contentPadding)
        LibraryTab.HISTORY -> HistoryList(state, viewModel, onOpen = nav.openHistory, contentPadding = contentPadding)
    }
}
