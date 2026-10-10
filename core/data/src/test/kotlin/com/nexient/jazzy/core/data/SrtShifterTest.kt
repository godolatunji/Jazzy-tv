package com.nexient.jazzy.core.data

import com.google.common.truth.Truth.assertThat
import com.nexient.jazzy.core.data.subtitles.SrtShifter
import org.junit.Test

class SrtShifterTest {
    private val srt = """
        1
        00:00:01,500 --> 00:00:03,000
        Hello --> world

        2
        01:59:59,900 --> 02:00:00,100
        Bye
    """.trimIndent()

    @Test
    fun shiftsForwardAcrossHourBoundaries() {
        val out = SrtShifter.shift(srt, 250)
        assertThat(out).contains("00:00:01,750 --> 00:00:03,250")
        assertThat(out).contains("02:00:00,150 --> 02:00:00,350")
    }

    @Test
    fun negativeShiftClampsAtZero() {
        assertThat(SrtShifter.shift(srt, -2_000)).contains("00:00:00,000 --> 00:00:01,000")
    }

    @Test
    fun textLinesAreLeftAlone() {
        assertThat(SrtShifter.shift(srt, 1_000).lines()).contains("Bye")
    }
}
