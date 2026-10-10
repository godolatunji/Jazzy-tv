package com.nexient.jazzy.core.torrent

/** Native side of the scheduler (libtorrent's piece priorities and deadlines). Faked in tests. */
interface PieceController {
    /** [priority] in libtorrent's 0..7 scale. */
    fun setPriority(piece: Int, priority: Int)

    /** Ask for [piece] within [deadlineMs] from now (time-critical picking). */
    fun setDeadline(piece: Int, deadlineMs: Int)

    fun resetDeadline(piece: Int)
}

/**
 * Playback-aware piece scheduler (TE-4, TE-5).
 *
 * - [start]: the first and last ~5 MB of the file get top priority and immediate, nearest-first
 *   deadlines (container indexes such as MP4 `moov` and MKV cues are often at the end).
 * - [onRead]: every read reports its position. A read inside (or just past) the current window
 *   slides it forward: newly covered pieces get deadlines `distance × step`, where `step` is the
 *   playback time of one piece. A read elsewhere is a seek: the old window's missing pieces lose
 *   their deadlines and drop back to normal priority, and a new window starts at the read.
 * - Window priorities are graded by distance from the reader: the next ~1 MB at 7, up to ~5 s
 *   of playback at 6, the rest of the window at 5, background 4. The file edges are graded the
 *   same way from each end, and only their 7/6 grades get deadlines. libtorrent's regular picker is
 *   rarest-first within a level, so a flat 7 over 20+ MB would spread bandwidth over the window.
 * - [refresh]: called periodically with the download rate so the window grows when the swarm
 *   is faster than the stream.
 * - Everything else in the file stays at normal priority (set through the file priority).
 *
 * The window is `max(20 MB, 30 s × bitrate)`, or 30 s of download when that is larger, capped by
 * [ReadAheadPolicy]. All methods are thread-safe.
 */
class PieceScheduler(
    private val span: FileSpan,
    private val controller: PieceController,
    private val have: (Int) -> Boolean,
    private val policy: ReadAheadPolicy = ReadAheadPolicy(),
    @Volatile var bitrateBps: Long,
) {
    private var window: IntRange = IntRange.EMPTY
    private var readPiece = -1
    private var protectedPieces: Set<Int> = emptySet()

    /** Seeks detected so far (for diagnostics). */
    var seekCount: Int = 0
        private set

    val currentWindow: IntRange @Synchronized get() = window

    @Synchronized
    fun start() {
        val head = span.headPieces(policy.headBytes)
        val tail = span.tailPieces(policy.tailBytes)
        protectedPieces = (head + tail).toSet()
        applyEdges(head, tail, baseDeadlineMs = 0)
    }

    /**
     * Head ascending and tail descending, interleaved: both file edges fill at the same time.
     * ExoPlayer needs the tail early for MP4 `moov`-at-end and MKV cues (the Matroska extractor
     * seeks to the cues before playing); they sit at the very end, so the tail is fetched backwards.
     * Priorities are graded by distance from the edge so the first bytes needed win.
     */
    private fun applyEdges(head: IntRange, tail: IntRange, baseDeadlineMs: Int) {
        val h = head.toList()
        val t = tail.reversed().filter { it !in head }
        var k = 0
        for (i in 0 until maxOf(h.size, t.size)) {
            for (p in listOfNotNull(h.getOrNull(i), t.getOrNull(i))) {
                if (have(p)) continue
                val priority = gradedPriority(i)
                controller.setPriority(p, priority)
                // Deadlines only where they matter: the rest of the edges waits behind playback.
                if (priority > PRIORITY_WINDOW) controller.setDeadline(p, baseDeadlineMs + k++ * IMMEDIATE_STEP_MS)
            }
        }
    }

    /** @return true when the window moved (slide or seek). */
    @Synchronized
    fun onRead(position: Long, downloadRateBps: Long): Boolean {
        val piece = span.pieceAt(position)
        if (piece == readPiece && !window.isEmpty()) return false
        readPiece = piece
        val end = windowEnd(piece, downloadRateBps)
        val current = window
        if (current.isEmpty() || piece < current.first || piece > current.last + 1) {
            seekTo(piece, end, current)
        } else {
            slideTo(piece, end, current)
        }
        return true
    }

    /** Grows the window when the download rate allows. */
    @Synchronized
    fun refresh(downloadRateBps: Long) {
        val current = window
        if (current.isEmpty() || readPiece < 0) return
        val end = windowEnd(readPiece, downloadRateBps)
        if (end > current.last) slideTo(readPiece, end, current)
    }

    /**
     * Re-applies priorities and deadlines for pieces still missing: the window once playback
     * reads, the file edges before that. libtorrent re-derives piece priorities from file
     * priorities when its asynchronous file-priority job completes, which can silently undo
     * earlier piece priorities.
     */
    @Synchronized
    fun reassert() {
        val current = window
        if (current.isEmpty()) {
            applyEdges(span.headPieces(policy.headBytes), span.tailPieces(policy.tailBytes), baseDeadlineMs = 0)
            return
        }
        applyWindow(current, origin = readPiece.coerceIn(current.first, current.last))
    }

    private fun windowEnd(piece: Int, downloadRateBps: Long): Int {
        val pieces = policy.windowPieces(span.pieceLength, bitrateBps, downloadRateBps)
        return minOf(span.lastPiece, piece + pieces - 1)
    }

    private fun seekTo(piece: Int, end: Int, old: IntRange) {
        if (!old.isEmpty()) {
            seekCount++
            for (p in old) {
                if (p in piece..end || p in protectedPieces || have(p)) continue
                controller.resetDeadline(p)
                controller.setPriority(p, PRIORITY_NORMAL)
            }
        }
        window = piece..end
        applyWindow(piece..end, origin = piece)
    }

    private fun slideTo(piece: Int, end: Int, old: IntRange) {
        val newEnd = maxOf(end, old.last)
        window = piece..newEnd
        if (newEnd > old.last) applyWindow((old.last + 1)..newEnd, origin = piece)
        // Pieces now closer to the playhead move up a priority grade.
        for (p in piece..minOf(old.last, piece + highPieces() - 1)) {
            if (!have(p)) controller.setPriority(p, gradedPriority(p - piece))
        }
        // The piece being read must never wait behind others.
        if (!have(piece)) controller.setDeadline(piece, 0)
    }

    private fun applyWindow(range: IntRange, origin: Int) {
        val step = policy.deadlineStepMs(span.pieceLength, bitrateBps)
        for (p in range) {
            if (have(p)) continue
            controller.setPriority(p, gradedPriority(p - origin))
            controller.setDeadline(p, maxOf(0, p - origin) * step)
        }
    }

    /** Top grade: the next ~[ReadAheadPolicy.topBytes] (2..8 pieces), what the reader needs now. */
    private fun topPieces(): Int = ceilDiv(policy.topBytes, span.pieceLength).coerceIn(2, MAX_TOP_PIECES)

    /**
     * Top and high grades together: about [ReadAheadPolicy.urgentSeconds] of playback (at least
     * [ReadAheadPolicy.urgentMinBytes]). libtorrent's regular picker is rarest-first within a
     * priority level, so the upper levels stay small and the picker fetches what is needed next.
     */
    private fun highPieces(): Int {
        val bytes = maxOf(policy.urgentMinBytes, bitrateBps * policy.urgentSeconds)
        return ceilDiv(bytes, span.pieceLength).coerceIn(topPieces() + 1, MAX_HIGH_PIECES)
    }

    private fun gradedPriority(distance: Int): Int = when {
        distance < topPieces() -> PRIORITY_TOP
        distance < highPieces() -> PRIORITY_HIGH
        else -> PRIORITY_WINDOW
    }

    private fun ceilDiv(bytes: Long, pieceLength: Int): Int = ((bytes + pieceLength - 1) / pieceLength).toInt()

    companion object {
        const val PRIORITY_IGNORE = 0
        const val PRIORITY_NORMAL = 4
        const val PRIORITY_WINDOW = 5
        const val PRIORITY_HIGH = 6
        const val PRIORITY_TOP = 7
        const val IMMEDIATE_STEP_MS = 10
        private const val MAX_TOP_PIECES = 8
        private const val MAX_HIGH_PIECES = 64
    }
}

