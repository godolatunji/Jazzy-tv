package com.nexient.jazzy.core.model

import kotlinx.serialization.Serializable

/** Everything the player needs to start one playback (passed through navigation). */
@Serializable
data class PlaybackRequest(
    val snapshot: TitleSnapshot,
    val playable: PlayableRef,
    val episodeTitle: String? = null,
    val source: TorrentSource,
    /** Position to start from; 0 = from the start. */
    val startPositionMs: Long = 0,
    /** Show seasons, for the in-player episodes panel (PL-5). */
    val seasonNumbers: List<Int> = emptyList(),
)
