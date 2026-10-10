package com.nexient.jazzy.core.torrent

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PieceSchedulerTest {
    private val mb = 1024L * 1024

    private class FakeController : PieceController {
        val priorities = mutableMapOf<Int, Int>()
        val deadlines = mutableMapOf<Int, Int>()
        val resets = mutableListOf<Int>()
        override fun setPriority(piece: Int, priority: Int) { priorities[piece] = priority }
        override fun setDeadline(piece: Int, deadlineMs: Int) { deadlines[piece] = deadlineMs }
        override fun resetDeadline(piece: Int) { deadlines.remove(piece); resets += piece }
    }

    // 1000 MB file, 1 MB pieces, starting at piece 0. Bitrate 500 kB/s → 20 MB window = 20 pieces.
    private val span = FileSpan(pieceLength = mb.toInt(), fileOffset = 0, fileSize = 1000 * mb)
    private val have = mutableSetOf<Int>()
    private val controller = FakeController()
    private val scheduler = PieceScheduler(span, controller, { it in have }, ReadAheadPolicy(), bitrateBps = 500_000)

    @Test fun `start prioritises head and tail, graded from each edge, edges filling together`() {
        // 1 MB pieces, bitrate 500 kB/s: top grade = 2 pieces, top+high = 3 pieces (2.5 MB).
        have += 1
        scheduler.start()
        assertThat(controller.priorities[0]).isEqualTo(PieceScheduler.PRIORITY_TOP)
        assertThat(controller.priorities[999]).isEqualTo(PieceScheduler.PRIORITY_TOP)
        assertThat(controller.priorities[998]).isEqualTo(PieceScheduler.PRIORITY_TOP)
        assertThat(controller.priorities[2]).isEqualTo(PieceScheduler.PRIORITY_HIGH)
        assertThat(controller.priorities[997]).isEqualTo(PieceScheduler.PRIORITY_HIGH)
        assertThat(controller.priorities[4]).isEqualTo(PieceScheduler.PRIORITY_WINDOW)
        assertThat(controller.priorities[995]).isEqualTo(PieceScheduler.PRIORITY_WINDOW)
        assertThat(controller.priorities).doesNotContainKey(1) // already downloaded
        // Immediate, nearest-first deadlines for the upper grades only; tail backwards.
        assertThat(controller.deadlines.keys).containsExactly(0, 999, 998, 2, 997)
        assertThat(controller.deadlines.getValue(0)).isLessThan(controller.deadlines.getValue(999))
        assertThat(controller.deadlines.getValue(999)).isLessThan(controller.deadlines.getValue(998))
        assertThat(controller.deadlines.getValue(998)).isLessThan(controller.deadlines.getValue(2))
        assertThat(controller.deadlines.values.max()).isLessThan(1_000)
    }

    @Test fun `first read opens a window with increasing deadlines`() {
        scheduler.onRead(0, downloadRateBps = 0)
        assertThat(scheduler.currentWindow).isEqualTo(0..19)
        val d = (0..19).map { controller.deadlines.getValue(it) }
        assertThat(d).isInOrder()
        assertThat(d.first()).isEqualTo(0)
        assertThat(controller.deadlines).doesNotContainKey(20)
    }

    @Test fun `sequential reads slide the window and only add new pieces`() {
        scheduler.onRead(0, 0)
        controller.deadlines.clear()
        have += 0..4
        scheduler.onRead(5 * mb, 0)
        assertThat(scheduler.currentWindow).isEqualTo(5..24)
        assertThat(controller.deadlines.keys).containsExactly(5, 20, 21, 22, 23, 24)
        assertThat(controller.deadlines[5]).isEqualTo(0)
        assertThat(scheduler.seekCount).isEqualTo(0)
    }

    @Test fun `reads inside the same piece do nothing`() {
        scheduler.onRead(0, 0)
        controller.deadlines.clear()
        assertThat(scheduler.onRead(1000, 0)).isFalse()
        assertThat(controller.deadlines).isEmpty()
    }

    @Test fun `seek re-prioritises from the new position and lowers the old window`() {
        scheduler.start()
        scheduler.onRead(0, 0)
        have += 0..2
        controller.resets.clear()
        scheduler.onRead(600 * mb, 0)
        assertThat(scheduler.seekCount).isEqualTo(1)
        assertThat(scheduler.currentWindow).isEqualTo(600..619)
        assertThat(controller.deadlines[600]).isEqualTo(0)
        assertThat(controller.priorities[600]).isEqualTo(PieceScheduler.PRIORITY_TOP)
        // Old window pieces still missing (5..19) lose deadlines and drop to normal; the
        // protected head (0..4) and downloaded pieces are left alone.
        assertThat(controller.resets).containsExactlyElementsIn(5..19)
        (5..19).forEach { assertThat(controller.priorities[it]).isEqualTo(PieceScheduler.PRIORITY_NORMAL) }
        assertThat(controller.priorities[3]).isNotEqualTo(PieceScheduler.PRIORITY_NORMAL) // protected head
    }

    @Test fun `window priorities are graded by distance and upgraded as the reader advances`() {
        // 1 MB pieces, bitrate 500 kB/s: top grade = 2 pieces, top+high = 3 pieces.
        scheduler.onRead(100 * mb, 0)
        assertThat((100..103).map { controller.priorities[it] }).containsExactly(7, 7, 6, 5).inOrder()
        assertThat(controller.priorities[110]).isEqualTo(PieceScheduler.PRIORITY_WINDOW)
        have += 100..101
        scheduler.onRead(102 * mb, 0)
        assertThat((102..105).map { controller.priorities[it] }).containsExactly(7, 7, 6, 5).inOrder()
    }

    @Test fun `backward seek is also a seek`() {
        scheduler.onRead(600 * mb, 0)
        scheduler.onRead(100 * mb, 0)
        assertThat(scheduler.seekCount).isEqualTo(1)
        assertThat(scheduler.currentWindow.first).isEqualTo(100)
    }

    @Test fun `window grows with the download rate`() {
        scheduler.onRead(0, 0)
        assertThat(scheduler.currentWindow).isEqualTo(0..19)
        scheduler.refresh(downloadRateBps = 2 * mb) // 30 s × 2 MB/s = 60 pieces
        assertThat(scheduler.currentWindow).isEqualTo(0..59)
        assertThat(controller.deadlines).containsKey(59)
        assertThat(controller.deadlines.getValue(59)).isGreaterThan(controller.deadlines.getValue(19))
    }

    @Test fun `window is clamped to the end of the file`() {
        scheduler.onRead(990 * mb, 0)
        assertThat(scheduler.currentWindow).isEqualTo(990..999)
    }

    @Test fun `reassert re-applies missing window pieces`() {
        scheduler.onRead(0, 0)
        have += 0..9
        controller.deadlines.clear()
        scheduler.reassert()
        assertThat(controller.deadlines.keys.filter { it < 20 }).containsExactlyElementsIn(10..19)
    }
}

