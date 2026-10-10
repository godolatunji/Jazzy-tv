package com.nexient.jazzy.core.data.library

import com.nexient.jazzy.core.common.ApplicationScope
import com.nexient.jazzy.core.common.Clock
import com.nexient.jazzy.core.common.IoDispatcher
import com.nexient.jazzy.core.database.app.LibraryDao
import com.nexient.jazzy.core.database.app.LikedTitleEntity
import com.nexient.jazzy.core.database.app.MyListItemEntity
import com.nexient.jazzy.core.database.app.PlaySessionDao
import com.nexient.jazzy.core.database.app.PlaySessionEntity
import com.nexient.jazzy.core.database.app.ProgressDao
import com.nexient.jazzy.core.database.app.SnapshotWithTime
import com.nexient.jazzy.core.database.app.TitleSnapshotDao
import com.nexient.jazzy.core.database.app.TitleSnapshotEntity
import com.nexient.jazzy.core.database.app.WatchProgressEntity
import com.nexient.jazzy.core.model.ContinueWatchingItem
import com.nexient.jazzy.core.model.HistoryEntry
import com.nexient.jazzy.core.model.LibraryItem
import com.nexient.jazzy.core.model.MediaType
import com.nexient.jazzy.core.model.PlayableRef
import com.nexient.jazzy.core.model.PlaySession
import com.nexient.jazzy.core.model.PlaybackEndReason
import com.nexient.jazzy.core.model.TitleRef
import com.nexient.jazzy.core.model.TitleSnapshot
import com.nexient.jazzy.core.model.WatchProgress
import com.nexient.jazzy.core.model.WatchRules
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Something changed that affects recommendations (spec §5.7 triggers). */
enum class LibraryEvent { SESSION_ENDED, LIKE_CHANGED, MY_LIST_CHANGED, HISTORY_CLEARED }

