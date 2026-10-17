package com.nexient.jazzy.feature.home.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.nexient.jazzy.core.data.catalog.RailStyle
import com.nexient.jazzy.core.designsystem.component.BackdropProgressCard
import com.nexient.jazzy.core.designsystem.component.PosterCard
import com.nexient.jazzy.core.designsystem.component.Rail
import com.nexient.jazzy.core.designsystem.component.RailError
import com.nexient.jazzy.core.designsystem.component.RailSkeleton
import com.nexient.jazzy.core.designsystem.component.SeeMoreCard
import com.nexient.jazzy.core.designsystem.component.TopTenCard
import com.nexient.jazzy.core.designsystem.text.asString
import com.nexient.jazzy.core.model.ContinueWatchingItem
import com.nexient.jazzy.core.model.TitleRef
import com.nexient.jazzy.core.model.TitleSummary
import com.nexient.jazzy.feature.home.HomeActions
import com.nexient.jazzy.feature.home.HomeRail
import com.nexient.jazzy.feature.home.HomeUiState
import com.nexient.jazzy.feature.home.HomeViewModel
import com.nexient.jazzy.feature.home.R
import com.nexient.jazzy.feature.home.restoreKey
import com.nexient.jazzy.feature.home.snapshot

/** A card's restore id: the rail key plus the title, so the same title in two rails stays distinct. */
private fun cardKey(railKey: String, ref: TitleRef): String = "$railKey|${ref.restoreKey()}"

/**
 * TV focus restoration: when the user comes Back from a title, the card it was opened from re-claims
 * focus (and pulls the list back to it) instead of the nav drawer. A no-op on phone, where
 * [HomeViewModel.pendingRestoreKey] is never set.
 */
@Composable
private fun restoreFocusModifier(viewModel: HomeViewModel, key: String): Modifier {
    val requester = remember { FocusRequester() }
    if (viewModel.pendingRestoreKey == key) {
        LaunchedEffect(key) {
            // Retry: on return the card may not be laid out yet, and the drawer can grab focus first
            // (more likely on a real device than the emulator), so keep claiming it until it sticks.
            repeat(12) {
                runCatching { requester.requestFocus() }
                kotlinx.coroutines.delay(40)
            }
            viewModel.clearRestore()
        }
        return Modifier.focusRequester(requester)
    }
    return Modifier
}

/** Renders one Home rail; shared by phone and TV layouts. */
@Composable
internal fun HomeRailContent(
    rail: HomeRail,
    state: HomeUiState,
    viewModel: HomeViewModel,
    actions: HomeActions,
    onFocusTitle: ((TitleSummary) -> Unit)? = null,
) {
    when (rail) {
        is HomeRail.ContinueWatching -> Rail(rail.title.asString()) {
            itemsIndexed(rail.items, key = { _, it -> "${it.snapshot.tmdbId}:${it.snapshot.mediaType}" }) { _, item ->
                ContinueWatchingCard(rail.key, item, viewModel, actions)
            }
        }
        is HomeRail.Fixed -> Rail(rail.title.asString()) {
            itemsIndexed(rail.items, key = { _, it -> "${it.tmdbId}:${it.mediaType.apiValue}" }) { _, item ->
                TitlePoster(rail.key, item, state, viewModel, actions, onFocusTitle)
            }
        }
        is HomeRail.Library -> Rail(rail.title.asString()) {
            itemsIndexed(rail.items, key = { _, it -> "${it.snapshot.tmdbId}:${it.snapshot.mediaType.apiValue}" }) { _, item ->
                val s = item.snapshot
                val key = cardKey(rail.key, s.ref)
                PosterCard(
                    title = s.title,
                    posterPath = s.posterPath,
                    onClick = { viewModel.rememberForRestore(key); actions.openTitle(s.ref, null, null, false) },
                    modifier = restoreFocusModifier(viewModel, key),
                    inMyList = s.ref in state.myList,
                    liked = s.ref in state.liked,
                )
            }
        }
        is HomeRail.Paged -> PagedRail(rail, state, viewModel, actions, onFocusTitle)
    }
}