class StallDetectorTest {
    @Test fun `stalls after 20 s of slow download with a low buffer`() {
        val d = StallDetector()
        val bitrate = 1_000_000L
        assertThat(d.update(0, 500_000, bitrate, 1_000_000, false)).isFalse()
        assertThat(d.update(19_000, 500_000, bitrate, 1_000_000, false)).isFalse()
        assertThat(d.update(20_000, 500_000, bitrate, 1_000_000, false)).isTrue()
    }

    @Test fun `recovers when the rate or the buffer is healthy`() {
        val d = StallDetector()
        val bitrate = 1_000_000L
        d.update(0, 0, bitrate, 0, false)
        assertThat(d.update(25_000, 0, bitrate, 0, false)).isTrue()
        assertThat(d.update(26_000, 2_000_000, bitrate, 0, false)).isFalse()
        assertThat(d.update(30_000, 0, bitrate, 0, false)).isFalse() // timer restarted
        assertThat(d.update(31_000, 0, bitrate, 10_000_000, false)).isFalse() // 10 s buffered
        assertThat(d.update(60_000, 0, bitrate, 0, true)).isFalse() // complete
    }
}

class BufferHealthTest {
    private val bitrate = 1_000_000L

    @Test fun `player buffer is added to what is on disk`() {
        assertThat(BufferHealth.bufferedAheadBytes(2_000_000, playerBufferMs = 30_000, bitrateBps = bitrate)).isEqualTo(32_000_000)
        assertThat(BufferHealth.bufferedAheadBytes(2_000_000, playerBufferMs = null, bitrateBps = bitrate)).isEqualTo(2_000_000)
    }

    @Test fun `without a hint only a starving reader counts as low buffer`() {
        val reading = BufferHealth.effectiveBufferBytes(0, playerBufferMs = null, readerWaitingMs = 200)
        assertThat(BufferHealth.isHealthy(reading, bitrate, reachedEndOfFile = false)).isTrue()
        val starving = BufferHealth.effectiveBufferBytes(0, playerBufferMs = null, readerWaitingMs = 3_000)
        assertThat(BufferHealth.isHealthy(starving, bitrate, reachedEndOfFile = false)).isFalse()
        assertThat(BufferHealth.isHealthy(starving, bitrate, reachedEndOfFile = true)).isTrue()
    }

    @Test fun `with a hint the real buffer decides`() {
        val low = BufferHealth.effectiveBufferBytes(BufferHealth.bufferedAheadBytes(0, 2_000, bitrate), 2_000, readerWaitingMs = 0)
        assertThat(BufferHealth.isHealthy(low, bitrate, reachedEndOfFile = false)).isFalse()
        val ok = BufferHealth.effectiveBufferBytes(BufferHealth.bufferedAheadBytes(0, 20_000, bitrate), 20_000, readerWaitingMs = 5_000)
        assertThat(BufferHealth.isHealthy(ok, bitrate, reachedEndOfFile = false)).isTrue()
    }
}
