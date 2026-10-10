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
                    title = if (collection.options.filter == TypeFilter.MOVIES) "No movies here yet" else "No TV shows here yet",
                    message = "Switch to All to see everything in ${if (kind == CollectionKind.MY_LIST) "My List" else "Liked"}.",
                    icon = Icons.Filled.FilterAltOff,
                    actionLabel = "Show all",
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
            JazzyChip(f.label, collection.options.filter == f, onClick = { viewModel.setFilter(kind, f) })
        }
        var open by remember { mutableStateOf(false) }
        Box {
            JazzyChip(
                "Sort: ${collection.options.sort.label(kind)}",
                selected = false,
                onClick = { open = true },
                trailingIcon = Icons.Filled.ArrowDropDown,
            )
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                LibrarySort.entries.forEach { s ->
                    DropdownMenuItem(
                        text = { Text(if (s == collection.options.sort) "✓  ${s.label(kind)}" else s.label(kind)) },
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
    val how = if (tv) "Select" else "Tap"
    when (kind) {
        CollectionKind.MY_LIST -> MessageState(
            title = "Your list is empty",
            message = "$how + My List on any movie or show to save it here for later. " +
                if (tv) "You can also press Menu on a card." else "You can also long-press a poster.",
            icon = Icons.Filled.BookmarkBorder,
        )
        CollectionKind.LIKED -> MessageState(
            title = "No liked titles yet",
            message = "$how ♥ Like on a movie or show you enjoyed. Likes also shape your recommendations.",
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
            DropdownMenuItem(text = { Text("Open details") }, onClick = { menu = false; onOpen(s) })
            DropdownMenuItem(
                text = { Text(if (kind == CollectionKind.MY_LIST) "Remove from My List" else "Unlike") },
                onClick = { menu = false; viewModel.remove(kind, item) },
            )
            when (kind) {
                CollectionKind.MY_LIST -> DropdownMenuItem(
                    text = { Text(if (s.ref in state.likedKeys) "Unlike" else "Like") },
                    onClick = { menu = false; viewModel.toggle(CollectionKind.LIKED, s) },
                )
                CollectionKind.LIKED -> DropdownMenuItem(
                    text = { Text(if (s.ref in state.myListKeys) "Remove from My List" else "Add to My List") },
                    onClick = { menu = false; viewModel.toggle(CollectionKind.MY_LIST, s) },
                )
            }
        }
    }
}
