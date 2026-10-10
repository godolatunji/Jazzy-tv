package com.nexient.jazzy.core.data.device

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.view.Display
import com.nexient.jazzy.core.model.HdrFormat
import com.nexient.jazzy.core.model.Resolution
import com.nexient.jazzy.core.model.TorrentSource
import com.nexient.jazzy.core.model.VideoCodec
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What this device can decode, for the "may not play on this device" badge (SL-4, E8-06).
 * Probed once from `MediaCodecList`; audio formats the player can decode in software (FFmpeg
 * extension, PL-2) are treated as supported when [softwareAudio] is true.
 */
@Singleton
class DeviceCapabilities @Inject constructor(@ApplicationContext private val context: Context) {
    private val decoders: List<MediaCodecInfo> by lazy {
        MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.filter { !it.isEncoder }
    }

    private fun hasDecoder(mime: String) = decoders.any { info -> info.supportedTypes.any { it.equals(mime, ignoreCase = true) } }

    val hevc: Boolean by lazy { hasDecoder(MediaFormat.MIMETYPE_VIDEO_HEVC) }
    val av1: Boolean by lazy { Build.VERSION.SDK_INT >= 29 && hasDecoder(MediaFormat.MIMETYPE_VIDEO_AV1) }
    val dolbyVision: Boolean by lazy { hasDecoder(MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION) }
    val dts: Boolean by lazy { hasDecoder("audio/vnd.dts") || passthrough(AudioFormat.ENCODING_DTS) }
    val trueHd: Boolean by lazy { hasDecoder("audio/true-hd") || (Build.VERSION.SDK_INT >= 28 && passthrough(AudioFormat.ENCODING_DOLBY_TRUEHD)) }

    val hdr: Boolean by lazy {
        val display = context.getSystemService(android.hardware.display.DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY)
        @Suppress("DEPRECATION")
        display?.hdrCapabilities?.supportedHdrTypes?.isNotEmpty() == true
    }

    /** Largest decodable height for H.264/HEVC, to flag 4K on 1080p-only boxes. */
    val maxHeight: Int by lazy {
        decoders.flatMap { info ->
            info.supportedTypes.filter { it.equals(MediaFormat.MIMETYPE_VIDEO_AVC, true) || it.equals(MediaFormat.MIMETYPE_VIDEO_HEVC, true) }
                .mapNotNull { runCatching { info.getCapabilitiesForType(it).videoCapabilities?.supportedHeights?.upper }.getOrNull() }
        }.maxOrNull() ?: 1080
    }

    var softwareAudio: Boolean = true

    private fun passthrough(encoding: Int): Boolean = runCatching {
        val am = context.getSystemService(AudioManager::class.java)
        if (Build.VERSION.SDK_INT >= 23) {
            am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { d -> d.encodings.contains(encoding) }
        } else false
    }.getOrDefault(false)

    /** True when something in the release name is unlikely to play here. */
    fun mayNotPlay(source: TorrentSource): Boolean {
        val r = source.release
        if (r.videoCodec == VideoCodec.H265 && !hevc) return true
        if (r.videoCodec == VideoCodec.AV1 && !av1) return true
        if (HdrFormat.DOLBY_VISION in r.hdr && r.hdr.size == 1 && !dolbyVision) return true
        if (r.resolution == Resolution.UHD_2160 && maxHeight < 2160) return true
        val audio = r.audio?.lowercase().orEmpty()
        if (!softwareAudio && "dts" in audio && !dts) return true
        if (!softwareAudio && "truehd" in audio && !trueHd) return true
        return false
    }
}
