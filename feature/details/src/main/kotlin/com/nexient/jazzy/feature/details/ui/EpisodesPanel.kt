package com.nexient.jazzy.feature.details.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexient.jazzy.core.designsystem.component.EpisodeRow
import com.nexient.jazzy.core.designsystem.component.MessageState
import com.nexient.jazzy.core.designsystem.component.RailSkeleton
import com.nexient.jazzy.core.model.Episode
import com.nexient.jazzy.core.model.SeasonSummary
import com.nexient.jazzy.core.model.WatchRules
import com.nexient.jazzy.feature.details.DetailsUiState

/** Season dropdown (phone). Specials (season 0) are listed last (spec §5.3.3). */
@Composable
internal fun SeasonDropdown(seasons: List<SeasonSummary>, selected: Int?, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }) {
            Text(seasons.firstOrNull { it.seasonNumber == selected }?.name ?: "Season ${selected ?: 1}")
            Icon(Icons.Filled.ArrowDropDown, null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            seasons.forEach { s ->
                DropdownMenuItem(text = { Text("${s.name} (${s.episodeCount})") }, onClick = { open = false; onSelect(s.seasonNumber) })
            }
        }
    }
}

/** Episode rows for the selected season; watched ✓ and progress bars come from local progress. */
@Composable
internal fun EpisodeList(
    state: DetailsUiState,
    onEpisode: (Episode) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val season = state.season
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        when {
            season == null || season.isLoadingWithoutData -> RailSkeleton(backdrop = true)
            season.isErrorWithoutData -> MessageState("Couldn't load episodes", season.error?.message)
            else -> season.data?.episodes?.forEach { ep ->
                val p = state.episodeProgress[ep.seasonNumber to ep.episodeNumber]
                EpisodeRow(
                    episode = ep,
                    progress = p?.fraction,
                    watched = p?.completed == true || (p != null && WatchRules.isWatched(p.positionMs, p.durationMs)),
                    selected = state.selectedEpisode?.episodeNumber == ep.episodeNumber,
                    onClick = { onEpisode(ep) },
                    compact = compact,
                )
            }
        }
    }
}

@Composable
internal fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = modifier.padding(vertical = 4.dp))
}
