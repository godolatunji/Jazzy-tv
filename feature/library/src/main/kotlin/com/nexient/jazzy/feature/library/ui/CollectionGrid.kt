package com.nexient.jazzy.feature.library.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nexient.jazzy.core.designsystem.component.JazzyChip
import com.nexient.jazzy.core.designsystem.component.MessageState
import com.nexient.jazzy.core.designsystem.component.PosterCard
import com.nexient.jazzy.core.designsystem.component.posterSkeletonItems
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.core.model.LibraryItem
import com.nexient.jazzy.core.model.LibrarySort
import com.nexient.jazzy.core.model.TitleSnapshot
import com.nexient.jazzy.feature.library.CollectionKind
import com.nexient.jazzy.feature.library.CollectionState
import com.nexient.jazzy.feature.library.LibraryUiState
import com.nexient.jazzy.feature.library.LibraryViewModel
import com.nexient.jazzy.feature.library.R
import com.nexient.jazzy.feature.library.TypeFilter
import com.nexient.jazzy.feature.library.label

/** My List / Liked: filter chips, sort, and a poster grid with a card menu (ML-3/4, LK-3/4). */
@Composable
internal fun CollectionGrid(
    collection: CollectionState,
    state: LibraryUiState,
    viewModel: LibraryViewModel,
    onOpen: (TitleSnapshot) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val kind = collection.kind
    val gap = if (JazzyTheme.isTv) 20.dp else 8.dp
    LazyVerticalGrid(
        columns = GridCells.Adaptive(JazzyTheme.spacing.posterWidth),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        if (collection.total > 0) {
            item(key = "controls", span = { GridItemSpan(maxLineSpan) }, contentType = "controls") {
                Controls(collection, viewModel)
            }
        }
        when {
            collection.loading -> posterSkeletonItems(12)
            collection.total == 0 -> item(key = "empty", span = { GridItemSpan(maxLineSpan) }) { EmptyCollection(kind) }
            collection.items.isEmpty() -> item(key = "no_match", span = { GridItemSpan(maxLineSpan) }) {
                MessageState(
                    title = stringResource(
                        if (collection.options.filter == TypeFilter.MOVIES) R.string.library_collection_no_movies else R.string.library_collection_no_tv,
                    ),
                    message = stringResource(
                        R.string.library_collection_no_match_message,
                        stringResource(if (kind == CollectionKind.MY_LIST) R.string.library_tab_my_list else R.string.library_tab_liked),
                    ),
                    icon = Icons.Filled.FilterAltOff,
                    actionLabel = stringResource(R.string.library_collection_show_all),
                    onAction = { viewModel.setFilter(kind, TypeFilter.ALL) },
                )
            }
            else -> items(collection.items, key = { "${it.snapshot.tmdbId}:${it.snapshot.mediaType.apiValue}" }) { item ->
                LibraryCard(item, kind, state, viewModel, onOpen)
            }
        }
    }
}

@Composable
private fun Controls(collection: CollectionState, viewModel: LibraryViewModel) {
    val kind = collection.kind
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TypeFilter.entries.forEach { f ->
            JazzyChip(stringResource(f.label), collection.options.filter == f, onClick = { viewModel.setFilter(kind, f) })
        }
        var open by remember { mutableStateOf(false) }
        Box {
            JazzyChip(
                stringResource(R.string.library_collection_sort_chip, stringResource(collection.options.sort.label(kind))),
                selected = false,
                onClick = { open = true },
                trailingIcon = Icons.Filled.ArrowDropDown,
            )
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                LibrarySort.entries.forEach { s ->
                    DropdownMenuItem(
                        text = {
                            val label = stringResource(s.label(kind))
                            Text(if (s == collection.options.sort) stringResource(R.string.library_collection_sort_selected, label) else label)
                        },
                        onClick = { open = false; viewModel.setSort(kind, s) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyCollection(kind: CollectionKind) {
    val tv = JazzyTheme.isTv
    when (kind) {
        CollectionKind.MY_LIST -> MessageState(
            title = stringResource(R.string.library_mylist_empty_title),
            message = stringResource(if (tv) R.string.library_mylist_empty_message_tv else R.string.library_mylist_empty_message_phone),
            icon = Icons.Filled.BookmarkBorder,
        )
        CollectionKind.LIKED -> MessageState(
            title = stringResource(R.string.library_liked_empty_title),
            message = stringResource(if (tv) R.string.library_liked_empty_message_tv else R.string.library_liked_empty_message_phone),
            icon = Icons.Filled.FavoriteBorder,
        )
    }
}

/** Poster with a card menu: long-press on phone, long-press or the Menu key on TV. */
@Composable
private fun LibraryCard(
    item: LibraryItem,
    kind: CollectionKind,
    state: LibraryUiState,
    viewModel: LibraryViewModel,
    onOpen: (TitleSnapshot) -> Unit,
) {
    val s = item.snapshot
    var menu by remember { mutableStateOf(false) }
    Box(
        Modifier.onPreviewKeyEvent { event ->
            if (event.key != Key.Menu) return@onPreviewKeyEvent false
            if (event.type == KeyEventType.KeyUp) menu = true
            true
        },
    ) {
        PosterCard(
            title = s.title,
            posterPath = s.posterPath,
            onClick = { onOpen(s) },
            modifier = Modifier.fillMaxWidth(),
            inMyList = s.ref in state.myListKeys,
            liked = s.ref in state.likedKeys,
            onLongClick = { menu = true },
        )
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.library_card_open_details)) }, onClick = { menu = false; onOpen(s) })
            DropdownMenuItem(
                text = {
                    Text(stringResource(if (kind == CollectionKind.MY_LIST) R.string.library_card_remove_from_my_list else R.string.library_card_unlike))
                },
                onClick = { menu = false; viewModel.remove(kind, item) },
            )
            when (kind) {
                CollectionKind.MY_LIST -> DropdownMenuItem(
                    text = { Text(stringResource(if (s.ref in state.likedKeys) R.string.library_card_unlike else R.string.library_card_like)) },
                    onClick = { menu = false; viewModel.toggle(CollectionKind.LIKED, s) },
                )
                CollectionKind.LIKED -> DropdownMenuItem(
                    text = {
                        Text(stringResource(if (s.ref in state.myListKeys) R.string.library_card_remove_from_my_list else R.string.library_card_add_to_my_list))
                    },
                    onClick = { menu = false; viewModel.toggle(CollectionKind.MY_LIST, s) },
                )
            }
        }
    }
}
