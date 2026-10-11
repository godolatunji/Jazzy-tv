package com.nexient.jazzy.core.ffmpeg

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** On the JVM the Android .so files can't load; the probes must degrade to false, not throw. */
class FfmpegAudioDecodersTest {
    @Test
    fun probesAreSafeWithoutNativeLibraries() {
        assertThat(FfmpegAudioDecoders.isAvailable).isFalse()
        assertThat(FfmpegAudioDecoders.dts).isFalse()
        assertThat(FfmpegAudioDecoders.trueHd).isFalse()
        assertThat(FfmpegAudioDecoders.version).isNull()
        assertThat(FfmpegAudioDecoders.supports("audio/vnd.dts")).isFalse()
    }
}
