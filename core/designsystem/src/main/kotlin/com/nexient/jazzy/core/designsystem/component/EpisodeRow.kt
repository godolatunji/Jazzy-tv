package com.nexient.jazzy.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexient.jazzy.core.common.formatRuntime
import com.nexient.jazzy.core.designsystem.R
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.core.model.Episode
import com.nexient.jazzy.core.model.TmdbImages

/** Episode row for the episodes side panel and phone list (spec §5.3.3, PL-5). */
@Composable
fun EpisodeRow(
    episode: Episode,
    progress: Float?,
    watched: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) JazzyColors.SurfaceRaised else Color.Transparent)
            .jazzyClickable(shape, focusScale = 1.03f, onClick = onClick)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val stillWidth = if (JazzyTheme.isTv) 200.dp else if (compact) 112.dp else 140.dp
        Box(Modifier.width(stillWidth)) {
            StillCard(TmdbImages.still(episode.stillPath), stringResource(R.string.designsystem_episode_still, episode.episodeNumber), Modifier.fillMaxWidth())
            if (progress != null && progress > 0f && !watched) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp),
                    color = JazzyColors.Accent,
                    trackColor = Color.White.copy(alpha = 0.25f),
                    drawStopIndicator = {},
                )
            }
            if (watched) {
                Icon(
                    Icons.Filled.CheckCircle, stringResource(R.string.designsystem_episode_watched), tint = JazzyColors.Good,
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp).background(JazzyColors.Scrim, CircleShape),
                )
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.designsystem_episode_title, episode.episodeNumber, episode.name),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                formatRuntime(episode.runtimeMinutes)?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = JazzyColors.TextSecondary)
                }
            }
            if (episode.overview.isNotBlank()) {
                Text(
                    episode.overview,
                    style = MaterialTheme.typography.bodySmall,
                    color = JazzyColors.TextSecondary,
                    maxLines = if (compact) 2 else 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
