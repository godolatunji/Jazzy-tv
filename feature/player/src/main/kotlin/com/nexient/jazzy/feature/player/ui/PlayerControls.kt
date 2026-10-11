package com.nexient.jazzy.feature.player.ui

import android.view.KeyEvent as AndroidKeyEvent
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.nexient.jazzy.core.common.formatDuration
import com.nexient.jazzy.core.designsystem.component.jazzyClickable
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.feature.player.PlayerPanel
import com.nexient.jazzy.feature.player.PlayerUiState
import com.nexient.jazzy.feature.player.PlayerViewModel
import com.nexient.jazzy.feature.player.R
import kotlin.math.roundToInt

/** Video scaling, cycled by the aspect button and pinch on phones. */
internal enum class AspectMode(val scale: ContentScale, @StringRes val label: Int, val icon: ImageVector) {
    FIT(ContentScale.Fit, R.string.player_aspect_fit, Icons.Filled.FitScreen),
    ZOOM(ContentScale.Crop, R.string.player_aspect_zoom, Icons.Filled.ZoomOutMap),
    STRETCH(ContentScale.FillBounds, R.string.player_aspect_stretch, Icons.Filled.AspectRatio);

    fun next(): AspectMode = entries[(ordinal + 1) % entries.size]
}

private val SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

/** The speed button cycles through [SPEEDS] (one press per step, which works the same with a remote). */
internal fun nextSpeed(current: Float): Float = SPEEDS.firstOrNull { it > current + 0.01f } ?: SPEEDS.first()

private fun speedText(speed: Float) = if (speed % 1f == 0f) speed.toInt().toString() else speed.toString()

/** What the controls ask of the screen around them; state that outlives the controls lives there. */
internal class ControlsActions(
    val onExit: () -> Unit,
    val onAspect: () -> Unit,
    val onToggleStats: () -> Unit,
    val onLock: () -> Unit,
    val onScrubBy: (direction: Int) -> Unit,
    val onScrubTo: (positionMs: Long) -> Unit,
    val onScrubCommit: () -> Unit,
    val onInteract: () -> Unit,
)

/**
 * Player controls (inspired by MX Player's layout). TV: everything sits in one bottom panel, a
 * focusable seek bar above a single row of round buttons, each named when focused, so every control
 * is one D-pad move from the next. Phone: top bar with title and actions, a row of quick chips,
 * large centre transport and a seek bar along the bottom.
 */
@Composable
internal fun PlayerControls(
    state: PlayerUiState,
    viewModel: PlayerViewModel,
    aspect: AspectMode,
    scrubMs: Long?,
    playFocus: FocusRequester,
    seekFocus: FocusRequester,
    actions: ControlsActions,
) {
    if (JazzyTheme.isTv) TvControls(state, viewModel, aspect, scrubMs, playFocus, seekFocus, actions)
    else PhoneControls(state, viewModel, aspect, scrubMs, playFocus, seekFocus, actions)
}

