package com.nexient.jazzy.core.torrent

import android.os.SystemClock
import android.util.Log
import com.nexient.jazzy.core.model.StreamRequest
import com.nexient.jazzy.core.model.StreamState
import com.nexient.jazzy.core.model.StreamStats
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import org.libtorrent4j.PieceIndexBitfield
import org.libtorrent4j.Priority
import org.libtorrent4j.TorrentHandle
import java.io.File
import java.io.IOException
import java.io.InterruptedIOException
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.BitSet
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/** The file chosen for playback inside an [ActiveStream]. */
internal class Selection(
    val fileIndex: Int,
    val path: String,
    val file: File,
    val span: FileSpan,
    val scheduler: PieceScheduler,
    val mimeType: String?,
    /** v1 SHA-1 of a piece, or null (v2-only torrent). */
    val pieceHash: (Int) -> ByteArray?,
)

/** Runtime state of one open torrent: piece availability, blocking waits, stats. */
internal class ActiveStream(val infoHash: String, @Volatile var request: StreamRequest) {
    @Volatile var handle: TorrentHandle? = null
    @Volatile var selection: Selection? = null
    @Volatile var torrentName: String = ""
    @Volatile var closed = false
    @Volatile var error: String? = null
    @Volatile var downloadRateBps = 0L
    @Volatile var lastReadPosition = 0L
    @Volatile var durationHintMs: Long? = null
    @Volatile var playerBufferMs: Long? = null

    /** When the current blocked read started waiting (elapsed ms), or -1 when no reader waits. */
    @Volatile var waitingSinceMs: Long = -1
    @Volatile var ticker: Job? = null

    /** The magnet's web seeds, held back unless the swarm is too thin (see LibtorrentEngine). */
    @Volatile var webSeeds: List<String> = emptyList()
    @Volatile var webSeedsEnabled = false
    @Volatile var fewPeersSinceMs = -1L

    val metadata = CompletableDeferred<Unit>()
    @Volatile var filePriorities = CompletableDeferred<Unit>()
    val stall = StallDetector()

    /** null once released, which completes [LibtorrentEngine.stats] collectors. */
    val stats = MutableStateFlow<StreamStats?>(
        StreamStats(infoHash, StreamState.FETCHING_METADATA, 0, 0, 0, 0, 0f, 0),
    )

    private val lock = ReentrantLock()
    private val changed = lock.newCondition()
    private val have = BitSet()

    /** Pieces whose bytes were confirmed on disk (see [StreamFileAccess.ensureOnDisk]). */
    private val onDisk = BitSet()
    private var flushesRequested = 0L
    private var flushesCompleted = 0L

    fun isOnDisk(piece: Int): Boolean = lock.withLock { onDisk.get(piece) }

    fun markOnDisk(piece: Int) = lock.withLock { onDisk.set(piece) }

    fun onCacheFlushed() = lock.withLock {
        flushesCompleted++
        changed.signalAll()
    }

