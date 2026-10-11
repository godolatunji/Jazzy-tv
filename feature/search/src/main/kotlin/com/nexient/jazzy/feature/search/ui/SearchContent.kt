package com.nexient.jazzy.feature.search.ui

import com.nexient.jazzy.core.designsystem.component.dpadFocusEscape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.nexient.jazzy.core.data.catalog.SearchFilter
import com.nexient.jazzy.core.designsystem.component.JazzyChip
import com.nexient.jazzy.core.designsystem.component.JazzyImage
import com.nexient.jazzy.core.designsystem.component.MessageState
import com.nexient.jazzy.core.designsystem.component.PosterCard
import com.nexient.jazzy.core.designsystem.component.gridHeader
import com.nexient.jazzy.core.designsystem.component.jazzyClickable
import com.nexient.jazzy.core.designsystem.component.pagingFooter
import com.nexient.jazzy.core.designsystem.component.posterSkeletonItems
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.core.model.PersonSummary
import com.nexient.jazzy.core.model.SearchResult
import com.nexient.jazzy.core.model.TitleSummary
import com.nexient.jazzy.core.model.TmdbImages
import com.nexient.jazzy.feature.search.MIN_QUERY_LENGTH
import com.nexient.jazzy.feature.search.R
import com.nexient.jazzy.feature.search.SearchUiState

/** Callbacks shared by the phone and TV search layouts. */
internal class SearchCallbacks(
    val onQueryChange: (String) -> Unit,
    val onClear: () -> Unit,
    val onFilter: (SearchFilter) -> Unit,
    val onOpenTitle: (TitleSummary) -> Unit,
    val onOpenPerson: (PersonSummary) -> Unit,
    val onOpenPopular: (TitleSummary) -> Unit,
    val onRecent: (String) -> Unit,
    val onRemoveRecent: (String) -> Unit,
    val onClearRecent: () -> Unit,
    val onRetryPopular: () -> Unit,
    val onVoice: (() -> Unit)?,
)

@Composable
internal fun SearchField(
    query: String,
    callbacks: SearchCallbacks,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    showVoiceInside: Boolean = true,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val tv = JazzyTheme.isTv
    TextField(
        value = query,
        onValueChange = callbacks.onQueryChange,
        modifier = Modifier.dpadFocusEscape().then(modifier.then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)),
        placeholder = { Text(stringResource(R.string.search_field_placeholder), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(Icons.Filled.Search, null) },
        trailingIcon = {
            Row {
                if (query.isNotEmpty()) {
                    IconButton(onClick = callbacks.onClear) { Icon(Icons.Filled.Close, stringResource(R.string.search_field_clear)) }
                }
                if (showVoiceInside && callbacks.onVoice != null) {
                    IconButton(onClick = callbacks.onVoice) { Icon(Icons.Filled.Mic, stringResource(R.string.search_voice_button)) }
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = JazzyColors.Focus.copy(alpha = if (tv) 1f else 0f),
            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            focusedContainerColor = JazzyColors.SurfaceRaised,
            unfocusedContainerColor = JazzyColors.Surface,
        ),
        // On TV, D-pad focus alone shouldn't cover the screen with the IME; OK/center opens it.
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, showKeyboardOnFocus = !tv),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
    )
}

/** Round mic button used beside the field on TV (the remote's mic launches the same recognizer). */
@Composable
internal fun VoiceButton(onVoice: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.search_voice_button)
    Box(
        modifier
            .size(56.dp)
            .jazzyClickable(CircleShape, onClick = onVoice)
            .background(JazzyColors.SurfaceRaised, CircleShape)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Mic, null, tint = JazzyColors.OnBackground, modifier = Modifier.size(28.dp))
    }
}

@Composable
internal fun SearchFilterRow(selected: SearchFilter, onSelect: (SearchFilter) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            SearchFilter.ALL to R.string.search_filter_all,
            SearchFilter.MOVIES to R.string.search_filter_movies,
            SearchFilter.TV to R.string.search_filter_tv,
            SearchFilter.PEOPLE to R.string.search_filter_people,
        ).forEach { (filter, label) -> JazzyChip(stringResource(label), filter == selected, onClick = { onSelect(filter) }) }
    }
}

/** True when the box holds a searchable query whose debounced results haven't started yet. */
internal fun isPending(query: String, state: SearchUiState): Boolean {
    val q = query.trim()
    return q.length >= MIN_QUERY_LENGTH && q != state.activeQuery
}

internal fun isIdle(query: String): Boolean = query.trim().length < MIN_QUERY_LENGTH