@Composable
private fun PagedRail(
    rail: HomeRail.Paged,
    state: HomeUiState,
    viewModel: HomeViewModel,
    actions: HomeActions,
    onFocusTitle: ((TitleSummary) -> Unit)?,
) {
    val items = rail.items.collectAsLazyPagingItems()
    val refresh = items.loadState.refresh
    when {
        items.itemCount == 0 && refresh is LoadState.Loading -> RailSkeleton()
        items.itemCount == 0 && refresh is LoadState.Error -> RailError(rail.title.asString(), onRetry = items::retry)
        items.itemCount == 0 -> Unit // empty rails are hidden (HM-1)
        else -> Rail(
            rail.title.asString(),
            onTitleClick = if (rail.definition.fullPage) ({ actions.openNetwork(rail.key) }) else null,
        ) {
            items(items.itemCount, key = items.itemKey { "${it.tmdbId}:${it.mediaType.apiValue}" }) { index ->
                val item = items[index] ?: return@items
                if (rail.style == RailStyle.TOP_TEN) {
                    val key = cardKey(rail.key, item.ref)
                    TopTenCard(index + 1, item.title, item.posterPath,
                        onClick = { viewModel.rememberForRestore(key); actions.openTitle(item.ref, null, null, false) },
                        modifier = restoreFocusModifier(viewModel, key),
                        onFocused = { if (it) onFocusTitle?.invoke(item) })
                } else {
                    TitlePoster(rail.key, item, state, viewModel, actions, onFocusTitle)
                }
            }
            // Once the rail's pages run out, a last card leads to the full page.
            if (rail.definition.fullPage && items.loadState.append.endOfPaginationReached) {
                item(key = "see_more") { SeeMoreCard(onClick = { actions.openNetwork(rail.key) }) }
            }
        }
    }
}

@Composable
private fun TitlePoster(
    railKey: String,
    item: TitleSummary,
    state: HomeUiState,
    viewModel: HomeViewModel,
    actions: HomeActions,
    onFocusTitle: ((TitleSummary) -> Unit)?,
) {
    var menu by remember { mutableStateOf(false) }
    val key = cardKey(railKey, item.ref)
    Column {
        PosterCard(
            title = item.title,
            posterPath = item.posterPath,
            onClick = { viewModel.rememberForRestore(key); actions.openTitle(item.ref, null, null, false) },
            modifier = restoreFocusModifier(viewModel, key),
            inMyList = item.ref in state.myList,
            liked = item.ref in state.liked,
            onLongClick = { menu = true },
            onFocused = { if (it) onFocusTitle?.invoke(item) },
        )
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(if (item.ref in state.myList) R.string.home_menu_remove_from_my_list else R.string.home_menu_add_to_my_list)) },
                onClick = { menu = false; viewModel.toggleMyList(item.snapshot()) },
            )
            DropdownMenuItem(
                text = { Text(stringResource(if (item.ref in state.liked) R.string.home_menu_unlike else R.string.home_menu_like)) },
                onClick = { menu = false; viewModel.toggleLike(item.snapshot()) },
            )
        }
    }
}

@Composable
private fun ContinueWatchingCard(railKey: String, item: ContinueWatchingItem, viewModel: HomeViewModel, actions: HomeActions) {
    var menu by remember { mutableStateOf(false) }
    val p = item.progress.playable
    val key = cardKey(railKey, item.snapshot.ref)
    Column {
        BackdropProgressCard(
            title = item.snapshot.title,
            subtitle = p.label?.let { label -> listOfNotNull(label, item.episodeTitle).joinToString(" · ") },
            backdropPath = item.snapshot.backdropPath,
            progress = item.progress.fraction,
            onClick = { viewModel.rememberForRestore(key); actions.openTitle(item.snapshot.ref, p.season.takeIf { p.isEpisode }, p.episode.takeIf { p.isEpisode }, true) },
            modifier = restoreFocusModifier(viewModel, key),
            onMenu = { menu = true },
        )
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.home_menu_resume)) }, onClick = {
                menu = false
                actions.openTitle(item.snapshot.ref, p.season.takeIf { p.isEpisode }, p.episode.takeIf { p.isEpisode }, true)
            })
            DropdownMenuItem(text = { Text(stringResource(R.string.home_menu_episodes_info)) }, onClick = { menu = false; actions.openTitle(item.snapshot.ref, null, null, false) })
            DropdownMenuItem(text = { Text(stringResource(R.string.home_menu_like)) }, onClick = { menu = false; viewModel.toggleLike(item.snapshot) })
            DropdownMenuItem(text = { Text(stringResource(R.string.home_menu_mark_watched)) }, onClick = { menu = false; viewModel.markWatched(item) })
            DropdownMenuItem(text = { Text(stringResource(R.string.home_menu_remove_from_row)) }, onClick = { menu = false; viewModel.removeFromContinueWatching(item.snapshot.ref) })
        }
    }
    Spacer(Modifier.height(0.dp))
}