/**
 * TE-7: stalled when the download rate stays below 1.1 × the stream bitrate for 20 s while less
 * than 5 s of playback is buffered. Pure, so it is unit-tested.
 */
class StallDetector(
    private val slowForMs: Long = 20_000,
    private val lowBufferSeconds: Int = 5,
    private val rateFactor: Double = 1.1,
) {
    private var slowSinceMs = -1L

    fun update(nowMs: Long, downloadRateBps: Long, bitrateBps: Long, bufferedAheadBytes: Long, complete: Boolean): Boolean {
        val slow = !complete &&
            downloadRateBps < bitrateBps * rateFactor &&
            bufferedAheadBytes < bitrateBps * lowBufferSeconds
        if (!slow) {
            slowSinceMs = -1
            return false
        }
        if (slowSinceMs < 0) slowSinceMs = nowMs
        return nowMs - slowSinceMs >= slowForMs
    }

    fun reset() {
        slowSinceMs = -1
    }
}

/**
 * Buffer health of a stream, from what the engine can see (contiguous bytes on disk past the
 * reader) plus the player's in-memory buffer when the player reports it. Pure, so it is unit-tested.
 */
object BufferHealth {
    /** Below this many seconds of buffer the stream counts as BUFFERING (and may be STALLED, TE-7). */
    const val LOW_BUFFER_SECONDS = 5

    /** Without a player hint, a read blocked this long means the player is starving. */
    const val READER_STARVING_MS = 1_000L

    /** Bytes buffered ahead of the playhead: on disk past the reader + the player's buffer. */
    fun bufferedAheadBytes(engineAheadBytes: Long, playerBufferMs: Long?, bitrateBps: Long): Long =
        engineAheadBytes + (playerBufferMs?.coerceAtLeast(0)?.let { it * bitrateBps / 1000 } ?: 0L)

    /**
     * Buffer to judge health by. Without a player hint the engine can't see the player's buffer:
     * ExoPlayer reads up to the download frontier, so "nothing on disk past the reader" is normal;
     * only a reader blocked for [READER_STARVING_MS] says the player is waiting on us.
     */
    fun effectiveBufferBytes(bufferedAheadBytes: Long, playerBufferMs: Long?, readerWaitingMs: Long): Long =
        if (playerBufferMs == null && readerWaitingMs < READER_STARVING_MS) Long.MAX_VALUE / 4 else bufferedAheadBytes

    fun isHealthy(effectiveBufferBytes: Long, bitrateBps: Long, reachedEndOfFile: Boolean): Boolean =
        reachedEndOfFile || effectiveBufferBytes >= bitrateBps * LOW_BUFFER_SECONDS
}
