package com.nexient.jazzy.core.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.nexient.jazzy.core.torrent.TorrentAwareDataSourceFactory
import com.nexient.jazzy.core.torrent.TorrentFileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the single ExoPlayer instance shared by the player screen and [PlaybackService] (which
 * wraps it in a MediaSession for notification / lock-screen / Google TV "Now Playing").
 */
@OptIn(UnstableApi::class)
@Singleton
class PlayerHolder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val files: TorrentFileProvider,
) {
    private var player: ExoPlayer? = null

    val current: ExoPlayer? get() = player

    fun get(): ExoPlayer = player ?: build().also { player = it }

    fun release() {
        player?.release()
        player = null
    }

    private fun build(): ExoPlayer {
        val upstream = DefaultDataSource.Factory(context)
        val dataSources = TorrentAwareDataSourceFactory(files, upstream)
        // FFmpeg (DTS/TrueHD, PL-2) is a fallback: hardware decoders and HDMI passthrough win when they
        // can handle a format, FFmpeg decodes only what the device can't.
        val renderers = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            .setEnableDecoderFallback(true)
        // Torrent reads can stall briefly; buffer more than the defaults before and during playback.
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(30_000, 120_000, 2_500, 5_000)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(buildUponParameters().setTunnelingEnabled(false))
        }
        return ExoPlayer.Builder(context, renderers)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSources))
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .setHandleAudioBecomingNoisy(true)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
    }
}
