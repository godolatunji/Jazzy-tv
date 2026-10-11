package com.nexient.jazzy.core.ffmpeg

import androidx.annotation.OptIn
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.decoder.ffmpeg.FfmpegLibrary
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Checks the prebuilt native libraries load on a real device/emulator and contain the decoders we
 * ship them for. Run with `./gradlew :core:ffmpeg:connectedDebugAndroidTest`.
 */
@OptIn(UnstableApi::class)
@RunWith(AndroidJUnit4::class)
class FfmpegLibraryDeviceTest {
    @Test
    fun nativeLibrariesLoad() {
        assertThat(FfmpegLibrary.isAvailable()).isTrue()
        // libavcodec 61.19.x is FFmpeg 7.1.x.
        assertThat(FfmpegLibrary.getVersion()).startsWith("Lavc61.19.")
        assertThat(FfmpegAudioDecoders.isAvailable).isTrue()
    }

    @Test
    fun dtsAndTrueHdAreSupported() {
        assertThat(FfmpegLibrary.supportsFormat(MimeTypes.AUDIO_DTS)).isTrue()
        assertThat(FfmpegLibrary.supportsFormat(MimeTypes.AUDIO_DTS_HD)).isTrue()
        assertThat(FfmpegLibrary.supportsFormat(MimeTypes.AUDIO_TRUEHD)).isTrue()
        assertThat(FfmpegAudioDecoders.dts).isTrue()
        assertThat(FfmpegAudioDecoders.trueHd).isTrue()
    }

    @Test
    fun otherBundledDecodersAreSupported() {
        listOf(
            MimeTypes.AUDIO_AC3, MimeTypes.AUDIO_E_AC3, MimeTypes.AUDIO_AAC, MimeTypes.AUDIO_MPEG,
            MimeTypes.AUDIO_FLAC, MimeTypes.AUDIO_ALAC, MimeTypes.AUDIO_OPUS, MimeTypes.AUDIO_VORBIS,
        ).forEach { assertWithMessage(it).that(FfmpegLibrary.supportsFormat(it)).isTrue() }
        // Video decoders are deliberately not built (ExperimentalFfmpegVideoRenderer stays inert).
        assertThat(FfmpegLibrary.supportsFormat(MimeTypes.VIDEO_H264)).isFalse()
    }
}
