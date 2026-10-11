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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.ui.compose.ContentFrame
import com.nexient.jazzy.core.common.formatCount
import com.nexient.jazzy.core.common.formatRate
import com.nexient.jazzy.core.designsystem.component.JazzyImage
import com.nexient.jazzy.core.designsystem.component.focusRing
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

/** How the D-pad reached the controls, which decides where focus lands when they appear. */
private enum class ControlsEntry { PLAY, SEEK }

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
internal fun PlayerContent(state: PlayerUiState, viewModel: PlayerViewModel, onExit: () -> Unit) {
    val tv = JazzyTheme.isTv
    var controlsVisible by remember { mutableStateOf(true) }
    var entry by remember { mutableStateOf(ControlsEntry.PLAY) }
    var interaction by remember { mutableIntStateOf(0) }
    var aspect by remember { mutableStateOf(AspectMode.FIT) }
    var aspectFlash by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var locked by remember { mutableStateOf(false) }
    var unlockVisible by remember { mutableStateOf(false) }
    var scrubMs by remember { mutableStateOf<Long?>(null) }
    var scrubSteps by remember { mutableIntStateOf(0) }
    val rootFocus = remember { FocusRequester() }
    val playFocus = remember { FocusRequester() }
    val seekFocus = remember { FocusRequester() }
    val context = LocalContext.current
    val audio = remember { context.getSystemService(AudioManager::class.java) }
    val overlayOpen = state.panel != PlayerPanel.NONE || state.error != null

    fun poke(target: ControlsEntry? = null) {
        if (locked) { unlockVisible = true; interaction++; return }
        if (!controlsVisible) entry = target ?: ControlsEntry.PLAY
        controlsVisible = true
        interaction++
    }

    // Scrubbing: Left/Right (or a drag) move a preview; the seek happens once input pauses, so the
    // engine re-prioritises pieces once per scrub rather than once per key press.
    fun scrubBy(direction: Int) {
        val duration = state.durationMs
        scrubSteps++
        val step = when {
            scrubSteps > 12 -> 60_000L
            scrubSteps > 4 -> 30_000L
            else -> 10_000L
        }
        val target = (scrubMs ?: state.positionMs) + direction * step
        scrubMs = if (duration > 0) target.coerceIn(0, duration) else target.coerceAtLeast(0)
        poke(ControlsEntry.SEEK)
    }
    fun commitScrub() {
        scrubMs?.let(viewModel::seekTo)
        scrubMs = null
        scrubSteps = 0
    }
    LaunchedEffect(scrubMs) {
        if (scrubMs == null) return@LaunchedEffect
        delay(SCRUB_COMMIT_MS)
        commitScrub()
    }

    // Auto-hide after a few seconds without input while playing.
    LaunchedEffect(controlsVisible, interaction, state.isPlaying, overlayOpen, scrubMs) {
        if (controlsVisible && state.isPlaying && !overlayOpen && scrubMs == null) {
            delay(CONTROLS_TIMEOUT_MS)
            controlsVisible = false
        }
    }
    LaunchedEffect(unlockVisible, interaction) {
        if (unlockVisible) { delay(CONTROLS_TIMEOUT_MS); unlockVisible = false }
    }
    LaunchedEffect(aspect) {
        if (aspectFlash) { delay(1_200); aspectFlash = false }
    }

    // Focus: the first control when the controls appear; the root (which takes the remote's keys) when
    // they hide or a panel closes, so key presses are never lost to a removed node.
    val controlsShown = controlsVisible && !locked && state.firstFrameMs != null
    LaunchedEffect(controlsShown, state.panel, state.error) {
        withFrameNanos { }
        runCatching {
            when {
                state.panel != PlayerPanel.NONE || state.error != null -> Unit
                state.firstFrameMs == null -> Unit // the pre-roll focuses its Cancel button
                controlsShown -> (if (entry == ControlsEntry.SEEK) seekFocus else playFocus).requestFocus()
                else -> rootFocus.requestFocus()
            }
        }
    }

    // Back, as a key event: with a control focused, Compose would otherwise spend the press on clearing
    // focus. In order: close the panel (or step back to its episode list), hide the TV controls, leave.
    // Decided on key down, done on key up. Gesture/back-button navigation still reaches PlayerRoute's handlers.
    var backAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    fun backActionNow(): () -> Unit = when {
        state.panel == PlayerPanel.EPISODES && state.episodes.chosen != null -> viewModel::backToEpisodeList
        state.panel != PlayerPanel.NONE -> viewModel::closePanel
        tv && controlsShown && state.error == null && state.countdown == null -> { { commitScrub(); controlsVisible = false } }
        else -> onExit
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootFocus)
            .focusable()
            .onPreviewKeyEvent { e ->
                val code = e.key.nativeKeyCode
                if (code == AndroidKeyEvent.KEYCODE_BACK) {
                    if (e.type == KeyEventType.KeyDown) backAction = backActionNow()
                    else if (e.type == KeyEventType.KeyUp) { backAction?.invoke(); backAction = null }
                    return@onPreviewKeyEvent true
                }
                if (e.type != KeyEventType.KeyDown || state.panel != PlayerPanel.NONE || state.firstFrameMs == null) return@onPreviewKeyEvent false
                when (code) {
                    AndroidKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, AndroidKeyEvent.KEYCODE_MEDIA_PLAY, AndroidKeyEvent.KEYCODE_MEDIA_PAUSE -> { viewModel.togglePlay(); poke(); true }
                    AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> { scrubBy(1); true }
                    AndroidKeyEvent.KEYCODE_MEDIA_REWIND -> { scrubBy(-1); true }
                    AndroidKeyEvent.KEYCODE_DPAD_LEFT, AndroidKeyEvent.KEYCODE_DPAD_RIGHT ->
                        // Hidden: start scrubbing on the seek bar. Visible: normal focus movement.
                        if (!controlsShown && state.error == null) { scrubBy(if (code == AndroidKeyEvent.KEYCODE_DPAD_LEFT) -1 else 1); true } else { interaction++; false }
                    AndroidKeyEvent.KEYCODE_DPAD_CENTER, AndroidKeyEvent.KEYCODE_ENTER,
                    AndroidKeyEvent.KEYCODE_DPAD_UP, AndroidKeyEvent.KEYCODE_DPAD_DOWN ->
                        if (!controlsShown && state.error == null && state.countdown == null && state.stalledSuggestion == null) { poke(); true } else { interaction++; false }
                    else -> { interaction++; false }
                }
            }
            .then(
                if (tv) Modifier else Modifier
                    .pointerInput(locked) {
                        detectTapGestures(
                            onTap = { if (locked) poke() else if (controlsVisible) controlsVisible = false else poke() },
                            onDoubleTap = { offset -> if (!locked) { viewModel.seekBy(if (offset.x < size.width / 2) -10_000 else 10_000); poke() } },
                        )
                    }
                    .pointerInput(locked) {
                        if (locked) return@pointerInput
                        detectTransformGestures { _, _, zoom, _ ->
                            val next = if (zoom > 1.05f) AspectMode.ZOOM else if (zoom < 0.95f) AspectMode.FIT else aspect
                            if (next != aspect) { aspect = next; aspectFlash = true }
                        }
                    }
                    .pointerInput(locked) {
                        if (locked) return@pointerInput
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
        ContentFrame(player = viewModel.player, modifier = Modifier.fillMaxSize(), contentScale = aspect.scale)
        SubtitleOverlay(viewModel.player, Modifier.fillMaxSize())

        // Pre-roll (PL-3): until the first frame, show the backdrop and live swarm stats.
        if (state.firstFrameMs == null && state.phase != PlayerPhase.ERROR) PreRoll(state, onCancel = onExit)

        // Mid-playback buffering spinner.
        if (state.firstFrameMs != null && state.phase == PlayerPhase.PLAYING && !state.isPlaying && state.stats?.state == StreamState.BUFFERING) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = JazzyColors.Accent)
        }

        AnimatedVisibility(controlsShown, enter = fadeIn(), exit = fadeOut()) {
            PlayerControls(
                state, viewModel, aspect, scrubMs, playFocus, seekFocus,
                ControlsActions(
                    onExit = onExit,
                    onAspect = { aspect = aspect.next(); aspectFlash = true; interaction++ },
                    onToggleStats = { showStats = !showStats; interaction++ },
                    onLock = { locked = true; controlsVisible = false; unlockVisible = true },
                    onScrubBy = ::scrubBy,
                    onScrubTo = { ms -> scrubMs = ms; interaction++ },
                    onScrubCommit = ::commitScrub,
                    onInteract = { interaction++ },
                ),
            )
        }

        if (locked && unlockVisible) {
            LockedOverlay(onUnlock = { locked = false; unlockVisible = false; poke() }, modifier = Modifier.align(Alignment.BottomStart))
        }

        if (aspectFlash) {
            Text(
                stringResource(aspect.label),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.Center).background(JazzyColors.Scrim, RoundedCornerShape(8.dp)).padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        if (showStats) StatsOverlay(state, Modifier.align(Alignment.TopEnd).padding(top = if (tv) 96.dp else 72.dp, end = 16.dp))

        state.stalledSuggestion?.let { next ->
            Prompt(
                text = stringResource(R.string.player_stall_prompt, next.release.qualityLabel, formatCount(next.seeders)),
                primary = stringResource(R.string.player_stall_try) to { viewModel.tryNextSource() },
                secondary = stringResource(R.string.player_stall_keep_waiting) to { viewModel.dismissStall() },
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }

        state.countdown?.let { c ->
            Prompt(
                text = c.title?.let { stringResource(R.string.player_next_episode_in_titled, c.secondsLeft, c.playable.label.toString(), it) }
                    ?: stringResource(R.string.player_next_episode_in, c.secondsLeft, c.playable.label.toString()),
                primary = stringResource(R.string.player_next_play_now) to { viewModel.playNextNow() },
                secondary = stringResource(R.string.player_common_cancel) to { viewModel.cancelCountdown() },
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }

        state.error?.let { err ->
            if (state.panel == PlayerPanel.NONE) {
                val errorFocus = remember { FocusRequester() }
                LaunchedEffect(err) { withFrameNanos { }; runCatching { errorFocus.requestFocus() } }
                Column(
                    Modifier.align(Alignment.Center).widthIn(max = 520.dp).background(JazzyColors.Scrim, RoundedCornerShape(12.dp)).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(err.message.asString(), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (err.canTryNext) {
                            Button(onClick = { viewModel.openPanel(PlayerPanel.SOURCES) }, modifier = Modifier.focusRequester(errorFocus).focusRing()) { Text(stringResource(R.string.player_error_choose_source)) }
                            OutlinedButton(onClick = viewModel::retry, modifier = Modifier.focusRing()) { Text(stringResource(R.string.player_common_retry)) }
                        } else {
                            OutlinedButton(onClick = viewModel::retry, modifier = Modifier.focusRequester(errorFocus).focusRing()) { Text(stringResource(R.string.player_common_retry)) }
                        }
                        TextButton(onClick = onExit, modifier = Modifier.focusRing()) { Text(stringResource(R.string.player_common_back)) }
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

private const val CONTROLS_TIMEOUT_MS = 5_000L
private const val SCRUB_COMMIT_MS = 900L

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
            val cancelFocus = remember { FocusRequester() }
            LaunchedEffect(Unit) { withFrameNanos { }; runCatching { cancelFocus.requestFocus() } }
            OutlinedButton(onClick = onCancel, modifier = Modifier.focusRequester(cancelFocus).focusRing()) { Text(stringResource(R.string.player_common_cancel)) }
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

/** A prompt over the video (stalled source, next episode). Takes focus on arrival so the remote can answer it. */
@Composable
private fun Prompt(text: String, primary: Pair<String, () -> Unit>, secondary: Pair<String, () -> Unit>, modifier: Modifier) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { withFrameNanos { }; runCatching { focus.requestFocus() } }
    Row(
        modifier.padding(24.dp).background(JazzyColors.SurfaceRaised, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.widthIn(max = 420.dp))
        Button(onClick = primary.second, modifier = Modifier.focusRequester(focus).focusRing()) { Text(primary.first) }
        TextButton(onClick = secondary.second, modifier = Modifier.focusRing()) { Text(secondary.first) }
    }
}
