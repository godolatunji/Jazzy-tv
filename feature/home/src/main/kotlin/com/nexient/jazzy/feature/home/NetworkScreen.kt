package com.nexient.jazzy.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.nexient.jazzy.core.designsystem.component.MessageState
import com.nexient.jazzy.core.designsystem.component.OfflineBanner
import com.nexient.jazzy.core.designsystem.component.PosterCard
import com.nexient.jazzy.core.designsystem.component.pagingFooter
import com.nexient.jazzy.core.designsystem.component.posterSkeletonItems
import com.nexient.jazzy.core.designsystem.text.asString
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.core.model.TitleRef
import com.nexient.jazzy.core.model.TitleSummary

/** A network's page, opened from its Home rail title or the rail's "See more" card. */
@Composable
fun NetworkRoute(railKey: String, onOpenTitle: (TitleRef) -> Unit, onBack: () -> Unit) {
    val viewModel = hiltViewModel<NetworkViewModel, NetworkViewModel.Factory>(
        key = "network_$railKey",
        creationCallback = { it.create(railKey) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val titles = viewModel.titles.collectAsLazyPagingItems()
    NetworkScreen(state, titles, onOpenTitle = { onOpenTitle(it.ref) }, onBack = onBack)
}

/** Infinite poster grid: the next page loads as the user nears the bottom. Phone gets a back button; TV uses Back. */
@Composable
internal fun NetworkScreen(
    state: NetworkUiState,
    titles: LazyPagingItems<TitleSummary>,
    onOpenTitle: (TitleSummary) -> Unit,
    onBack: () -> Unit,
) {
    val tv = JazzyTheme.isTv
    val spacing = JazzyTheme.spacing
    val firstItem = remember { FocusRequester() }
    val hasItems = titles.itemCount > 0
    if (tv) LaunchedEffect(hasItems) { if (hasItems) runCatching { firstItem.requestFocus() } }

    Column(
        Modifier
            .fillMaxSize()
            .then(if (tv) Modifier.padding(top = spacing.verticalSafe) else Modifier.windowInsetsPadding(WindowInsets.statusBars)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = if (tv) spacing.gutter else 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!tv) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.home_network_back)) }
            Text(
                state.title.asString(),
                style = if (tv) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
        }
        if (state.offline) OfflineBanner()

        val refresh = titles.loadState.refresh
        when {
            !hasItems && refresh is LoadState.Error -> MessageState(
                title = stringResource(if (state.offline) R.string.home_network_offline_title else R.string.home_network_error_title),
                message = stringResource(R.string.home_network_error_message),
                icon = Icons.Filled.WifiOff,
                actionLabel = stringResource(R.string.home_network_retry),
                onAction = titles::retry,
            )
            !hasItems && refresh is LoadState.NotLoading -> MessageState(
                title = stringResource(R.string.home_network_empty_title),
                message = stringResource(R.string.home_network_empty_message),
                icon = Icons.Filled.Movie,
            )
            else -> {
                val gap = if (tv) 20.dp else 8.dp
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(spacing.posterWidth),
                    state = rememberLazyGridState(),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = spacing.gutter, end = spacing.gutter, top = 8.dp, bottom = spacing.verticalSafe + 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalArrangement = Arrangement.spacedBy(gap),
                ) {
                    if (!hasItems) {
                        posterSkeletonItems(18)
                        return@LazyVerticalGrid
                    }
                    // No keys: discover pages can repeat a title as popularity shifts, and duplicate keys crash a lazy grid.
                    items(titles.itemCount, contentType = { "title" }) { index ->
                        val item = titles[index]
                        if (item == null) {
                            Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f))
                        } else {
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
                    pagingFooter(titles)
                }
            }
        }
    }
}
