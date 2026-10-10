package com.nexient.jazzy.feature.player.ui

import android.media.AudioManager
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.ui.compose.ContentFrame
import com.nexient.jazzy.core.common.formatCount
import com.nexient.jazzy.core.common.formatDuration
import com.nexient.jazzy.core.common.formatRate
import com.nexient.jazzy.core.designsystem.component.JazzyImage
import com.nexient.jazzy.core.designsystem.text.asString
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.core.model.StreamState
import com.nexient.jazzy.core.model.TmdbImages
import com.nexient.jazzy.feature.player.PlayerPanel
import com.nexient.jazzy.feature.player.PlayerPhase
import com.nexient.jazzy.feature.player.PlayerUiState
import com.nexient.jazzy.feature.player.PlayerViewModel
import com.nexient.jazzy.feature.player.R
import kotlinx.coroutines.delay

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
internal fun PlayerContent(state: PlayerUiState, viewModel: PlayerViewModel, onExit: () -> Unit) {
    val tv = JazzyTheme.isTv
    var controlsVisible by remember { mutableStateOf(true) }
    var interaction by remember { mutableIntStateOf(0) }
    var contentScale by remember { mutableStateOf(ContentScale.Fit) }
    var showStats by remember { mutableStateOf(false) }
    val rootFocus = remember { FocusRequester() }
    val context = LocalContext.current
    val audio = remember { context.getSystemService(AudioManager::class.java) }

    // Auto-hide controls after 4 s of no interaction while playing.
    LaunchedEffect(controlsVisible, interaction, state.isPlaying, state.panel) {
        if (controlsVisible && state.isPlaying && state.panel == PlayerPanel.NONE) {
            delay(4_000)
            controlsVisible = false
        }
    }
    LaunchedEffect(Unit) { runCatching { rootFocus.requestFocus() } }
    fun poke() { controlsVisible = true; interaction++ }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootFocus)
            .focusable()
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown || state.panel != PlayerPanel.NONE) return@onPreviewKeyEvent false
                // TV remote mapping (PL-6). With controls hidden, left/right seek; centre shows controls.
                when (e.key.nativeKeyCode) {
                    AndroidKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, AndroidKeyEvent.KEYCODE_MEDIA_PLAY, AndroidKeyEvent.KEYCODE_MEDIA_PAUSE -> { viewModel.togglePlay(); poke(); true }
                    AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> { viewModel.seekBy(10_000); poke(); true }
                    AndroidKeyEvent.KEYCODE_MEDIA_REWIND -> { viewModel.seekBy(-10_000); poke(); true }
                    AndroidKeyEvent.KEYCODE_DPAD_LEFT -> if (!controlsVisible) { viewModel.seekBy(-10_000); poke(); true } else false
                    AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> if (!controlsVisible) { viewModel.seekBy(10_000); poke(); true } else false
                    AndroidKeyEvent.KEYCODE_DPAD_CENTER, AndroidKeyEvent.KEYCODE_ENTER -> if (!controlsVisible) { poke(); true } else { interaction++; false }
                    AndroidKeyEvent.KEYCODE_DPAD_UP, AndroidKeyEvent.KEYCODE_DPAD_DOWN -> if (!controlsVisible) { poke(); true } else { interaction++; false }
                    else -> false
                }
            }
            .then(
                if (tv) Modifier else Modifier
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { if (controlsVisible) controlsVisible = false else poke() },
                            onDoubleTap = { offset -> viewModel.seekBy(if (offset.x < size.width / 2) -10_000 else 10_000); poke() },
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            if (zoom > 1.05f) contentScale = ContentScale.Crop else if (zoom < 0.95f) contentScale = ContentScale.Fit
                        }
                    }
                    .pointerInput(Unit) {
                        // Right half: volume. Left half: brightness (PL-7).
                        detectVerticalDragGestures { change, drag ->
                            val rightHalf = change.position.x > size.width / 2
                            if (rightHalf) {
                                audio?.adjustStreamVolume(AudioManager.STREAM_MUSIC, if (drag < 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER, 0)
                            } else {
                                (context as? android.app.Activity)?.window?.let { w ->
                                    val lp = w.attributes
                                    val current = if (lp.screenBrightness < 0) 0.5f else lp.screenBrightness
                                    lp.screenBrightness = (current - drag / 2000f).coerceIn(0.05f, 1f)
                                    w.attributes = lp
                                }
                            }
                        }
                    },
            ),
    ) {
        ContentFrame(player = viewModel.player, modifier = Modifier.fillMaxSize(), contentScale = contentScale)
        SubtitleOverlay(viewModel.player, Modifier.fillMaxSize())

        // Pre-roll (PL-3): until the first frame, show the backdrop and live swarm stats.
        if (state.firstFrameMs == null && state.phase != PlayerPhase.ERROR) PreRoll(state, onCancel = onExit)

        // Mid-playback buffering spinner.
        if (state.firstFrameMs != null && state.phase == PlayerPhase.PLAYING && !state.isPlaying && state.stats?.state == StreamState.BUFFERING) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = JazzyColors.Accent)
        }

        AnimatedVisibility(controlsVisible && state.firstFrameMs != null, enter = fadeIn(), exit = fadeOut()) {
            Controls(
                state, viewModel, onExit,
                onToggleScale = { contentScale = if (contentScale == ContentScale.Fit) ContentScale.Crop else if (contentScale == ContentScale.Crop) ContentScale.FillBounds else ContentScale.Fit },
                onToggleStats = { showStats = !showStats },
                onInteract = { interaction++ },
            )
        }

        if (showStats) StatsOverlay(state, Modifier.align(Alignment.TopEnd).padding(top = 72.dp, end = 16.dp))

        state.stalledSuggestion?.let { next ->
            Prompt(
                text = stringResource(R.string.player_stall_prompt, next.release.qualityLabel, formatCount(next.seeders)),
                primary = stringResource(R.string.player_stall_try) to { viewModel.tryNextSource() },
                secondary = stringResource(R.string.player_stall_keep_waiting) to { viewModel.dismissStall() },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        state.countdown?.let { c ->
            Prompt(
                text = c.title?.let { stringResource(R.string.player_next_episode_in_titled, c.secondsLeft, c.playable.label.toString(), it) }
                    ?: stringResource(R.string.player_next_episode_in, c.secondsLeft, c.playable.label.toString()),
                primary = stringResource(R.string.player_next_play_now) to { viewModel.playNextNow() },
                secondary = stringResource(R.string.player_common_cancel) to { viewModel.cancelCountdown() },
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }

        state.error?.let { err ->
            if (state.panel == PlayerPanel.NONE) {
                Column(
                    Modifier.align(Alignment.Center).widthIn(max = 520.dp).background(JazzyColors.Scrim, RoundedCornerShape(12.dp)).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(err.message.asString(), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (err.canTryNext) Button(onClick = { viewModel.openPanel(PlayerPanel.SOURCES) }) { Text(stringResource(R.string.player_error_choose_source)) }
                        OutlinedButton(onClick = viewModel::retry) { Text(stringResource(R.string.player_common_retry)) }
                        TextButton(onClick = onExit) { Text(stringResource(R.string.player_common_back)) }
                    }
                }
            }
        }

        AnimatedVisibility(
            state.panel != PlayerPanel.NONE,
            enter = slideInHorizontally { it },
            exit = slideOutHorizontally { it },
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            SidePanel(state, viewModel, Modifier.fillMaxHeight().fillMaxWidth(if (tv) 0.42f else 0.5f))
        }
    }
}

@Composable
private fun PreRoll(state: PlayerUiState, onCancel: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        JazzyImage(TmdbImages.backdrop(state.request.snapshot.backdropPath, tv = true), null, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(JazzyColors.Scrim, Color.Black))))
        Column(Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(state.title, style = MaterialTheme.typography.headlineMedium)
            state.subtitle?.let { Text(it, style = MaterialTheme.typography.titleMedium, color = JazzyColors.TextSecondary) }
            CircularProgressIndicator(color = JazzyColors.Accent)
            val s = state.stats
            val line = when {
                state.phase == PlayerPhase.CONNECTING || s == null || s.state == StreamState.FETCHING_METADATA ->
                    s?.let { pluralStringResource(R.plurals.player_preroll_connecting_peers, it.peers, it.peers) } ?: stringResource(R.string.player_preroll_connecting)
                else -> pluralStringResource(R.plurals.player_preroll_buffering, s.peers, s.peers, formatRate(s.downloadRateBps), (s.fileProgress * 100).toInt())
            }
            Text(line, style = MaterialTheme.typography.bodyLarge, color = JazzyColors.TextSecondary)
            Text(stringResource(R.string.player_preroll_source, state.request.source.release.qualityLabel, state.request.source.providerNames.joinToString()), style = MaterialTheme.typography.bodySmall, color = JazzyColors.TextTertiary)
            OutlinedButton(onClick = onCancel) { Text(stringResource(R.string.player_common_cancel)) }
        }
    }
}

