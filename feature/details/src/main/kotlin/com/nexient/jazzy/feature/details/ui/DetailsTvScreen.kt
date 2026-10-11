package com.nexient.jazzy.feature.details.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nexient.jazzy.core.designsystem.component.FullScreenLoading
import com.nexient.jazzy.core.designsystem.component.HeroBackdrop
import com.nexient.jazzy.core.designsystem.component.MessageState
import com.nexient.jazzy.core.designsystem.component.SourceRow
import com.nexient.jazzy.core.designsystem.component.jazzyClickable
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.core.model.MediaType
import com.nexient.jazzy.core.model.TitleDetails
import com.nexient.jazzy.feature.details.DetailsActions
import com.nexient.jazzy.feature.details.DetailsUiState
import com.nexient.jazzy.feature.details.DetailsViewModel
import com.nexient.jazzy.feature.details.R

/**
 * TV Details (spec §5.3.3). Movie: info left (~40%), Sources right with initial focus.
 * Show: info + season column left, episodes side panel right; an episode slides in its Sources.
 */
@Composable
internal fun DetailsTvScreen(state: DetailsUiState, viewModel: DetailsViewModel, actions: DetailsActions, snackbar: SnackbarHostState) {
    val details = state.details.data
    Box(Modifier.fillMaxSize()) {
        when {
            details == null && state.details.isErrorWithoutData -> MessageState(
                stringResource(R.string.details_error_load_title), state.details.error?.message,
                actionLabel = stringResource(R.string.details_common_retry), onAction = viewModel::retryDetails,
            )
            details == null -> FullScreenLoading()
            state.blockedByMaturity -> MessageState(
                stringResource(R.string.details_blocked_title), stringResource(R.string.details_blocked_message),
                actionLabel = stringResource(R.string.details_common_back), onAction = actions.back,
            )
            else -> TvContent(details, state, viewModel, actions)
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun TvContent(details: TitleDetails, state: DetailsUiState, viewModel: DetailsViewModel, actions: DetailsActions) {
    val spacing = JazzyTheme.spacing
    val isShow = details.mediaType == MediaType.TV
    var showSources by remember { mutableStateOf(!isShow || state.focusSources) }
    BackHandler(enabled = isShow && showSources) { showSources = false }
    val rightFocus = remember { FocusRequester() }
    val firstRowFocus = remember { FocusRequester() }
    LaunchedEffect(showSources, details.tmdbId) { runCatching { rightFocus.requestFocus() } }
    // Spec §5.10: initial focus on the Resume row, else the first source row's Play, once rows arrive.
    val focusKey = state.sources.rows.firstOrNull { it.flags.resumePositionMs != null }?.source?.key ?: state.sources.rows.firstOrNull()?.source?.key
    var focusedFirstRowFor by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(focusKey, showSources) {
        val key = focusKey ?: return@LaunchedEffect
        if (showSources && focusedFirstRowFor != (state.sources.query?.cacheKey ?: "") ) {
            kotlinx.coroutines.delay(150)
            if (runCatching { firstRowFocus.requestFocus() }.isSuccess) focusedFirstRowFor = state.sources.query?.cacheKey ?: ""
        }
    }

    Box(Modifier.fillMaxSize()) {
        HeroBackdrop(details.toHeroContent(), Modifier.fillMaxSize())
        Row(Modifier.fillMaxSize().padding(horizontal = spacing.gutter, vertical = spacing.verticalSafe), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            LazyColumn(Modifier.weight(0.4f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    DetailsInfo(
                        details, state, viewModel::toggleMyList, viewModel::toggleLike, viewModel::openTrailer,
                        onResumeBanner = { showSources = true }, compactOverview = true,
                    )
                }
                if (isShow) {
                    item { Text(stringResource(R.string.details_seasons_title), style = MaterialTheme.typography.titleMedium, color = JazzyColors.TextSecondary) }
                    items(details.seasons, key = { "season_${it.seasonNumber}" }) { s ->
                        val selected = s.seasonNumber == state.selectedSeason
                        Text(
                            pluralStringResource(R.plurals.details_season_row, s.episodeCount, s.name, s.episodeCount),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (selected) JazzyColors.OnBackground else JazzyColors.TextSecondary,
                            modifier = Modifier.jazzyClickable(focusScale = 1.03f) { viewModel.selectSeason(s.seasonNumber); showSources = false }.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            }
            AnimatedContent(targetState = showSources, label = "rightPane", modifier = Modifier.weight(0.6f).fillMaxHeight()) { sources ->
                if (!sources && isShow) {
                    LazyColumn(Modifier.focusRequester(rightFocus), contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        item { SectionTitle(stringResource(R.string.details_episodes_title)) }
                        item { EpisodeList(state, onEpisode = { viewModel.selectEpisode(it); showSources = true }) }
                    }
                } else {
                    Column(Modifier.focusRequester(rightFocus)) {
                        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            item {
                                val ep = state.selectedEpisode
                                SourcesHeader(
                                    state.sources,
                                    title = if (isShow && ep != null) stringResource(R.string.details_sources_episode_title, ep.seasonNumber, ep.episodeNumber, ep.name)
                                    else stringResource(R.string.details_sources_title),
                                    onFilters = viewModel::setFilters,
                                    onRefresh = viewModel::refreshSources,
                                )
                            }
                            items(state.sources.rows, key = { it.source.key }) { row ->
                                SourceRow(
                                    row.source, row.flags,
                                    onPlay = { viewModel.play(row.source) },
                                    onResume = { viewModel.resume(row.source) }.takeIf { row.flags.resumePositionMs != null },
                                    playFocusRequester = firstRowFocus.takeIf { row.source.key == focusKey },
                                )
                            }
                            item { SourcesEmpty(state.sources, viewModel::refreshSources, actions.openSitesSettings) }
                            item { MoreLikeThis(details, actions.openTitle) }
                        }
                    }
                }
            }
        }
    }
}