@Composable
private fun TvControls(
    state: PlayerUiState,
    viewModel: PlayerViewModel,
    aspect: AspectMode,
    scrubMs: Long?,
    playFocus: FocusRequester,
    seekFocus: FocusRequester,
    actions: ControlsActions,
) {
    val isEpisode = state.request.playable.isEpisode
    val externalPlayer = rememberExternalPlayerLauncher(viewModel)
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)))
                .padding(horizontal = 48.dp, vertical = 27.dp),
        ) {
            Text(state.title, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            state.subtitle?.let { Text(it, style = MaterialTheme.typography.titleMedium, color = JazzyColors.TextSecondary, maxLines = 1) }
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f), Color.Black.copy(alpha = 0.9f))))
                .padding(start = 48.dp, end = 48.dp, top = 48.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SeekRow(state, scrubMs, seekFocus, actions)
            val caption = remember { CaptionState() }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TvButton(caption, Icons.Filled.Replay10, stringResource(R.string.player_controls_back_10), { viewModel.seekBy(-10_000); actions.onInteract() })
                TvButton(caption, 
                    if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    stringResource(if (state.isPlaying) R.string.player_controls_pause else R.string.player_controls_play),
                    { viewModel.togglePlay(); actions.onInteract() },
                    Modifier.focusRequester(playFocus),
                    emphasized = true,
                )
                TvButton(caption, Icons.Filled.Forward10, stringResource(R.string.player_controls_forward_10), { viewModel.seekBy(10_000); actions.onInteract() })
                if (isEpisode) TvButton(caption, Icons.Filled.SkipNext, stringResource(R.string.player_controls_next_episode), viewModel::skipToNextEpisode)
                Spacer(Modifier.weight(1f))
                TvButton(caption, Icons.Filled.Subtitles, stringResource(R.string.player_controls_tracks), { viewModel.openPanel(PlayerPanel.TRACKS) })
                TvButton(caption, Icons.Filled.Speed, stringResource(R.string.player_controls_speed_label, speedText(state.speed)), { viewModel.setSpeed(nextSpeed(state.speed)); actions.onInteract() })
                TvButton(caption, aspect.icon, stringResource(R.string.player_aspect_label, stringResource(aspect.label)), actions.onAspect)
                TvButton(caption, Icons.Filled.SwapHoriz, stringResource(R.string.player_controls_sources), { viewModel.openPanel(PlayerPanel.SOURCES) })
                if (isEpisode) TvButton(caption, Icons.Filled.VideoLibrary, stringResource(R.string.player_controls_episodes), { viewModel.openPanel(PlayerPanel.EPISODES) })
                TvButton(caption, 
                    if (state.liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    stringResource(if (state.liked) R.string.player_controls_unlike else R.string.player_controls_like),
                    { viewModel.toggleLike(); actions.onInteract() },
                    tint = if (state.liked) JazzyColors.Accent else Color.White,
                )
                TvButton(caption, Icons.Filled.Insights, stringResource(R.string.player_controls_stats), actions.onToggleStats)
                TvButton(caption, Icons.Filled.OpenInNew, stringResource(R.string.player_controls_external_player), externalPlayer)
            }
            Caption(caption)
        }
    }
}

/** The name of the focused TV button, drawn once under the row and centred on that button. */
private class CaptionState {
    var label by mutableStateOf<String?>(null)
    var centerX by mutableFloatStateOf(0f)
}

@Composable
private fun Caption(state: CaptionState) {
    var width by remember { mutableIntStateOf(0) }
    var available by remember { mutableIntStateOf(0) }
    Box(Modifier.fillMaxWidth().height(24.dp).onSizeChanged { available = it.width }) {
        val label = state.label ?: return@Box
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            modifier = Modifier
                .onSizeChanged { width = it.width }
                // Centred on the button, but kept on screen for the first and last buttons.
                .offset { IntOffset((state.centerX - width / 2f).roundToInt().coerceIn(0, (available - width).coerceAtLeast(0)), 0) },
        )
    }
}

/** A round TV button that names itself in the shared [Caption] while it has focus. */
@Composable
private fun TvButton(
    caption: CaptionState,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    tint: Color = Color.White,
) {
    var centerX by remember { mutableFloatStateOf(0f) }
    var focused by remember { mutableStateOf(false) }
    // Keep the caption current when the label changes while focused (Play -> Pause, Speed 1x -> 1.25x).
    LaunchedEffect(label, focused) { if (focused) caption.label = label }
    RoundButton(
        icon, label, onClick,
        modifier.onGloballyPositioned { centerX = it.positionInParent().x + it.size.width / 2f },
        size = if (emphasized) 64.dp else 52.dp,
        iconSize = if (emphasized) 40.dp else 28.dp,
        tint = tint,
        emphasized = emphasized,
        onFocused = {
            focused = it
            if (it) { caption.label = label; caption.centerX = centerX } else if (caption.label == label) caption.label = null
        },
    )
}