@Composable
private fun Controls(
    state: PlayerUiState,
    viewModel: PlayerViewModel,
    onExit: () -> Unit,
    onToggleScale: () -> Unit,
    onToggleStats: () -> Unit,
    onInteract: () -> Unit,
) {
    val playFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { playFocus.requestFocus() } }
    var speedMenu by remember { mutableStateOf(false) }
    var scrub by remember { mutableFloatStateOf(-1f) }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(JazzyColors.Scrim, Color.Transparent, Color.Transparent, JazzyColors.Scrim)))) {
        // Top bar
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onExit) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.player_common_back)) }
            Column(Modifier.weight(1f)) {
                Text(state.title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                state.subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = JazzyColors.TextSecondary) }
            }
            IconButton(onClick = { viewModel.toggleLike(); onInteract() }) {
                Icon(if (state.liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, stringResource(if (state.liked) R.string.player_controls_unlike else R.string.player_controls_like), tint = if (state.liked) JazzyColors.Accent else Color.White)
            }
            IconButton(onClick = { viewModel.openPanel(PlayerPanel.SOURCES) }) { Icon(Icons.Filled.SwapHoriz, stringResource(R.string.player_controls_sources)) }
            if (state.request.playable.isEpisode) IconButton(onClick = { viewModel.openPanel(PlayerPanel.EPISODES) }) { Icon(Icons.Filled.VideoLibrary, stringResource(R.string.player_controls_episodes)) }
            IconButton(onClick = { viewModel.openPanel(PlayerPanel.TRACKS) }) { Icon(Icons.Filled.Subtitles, stringResource(R.string.player_controls_tracks)) }
            IconButton(onClick = onToggleStats) { Icon(Icons.Filled.Insights, stringResource(R.string.player_controls_stats)) }
            val context = LocalContext.current
            val openWith = stringResource(R.string.player_controls_open_with)
            IconButton(onClick = {
                viewModel.externalPlayerUrl()?.let { url ->
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).setDataAndType(android.net.Uri.parse(url), "video/*")
                    runCatching { context.startActivity(android.content.Intent.createChooser(intent, openWith)) }
                }
            }) { Icon(Icons.Filled.OpenInNew, stringResource(R.string.player_controls_external_player)) }
        }
        // Centre transport
        Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.seekBy(-10_000); onInteract() }, Modifier.size(56.dp)) { Icon(Icons.Filled.Replay10, stringResource(R.string.player_controls_back_10), Modifier.size(40.dp)) }
            IconButton(onClick = { viewModel.togglePlay(); onInteract() }, Modifier.size(72.dp).focusRequester(playFocus)) {
                Icon(if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, stringResource(if (state.isPlaying) R.string.player_controls_pause else R.string.player_controls_play), Modifier.size(56.dp))
            }
            IconButton(onClick = { viewModel.seekBy(10_000); onInteract() }, Modifier.size(56.dp)) { Icon(Icons.Filled.Forward10, stringResource(R.string.player_controls_forward_10), Modifier.size(40.dp)) }
        }
        // Bottom: scrub bar with buffered range, times, speed, aspect
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
            val duration = state.durationMs.coerceAtLeast(1)
            Box {
                Slider(
                    value = state.bufferedMs.toFloat() / duration,
                    onValueChange = {},
                    enabled = false,
                    colors = SliderDefaults.colors(disabledActiveTrackColor = Color.White.copy(alpha = 0.35f), disabledInactiveTrackColor = Color.White.copy(alpha = 0.15f), disabledThumbColor = Color.Transparent),
                )
                Slider(
                    value = if (scrub >= 0) scrub else state.positionMs.toFloat() / duration,
                    onValueChange = { scrub = it; onInteract() },
                    onValueChangeFinished = { viewModel.seekTo((scrub * duration).toLong()); scrub = -1f },
                    colors = SliderDefaults.colors(thumbColor = JazzyColors.Accent, activeTrackColor = JazzyColors.Accent, inactiveTrackColor = Color.Transparent),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.player_controls_time, formatDuration(if (scrub >= 0) (scrub * duration).toLong() else state.positionMs), formatDuration(state.durationMs)), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                Box {
                    TextButton(onClick = { speedMenu = true }) { Icon(Icons.Filled.Speed, null); Text(stringResource(R.string.player_controls_speed, state.speed.toString())) }
                    DropdownMenu(expanded = speedMenu, onDismissRequest = { speedMenu = false }) {
                        listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { s ->
                            DropdownMenuItem(text = { Text(stringResource(R.string.player_controls_speed_option, s.toString())) }, onClick = { speedMenu = false; viewModel.setSpeed(s) })
                        }
                    }
                }
                IconButton(onClick = onToggleScale) { Icon(Icons.Filled.AspectRatio, stringResource(R.string.player_controls_aspect_ratio)) }
            }
        }
    }
}

