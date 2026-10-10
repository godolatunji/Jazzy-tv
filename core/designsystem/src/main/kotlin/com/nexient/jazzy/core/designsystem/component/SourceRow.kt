package com.nexient.jazzy.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexient.jazzy.core.common.formatBytes
import com.nexient.jazzy.core.common.formatCount
import com.nexient.jazzy.core.common.formatDuration
import com.nexient.jazzy.core.designsystem.R
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.core.model.TorrentSource

/** Extra row state that comes from the profile and device rather than the site (SL-4, SL-6, SL-7). */
data class SourceRowFlags(
    val lastPlayed: Boolean = false,
    val resumePositionMs: Long? = null,
    val failedRecently: Boolean = false,
    val mayNotPlay: Boolean = false,
    val resolving: Boolean = false,
)

/**
 * One magnet result with its own Play button (spec §5.3.2 SL-3). Release names are cut to one line
 * and expand on long-press / focus-hold.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SourceRow(
    source: TorrentSource,
    flags: SourceRowFlags,
    onPlay: () -> Unit,
    onResume: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val release = source.release
    val a11yBase = stringResource(R.string.designsystem_source_a11y, release.qualityLabel, formatBytes(source.sizeBytes), formatCount(source.seeders))
    val a11y = if (flags.lastPlayed) stringResource(R.string.designsystem_source_a11y_last_played, a11yBase) else a11yBase
    Row(
        modifier
            .fillMaxWidth()
            .background(if (flags.lastPlayed) JazzyColors.SurfaceRaised else JazzyColors.Surface, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics(mergeDescendants = false) { contentDescription = a11y },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                HealthDot(source.health, Modifier.align(Alignment.CenterVertically))
                release.resolution?.let { Badge(it.label, tone = BadgeTone.ACCENT) }
                release.source?.let { Badge(it.label, tone = if (it.isLowQuality) BadgeTone.WARNING else BadgeTone.NEUTRAL) }
                release.videoCodec?.let { Badge(it.label, outlined = true) }
                release.hdr.forEach { Badge(it.label, tone = BadgeTone.INFO, outlined = true) }
                release.audio?.let { Badge(listOfNotNull(it, release.audioChannels).joinToString(" "), outlined = true) }
                if (release.isSeasonPack || release.isCompleteSeries) Badge(stringResource(if (release.isCompleteSeries) R.string.designsystem_source_complete_series else R.string.designsystem_source_season_pack), tone = BadgeTone.INFO)
                if (flags.lastPlayed) Badge(
                    flags.resumePositionMs?.let { stringResource(R.string.designsystem_source_last_played_stopped_at, formatDuration(it)) }
                        ?: stringResource(R.string.designsystem_source_last_played),
                    tone = BadgeTone.SUCCESS,
                )
                if (flags.failedRecently) Badge(stringResource(R.string.designsystem_source_failed_recently), tone = BadgeTone.DANGER)
                if (flags.mayNotPlay) Badge(stringResource(R.string.designsystem_source_may_not_play), tone = BadgeTone.WARNING, outlined = true)
                if (release.source?.isLowQuality == true) Badge(stringResource(R.string.designsystem_source_low_quality), tone = BadgeTone.WARNING, outlined = true)
            }
            Text(
                text = stringResource(
                    R.string.designsystem_source_stats,
                    formatBytes(source.sizeBytes), formatCount(source.seeders), formatCount(source.leechers), source.providerNames.joinToString(", "),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = JazzyColors.TextSecondary,
            )
            Text(
                text = source.name,
                style = MaterialTheme.typography.bodySmall,
                color = JazzyColors.TextTertiary,
                maxLines = if (expanded) 4 else 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (flags.resolving) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = JazzyColors.Accent)
                    Text(stringResource(R.string.designsystem_source_getting_link), style = MaterialTheme.typography.labelSmall, color = JazzyColors.TextSecondary)
                }
            } else if (onResume != null && flags.resumePositionMs != null) {
                FilledTonalButton(
                    onClick = onResume,
                    modifier = Modifier.jazzyFocusHold { expanded = it },
                ) {
                    Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp))
                    Text(stringResource(R.string.designsystem_source_resume, formatDuration(flags.resumePositionMs)))
                }
                OutlinedButton(onClick = onPlay) {
                    Icon(Icons.Filled.Replay, null, Modifier.size(16.dp))
                    Text(stringResource(if (JazzyTheme.isTv) R.string.designsystem_source_from_start else R.string.designsystem_source_start_over))
                }
            } else {
                FilledTonalButton(onClick = onPlay, modifier = Modifier.jazzyFocusHold { expanded = it }) {
                    Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp))
                    Text(stringResource(R.string.designsystem_source_play))
                }
            }
        }
    }
}

/** Expands the release name while the Play button is focused (TV focus-hold). */
private fun Modifier.jazzyFocusHold(onFocus: (Boolean) -> Unit): Modifier =
    this.onFocusChanged { onFocus(it.isFocused) }
