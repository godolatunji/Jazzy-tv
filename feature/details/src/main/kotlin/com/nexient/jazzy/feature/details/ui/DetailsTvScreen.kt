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

/**
 * TV Details (spec §5.3.3). Movie: info left (~40%), Sources right with initial focus.
 * Show: info + season column left, episodes side panel right; an episode slides in its Sources.
 */
@Composable
internal fun DetailsTvScreen(state: DetailsUiState, viewModel: DetailsViewModel, actions: DetailsActions, snackbar: SnackbarHostState) {
    val details = state.details.data
    Box(Modifier.fillMaxSize()) {
        when {
            details == null && state.details.isErrorWithoutData -> MessageState("Couldn't load this title", state.details.error?.message, actionLabel = "Retry", onAction = viewModel::retryDetails)
            details == null -> FullScreenLoading()
            state.blockedByMaturity -> MessageState("Not available on this profile", "This title is above the profile's maturity setting.", actionLabel = "Back", onAction = actions.back)
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
    LaunchedEffect(showSources, details.tmdbId) { runCatching { rightFocus.requestFocus() } }

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
                    item { Text("Seasons", style = MaterialTheme.typography.titleMedium, color = JazzyColors.TextSecondary) }
                    items(details.seasons, key = { "season_${it.seasonNumber}" }) { s ->
                        val selected = s.seasonNumber == state.selectedSeason
                        Text(
                            "${s.name}  ·  ${s.episodeCount} episodes",
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
                        item { SectionTitle("Episodes") }
                        item { EpisodeList(state, onEpisode = { viewModel.selectEpisode(it); showSources = true }) }
                    }
                } else {
                    Column(Modifier.focusRequester(rightFocus)) {
                        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            item {
                                val ep = state.selectedEpisode
                                SourcesHeader(
                                    state.sources,
                                    title = if (isShow && ep != null) "S${ep.seasonNumber}:E${ep.episodeNumber} · ${ep.name}" else "Sources",
                                    onFilters = viewModel::setFilters,
                                    onRefresh = viewModel::refreshSources,
                                )
                            }
                            items(state.sources.rows, key = { it.source.key }) { row ->
                                SourceRow(
                                    row.source, row.flags,
                                    onPlay = { viewModel.play(row.source) },
                                    onResume = { viewModel.resume(row.source) }.takeIf { row.flags.resumePositionMs != null },
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
