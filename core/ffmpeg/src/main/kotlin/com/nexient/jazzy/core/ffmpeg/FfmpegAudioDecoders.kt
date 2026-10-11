package com.nexient.jazzy.core.ffmpeg

import androidx.annotation.OptIn
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.decoder.ffmpeg.FfmpegLibrary

/**
 * What the bundled FFmpeg audio decoder (PL-2) can decode in software. Lets callers such as
 * `DeviceCapabilities` ask without depending on Media3 extension internals.
 *
 * The first access loads the native libraries (a few ms); every probe is safe off-device and
 * returns false when the libraries can't be loaded (e.g. JVM unit tests).
 */
@OptIn(UnstableApi::class)
object FfmpegAudioDecoders {
    /** True when `libffmpegJNI.so` and the FFmpeg shared libraries loaded. */
    val isAvailable: Boolean by lazy { runCatching { FfmpegLibrary.isAvailable() }.getOrDefault(false) }

    /** DTS, DTS-HD HRA/MA (FFmpeg decodes the XLL lossless layer) and DTS Express; DTS:X objects are not rendered. */
    val dts: Boolean by lazy { supports(MimeTypes.AUDIO_DTS) }

    /** Dolby TrueHD (lossless; Atmos metadata is not rendered, the 7.1/5.1 bed is). */
    val trueHd: Boolean by lazy { supports(MimeTypes.AUDIO_TRUEHD) }

    /** libavcodec ident, e.g. "Lavc61.19.101" (FFmpeg 7.1.x), or null when unavailable. */
    val version: String? by lazy { if (isAvailable) runCatching { FfmpegLibrary.getVersion() }.getOrNull() else null }

    /** Whether the bundled FFmpeg has a decoder for [mimeType] (a Media3 `MimeTypes` value). */
    fun supports(mimeType: String): Boolean = isAvailable && runCatching { FfmpegLibrary.supportsFormat(mimeType) }.getOrDefault(false)
}