/** Results, or the idle "recent + popular" content, depending on the query. */
@Composable
internal fun SearchBody(
    query: String,
    state: SearchUiState,
    results: LazyPagingItems<SearchResult>,
    callbacks: SearchCallbacks,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState(),
) {
    if (isIdle(query)) {
        IdleGrid(state, callbacks, contentPadding, modifier)
        return
    }
    val refresh = results.loadState.refresh
    val pending = isPending(query, state)
    when {
        !pending && results.itemCount == 0 && refresh is LoadState.Error -> MessageState(
            title = stringResource(if (state.offline) R.string.search_error_offline_title else R.string.search_error_title),
            message = stringResource(if (state.offline) R.string.search_error_offline_message else R.string.search_common_check_connection),
            icon = Icons.Filled.WifiOff,
            actionLabel = stringResource(R.string.search_common_retry),
            onAction = results::retry,
            modifier = modifier.padding(contentPadding),
        )
        !pending && results.itemCount == 0 && refresh is LoadState.NotLoading -> MessageState(
            title = stringResource(R.string.search_empty_title, query.trim()),
            message = stringResource(if (state.filter == SearchFilter.ALL) R.string.search_empty_message_all else R.string.search_empty_message_filtered),
            icon = Icons.Filled.SearchOff,
            actionLabel = if (state.filter == SearchFilter.ALL) null else stringResource(R.string.search_empty_show_all),
            onAction = { callbacks.onFilter(SearchFilter.ALL) },
            modifier = modifier.padding(contentPadding),
        )
        else -> ResultsGrid(results, state, pending || (results.itemCount == 0 && refresh is LoadState.Loading), callbacks, contentPadding, modifier, gridState)
    }
}

@Composable
private fun ResultsGrid(
    results: LazyPagingItems<SearchResult>,
    state: SearchUiState,
    loading: Boolean,
    callbacks: SearchCallbacks,
    contentPadding: PaddingValues,
    modifier: Modifier,
    gridState: LazyGridState,
) {
    val spacing = JazzyTheme.spacing
    val gap = if (JazzyTheme.isTv) 20.dp else 8.dp
    LazyVerticalGrid(
        columns = GridCells.Adaptive(spacing.posterWidth),
        state = gridState,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        if (loading) {
            posterSkeletonItems(18)
            return@LazyVerticalGrid
        }
        // No stable keys: /search/multi can repeat an item across pages, and duplicate keys crash a lazy grid.
        items(results.itemCount, contentType = { i -> if (results.peek(i) is SearchResult.Person) "person" else "title" }) { index ->
            when (val item = results[index]) {
                is SearchResult.Title -> PosterCard(
                    title = item.title.title,
                    posterPath = item.title.posterPath,
                    onClick = { callbacks.onOpenTitle(item.title) },
                    modifier = Modifier.fillMaxWidth(),
                    inMyList = item.title.ref in state.myList,
                    liked = item.title.ref in state.liked,
                )
                is SearchResult.Person -> PersonCell(item.person, onClick = { callbacks.onOpenPerson(item.person) })
                null -> Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f))
            }
        }
        pagingFooter(results)
    }
}

/** A person result: a circular profile photo and name in a poster-sized cell. */
@Composable
internal fun PersonCell(person: PersonSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(8.dp)
    val a11y = stringResource(R.string.search_result_person, person.name)
    Column(
        modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
            .jazzyClickable(shape, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = a11y }
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        JazzyImage(
            TmdbImages.profile(person.profilePath),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth(0.9f).aspectRatio(1f).clip(CircleShape),
            fallbackText = person.name.split(' ').mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString(""),
        )
        Text(person.name, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        person.knownFor?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = JazzyColors.TextSecondary, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun IdleGrid(state: SearchUiState, callbacks: SearchCallbacks, contentPadding: PaddingValues, modifier: Modifier) {
    val spacing = JazzyTheme.spacing
    val gap = if (JazzyTheme.isTv) 20.dp else 8.dp
    val recentTitle = stringResource(R.string.search_recent_title)
    val popularTitle = stringResource(R.string.search_popular_title)
    LazyVerticalGrid(
        columns = GridCells.Adaptive(spacing.posterWidth),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        if (state.recent.isNotEmpty()) {
            gridHeader("recent_header", recentTitle) {
                TextButton(onClick = callbacks.onClearRecent) { Text(stringResource(R.string.search_recent_clear_all)) }
            }
            items(state.recent, key = { "recent:$it" }, span = { GridItemSpan(maxLineSpan) }, contentType = { "recent" }) { q ->
                RecentRow(q, onClick = { callbacks.onRecent(q) }, onRemove = { callbacks.onRemoveRecent(q) })
            }
        }
        gridHeader("popular_header", popularTitle)
        val popular = state.popular
        when {
            popular.items.isEmpty() && popular.loading -> posterSkeletonItems(12, "popular_skeleton")
            popular.items.isEmpty() && popular.failed -> item(key = "popular_error", span = { GridItemSpan(maxLineSpan) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(if (state.offline) R.string.search_popular_offline else R.string.search_popular_error),
                        color = JazzyColors.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = callbacks.onRetryPopular) { Text(stringResource(R.string.search_common_retry)) }
                }
            }
            else -> items(popular.items, key = { "popular:${it.tmdbId}:${it.mediaType.apiValue}" }, contentType = { "title" }) { item ->
                PosterCard(
                    title = item.title,
                    posterPath = item.posterPath,
                    onClick = { callbacks.onOpenPopular(item) },
                    modifier = Modifier.fillMaxWidth(),
                    inMyList = item.ref in state.myList,
                    liked = item.ref in state.liked,
                )
            }
        }
    }
}

@Composable
private fun RecentRow(query: String, onClick: () -> Unit, onRemove: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .jazzyClickable(focusScale = 1.02f, onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Filled.History, null, tint = JazzyColors.TextSecondary)
            Text(query, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onRemove) { Icon(Icons.Filled.Close, stringResource(R.string.search_recent_remove, query), tint = JazzyColors.TextSecondary) }
    }
}