/** My List, Liked, resume points, Continue Watching and History (spec §5.6). */
@Singleton
class LibraryRepository @Inject constructor(
    private val libraryDao: LibraryDao,
    private val progressDao: ProgressDao,
    private val sessionDao: PlaySessionDao,
    private val snapshotDao: TitleSnapshotDao,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    private val _events = MutableSharedFlow<LibraryEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<LibraryEvent> = _events

    // ---- Snapshots (LK-2, ML-2) ----

    suspend fun saveSnapshot(snapshot: TitleSnapshot) = withContext(io) {
        snapshotDao.upsert(snapshot.toEntity(clock.nowMillis()))
    }

    // ---- My List ----

    fun isInMyList(profileId: Long, ref: TitleRef): Flow<Boolean> =
        libraryDao.observeInMyList(profileId, ref.tmdbId, ref.mediaType.apiValue)

    fun myList(profileId: Long): Flow<List<LibraryItem>> = libraryDao.observeMyList(profileId).map { it.map(SnapshotWithTime::toItem) }

    fun myListKeys(profileId: Long): Flow<Set<TitleRef>> = libraryDao.observeMyListKeys(profileId).map { it.toRefs() }

    suspend fun setInMyList(profileId: Long, snapshot: TitleSnapshot, inList: Boolean) = withContext(io) {
        if (inList) {
            snapshotDao.upsert(snapshot.toEntity(clock.nowMillis()))
            libraryDao.addToMyList(MyListItemEntity(profileId, snapshot.tmdbId, snapshot.mediaType.apiValue, clock.nowMillis()))
        } else {
            libraryDao.removeFromMyList(profileId, snapshot.tmdbId, snapshot.mediaType.apiValue)
        }
        _events.tryEmit(LibraryEvent.MY_LIST_CHANGED)
    }

    // ---- Liked ----

    fun isLiked(profileId: Long, ref: TitleRef): Flow<Boolean> =
        libraryDao.observeLiked(profileId, ref.tmdbId, ref.mediaType.apiValue)

    fun liked(profileId: Long): Flow<List<LibraryItem>> = libraryDao.observeLikedTitles(profileId).map { it.map(SnapshotWithTime::toItem) }

    fun likedKeys(profileId: Long): Flow<Set<TitleRef>> = libraryDao.observeLikedKeys(profileId).map { it.toRefs() }

    suspend fun setLiked(profileId: Long, snapshot: TitleSnapshot, liked: Boolean) = withContext(io) {
        if (liked) {
            snapshotDao.upsert(snapshot.toEntity(clock.nowMillis()))
            libraryDao.like(LikedTitleEntity(profileId, snapshot.tmdbId, snapshot.mediaType.apiValue, clock.nowMillis()))
        } else {
            libraryDao.unlike(profileId, snapshot.tmdbId, snapshot.mediaType.apiValue)
        }
        _events.tryEmit(LibraryEvent.LIKE_CHANGED)
    }

    // ---- Resume points (RS-1 … RS-6) ----

    fun progress(profileId: Long, playable: PlayableRef): Flow<WatchProgress?> = progressDao.observe(
        profileId, playable.tmdbId, playable.mediaType.apiValue, playable.season, playable.episode,
    ).map { it?.toModel() }

    fun progressForTitle(profileId: Long, ref: TitleRef): Flow<List<WatchProgress>> =
        progressDao.observeForTitle(profileId, ref.tmdbId, ref.mediaType.apiValue).map { rows -> rows.map { it.toModel() } }

    suspend fun latestProgress(profileId: Long, ref: TitleRef): WatchProgress? = withContext(io) {
        progressDao.latestForTitle(profileId, ref.tmdbId, ref.mediaType.apiValue)?.toModel()
    }

    suspend fun saveProgress(
        profileId: Long,
        playable: PlayableRef,
        positionMs: Long,
        durationMs: Long,
        source: SourceRef?,
        episodeTitle: String?,
    ): WatchProgress = withContext(io) {
        val completed = WatchRules.isWatched(positionMs, durationMs)
        val entity = WatchProgressEntity(
            profileId = profileId, tmdbId = playable.tmdbId, mediaType = playable.mediaType.apiValue,
            season = playable.season, episode = playable.episode, positionMs = positionMs, durationMs = durationMs,
            completed = completed, lastInfoHash = source?.infoHash, lastFileIndex = source?.fileIndex,
            lastSourceName = source?.name, lastQuality = source?.quality, episodeTitle = episodeTitle,
            updatedAt = clock.nowMillis(),
        )
        progressDao.upsert(entity)
        entity.toModel()
    }

    /** RS-6: no history entry is created. */
    suspend fun markWatched(profileId: Long, playable: PlayableRef, watched: Boolean, durationMs: Long = 1) = withContext(io) {
        val existing = progressDao.get(profileId, playable.tmdbId, playable.mediaType.apiValue, playable.season, playable.episode)
        val duration = existing?.durationMs?.takeIf { it > 0 } ?: durationMs
        progressDao.upsert(
            (existing ?: WatchProgressEntity(profileId, playable.tmdbId, playable.mediaType.apiValue, playable.season, playable.episode, 0, duration, false, null, null, null, null, null, 0))
                .copy(positionMs = if (watched) duration else 0, durationMs = duration, completed = watched, updatedAt = clock.nowMillis()),
        )
    }

    suspend fun removeFromContinueWatching(profileId: Long, ref: TitleRef) = withContext(io) {
        progressDao.hideFromContinue(profileId, ref.tmdbId, ref.mediaType.apiValue)
    }

    /**
     * Continue Watching (RS-4): latest progress row per title that qualifies, newest first. A
     * finished episode is replaced by [nextEpisodeResolver]'s next episode at position 0 (RS-3).
     */
    fun continueWatching(
        profileId: Long,
        nextEpisodeResolver: suspend (WatchProgress) -> Pair<PlayableRef, String?>? = { null },
    ): Flow<List<ContinueWatchingItem>> = combine(
        progressDao.observeLatestPerTitle(profileId, 60),
        libraryDao.observeMyList(profileId), // re-emit when snapshots change
    ) { rows, _ -> rows }.map { rows ->
        withContext(io) {
            rows.filter { it.updatedAt > 0 }.mapNotNull { row ->
                val snapshot = snapshotDao.get(row.tmdbId, row.mediaType)?.toModel() ?: return@mapNotNull null
                val progress = row.toModel()
                when {
                    WatchRules.qualifiesForContinue(row.positionMs, row.durationMs) -> ContinueWatchingItem(snapshot, progress, row.episodeTitle)
                    row.completed && progress.playable.isEpisode -> nextEpisodeResolver(progress)?.let { (next, title) ->
                        val nextRow = progressDao.get(profileId, next.tmdbId, next.mediaType.apiValue, next.season, next.episode)
                        if (nextRow?.completed == true) null
                        else ContinueWatchingItem(
                            snapshot,
                            progress.copy(playable = next, positionMs = nextRow?.positionMs ?: 0, completed = false),
                            title,
                        )
                    }
                    else -> null
                }
            }.take(WatchRules.CONTINUE_MAX_ITEMS)
        }
    }

    // ---- Play sessions and History (HS-1 … HS-8) ----

    suspend fun startSession(profileId: Long, playable: PlayableRef, episodeTitle: String?, source: SourceRef?, startPositionMs: Long, snapshot: TitleSnapshot?): Long = withContext(io) {
        snapshot?.let { snapshotDao.upsert(it.toEntity(clock.nowMillis())) }
        val now = clock.nowMillis()
        sessionDao.insert(
            PlaySessionEntity(
                profileId = profileId, tmdbId = playable.tmdbId, mediaType = playable.mediaType.apiValue,
                season = playable.season, episode = playable.episode, episodeTitle = episodeTitle,
                infoHash = source?.infoHash, fileIndex = source?.fileIndex, sourceName = source?.name,
                quality = source?.quality, providerId = source?.providerId, startedAt = now, endedAt = null,
                startPositionMs = startPositionMs, stopPositionMs = startPositionMs, durationMs = 0, watchedMs = 0, endReason = null,
            ),
        )
    }

    suspend fun checkpointSession(id: Long, positionMs: Long, durationMs: Long, watchedMs: Long) = withContext(io) {
        sessionDao.checkpoint(id, positionMs, durationMs, watchedMs, clock.nowMillis())
    }

    /** Finishes a session; always called off the UI's lifecycle so it survives screen exit. */
    fun finishSession(id: Long, positionMs: Long, durationMs: Long, watchedMs: Long, reason: PlaybackEndReason) {
        appScope.launch(io) {
            sessionDao.finish(id, clock.nowMillis(), positionMs, durationMs, watchedMs, reason.name)
            _events.tryEmit(LibraryEvent.SESSION_ENDED)
        }
    }

    fun history(profileId: Long): Flow<List<HistoryDay>> = sessionDao.observeVisible(profileId, WatchRules.HISTORY_MIN_WATCHED_MS).map { rows ->
        withContext(io) {
            val zone = ZoneId.systemDefault()
            val snapshots = mutableMapOf<Pair<Int, String>, TitleSnapshot?>()
            rows.groupBy { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() }
                .map { (day, sessions) ->
                    val entries = sessions.groupBy { it.tmdbId to it.mediaType }.mapNotNull { (key, group) ->
                        val snapshot = snapshots.getOrPut(key) { snapshotDao.get(key.first, key.second)?.toModel() } ?: return@mapNotNull null
                        val latest = group.maxBy { it.startedAt }
                        HistoryEntry(snapshot, latest.toModel(), group.size, group.sumOf { it.watchedMs }) to group.map { it.id }
                    }
                    HistoryDay(day, entries.map { it.first }, entries.associate { it.first.latest.id to it.second })
                }
                .sortedByDescending { it.date }
        }
    }

    suspend fun deleteSessions(ids: List<Long>) = withContext(io) {
        sessionDao.delete(ids)
        snapshotDao.deleteUnreferenced()
    }

    suspend fun deleteHistoryForTitle(profileId: Long, ref: TitleRef) = withContext(io) {
        sessionDao.deleteForTitle(profileId, ref.tmdbId, ref.mediaType.apiValue)
        snapshotDao.deleteUnreferenced()
    }

    suspend fun clearHistory(profileId: Long) = withContext(io) {
        sessionDao.deleteAll(profileId)
        snapshotDao.deleteUnreferenced()
        _events.tryEmit(LibraryEvent.HISTORY_CLEARED)
    }
}

