package com.nexient.jazzy.feature.library.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexient.jazzy.core.designsystem.component.FullScreenLoading
import com.nexient.jazzy.core.designsystem.component.JazzyImage
import com.nexient.jazzy.core.designsystem.component.MessageState
import com.nexient.jazzy.core.designsystem.component.jazzyClickable
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.core.model.TmdbImages
import com.nexient.jazzy.feature.library.HistoryItem
import com.nexient.jazzy.feature.library.LibraryUiState
import com.nexient.jazzy.feature.library.LibraryViewModel
import com.nexient.jazzy.feature.library.episodeLine
import com.nexient.jazzy.feature.library.metaLine
import com.nexient.jazzy.feature.library.progress
import com.nexient.jazzy.feature.library.stoppedLine

/**
 * History (HS-3 … HS-7): grouped by day, newest first. Selecting an entry resumes it in Details;
 * entries can be removed (swipe on phone, menu on TV), folded entries expanded, and the list
 * searched by title.
 */
@Composable
internal fun HistoryList(
    state: LibraryUiState,
    viewModel: LibraryViewModel,
    onOpen: (HistoryItem) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val history = state.history
    val tv = JazzyTheme.isTv
    var searchOpen by rememberSaveable { mutableStateOf(viewModel.historyQuery.isNotEmpty()) }
    var expanded by rememberSaveable { mutableStateOf(setOf<Long>()) }

    LazyColumn(modifier.fillMaxSize(), contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (history.totalEntries > 0) {
            item(key = "toolbar", contentType = "toolbar") {
                HistoryToolbar(
                    query = viewModel.historyQuery,
                    searchOpen = searchOpen,
                    onToggleSearch = {
                        if (searchOpen) viewModel.onHistoryQueryChange("")
                        searchOpen = !searchOpen
                    },
                    onQueryChange = viewModel::onHistoryQueryChange,
                    onClearAll = viewModel::requestClearHistory,
                )
            }
        }
        when {
            history.loading -> item(key = "loading") { FullScreenLoading(Modifier.height(240.dp)) }
            history.totalEntries == 0 -> item(key = "empty") {
                MessageState(
                    title = "No watch history yet",
                    message = "Everything you play shows up here with where you stopped, so you can pick it up again.",
                    icon = Icons.Filled.History,
                )
            }
            history.sections.isEmpty() -> item(key = "no_match") {
                MessageState(
                    title = "No history matches “${viewModel.historyQuery.trim()}”",
                    message = "Try another title.",
                    icon = Icons.Filled.SearchOff,
                )
            }
            else -> history.sections.forEach { section ->
                item(key = "day:${section.date}", contentType = "header") {
                    Text(
                        section.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = JazzyColors.TextSecondary,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp).semantics { heading() },
                    )
                }
                items(section.items, key = { "entry:${it.key}" }, contentType = { "entry" }) { item ->
                    val isExpanded = item.key in expanded
                    val toggle = { expanded = if (isExpanded) expanded - item.key else expanded + item.key }
                    val row = @Composable {
                        HistoryRow(
                            item = item,
                            expanded = isExpanded,
                            onToggleExpand = toggle,
                            onOpen = { onOpen(item) },
                            onRemove = { viewModel.removeHistoryItem(item) },
                            onRemoveAll = { viewModel.removeAllForTitle(item) },
                            onRemoveSession = viewModel::removeSession,
                        )
                    }
                    if (tv) row() else SwipeToRemove(onRemove = { viewModel.removeHistoryItem(item) }) { row() }
                }
            }
        }
    }
}

@Composable
private fun HistoryToolbar(
    query: String,
    searchOpen: Boolean,
    onToggleSearch: () -> Unit,
    onQueryChange: (String) -> Unit,
    onClearAll: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onToggleSearch) {
                Icon(if (searchOpen) Icons.Filled.Close else Icons.Filled.Search, null, Modifier.size(18.dp))
                Text(if (searchOpen) "Close search" else "Search", Modifier.padding(start = 6.dp))
            }
            OutlinedButton(onClick = onClearAll) {
                Icon(Icons.Filled.DeleteSweep, null, Modifier.size(18.dp))
                Text("Clear all history", Modifier.padding(start = 6.dp))
            }
        }
        if (searchOpen) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search history by title") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = if (query.isNotEmpty()) ({ IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Filled.Close, "Clear search") } }) else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )
        }
    }
}

