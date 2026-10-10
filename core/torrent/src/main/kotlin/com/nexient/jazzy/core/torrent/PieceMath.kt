package com.nexient.jazzy.core.torrent

import kotlin.math.max
import kotlin.math.min

/**
 * Maps byte offsets of one file to torrent pieces (TE-5: piece = (fileOffset + o) / pieceLength).
 * Pure, so the window math is unit-tested without native code.
 */
data class FileSpan(
    val pieceLength: Int,
    val fileOffset: Long,
    val fileSize: Long,
    /** Total torrent size, so the (shorter) last piece of the torrent is handled. */
    val torrentSize: Long = Long.MAX_VALUE,
) {
    init {
        require(pieceLength > 0) { "pieceLength must be > 0" }
        require(fileSize > 0) { "fileSize must be > 0" }
    }

    val firstPiece: Int = (fileOffset / pieceLength).toInt()
    val lastPiece: Int = ((fileOffset + fileSize - 1) / pieceLength).toInt()
    val pieceCount: Int get() = lastPiece - firstPiece + 1

    /** Piece holding the byte at [position] of the file (clamped to the file). */
    fun pieceAt(position: Long): Int = ((fileOffset + position.coerceIn(0, fileSize - 1)) / pieceLength).toInt()

    /** First file byte covered by [piece]. */
    fun pieceStart(piece: Int): Long = max(0L, piece.toLong() * pieceLength - fileOffset)

    /** File byte just past the end of [piece]. */
    fun pieceEnd(piece: Int): Long = min(fileSize, (piece.toLong() + 1) * pieceLength - fileOffset)

    /** True when [piece] holds only bytes of this file, so its hash can be checked from this file alone. */
    fun isInterior(piece: Int): Boolean {
        val start = piece.toLong() * pieceLength
        val end = minOf(start + pieceLength, torrentSize)
        return start >= fileOffset && end <= fileOffset + fileSize
    }

    /** Pieces covering file bytes `[start, start + length)`. */
    fun piecesFor(start: Long, length: Long): IntRange {
        if (length <= 0 || start >= fileSize) return IntRange.EMPTY
        val end = min(fileSize, start + length) - 1
        return pieceAt(start)..pieceAt(end)
    }

    fun headPieces(bytes: Long): IntRange = piecesFor(0, bytes)

    fun tailPieces(bytes: Long): IntRange = piecesFor(max(0L, fileSize - bytes), bytes)

    /**
     * End (exclusive file offset) of the run of downloaded pieces starting at [position];
     * equals [position] when the piece at [position] isn't downloaded yet.
     */
    fun contiguousEnd(position: Long, have: (Int) -> Boolean): Long {
        if (position >= fileSize) return fileSize
        var piece = pieceAt(position)
        if (!have(piece)) return position
        while (piece < lastPiece && have(piece + 1)) piece++
        return pieceEnd(piece)
    }

    /** Bytes of this file covered by downloaded pieces (TE-12 file progress). */
    fun downloadedBytes(have: (Int) -> Boolean): Long {
        var total = 0L
        for (p in firstPiece..lastPiece) if (have(p)) total += pieceEnd(p) - pieceStart(p)
        return total
    }
}

/** Sizes the read-ahead window (TE-4). All rates are bytes per second. */
data class ReadAheadPolicy(
    val minWindowBytes: Long = 20L * MB,
    val bitrateSeconds: Int = 30,
    /** When the swarm is faster than the stream, the window grows to this many seconds of download. */
    val growthSeconds: Int = 30,
    val maxWindowBytes: Long = 256L * MB,
    val maxWindowPieces: Int = 400,
    val headBytes: Long = 5L * MB,
    val tailBytes: Long = 5L * MB,
    /** Bytes ahead of the reader in the top priority grade. */
    val topBytes: Long = 1L * MB,
    /** Top + high grades: this many seconds of playback ahead of the reader… */
    val urgentSeconds: Int = 5,
    /** …and at least this many bytes. */
    val urgentMinBytes: Long = 2L * MB,
) {
    fun windowBytes(bitrateBps: Long, downloadRateBps: Long): Long {
        val byBitrate = bitrateBps * bitrateSeconds
        val byRate = if (downloadRateBps > bitrateBps) downloadRateBps * growthSeconds else 0L
        return min(maxWindowBytes, maxOf(minWindowBytes, byBitrate, byRate))
    }

    fun windowPieces(pieceLength: Int, bitrateBps: Long, downloadRateBps: Long): Int {
        val bytes = windowBytes(bitrateBps, downloadRateBps)
        return ((bytes + pieceLength - 1) / pieceLength).toInt().coerceIn(1, maxWindowPieces)
    }

    /** Playback time of one piece, used as the deadline step so nearer pieces are due first. */
    fun deadlineStepMs(pieceLength: Int, bitrateBps: Long): Int =
        (pieceLength * 1000L / max(1L, bitrateBps)).coerceIn(MIN_STEP_MS.toLong(), MAX_STEP_MS.toLong()).toInt()

    companion object {
        const val MB = 1024L * 1024
        const val MIN_STEP_MS = 20
        const val MAX_STEP_MS = 3_000
    }
}

object BitrateEstimator {
    private const val MOVIE_SECONDS = 6_600L // ~110 min
    private const val EPISODE_SECONDS = 2_700L // ~45 min
    private const val MIN_BPS = 125_000L // 1 Mbit/s
    private const val MAX_BPS = 12_500_000L // 100 Mbit/s

    /** Bytes per second of the stream: from the real duration when known, else a typical runtime. */
    fun estimate(fileSize: Long, durationMs: Long?, isEpisode: Boolean): Long {
        val raw = if (durationMs != null && durationMs > 0) {
            fileSize * 1000 / durationMs
        } else {
            fileSize / if (isEpisode) EPISODE_SECONDS else MOVIE_SECONDS
        }
        return raw.coerceIn(MIN_BPS, MAX_BPS)
    }
}