@Composable
private fun PhoneControls(
    state: PlayerUiState,
    viewModel: PlayerViewModel,
    aspect: AspectMode,
    scrubMs: Long?,
    playFocus: FocusRequester,
    seekFocus: FocusRequester,
    actions: ControlsActions,
) {
    val isEpisode = state.request.playable.isEpisode
    val externalPlayer = rememberExternalPlayerLauncher(viewModel)
    var menu by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.7f))))) {
        Column(Modifier.align(Alignment.TopStart).fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.player_common_back), actions.onExit)
                Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    Text(state.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    state.subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = JazzyColors.TextSecondary, maxLines = 1) }
                }
                RoundButton(Icons.Filled.Subtitles, stringResource(R.string.player_controls_tracks), { viewModel.openPanel(PlayerPanel.TRACKS) })
                RoundButton(Icons.Filled.SwapHoriz, stringResource(R.string.player_controls_sources), { viewModel.openPanel(PlayerPanel.SOURCES) })
                if (isEpisode) RoundButton(Icons.Filled.VideoLibrary, stringResource(R.string.player_controls_episodes), { viewModel.openPanel(PlayerPanel.EPISODES) })
                Box {
                    RoundButton(Icons.Filled.MoreVert, stringResource(R.string.player_controls_more), { menu = true; actions.onInteract() })
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(if (state.liked) R.string.player_controls_unlike else R.string.player_controls_like)) },
                            leadingIcon = { Icon(if (state.liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, null) },
                            onClick = { menu = false; viewModel.toggleLike() },
                        )
                        DropdownMenuItem(text = { Text(stringResource(R.string.player_controls_stats)) }, leadingIcon = { Icon(Icons.Filled.Insights, null) }, onClick = { menu = false; actions.onToggleStats() })
                        DropdownMenuItem(text = { Text(stringResource(R.string.player_controls_external_player)) }, leadingIcon = { Icon(Icons.Filled.OpenInNew, null) }, onClick = { menu = false; externalPlayer() })
                    }
                }
            }
            // Quick chips (MX's second row).
            Row(Modifier.padding(start = 4.dp, top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickChip(stringResource(R.string.player_controls_speed_label, speedText(state.speed)), { viewModel.setSpeed(nextSpeed(state.speed)); actions.onInteract() }) {
                    Text(stringResource(R.string.player_controls_speed_option, speedText(state.speed)), style = MaterialTheme.typography.labelLarge)
                }
                QuickChip(stringResource(R.string.player_aspect_label, stringResource(aspect.label)), actions.onAspect) { Icon(aspect.icon, null, Modifier.size(22.dp)) }
                QuickChip(stringResource(R.string.player_controls_lock), actions.onLock) { Icon(Icons.Filled.Lock, null, Modifier.size(22.dp)) }
            }
        }
        Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundButton(Icons.Filled.Replay10, stringResource(R.string.player_controls_back_10), { viewModel.seekBy(-10_000); actions.onInteract() }, size = 56.dp, iconSize = 36.dp)
            RoundButton(
                if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                stringResource(if (state.isPlaying) R.string.player_controls_pause else R.string.player_controls_play),
                { viewModel.togglePlay(); actions.onInteract() },
                Modifier.focusRequester(playFocus),
                size = 76.dp, iconSize = 52.dp, emphasized = true,
            )
            RoundButton(Icons.Filled.Forward10, stringResource(R.string.player_controls_forward_10), { viewModel.seekBy(10_000); actions.onInteract() }, size = 56.dp, iconSize = 36.dp)
            if (isEpisode) RoundButton(Icons.Filled.SkipNext, stringResource(R.string.player_controls_next_episode), viewModel::skipToNextEpisode, size = 56.dp, iconSize = 36.dp)
        }
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
            SeekRow(state, scrubMs, seekFocus, actions)
        }
    }
}

/** Time, seek bar, duration. The time turns gold and follows the preview while scrubbing. */
@Composable
private fun SeekRow(state: PlayerUiState, scrubMs: Long?, seekFocus: FocusRequester, actions: ControlsActions) {
    val shown = scrubMs ?: state.positionMs
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            formatDuration(shown),
            style = MaterialTheme.typography.labelLarge,
            color = if (scrubMs != null) JazzyColors.Focus else Color.White,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 64.dp),
        )
        SeekBar(
            positionMs = shown,
            bufferedMs = state.bufferedMs,
            durationMs = state.durationMs,
            focusRequester = seekFocus,
            actions = actions,
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
        )
        Text(formatDuration(state.durationMs), style = MaterialTheme.typography.labelLarge, modifier = Modifier.widthIn(min = 64.dp))
    }
}

/**
 * Seek bar for touch and D-pad. Focused, Left/Right move a preview position (the screen commits it
 * once the keys stop, so a long scrub doesn't make the torrent engine chase every step) and Centre
 * commits at once. Touch: tap or drag.
 */
