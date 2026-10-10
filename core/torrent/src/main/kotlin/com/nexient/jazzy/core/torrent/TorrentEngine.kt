package com.nexient.jazzy.core.torrent

import android.net.Uri
import com.nexient.jazzy.core.model.EngineSettings
import com.nexient.jazzy.core.model.StreamRequest
import com.nexient.jazzy.core.model.StreamStats
import kotlinx.coroutines.flow.Flow

/**
 * Public entry point of `:core:torrent` (spec §5.9.1). Streams one file of a torrent on demand.
 * Media3 reads it through [TorrentDataSource] using [TorrentStream.uri].
 */
interface TorrentEngine {
    /**
     * Adds the magnet, waits for metadata (30 s timeout), picks the file (TE-3), verifies it
     * matches the request (TE-2) and starts prioritising the head and tail pieces.
     * @throws StreamException
     */
    suspend fun open(request: StreamRequest): TorrentStream

    /** Live stats for an open stream (TE-12). Completes when the stream is released. */
    fun stats(infoHash: String): Flow<StreamStats>

    /** Stops the torrent. Data stays in the cache for LRU eviction unless [deleteData]. */
    suspend fun release(infoHash: String, deleteData: Boolean = false)

    suspend fun cacheSizeBytes(): Long

    suspend fun clearCache()
}

interface TorrentStream {
    val infoHash: String
    val torrentName: String
    val fileIndex: Int
    val fileName: String
    val fileSize: Long
    val mimeType: String?

    /** `jazzytorrent://<infoHash>/<fileIndex>`, resolved by [TorrentDataSource]. */
    val uri: Uri
}

class StreamException(val failure: StreamFailure, message: String? = null, cause: Throwable? = null) :
    Exception(message ?: failure.name, cause)

enum class StreamFailure { METADATA_TIMEOUT, NO_VIDEO_FILE, TITLE_MISMATCH, NETWORK_NOT_ALLOWED, ENGINE_ERROR }

/** Engine settings, implemented by `:core:data` on top of jazzy.db. */
interface EngineSettingsStore {
    val settings: Flow<EngineSettings>
}

/**
 * Optional, user-configurable list of public trackers appended to every magnet (TE-2).
 * `:core:data` may bind one (`@Binds TrackerListStore`); when nothing is bound the engine
 * falls back to [DefaultTrackers.LIST].
 */
interface TrackerListStore {
    val trackers: Flow<List<String>>
}

/**
 * Byte-level, blocking access to the selected file of an open stream. Used by
 * [TorrentDataSource] and [LoopbackStreamServer]; implemented by the engine.
 */
interface TorrentFileProvider {
    /** Returns null when no open stream has selected [fileIndex] of [infoHash]. */
    fun openFile(infoHash: String, fileIndex: Int): TorrentFileAccess?

    /**
     * Optional playback hint: once the player knows the media duration it can pass it so the
     * scheduler sizes its read-ahead window from the real bitrate instead of an estimate.
     */
    fun setDurationHint(infoHash: String, durationMs: Long) {}

    /**
     * Optional playback hint: how much media the player holds in memory ahead of the playhead
     * (`player.totalBufferedDuration`). ExoPlayer reads right up to the download frontier, so the
     * engine alone only sees what is on disk past the reader; with this hint
     * [com.nexient.jazzy.core.model.StreamStats.bufferedAheadBytes] and the BUFFERING / STREAMING /
     * STALLED states (TE-7) cover the whole buffer. Call it about once a second while playing.
     */
    fun reportPlayerBuffer(infoHash: String, bufferedAheadMs: Long) {}
}

interface TorrentFileAccess : java.io.Closeable {
    val fileSize: Long

    /** Tells the scheduler that the reader jumped to [position] (a seek or a new `open`). */
    fun seek(position: Long)

    /**
     * Blocks until the bytes at [position] are downloaded (TE-6), then reads up to [length] bytes.
     * @return bytes read, or -1 at end of file.
     * @throws java.io.InterruptedIOException when the thread is interrupted or the access is closed.
     * @throws TorrentReadTimeoutException when the piece doesn't arrive within [timeoutMs].
     */
    fun read(position: Long, buffer: ByteArray, offset: Int, length: Int, timeoutMs: Long): Int
}

class TorrentReadTimeoutException(message: String) : java.io.IOException(message)

const val TORRENT_URI_SCHEME = "jazzytorrent"
