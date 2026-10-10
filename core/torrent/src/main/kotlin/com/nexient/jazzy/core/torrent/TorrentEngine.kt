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

const val TORRENT_URI_SCHEME = "jazzytorrent"
