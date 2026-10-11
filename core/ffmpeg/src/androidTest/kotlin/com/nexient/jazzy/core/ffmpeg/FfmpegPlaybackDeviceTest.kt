package com.nexient.jazzy.core.ffmpeg

import android.os.Handler
import android.os.HandlerThread
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DecoderCounters
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Plays 1 s, 5.1, 48 kHz 440 Hz sine tones (generated with desktop ffmpeg, see build-ffmpeg/README.md)
 * through ExoPlayer configured like `PlayerHolder` and checks FFmpeg decoded every sample. Muted.
 */
@OptIn(UnstableApi::class)
@RunWith(AndroidJUnit4::class)
class FfmpegPlaybackDeviceTest {
    @Test
    fun decodesDts() = assertDecodedByFfmpeg("sine-dts-5.1.mka")

    @Test
    fun decodesTrueHd() = assertDecodedByFfmpeg("sine-truehd-5.1.mka")

    private fun assertDecodedByFfmpeg(asset: String) {
        val context = InstrumentationRegistry.getInstrumentation().context
        val thread = HandlerThread("playback").apply { start() }
        val ended = CountDownLatch(1)
        var error: PlaybackException? = null
        var decoderName: String? = null
        var counters: DecoderCounters? = null
        var player: ExoPlayer? = null
        Handler(thread.looper).post {
            val renderers = DefaultRenderersFactory(context)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            player = ExoPlayer.Builder(context, renderers).setLooper(thread.looper).build().apply {
                volume = 0f
                addAnalyticsListener(object : AnalyticsListener {
                    override fun onAudioDecoderInitialized(
                        eventTime: AnalyticsListener.EventTime,
                        name: String,
                        initializedTimestampMs: Long,
                        initializationDurationMs: Long,
                    ) { decoderName = name }
                })
                val self = this
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_ENDED) {
                            counters = self.audioDecoderCounters?.also { it.ensureUpdated() }
                            ended.countDown()
                        }
                    }
                    override fun onPlayerError(e: PlaybackException) { error = e; ended.countDown() }
                })
                setMediaItem(MediaItem.fromUri("asset:///$asset"))
                prepare()
                play()
            }
        }
        val finished = ended.await(20, TimeUnit.SECONDS)
        val released = CountDownLatch(1)
        Handler(thread.looper).post { player?.release(); released.countDown() }
        released.await(5, TimeUnit.SECONDS)
        thread.quitSafely()

        assertThat(error).isNull()
        assertThat(finished).isTrue()
        assertThat(decoderName).startsWith("ffmpeg")
        val c = checkNotNull(counters)
        assertThat(c.renderedOutputBufferCount).isGreaterThan(0)
        assertThat(c.skippedOutputBufferCount + c.droppedBufferCount).isEqualTo(0)
    }
}
