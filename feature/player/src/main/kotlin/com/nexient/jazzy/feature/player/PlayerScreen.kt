package com.nexient.jazzy.feature.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.os.Build
import android.util.Rational
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.core.model.PlaybackRequest
import com.nexient.jazzy.core.player.PlaybackService
import com.nexient.jazzy.feature.player.ui.PlayerContent

@Composable
fun PlayerRoute(request: PlaybackRequest, onBack: () -> Unit) {
    val viewModel = hiltViewModel<PlayerViewModel, PlayerViewModel.Factory>(
        key = "player:${request.playable}:${request.source.key}",
        creationCallback = { it.create(request) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity
    val tv = JazzyTheme.isTv

    // Connect a MediaController so PlaybackService posts the media notification and stays foreground (PL-11, TE-8).
    DisposableEffect(Unit) {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        onDispose { MediaController.releaseFuture(future) }
    }

    // Phone: landscape, immersive, screen on, auto-enter picture-in-picture (PL-7).
    DisposableEffect(activity) {
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        val previousOrientation = activity?.requestedOrientation
        if (!tv) activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        if (!tv && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            activity?.setPictureInPictureParams(PictureInPictureParams.Builder().setAutoEnterEnabled(true).setAspectRatio(Rational(16, 9)).build())
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            controller?.show(WindowInsetsCompat.Type.systemBars())
            if (!tv) activity?.requestedOrientation = previousOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            if (!tv && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                activity?.setPictureInPictureParams(PictureInPictureParams.Builder().setAutoEnterEnabled(false).build())
            }
        }
    }

    LaunchedEffect(state.finished) { if (state.finished) { viewModel.exit(); onBack() } }

    val exit = { viewModel.exit(); onBack() }
    BackHandler(enabled = state.panel != PlayerPanel.NONE) { viewModel.closePanel() }
    BackHandler(enabled = state.panel == PlayerPanel.NONE) { exit() }

    PlayerContent(state, viewModel, onExit = exit)
}
