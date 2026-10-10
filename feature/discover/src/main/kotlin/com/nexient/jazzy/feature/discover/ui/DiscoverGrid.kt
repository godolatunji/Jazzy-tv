package com.nexient.jazzy.feature.discover.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.nexient.jazzy.core.designsystem.component.MessageState
import com.nexient.jazzy.core.designsystem.component.PosterCard
import com.nexient.jazzy.core.designsystem.component.pagingFooter
import com.nexient.jazzy.core.designsystem.component.posterSkeletonItems
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.core.model.TitleSummary
import com.nexient.jazzy.feature.discover.DiscoverUiState

/** Infinite results grid with loading, empty and error states (spec §5.5). */
@Composable
internal fun DiscoverResultsGrid(
    items: LazyPagingItems<TitleSummary>,
    state: DiscoverUiState,
    onOpenTitle: (TitleSummary) -> Unit,
    onReset: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState(),
) {
    val refresh = items.loadState.refresh
    when {
        items.itemCount == 0 && refresh is LoadState.Error -> MessageState(
            title = if (state.offline) "You're offline" else "Couldn't load titles",
            message = if (state.offline) "Connect to the internet to discover titles." else "Check your connection and try again.",
            icon = Icons.Filled.WifiOff,
            actionLabel = "Retry",
            onAction = items::retry,
            modifier = modifier.padding(contentPadding),
        )
        items.itemCount == 0 && refresh is LoadState.NotLoading -> MessageState(
            title = "Nothing matches these filters",
            message = "Try fewer genres, a wider year range or a lower rating.",
            icon = Icons.Filled.FilterAltOff,
            actionLabel = if (state.filters.activeCount > 0) "Reset filters" else null,
            onAction = onReset,
            modifier = modifier.padding(contentPadding),
        )
        else -> {
            val gap = if (JazzyTheme.isTv) 20.dp else 8.dp
            LazyVerticalGrid(
                columns = GridCells.Adaptive(JazzyTheme.spacing.posterWidth),
                state = gridState,
                modifier = modifier.fillMaxSize(),
                contentPadding = contentPadding,
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalArrangement = Arrangement.spacedBy(gap),
            ) {
                if (items.itemCount == 0) {
                    posterSkeletonItems(18)
                    return@LazyVerticalGrid
                }
                // No keys: discover pages can repeat a title as popularity shifts, and duplicate keys crash a lazy grid.
                items(items.itemCount, contentType = { "title" }) { index ->
                    val item = items[index]
                    if (item == null) {
                        Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f))
                    } else {
                        PosterCard(
                            title = item.title,
                            posterPath = item.posterPath,
                            onClick = { onOpenTitle(item) },
                            modifier = Modifier.fillMaxWidth(),
                            inMyList = item.ref in state.myList,
                            liked = item.ref in state.liked,
                        )
                    }
                }
                pagingFooter(items)
            }
        }
    }
}
