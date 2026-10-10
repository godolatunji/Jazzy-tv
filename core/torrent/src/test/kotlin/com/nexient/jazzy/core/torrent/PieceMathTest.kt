package com.nexient.jazzy.core.torrent

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PieceMathTest {
    private val mb = 1024L * 1024

    // File starts 1.5 pieces into the torrent (after another file), 10 MB long, 1 MB pieces.
    private val span = FileSpan(pieceLength = mb.toInt(), fileOffset = mb + mb / 2, fileSize = 10 * mb)

    @Test fun `piece mapping with a file offset`() {
        assertThat(span.firstPiece).isEqualTo(1)
        assertThat(span.lastPiece).isEqualTo(11)
        assertThat(span.pieceAt(0)).isEqualTo(1)
        assertThat(span.pieceAt(mb / 2 - 1)).isEqualTo(1)
        assertThat(span.pieceAt(mb / 2)).isEqualTo(2)
        assertThat(span.pieceAt(10 * mb - 1)).isEqualTo(11)
        assertThat(span.pieceAt(99 * mb)).isEqualTo(11) // clamped
        assertThat(span.pieceStart(1)).isEqualTo(0)
        assertThat(span.pieceEnd(1)).isEqualTo(mb / 2)
        assertThat(span.pieceEnd(11)).isEqualTo(10 * mb)
    }

    @Test fun `head and tail pieces`() {
        assertThat(span.headPieces(2 * mb)).isEqualTo(1..3)
        assertThat(span.tailPieces(2 * mb)).isEqualTo(9..11)
        assertThat(span.headPieces(100 * mb)).isEqualTo(1..11)
    }

    @Test fun `contiguous end stops at the first missing piece`() {
        val have = setOf(2, 3, 4, 6)
        assertThat(span.contiguousEnd(0) { it in have }).isEqualTo(0) // piece 1 missing
        assertThat(span.contiguousEnd(mb) { it in have }).isEqualTo(span.pieceEnd(4))
        assertThat(span.contiguousEnd(10 * mb) { true }).isEqualTo(10 * mb)
        assertThat(span.downloadedBytes { true }).isEqualTo(10 * mb)
        assertThat(span.downloadedBytes { it == 1 }).isEqualTo(mb / 2)
    }

    @Test fun `window is at least 20 MB, else 30 s of bitrate, and grows with rate`() {
        val policy = ReadAheadPolicy()
        assertThat(policy.windowBytes(bitrateBps = 250_000, downloadRateBps = 0)).isEqualTo(20 * mb)
        assertThat(policy.windowBytes(bitrateBps = 2_000_000, downloadRateBps = 0)).isEqualTo(60_000_000)
        // Swarm faster than the stream: 30 s of download.
        assertThat(policy.windowBytes(bitrateBps = 1_000_000, downloadRateBps = 3_000_000)).isEqualTo(90_000_000)
        // Slow swarm doesn't shrink below the bitrate window.
        assertThat(policy.windowBytes(bitrateBps = 1_000_000, downloadRateBps = 100_000)).isEqualTo(30_000_000)
        // Capped.
        assertThat(policy.windowBytes(bitrateBps = 1_000_000, downloadRateBps = 50_000_000)).isEqualTo(256 * mb)
    }

    @Test fun `window pieces and deadline step`() {
        val policy = ReadAheadPolicy()
        assertThat(policy.windowPieces(pieceLength = 2 * mb.toInt(), bitrateBps = 250_000, downloadRateBps = 0)).isEqualTo(10)
        assertThat(policy.windowPieces(pieceLength = 16 * 1024, bitrateBps = 250_000, downloadRateBps = 0)).isEqualTo(400)
        assertThat(policy.deadlineStepMs(pieceLength = 1_000_000, bitrateBps = 1_000_000)).isEqualTo(1_000)
        assertThat(policy.deadlineStepMs(pieceLength = 16_384, bitrateBps = 10_000_000)).isEqualTo(ReadAheadPolicy.MIN_STEP_MS)
        assertThat(policy.deadlineStepMs(pieceLength = 16 * mb.toInt(), bitrateBps = 125_000)).isEqualTo(ReadAheadPolicy.MAX_STEP_MS)
    }

    @Test fun `bitrate estimate`() {
        assertThat(BitrateEstimator.estimate(6_600_000_000, durationMs = null, isEpisode = false)).isEqualTo(1_000_000)
        assertThat(BitrateEstimator.estimate(1_000_000_000, durationMs = 1_000_000, isEpisode = false)).isEqualTo(1_000_000)
        assertThat(BitrateEstimator.estimate(1_000, durationMs = null, isEpisode = true)).isEqualTo(125_000)
    }
}

class FileSpanInteriorTest {
    @Test fun `interior pieces`() {
        // 100-byte pieces; the file covers torrent bytes [150, 450) of a 500-byte torrent.
        val span = FileSpan(pieceLength = 100, fileOffset = 150, fileSize = 300, torrentSize = 500)
        assertThat((span.firstPiece..span.lastPiece).filter(span::isInterior)).containsExactly(2, 3)
        // The last file of the torrent: its final short piece is interior.
        val last = FileSpan(pieceLength = 100, fileOffset = 200, fileSize = 250, torrentSize = 450)
        assertThat(last.isInterior(4)).isTrue()
    }
}