/** Phone: swipe an entry from the end to remove it (HS-6). */
@Composable
private fun SwipeToRemove(onRemove: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        onDismiss = { if (it == SwipeToDismissBoxValue.EndToStart) onRemove() },
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)).background(JazzyColors.Poor).padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Filled.Delete, "Remove", tint = Color.White)
            }
        },
    ) { content() }
}

@Composable
private fun HistoryRow(
    item: HistoryItem,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
    onRemoveAll: () -> Unit,
    onRemoveSession: (Long) -> Unit,
) {
    val tv = JazzyTheme.isTv
    val entry = item.entry
    val s = entry.snapshot
    var menu by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Column(Modifier.fillMaxWidth().background(JazzyColors.Background)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .weight(1f)
                    .onPreviewKeyEvent { event ->
                        if (event.key != Key.Menu) return@onPreviewKeyEvent false
                        if (event.type == KeyEventType.KeyUp) menu = true
                        true
                    },
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .jazzyClickable(shape, focusScale = 1.02f, onLongClick = { menu = true }, onClick = onOpen)
                        .semantics {
                            customActions = listOf(
                                CustomAccessibilityAction("Remove from history") { onRemove(); true },
                                CustomAccessibilityAction("Remove all for this title") { onRemoveAll(); true },
                            )
                        }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.width(if (tv) 220.dp else 136.dp).aspectRatio(16f / 9f).clip(shape)) {
                        JazzyImage(
                            TmdbImages.backdrop(s.backdropPath, tv) ?: TmdbImages.poster(s.posterPath, tv),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            fallbackText = s.title,
                        )
                        LinearProgressIndicator(
                            progress = { entry.progress() },
                            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp),
                            color = JazzyColors.Accent,
                            trackColor = Color.White.copy(alpha = 0.25f),
                            drawStopIndicator = {},
                        )
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(s.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        entry.episodeLine()?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = JazzyColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(entry.stoppedLine(), style = MaterialTheme.typography.bodySmall)
                        Text(entry.metaLine(), style = MaterialTheme.typography.bodySmall, color = JazzyColors.TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (item.folded) {
                            Text("Watched ${entry.sessionCount} times", style = MaterialTheme.typography.labelMedium, color = JazzyColors.Accent)
                        }
                    }
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Resume") }, onClick = { menu = false; onOpen() })
                    if (item.folded) {
                        DropdownMenuItem(text = { Text(if (expanded) "Hide sessions" else "Show sessions") }, onClick = { menu = false; onToggleExpand() })
                    }
                    DropdownMenuItem(
                        text = { Text(if (item.folded) "Remove these ${entry.sessionCount} sessions" else "Remove from history") },
                        onClick = { menu = false; onRemove() },
                    )
                    DropdownMenuItem(text = { Text("Remove all for this title") }, onClick = { menu = false; onRemoveAll() })
                }
            }
            if (item.folded) {
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        if (expanded) "Hide sessions" else "Show ${entry.sessionCount} sessions",
                        tint = JazzyColors.TextSecondary,
                    )
                }
            }
            if (!tv) {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "More options for ${s.title}", tint = JazzyColors.TextSecondary) }
            }
        }
        if (expanded && item.folded) SessionList(item, onRemoveSession)
    }
}

/** The sessions folded into an entry (HS-5), newest first; each can be removed on its own. */
@Composable
private fun SessionList(item: HistoryItem, onRemoveSession: (Long) -> Unit) {
    val time = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
    Column(Modifier.fillMaxWidth().padding(start = if (JazzyTheme.isTv) 232.dp else 148.dp)) {
        item.sessions.forEach { session ->
            val started = java.time.Instant.ofEpochMilli(session.startedAt).atZone(java.time.ZoneId.systemDefault()).format(time)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$started · stopped at ${com.nexient.jazzy.core.common.formatDuration(session.stopPositionMs)}" +
                        " · watched ${com.nexient.jazzy.core.common.formatDuration(session.watchedMs)}" +
                        (session.quality?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = JazzyColors.TextSecondary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onRemoveSession(session.id) }) {
                    Icon(Icons.Filled.Close, "Remove this session", tint = JazzyColors.TextTertiary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
