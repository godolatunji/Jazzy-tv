package com.nexient.jazzy.feature.search.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexient.jazzy.core.designsystem.component.MessageState
import com.nexient.jazzy.core.designsystem.component.PosterCard
import com.nexient.jazzy.core.designsystem.component.posterSkeletonItems
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.core.model.TitleSummary
import com.nexient.jazzy.feature.search.PersonUiState

/** Filmography grid for a person; phone gets a back button, TV relies on the remote's Back. */
@Composable
internal fun PersonScreen(
    name: String,
    state: PersonUiState,
    onOpenTitle: (TitleSummary) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    val tv = JazzyTheme.isTv
    val spacing = JazzyTheme.spacing
    val firstItem = remember { FocusRequester() }
    if (tv) {
        LaunchedEffect(state.credits.isNotEmpty()) { if (state.credits.isNotEmpty()) runCatching { firstItem.requestFocus() } }
    }

    Column(
        Modifier
            .fillMaxSize()
            .then(if (tv) Modifier.padding(top = spacing.verticalSafe) else Modifier.windowInsetsPadding(WindowInsets.statusBars)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = if (tv) spacing.gutter else 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!tv) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    style = if (tv) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
                if (state.credits.isNotEmpty()) {
                    Text("${state.credits.size} titles", style = MaterialTheme.typography.bodyMedium, color = JazzyColors.TextSecondary)
                }
            }
        }
        when {
            state.error -> MessageState(
                title = "Couldn't load $name's titles",
                message = "Check your connection and try again.",
                icon = Icons.Filled.WifiOff,
                actionLabel = "Retry",
                onAction = onRetry,
            )
            !state.loading && state.credits.isEmpty() -> MessageState(
                title = "No titles to show",
                message = "There's nothing from $name available on this profile.",
                icon = Icons.Filled.Movie,
            )
            else -> {
                val gap = if (tv) 20.dp else 8.dp
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(spacing.posterWidth),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = spacing.gutter, end = spacing.gutter, top = 8.dp, bottom = spacing.verticalSafe + 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalArrangement = Arrangement.spacedBy(gap),
                ) {
                    if (state.loading) {
                        posterSkeletonItems(18)
                    } else {
                        itemsIndexed(state.credits, key = { _, it -> "${it.tmdbId}:${it.mediaType.apiValue}" }) { index, item ->
                            PosterCard(
                                title = item.title,
                                posterPath = item.posterPath,
                                onClick = { onOpenTitle(item) },
                                modifier = Modifier.fillMaxWidth().then(if (index == 0) Modifier.focusRequester(firstItem) else Modifier),
                                inMyList = item.ref in state.myList,
                                liked = item.ref in state.liked,
                            )
                        }
                    }
                }
            }
        }
    }
}