@Composable
private fun SeekBar(
    positionMs: Long,
    bufferedMs: Long,
    durationMs: Long,
    focusRequester: FocusRequester,
    actions: ControlsActions,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val tv = JazzyTheme.isTv
    val description = stringResource(R.string.player_controls_seek)
    fun fraction(ms: Long) = if (durationMs > 0) (ms.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    Box(
        modifier
            .height(if (tv) 32.dp else 36.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .onKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (e.key.nativeKeyCode) {
                    AndroidKeyEvent.KEYCODE_DPAD_LEFT -> { actions.onScrubBy(-1); true }
                    AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> { actions.onScrubBy(1); true }
                    AndroidKeyEvent.KEYCODE_DPAD_CENTER, AndroidKeyEvent.KEYCODE_ENTER -> { actions.onScrubCommit(); true }
                    else -> false
                }
            }
            .focusable()
            .semantics { contentDescription = description }
            .pointerInput(durationMs) {
                detectTapGestures { offset ->
                    actions.onScrubTo((offset.x / size.width * durationMs).toLong())
                    actions.onScrubCommit()
                }
            }
            .pointerInput(durationMs) {
                detectHorizontalDragGestures(onDragEnd = actions.onScrubCommit) { change, _ ->
                    actions.onScrubTo((change.position.x / size.width * durationMs).toLong().coerceIn(0, durationMs))
                }
            }
            .drawBehind {
                val trackHeight = (if (focused) 6.dp else 4.dp).toPx()
                val y = size.height / 2
                val radius = CornerRadius(trackHeight / 2)
                val top = Offset(0f, y - trackHeight / 2)
                drawRoundRect(Color.White.copy(alpha = 0.25f), top, Size(size.width, trackHeight), radius)
                drawRoundRect(Color.White.copy(alpha = 0.45f), top, Size(size.width * fraction(bufferedMs), trackHeight), radius)
                val x = size.width * fraction(positionMs)
                drawRoundRect(JazzyColors.Accent, top, Size(x, trackHeight), radius)
                if (focused) drawCircle(JazzyColors.Focus, radius = 11.dp.toPx(), center = Offset(x, y))
                drawCircle(if (focused) Color.White else JazzyColors.Accent, radius = (if (focused) 8.dp else 7.dp).toPx(), center = Offset(x, y))
            },
    )
}

/** Round icon button on the video: translucent disc, gold ring and lighter fill when focused. */
@Composable
private fun RoundButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 24.dp,
    tint: Color = Color.White,
    emphasized: Boolean = false,
    onFocused: ((Boolean) -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier
            .size(size)
            .jazzyClickable(CircleShape, focusScale = 1.12f, clipContent = true, onFocused = { focused = it; onFocused?.invoke(it) }, onClick = onClick)
            .background(
                when {
                    focused -> Color.White.copy(alpha = 0.28f)
                    emphasized -> Color.White.copy(alpha = 0.16f)
                    else -> Color.Transparent
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, Modifier.size(iconSize), tint = tint)
    }
}

/** MX-style round quick-action chip on phones. */
@Composable
private fun QuickChip(label: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .jazzyClickable(CircleShape, clipContent = true, onClick = onClick)
            .background(Color.Black.copy(alpha = 0.55f))
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Shown alone while the phone screen is locked: one button to unlock. */
@Composable
internal fun LockedOverlay(onUnlock: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.padding(20.dp)) {
        Box(
            Modifier
                .size(52.dp)
                .jazzyClickable(CircleShape, clipContent = true, onClick = onUnlock)
                .background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Lock, stringResource(R.string.player_controls_unlock), Modifier.size(26.dp), tint = Color.White)
        }
    }
}

@Composable
private fun rememberExternalPlayerLauncher(viewModel: PlayerViewModel): () -> Unit {
    val context = LocalContext.current
    val chooserTitle = stringResource(R.string.player_controls_open_with)
    return {
        viewModel.externalPlayerUrl()?.let { url ->
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).setDataAndType(url.toUri(), "video/*")
            runCatching { context.startActivity(android.content.Intent.createChooser(intent, chooserTitle)) }
        }
    }
}
