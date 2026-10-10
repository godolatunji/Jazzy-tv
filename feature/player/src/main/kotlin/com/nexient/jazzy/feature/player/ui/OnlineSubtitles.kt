package com.nexient.jazzy.feature.player.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexient.jazzy.core.designsystem.component.jazzyClickable
import com.nexient.jazzy.core.designsystem.text.asString
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.feature.player.PlayerUiState
import com.nexient.jazzy.feature.player.PlayerViewModel
import com.nexient.jazzy.feature.player.R
import java.util.Locale

/** "Find subtitles online" (OpenSubtitles) with a ±0.5 s sync offset (PL-8). */
@Composable
internal fun OnlineSubtitlesSection(state: PlayerUiState, viewModel: PlayerViewModel) {
    val os = state.onlineSubtitles
    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.player_online_subtitles), style = MaterialTheme.typography.titleMedium, color = JazzyColors.TextSecondary)
        if (!os.configured) {
            Text(stringResource(R.string.player_online_subtitles_not_configured), style = MaterialTheme.typography.bodySmall, color = JazzyColors.TextTertiary)
            return@Column
        }
        os.applied?.let { applied ->
            Text(stringResource(R.string.player_online_subtitles_using, applied.release), style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.adjustSubtitleOffset(-500) }) { Text(stringResource(R.string.player_subtitle_earlier)) }
                Text(stringResource(R.string.player_subtitle_offset, String.format(Locale.US, "%+.1f", os.offsetMs / 1000.0)), style = MaterialTheme.typography.labelLarge)
                OutlinedButton(onClick = { viewModel.adjustSubtitleOffset(500) }) { Text(stringResource(R.string.player_subtitle_later)) }
            }
        }
        if (os.results == null) {
            OutlinedButton(onClick = viewModel::searchOnlineSubtitles, enabled = !os.searching) {
                Text(stringResource(if (os.searching) R.string.player_online_subtitles_searching else R.string.player_online_subtitles_search))
            }
        }
        os.error?.let { Text(it.asString(), style = MaterialTheme.typography.bodySmall, color = JazzyColors.Poor) }
        os.results?.let { results ->
            if (results.isEmpty()) Text(stringResource(R.string.player_online_subtitles_none), style = MaterialTheme.typography.bodySmall, color = JazzyColors.TextTertiary)
            results.take(30).forEach { sub ->
                Row(
                    Modifier.fillMaxWidth().jazzyClickable(focusScale = 1.02f) { viewModel.applyOnlineSubtitle(sub) }.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            Locale.forLanguageTag(sub.language).displayLanguage.ifBlank { sub.language }.let {
                                if (os.applied?.fileId == sub.fileId) stringResource(R.string.player_online_subtitles_selected, it) else it
                            },
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            listOfNotNull(
                                sub.release,
                                pluralStringResource(R.plurals.player_online_subtitles_downloads, sub.downloads, sub.downloads),
                                stringResource(R.string.player_online_subtitles_hi).takeIf { sub.hearingImpaired },
                                stringResource(R.string.player_online_subtitles_machine).takeIf { sub.machineTranslated },
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = JazzyColors.TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (os.applying == sub.fileId) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = JazzyColors.Accent)
                }
            }
        }
    }
}
