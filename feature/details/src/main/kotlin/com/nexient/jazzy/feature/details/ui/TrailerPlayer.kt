package com.nexient.jazzy.feature.details.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color as AndroidColor
import android.view.KeyEvent
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nexient.jazzy.core.designsystem.component.focusRing
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.feature.details.R

/** YouTube IFrame player states (developers.google.com/youtube/iframe_api_reference#onStateChange). */
private const val YT_UNSTARTED = -1
private const val YT_ENDED = 0
private const val YT_PLAYING = 1
private const val YT_PAUSED = 2

private const val SEEK_STEP_SECONDS = 10

/**
 * Plays a YouTube trailer inside the app, full screen, through the YouTube IFrame Player API in a WebView,
 * so watching a trailer never hands the user off to the YouTube app or a browser.
 *
 * The page is loaded with `https://<applicationId>` as its base URL, which YouTube reads as the embedding
 * app's identity (the HTTP Referer it requires of WebView embeds; missing → error 153). The WebView Media
 * Integrity API attests the app automatically. TV hides YouTube's own controls (they can't be reached with
 * a D-pad) and maps the remote: OK/play-pause toggles, left/right seek ±10 s, Back closes.
 */
@Composable
internal fun TrailerPlayerDialog(youtubeKey: String, onDismiss: () -> Unit) {
    val tv = JazzyTheme.isTv
    val activity = LocalContext.current as? Activity
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    // Phone: landscape for the trailer, like the movie player; restore on close.
    DisposableEffect(activity) {
        val previous = activity?.requestedOrientation
        if (!tv) activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose { if (!tv) activity?.requestedOrientation = previous ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        DisposableEffect(dialogWindow) {
            val controller = dialogWindow?.let { WindowCompat.getInsetsController(it, it.decorView) }
            controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
            onDispose { }
        }

        var playerState by remember { mutableIntStateOf(YT_UNSTARTED) }
        var started by remember { mutableStateOf(false) }
        var errorCode by remember { mutableStateOf<Int?>(null) }

        Column(Modifier.fillMaxSize().background(Color.Black)) {
            // Phone: the close button gets its own strip so it never covers YouTube's title row.
            if (!tv) {
                Box(Modifier.fillMaxWidth().height(48.dp)) {
                    IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp)) {
                        Icon(Icons.Filled.Close, stringResource(R.string.details_trailer_close), tint = Color.White)
                    }
                }
            }
            Box(Modifier.fillMaxWidth().weight(1f)) {
                if (errorCode == null) {
                    TrailerWebView(
                        youtubeKey = youtubeKey,
                        showYoutubeControls = !tv,
                        onState = { state ->
                            playerState = state
                            if (state == YT_PLAYING) started = true
                            if (state == YT_ENDED) currentOnDismiss()
                        },
                        onError = { errorCode = it },
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (!started) CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
                    if (tv && started && playerState == YT_PAUSED) {
                        Icon(
                            Icons.Filled.PlayArrow, null,
                            Modifier.align(Alignment.Center).size(96.dp).background(Color.Black.copy(alpha = 0.5f), MaterialTheme.shapes.extraLarge),
                            tint = Color.White,
                        )
                    }
                } else {
                    TrailerError(errorCode!!, onDismiss, Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
private fun TrailerError(code: Int, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val message = when (code) {
        101, 150 -> R.string.details_trailer_error_not_embeddable
        100 -> R.string.details_trailer_error_not_found
        else -> R.string.details_trailer_error_generic
    }
    Column(modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(message), color = Color.White, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        OutlinedButton(onClick = onClose, modifier = Modifier.focusRequester(focus).focusRing()) { Text(stringResource(R.string.details_trailer_close)) }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun TrailerWebView(
    youtubeKey: String,
    showYoutubeControls: Boolean,
    onState: (Int) -> Unit,
    onError: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnState by rememberUpdatedState(onState)
    val currentOnError by rememberUpdatedState(onError)
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    // One WebView for the dialog's lifetime: created here, destroyed only when it leaves composition.
    val webView = remember(youtubeKey) {
        TrailerWebView(context).apply {
            // AndroidView defaults to WRAP_CONTENT, which WebView treats as "size to content": the page's
            // 100% heights collapse to 0 and the video is invisible. MATCH_PARENT gives it a real viewport.
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(AndroidColor.BLACK)
            keepScreenOn = true
            isFocusable = true
            isFocusableInTouchMode = true
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false // autoplay with sound
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                // Taps on the YouTube logo / title would navigate away; keep everything in the player.
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = request.isForMainFrame
            }
            addJavascriptInterface(
                object {
                    @JavascriptInterface fun onState(state: Int) { post { currentOnState(state) } }
                    @JavascriptInterface fun onError(code: Int) { post { currentOnError(code) } }
                },
                "Jazzy",
            )
            val origin = "https://${context.packageName}"
            loadDataWithBaseURL("$origin/", playerHtml(youtubeKey, origin, showYoutubeControls), "text/html", "utf-8", null)
        }
    }

    AndroidView(factory = { webView }, modifier = modifier)

    LaunchedEffect(webView) { webView.requestFocus() }

    DisposableEffect(webView) {
        onDispose {
            webView.removeJavascriptInterface("Jazzy")
            webView.loadUrl("about:blank")
            webView.destroy()
        }
    }

    DisposableEffect(lifecycle, webView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> { webView.evaluateJavascript("jazzyPause()", null); webView.onPause() }
                Lifecycle.Event.ON_RESUME -> webView.onResume()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
}

/** Routes remote / media keys to the IFrame player. Back is left alone so the dialog closes. */
private class TrailerWebView(context: Context) : WebView(context) {
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val script = when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> "jazzyToggle()"
            KeyEvent.KEYCODE_MEDIA_PLAY -> "jazzyPlay()"
            KeyEvent.KEYCODE_MEDIA_PAUSE -> "jazzyPause()"
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_REWIND -> "jazzySeek(-$SEEK_STEP_SECONDS)"
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> "jazzySeek($SEEK_STEP_SECONDS)"
            else -> return super.dispatchKeyEvent(event)
        }
        if (event.action == KeyEvent.ACTION_UP) evaluateJavascript(script, null)
        return true
    }
}

private fun playerHtml(youtubeKey: String, origin: String, controls: Boolean): String {
    // TMDB keys are 11-char YouTube IDs; strip anything else so the key can't break out of the JS string.
    val videoId = youtubeKey.filter { it.isLetterOrDigit() || it == '-' || it == '_' }
    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
          <style>
            html, body { margin: 0; padding: 0; width: 100%; height: 100%; background: #000; overflow: hidden; }
            #player { position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: 0; }
          </style>
        </head>
        <body>
          <div id="player"></div>
          <script>
            var tag = document.createElement('script');
            tag.src = 'https://www.youtube.com/iframe_api';
            var first = document.getElementsByTagName('script')[0];
            first.parentNode.insertBefore(tag, first);

            var player;
            function onYouTubeIframeAPIReady() {
              player = new YT.Player('player', {
                width: '100%',
                height: '100%',
                videoId: '$videoId',
                playerVars: {
                  autoplay: 1,
                  playsinline: 1,
                  controls: ${if (controls) 1 else 0},
                  fs: 0,
                  rel: 0,
                  iv_load_policy: 3,
                  enablejsapi: 1,
                  origin: '$origin'
                },
                events: {
                  onReady: function (e) { e.target.playVideo(); },
                  onStateChange: function (e) { Jazzy.onState(e.data); },
                  onError: function (e) { Jazzy.onError(e.data); }
                }
              });
            }
            function jazzyPlay() { if (player && player.playVideo) player.playVideo(); }
            function jazzyPause() { if (player && player.pauseVideo) player.pauseVideo(); }
            function jazzyToggle() {
              if (!player || !player.getPlayerState) return;
              if (player.getPlayerState() === YT.PlayerState.PLAYING) player.pauseVideo(); else player.playVideo();
            }
            function jazzySeek(delta) {
              if (!player || !player.getCurrentTime) return;
              player.seekTo(Math.max(0, player.getCurrentTime() + delta), true);
            }
          </script>
        </body>
        </html>
    """.trimIndent()
}
