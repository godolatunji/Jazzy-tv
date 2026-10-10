package com.nexient.jazzy.feature.search.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.nexient.jazzy.core.designsystem.component.OfflineBanner
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.core.model.SearchResult
import com.nexient.jazzy.feature.search.SearchUiState

/**
 * 10-foot Search: the box (system IME, opened with OK) and a voice button on top, filter chips,
 * then a D-pad navigable grid. Initial focus is on the search box (spec §5.10).
 */
@Composable
internal fun SearchTvScreen(
    query: String,
    state: SearchUiState,
    results: LazyPagingItems<SearchResult>,
    callbacks: SearchCallbacks,
) {
    val spacing = JazzyTheme.spacing
    val fieldFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { fieldFocus.requestFocus() } }

    Column(Modifier.fillMaxSize().padding(top = spacing.verticalSafe)) {
        Row(
            Modifier.padding(horizontal = spacing.gutter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SearchField(
                query = query,
                callbacks = callbacks,
                modifier = Modifier.weight(1f, fill = false).widthIn(min = 360.dp, max = 720.dp),
                focusRequester = fieldFocus,
                showVoiceInside = false,
            )
            callbacks.onVoice?.let { VoiceButton(it) }
        }
        if (!isIdle(query)) {
            SearchFilterRow(state.filter, callbacks.onFilter, Modifier.padding(horizontal = spacing.gutter).padding(top = 16.dp))
        }
        if (state.offline) OfflineBanner(Modifier.padding(top = 8.dp))
        SearchBody(
            query = query,
            state = state,
            results = results,
            callbacks = callbacks,
            contentPadding = PaddingValues(start = spacing.gutter, end = spacing.gutter, top = 20.dp, bottom = spacing.verticalSafe + 24.dp),
        )
    }
}
