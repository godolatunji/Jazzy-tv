package com.nexient.jazzy.core.torrent

import android.content.Context
import android.graphics.ImageFormat
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nexient.jazzy.core.model.EngineSettings
import com.nexient.jazzy.core.model.StreamRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Optional
import java.util.concurrent.CountDownLatch

/**
 * Engine spike (DEVELOPMENT_PLAN §10, G1). Streams legal CC-BY Blender films through
 * LibtorrentEngine → TorrentAwareDataSourceFactory → ExoPlayer and logs, with tag `JazzySpike`:
 * metadata time, time to first frame (TTFF) and seek-to-unbuffered latency.
 *
 * Run: `ANDROID_SERIAL=emulator-5554 ./gradlew :core:torrent:connectedDebugAndroidTest`
 * then `adb logcat -d -s JazzySpike`.
 */
@OptIn(UnstableApi::class)
@RunWith(AndroidJUnit4::class)
class TorrentSpikeTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var engine: LibtorrentEngine
    private lateinit var playerThread: HandlerThread
    private lateinit var handler: Handler
    private lateinit var imageReader: ImageReader

    private val settings = object : EngineSettingsStore {
        override val settings = flowOf(EngineSettings(wifiOnly = false, cacheCapBytes = 2L * 1024 * 1024 * 1024))
    }

    @Before fun setUp(): Unit = runBlocking {
        engine = LibtorrentEngine(context, settings, Optional.empty())
        engine.clearCache() // cold numbers: nothing cached from earlier runs
        playerThread = HandlerThread("spike-player").apply { start() }
        handler = Handler(playerThread.looper)
        // A real decoder output surface so onRenderedFirstFrame fires; drained continuously.
        imageReader = ImageReader.newInstance(1280, 720, ImageFormat.PRIVATE, 4).apply {
            setOnImageAvailableListener({ r -> r.acquireLatestImage()?.close() }, handler)
        }
    }

    @After fun tearDown(): Unit = runBlocking {
        engine.shutdown()
        imageReader.close()
        playerThread.quitSafely()
    }

    @Test fun bigBuckBunny(): Unit = spike(
        name = "BigBuckBunny",
        request = StreamRequest(
            magnet = "magnet:?xt=urn:btih:dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c&dn=Big+Buck+Bunny" +
                "&tr=udp%3A%2F%2Fexplodie.org%3A6969&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337" +
                "&ws=https%3A%2F%2Fwebtorrent.io%2Ftorrents%2F",
            infoHash = "dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c",
            expectedTitle = "Big Buck Bunny",
            expectedYear = 2008,
        ),
    )

    @Test fun sintel(): Unit = spike(
        name = "Sintel",
        request = StreamRequest(
            magnet = "magnet:?xt=urn:btih:08ada5a7a6183aae1e09d831df6748d566095a10&dn=Sintel" +
                "&tr=udp%3A%2F%2Fexplodie.org%3A6969&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337" +
                "&ws=https%3A%2F%2Fwebtorrent.io%2Ftorrents%2F",
            infoHash = "08ada5a7a6183aae1e09d831df6748d566095a10",
            expectedTitle = "Sintel",
            expectedYear = 2010,
        ),
    )

    private fun spike(name: String, request: StreamRequest): Unit = runBlocking {
        val hash = requireNotNull(request.infoHash)
        val statsJob = launch(Dispatchers.IO) {
            engine.stats(hash).collect { s ->
                log(
                    "$name stats state=${s.state} down=${s.downloadRateBps / 1024} KiB/s up=${s.uploadRateBps / 1024} KiB/s " +
                        "peers=${s.peers} seeds=${s.seeds} progress=${"%.1f".format(s.fileProgress * 100)}% " +
                        "ahead=${s.bufferedAheadBytes / 1024} KiB",
                )
            }
        }
        val t0 = SystemClock.elapsedRealtime()
        val stream = try {
            engine.open(request)
        } catch (e: StreamException) {
            log("$name RESULT open failed: ${e.failure} ${e.message} after ${SystemClock.elapsedRealtime() - t0} ms")
            statsJob.cancel()
            throw e
        }
        val openMs = SystemClock.elapsedRealtime() - t0
        log("$name metadata+select ${openMs} ms → $stream mime=${stream.mimeType} uri=${stream.uri}")

        val firstFrame = CompletableDeferred<Long>()
        val ready = MutableStateFlow(0) // count of STATE_READY transitions
        val error = CompletableDeferred<PlaybackException>()
        var player: ExoPlayer? = null
        onPlayer {
            val factory = TorrentAwareDataSourceFactory(engine, DefaultDataSource.Factory(context))
            player = ExoPlayer.Builder(context)
                .setLooper(playerThread.looper)
                .setMediaSourceFactory(DefaultMediaSourceFactory(factory))
                .build().apply {
                    setVideoSurface(imageReader.surface)
                    volume = 0f
                    addListener(object : Player.Listener {
                        override fun onRenderedFirstFrame() {
                            val now = SystemClock.elapsedRealtime()
                            if (!firstFrame.isCompleted) firstFrame.complete(now)
                            log("$name rendered first frame at +${now - t0} ms")
                        }

                        override fun onPlaybackStateChanged(state: Int) {
                            log("$name player state=${stateName(state)} at +${SystemClock.elapsedRealtime() - t0} ms pos=${currentPosition}")
                            if (state == Player.STATE_READY) ready.value += 1
                        }

                        override fun onPlayerError(e: PlaybackException) {
                            log("$name player error ${e.errorCodeName}: ${e.message} / ${e.cause}")
                            error.complete(e)
                        }
                    })
                    setMediaItem(MediaItem.Builder().setUri(stream.uri).setMimeType(stream.mimeType).build())
                    prepare()
                    play()
                }
        }
        val p = requireNotNull(player)
        val hintJob = launch(Dispatchers.IO) {
            while (true) {
                onPlayer { engine.reportPlayerBuffer(hash, p.totalBufferedDuration) }
                kotlinx.coroutines.delay(1_000)
            }
        }
        try {
            val frameAt = withTimeout(PLAY_TIMEOUT_MS) {
                kotlinx.coroutines.selects.select {
                    firstFrame.onAwait { it }
                    error.onAwait { throw AssertionError("$name playback failed: ${it.errorCodeName}", it) }
                }
            }
            val ttff = frameAt - t0
            log("$name RESULT metadata_ms=$openMs ttff_ms=$ttff (from open() return: ${frameAt - t0 - openMs} ms)")

            // Play a little, then seek to ~75% (an undownloaded position).
            kotlinx.coroutines.delay(3_000)
            var durationMs = 0L
            onPlayer { durationMs = p.duration; engine.setDurationHint(hash, p.duration) }
            val target = durationMs * 3 / 4
            val readyBefore = ready.value
            val seekStart = SystemClock.elapsedRealtime()
            onPlayer { p.seekTo(target) }
            withTimeout(SEEK_TIMEOUT_MS) { ready.first { it > readyBefore } }
            val seekMs = SystemClock.elapsedRealtime() - seekStart
            var posAfter = 0L
            onPlayer { posAfter = p.currentPosition }
            log("$name RESULT seek_ms=$seekMs target=${target} ms of $durationMs ms (now at $posAfter ms)")
            kotlinx.coroutines.delay(3_000)
            onPlayer { posAfter = p.currentPosition }
            log("$name after seek +3 s position=$posAfter ms")
            assertTrue(ttff > 0)
        } finally {
            hintJob.cancel()
            onPlayer { p.release() }
            statsJob.cancel()
            engine.release(hash, deleteData = true)
        }
    }

    private fun onPlayer(block: () -> Unit) {
        val latch = CountDownLatch(1)
        var failure: Throwable? = null
        handler.post {
            try { block() } catch (t: Throwable) { failure = t } finally { latch.countDown() }
        }
        latch.await()
        failure?.let { throw it }
    }

    private fun stateName(state: Int) = when (state) {
        Player.STATE_IDLE -> "IDLE"
        Player.STATE_BUFFERING -> "BUFFERING"
        Player.STATE_READY -> "READY"
        Player.STATE_ENDED -> "ENDED"
        else -> state.toString()
    }

    private fun log(message: String) {
        Log.i(TAG, message)
    }

    private companion object {
        const val TAG = "JazzySpike"
        const val PLAY_TIMEOUT_MS = 180_000L
        const val SEEK_TIMEOUT_MS = 120_000L
    }
}
