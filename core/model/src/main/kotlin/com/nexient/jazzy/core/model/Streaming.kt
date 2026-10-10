package com.nexient.jazzy.core.model

/** Ask the engine to stream one file of a torrent. */
data class StreamRequest(
    val magnet: String,
    val infoHash: String?,
    /** What we expect inside the torrent, for file selection and bait-and-switch checks (TE-2, TE-3). */
    val expectedTitle: String,
    val expectedYear: Int?,
    val season: Int? = null,
    val episode: Int? = null,
)

data class StreamStats(
    val infoHash: String,
    val state: StreamState,
    val downloadRateBps: Long,
    val uploadRateBps: Long,
    val peers: Int,
    val seeds: Int,
    /** Fraction of the selected file downloaded, 0..1. */
    val fileProgress: Float,
    /** Contiguous downloaded bytes ahead of the current read position. */
    val bufferedAheadBytes: Long,
)

enum class StreamState { FETCHING_METADATA, BUFFERING, STREAMING, STALLED, FINISHED, ERROR }

/** User-tunable engine settings (spec TE-9 – TE-11). */
data class EngineSettings(
    val wifiOnly: Boolean = true,
    val uploadLimitBps: Long = 1_000_000,
    val stopSeedingOnStop: Boolean = true,
    val maxConnections: Int = 200,
    val cacheCapBytes: Long = 4L * 1024 * 1024 * 1024,
    val listenPort: Int = 0,
    val preferEncryption: Boolean = true,
)
