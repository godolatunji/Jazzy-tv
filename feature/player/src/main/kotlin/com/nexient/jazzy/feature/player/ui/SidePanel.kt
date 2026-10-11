package com.nexient.jazzy.feature.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import com.nexient.jazzy.core.designsystem.component.EpisodeRow
import com.nexient.jazzy.core.designsystem.component.SourceRow
import com.nexient.jazzy.core.designsystem.component.SourceRowFlags
import com.nexient.jazzy.core.designsystem.component.jazzyClickable
import com.nexient.jazzy.core.designsystem.text.asString
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.player.TrackHelper
import com.nexient.jazzy.feature.player.PlayerPanel
import com.nexient.jazzy.feature.player.PlayerUiState
import com.nexient.jazzy.feature.player.PlayerViewModel
import com.nexient.jazzy.feature.player.R

/** Right-hand panel: Sources (PL-4), Episodes (PL-5) or Tracks (PL-8). Playback continues behind it. */
@Composable
internal fun SidePanel(state: PlayerUiState, viewModel: PlayerViewModel, modifier: Modifier) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(state.panel, state.episodes.chosen) { runCatching { focus.requestFocus() } }
    LazyColumn(
        modifier.background(JazzyColors.Background.copy(alpha = 0.94f)).focusRequester(focus),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.panel == PlayerPanel.EPISODES && state.episodes.chosen != null) {
                    IconButton(onClick = viewModel::backToEpisodeList) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.player_panel_back_to_episodes)) }
                }
                Text(
                    when (state.panel) {
                        PlayerPanel.SOURCES -> stringResource(R.string.player_panel_sources)
                        PlayerPanel.EPISODES -> state.episodes.chosen?.let { stringResource(R.string.player_panel_episode_title, it.seasonNumber, it.episodeNumber, it.name) }
                            ?: stringResource(R.string.player_panel_episodes)
                        PlayerPanel.TRACKS -> stringResource(R.string.player_panel_tracks)
                        PlayerPanel.NONE -> ""
                    },
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = viewModel::closePanel) { Icon(Icons.Filled.Close, stringResource(R.string.player_panel_close)) }
            }
        }
        // Why we're here after a failure (the error overlay is hidden while a panel is open).
        state.error?.let { err ->
            item {
                Text(
                    err.message.asString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = JazzyColors.Poor,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
            }
        }
        when (state.panel) {
            PlayerPanel.SOURCES -> {
                if (state.sources.isEmpty()) item { CircularProgressIndicator(color = JazzyColors.Accent) }
                items(state.sources, key = { it.key }) { source ->
                    val current = source.key == state.request.source.key
                    SourceRow(
                        source,
                        SourceRowFlags(lastPlayed = current),
                        onPlay = { if (!current) viewModel.switchSource(source) else viewModel.closePanel() },
                        onResume = null,
                    )
                }
            }
            PlayerPanel.EPISODES -> {
                val ep = state.episodes
                if (ep.chosen == null) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(ep.seasonNumbers) { n ->
                                FilterChip(selected = n == ep.season, onClick = { viewModel.selectPanelSeason(n) }, label = { Text(if (n == 0) stringResource(R.string.player_panel_specials) else stringResource(R.string.player_panel_season, n)) })
                            }
                        }
                    }
                    if (ep.loading) item { CircularProgressIndicator(color = JazzyColors.Accent) }
                    items(ep.episodes, key = { "${it.seasonNumber}:${it.episodeNumber}" }) { e ->
                        val currentEp = state.request.playable.season == e.seasonNumber && state.request.playable.episode == e.episodeNumber
                        EpisodeRow(e, progress = null, watched = false, selected = currentEp, onClick = { viewModel.chooseEpisode(e) }, compact = true)
                    }
                } else {
                    if (ep.sourcesLoading && ep.sources.isEmpty()) item { CircularProgressIndicator(color = JazzyColors.Accent) }
                    items(ep.sources, key = { it.key }) { source ->
                        SourceRow(source, SourceRowFlags(), onPlay = { viewModel.playEpisode(ep.chosen, source) }, onResume = null)
                    }
                }
            }
            PlayerPanel.TRACKS -> {
                item { TrackSection(stringResource(R.string.player_tracks_audio), viewModel.player, C.TRACK_TYPE_AUDIO) }
                item { TrackSection(stringResource(R.string.player_tracks_subtitles), viewModel.player, C.TRACK_TYPE_TEXT) }
                item { OnlineSubtitlesSection(state, viewModel) }
            }
            PlayerPanel.NONE -> Unit
        }
    }
}

@Composable
private fun TrackSection(title: String, player: Player, type: Int) {
    var version by remember { mutableIntStateOf(0) }
    val options = remember(version) { TrackHelper.options(player, type) }
    androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = JazzyColors.TextSecondary)
        if (options.isEmpty() || (type == C.TRACK_TYPE_TEXT && options.size == 1)) {
            Text(stringResource(if (type == C.TRACK_TYPE_TEXT) R.string.player_tracks_no_subtitles else R.string.player_tracks_no_audio), style = MaterialTheme.typography.bodySmall, color = JazzyColors.TextTertiary)
        }
        options.forEach { o ->
            Text(
                stringResource(if (o.selected) R.string.player_tracks_selected else R.string.player_tracks_unselected, o.label),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth().jazzyClickable(focusScale = 1.02f) { TrackHelper.select(player, type, o); version++ }.padding(10.dp),
            )
        }
    }
}