@Composable
private fun StatsOverlay(state: PlayerUiState, modifier: Modifier) {
    val s = state.stats
    Column(modifier.background(JazzyColors.Scrim, RoundedCornerShape(8.dp)).padding(12.dp)) {
        Text(stringResource(R.string.player_stats_title), style = MaterialTheme.typography.labelLarge)
        listOfNotNull(
            state.fileName?.let { stringResource(R.string.player_stats_file, it) },
            state.metadataMs?.let { stringResource(R.string.player_stats_metadata, it) },
            state.firstFrameMs?.let { stringResource(R.string.player_stats_first_frame, it) },
            s?.let { stringResource(R.string.player_stats_rates, formatRate(it.downloadRateBps), formatRate(it.uploadRateBps)) },
            s?.let { stringResource(R.string.player_stats_peers, it.peers, it.seeds) },
            s?.let { stringResource(R.string.player_stats_downloaded, (it.fileProgress * 100).toInt(), it.bufferedAheadBytes / 1_048_576) },
            s?.let { stringResource(R.string.player_stats_state, it.state.toString()) },
        ).forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = JazzyColors.TextSecondary) }
    }
}

@Composable
private fun Prompt(text: String, primary: Pair<String, () -> Unit>, secondary: Pair<String, () -> Unit>, modifier: Modifier) {
    Row(
        modifier.padding(24.dp).background(JazzyColors.SurfaceRaised, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.widthIn(max = 420.dp))
        Button(onClick = primary.second) { Text(primary.first) }
        TextButton(onClick = secondary.second) { Text(secondary.first) }
    }
}
