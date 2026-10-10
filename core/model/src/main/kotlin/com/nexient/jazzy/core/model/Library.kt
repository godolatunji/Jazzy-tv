package com.nexient.jazzy.core.model

import kotlinx.serialization.Serializable

/** Season/episode value used for movies in keys and tables. */
const val NO_EPISODE = -1

/** Minimal title data kept for anything in a list, history or resume point (spec §7.1). */
@Serializable
data class TitleSnapshot(
    val tmdbId: Int,
    val mediaType: MediaType,
    val title: String,
    val originalTitle: String,
    val year: Int?,
    val posterPath: String?,
    val backdropPath: String?,
    val genreIds: List<Int>,
    val imdbId: String?,
) {
    val ref: TitleRef get() = TitleRef(tmdbId, mediaType)
}

fun TitleSummary.toSnapshot(imdbId: String? = null) = TitleSnapshot(
    tmdbId = tmdbId,
    mediaType = mediaType,
    title = title,
    originalTitle = originalTitle,
    year = year,
    posterPath = posterPath,
    backdropPath = backdropPath,
    genreIds = genreIds,
    imdbId = imdbId,
)

/** An item in My List or Liked. */
data class LibraryItem(val snapshot: TitleSnapshot, val addedAt: Long)

enum class LibrarySort { DATE_ADDED, TITLE, RELEASE_YEAR }

/** A playable unit: a movie, or one episode of a show. */
@Serializable
data class PlayableRef(
    val tmdbId: Int,
    val mediaType: MediaType,
    val season: Int = NO_EPISODE,
    val episode: Int = NO_EPISODE,
) {
    val isEpisode: Boolean get() = mediaType == MediaType.TV && season != NO_EPISODE
    val label: String? get() = if (isEpisode) "S$season:E$episode" else null
}

data class WatchProgress(
    val profileId: Long,
    val playable: PlayableRef,
    val positionMs: Long,
    val durationMs: Long,
    val completed: Boolean,
    val lastInfoHash: String?,
    val lastFileIndex: Int?,
    val lastSourceName: String?,
    val lastQuality: String?,
    val updatedAt: Long,
) {
    val fraction: Float get() = if (durationMs <= 0) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
}

/** A Continue Watching entry: progress plus what to show. */
data class ContinueWatchingItem(
    val snapshot: TitleSnapshot,
    val progress: WatchProgress,
    val episodeTitle: String?,
)

enum class PlaybackEndReason { USER_EXIT, COMPLETED, NEXT_EPISODE, SOURCE_SWITCH, ERROR, STALLED }

data class PlaySession(
    val id: Long,
    val profileId: Long,
    val playable: PlayableRef,
    val episodeTitle: String?,
    val infoHash: String?,
    val fileIndex: Int?,
    val sourceName: String?,
    val quality: String?,
    val providerId: String?,
    val startedAt: Long,
    val endedAt: Long?,
    val startPositionMs: Long,
    val stopPositionMs: Long,
    val durationMs: Long,
    val watchedMs: Long,
    val endReason: PlaybackEndReason?,
)

/** A row on the History screen (sessions of one title on one day are folded together, HS-5). */
data class HistoryEntry(
    val snapshot: TitleSnapshot,
    val latest: PlaySession,
    val sessionCount: Int,
    val totalWatchedMs: Long,
)

/** Thresholds from spec RS-3 / RS-4 / HS-2. */
object WatchRules {
    const val WATCHED_FRACTION = 0.90f
    const val CONTINUE_MIN_MS = 2 * 60 * 1000L
    const val CONTINUE_MIN_FRACTION = 0.03f
    const val CONTINUE_MAX_ITEMS = 20
    const val HISTORY_MIN_WATCHED_MS = 10 * 1000L
    const val PROGRESS_TICK_MS = 10 * 1000L

    fun isWatched(positionMs: Long, durationMs: Long) =
        durationMs > 0 && positionMs.toFloat() / durationMs >= WATCHED_FRACTION

    fun qualifiesForContinue(positionMs: Long, durationMs: Long) =
        positionMs >= CONTINUE_MIN_MS && durationMs > 0 &&
            positionMs.toFloat() / durationMs >= CONTINUE_MIN_FRACTION && !isWatched(positionMs, durationMs)
}