/** The source a playback used, as stored with progress and sessions. */
data class SourceRef(
    val infoHash: String?,
    val fileIndex: Int?,
    val name: String?,
    val quality: String?,
    val providerId: String?,
)

/** History grouped by day (HS-3); [sessionIds] maps each entry's latest session ID to all folded sessions. */
data class HistoryDay(val date: LocalDate, val entries: List<HistoryEntry>, val sessionIds: Map<Long, List<Long>>)

internal fun TitleSnapshot.toEntity(now: Long) = TitleSnapshotEntity(
    tmdbId, mediaType.apiValue, title, originalTitle, year, posterPath, backdropPath, genreIds, imdbId, now,
)

internal fun TitleSnapshotEntity.toModel() = TitleSnapshot(
    tmdbId, MediaType.fromApi(mediaType) ?: MediaType.MOVIE, title, originalTitle, year, posterPath, backdropPath, genreIds, imdbId,
)

internal fun SnapshotWithTime.toItem() = LibraryItem(
    TitleSnapshot(tmdbId, MediaType.fromApi(mediaType) ?: MediaType.MOVIE, title, originalTitle, year, posterPath, backdropPath, genreIds, imdbId),
    addedAt,
)

internal fun WatchProgressEntity.toModel() = WatchProgress(
    profileId = profileId,
    playable = PlayableRef(tmdbId, MediaType.fromApi(mediaType) ?: MediaType.MOVIE, season, episode),
    positionMs = positionMs,
    durationMs = durationMs,
    completed = completed,
    lastInfoHash = lastInfoHash,
    lastFileIndex = lastFileIndex,
    lastSourceName = lastSourceName,
    lastQuality = lastQuality,
    updatedAt = updatedAt,
)

internal fun PlaySessionEntity.toModel() = PlaySession(
    id = id, profileId = profileId,
    playable = PlayableRef(tmdbId, MediaType.fromApi(mediaType) ?: MediaType.MOVIE, season, episode),
    episodeTitle = episodeTitle, infoHash = infoHash, fileIndex = fileIndex, sourceName = sourceName, quality = quality,
    providerId = providerId, startedAt = startedAt, endedAt = endedAt, startPositionMs = startPositionMs,
    stopPositionMs = stopPositionMs, durationMs = durationMs, watchedMs = watchedMs,
    endReason = endReason?.let { runCatching { PlaybackEndReason.valueOf(it) }.getOrNull() },
)

private fun List<String>.toRefs(): Set<TitleRef> = mapNotNull { key ->
    val (id, type) = key.split(':').takeIf { it.size == 2 } ?: return@mapNotNull null
    TitleRef(id.toIntOrNull() ?: return@mapNotNull null, MediaType.fromApi(type) ?: return@mapNotNull null)
}.toSet()