    /**
     * Asks libtorrent to flush the torrent's storage and waits for `cache_flushed_alert`.
     * `flush_cache` is a fence job: it runs after every write queued before it.
     * @return false on timeout.
     */
    fun flushAndWait(timeoutMs: Long, isCancelled: () -> Boolean): Boolean {
        val h = handle ?: return false
        val target = lock.withLock { ++flushesRequested }
        h.flushCache()
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
        try {
            lock.lockInterruptibly()
            try {
                while (flushesCompleted < target) {
                    if (closed || isCancelled()) throw InterruptedIOException("Torrent stream closed")
                    val remaining = deadline - System.nanoTime()
                    if (remaining <= 0) return false
                    changed.awaitNanos(minOf(remaining, POLL_NANOS))
                }
                return true
            } finally {
                lock.unlock()
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw InterruptedIOException("Interrupted while flushing").apply { initCause(e) }
        }
    }

    val isEpisode: Boolean get() = request.season != null && request.episode != null

    fun hasPiece(piece: Int): Boolean = lock.withLock { have.get(piece) }

    fun markHave(piece: Int) = lock.withLock {
        have.set(piece)
        changed.signalAll()
    }

    /** Copies libtorrent's bitfield for [span] (catches pieces from a recheck or dropped alerts). */
    fun syncHave(pieces: PieceIndexBitfield, span: FileSpan) {
        val size = pieces.size()
        var added = false
        for (p in span.firstPiece..minOf(span.lastPiece, size - 1)) {
            if (!hasPiece(p) && pieces.getBit(p)) {
                lock.withLock { have.set(p) }
                added = true
            }
        }
        if (added) wake()
    }

    fun contiguousEnd(span: FileSpan, position: Long): Long = lock.withLock { span.contiguousEnd(position) { have.get(it) } }

    fun downloadedBytes(span: FileSpan): Long = lock.withLock { span.downloadedBytes { have.get(it) } }

    fun wake() = lock.withLock { changed.signalAll() }

    fun onRead(selection: Selection, position: Long) {
        lastReadPosition = position
        val seeks = selection.scheduler.seekCount
        if (selection.scheduler.onRead(position, downloadRateBps) && selection.scheduler.seekCount != seeks) {
            Log.i(LibtorrentEngine.TAG, "seek: read at $position → window ${selection.scheduler.currentWindow}")
        }
    }

    /** TE-6: blocks until [piece] is downloaded, the timeout passes, or the reader is cancelled. */
    fun awaitPiece(piece: Int, timeoutMs: Long, isCancelled: () -> Boolean) {
        if (hasPiece(piece)) return
        val start = SystemClock.elapsedRealtime()
        waitingSinceMs = start
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
        try {
            lock.lockInterruptibly()
            try {
                while (!have.get(piece)) {
                    if (closed || isCancelled()) throw InterruptedIOException("Torrent stream closed")
                    error?.let { throw IOException("Torrent error: $it") }
                    val remaining = deadline - System.nanoTime()
                    if (remaining <= 0) throw TorrentReadTimeoutException("Piece $piece of $infoHash not downloaded within $timeoutMs ms")
                    changed.awaitNanos(minOf(remaining, POLL_NANOS))
                }
            } finally {
                lock.unlock()
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw InterruptedIOException("Interrupted while waiting for piece $piece").apply { initCause(e) }
        } finally {
            waitingSinceMs = -1
            val waited = SystemClock.elapsedRealtime() - start
            if (waited >= SLOW_WAIT_LOG_MS) Log.i(LibtorrentEngine.TAG, "read waited $waited ms for piece $piece")
        }
    }

    fun close() {
        closed = true
        ticker?.cancel()
        stats.value = null
        wake()
    }

    private companion object {
        val POLL_NANOS = TimeUnit.MILLISECONDS.toNanos(250)
        const val SLOW_WAIT_LOG_MS = 1_000L
    }
}

private const val HASH_RETRY_MS = 3_000L
private const val HASH_RETRY_SLEEP_MS = 20L
private const val FLUSH_WAIT_MS = 5_000L

/** [TorrentFileAccess] over the selected file of an [ActiveStream]. */
internal class StreamFileAccess(private val stream: ActiveStream, private val selection: Selection) : TorrentFileAccess {
    override val fileSize: Long = selection.span.fileSize

    @Volatile private var closed = false
    private var file: RandomAccessFile? = null

    override fun seek(position: Long) {
        if (!closed) stream.onRead(selection, position.coerceIn(0, fileSize))
    }

    override fun read(position: Long, buffer: ByteArray, offset: Int, length: Int, timeoutMs: Long): Int {
        if (closed) throw InterruptedIOException("Torrent file access closed")
        if (position >= fileSize) return -1
        if (length == 0) return 0
        stream.onRead(selection, position)
        val span = selection.span
        val piece = span.pieceAt(position)
        stream.awaitPiece(piece, timeoutMs) { closed }
        ensureOnDisk(piece, timeoutMs)
        // One piece per read: the next piece gets its own on-disk check.
        val toRead = minOf(length.toLong(), span.pieceEnd(piece) - position).toInt()
        synchronized(this) {
            if (closed) throw InterruptedIOException("Torrent file access closed")
            val raf = openFile()
            raf.seek(position)
            val n = raf.read(buffer, offset, toRead)
            if (n < 0) throw IOException("Unexpected end of ${selection.file} at $position")
            return n
        }
    }

    /**
     * libtorrent reports a piece as finished once its hash passed, which can be before the disk
     * thread has written it out (the hash is computed from the in-memory store buffer). Before the
     * first read of a piece, confirm its bytes are really in the file:
     * - pieces wholly inside this file (v1 torrents): SHA-1 of the bytes on disk must match;
     * - edge pieces shared with neighbouring files, and v2-only torrents: flush and wait.
     */
    private fun ensureOnDisk(piece: Int, timeoutMs: Long) {
        if (stream.isOnDisk(piece)) return
        val span = selection.span
        val expected = if (span.isInterior(piece)) selection.pieceHash(piece) else null
        if (expected != null) {
            val start = span.pieceStart(piece)
            val bytes = ByteArray((span.pieceEnd(piece) - start).toInt())
            val digest = MessageDigest.getInstance("SHA-1")
            val deadline = SystemClock.elapsedRealtime() + minOf(timeoutMs, HASH_RETRY_MS)
            var attempts = 0
            while (true) {
                if (closed) throw InterruptedIOException("Torrent file access closed")
                attempts++
                val read = synchronized(this) {
                    val raf = openFile()
                    raf.seek(start)
                    raf.read(bytes, 0, bytes.size)
                }
                if (read == bytes.size && digest.digest(bytes).contentEquals(expected)) {
                    if (attempts > 1) Log.i(LibtorrentEngine.TAG, "piece $piece reached the disk after $attempts checks")
                    stream.markOnDisk(piece)
                    return
                }
                if (SystemClock.elapsedRealtime() >= deadline) break
                try {
                    Thread.sleep(HASH_RETRY_SLEEP_MS)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw InterruptedIOException("Interrupted").apply { initCause(e) }
                }
            }
            Log.w(LibtorrentEngine.TAG, "piece $piece still doesn't match its hash on disk; flushing")
        }
        if (!stream.flushAndWait(minOf(timeoutMs, FLUSH_WAIT_MS)) { closed }) {
            Log.w(LibtorrentEngine.TAG, "flush for piece $piece timed out; reading anyway")
        }
        stream.markOnDisk(piece)
    }

    private fun openFile(): RandomAccessFile = file ?: RandomAccessFile(selection.file, "r").also { file = it }

    override fun close() {
        closed = true
        stream.wake()
        synchronized(this) {
            runCatching { file?.close() }
            file = null
        }
    }
}

/** Maps [PieceController] onto libtorrent. Each call is an asynchronous post to libtorrent's network thread. */
internal class HandleController(private val handle: TorrentHandle) : PieceController {
    override fun setPriority(piece: Int, priority: Int) = handle.piecePriority(piece, Priority.fromSwig(priority))

    override fun setDeadline(piece: Int, deadlineMs: Int) = handle.setPieceDeadline(piece, deadlineMs)

    override fun resetDeadline(piece: Int) = handle.resetPieceDeadline(piece)
}
