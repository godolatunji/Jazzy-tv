package com.nexient.jazzy.feature.player.ui

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView

/** Renders embedded/sidecar subtitles (PL-8) using the system caption style. */
@OptIn(UnstableApi::class)
@Composable
internal fun SubtitleOverlay(player: Player, modifier: Modifier) {
    val context = LocalContext.current
    val view = remember {
        SubtitleView(context).apply {
            setUserDefaultStyle()
            setUserDefaultTextSize()
            setApplyEmbeddedStyles(true)
            setStyle(CaptionStyleCompat.DEFAULT)
        }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onCues(cueGroup: CueGroup) = view.setCues(cueGroup.cues)
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    AndroidView(factory = { view }, modifier = modifier)
}
