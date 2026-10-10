package com.nexient.jazzy.core.data.recommendations

import com.nexient.jazzy.core.common.Clock
import com.nexient.jazzy.core.common.DAY_MS
import com.nexient.jazzy.core.common.HOUR_MS
import com.nexient.jazzy.core.common.IoDispatcher
import com.nexient.jazzy.core.data.catalog.CatalogRepository
import com.nexient.jazzy.core.data.catalog.RailSource
import com.nexient.jazzy.core.database.app.GenreAffinityEntity
import com.nexient.jazzy.core.database.app.LibraryDao
import com.nexient.jazzy.core.database.app.PlaySessionDao
import com.nexient.jazzy.core.database.app.ProgressDao
import com.nexient.jazzy.core.database.app.RecommendationStateDao
import com.nexient.jazzy.core.database.app.TitleSnapshotDao
import com.nexient.jazzy.core.database.cache.RecommendationCacheDao
import com.nexient.jazzy.core.database.cache.RecommendationCacheEntity
import com.nexient.jazzy.core.model.MediaType
import com.nexient.jazzy.core.model.Profile
import com.nexient.jazzy.core.model.TitleSummary
import com.nexient.jazzy.core.recommendations.AffinityCalculator
import com.nexient.jazzy.core.recommendations.RecommendationPlanner
import com.nexient.jazzy.core.recommendations.TitleSignal
import com.nexient.jazzy.core.recommendations.UnifiedGenre
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

data class RecommendationRail(val key: String, val title: String, val items: List<TitleSummary>)

/** On-device genre recommendations (spec §5.7). Nothing here leaves the device except Discover queries. */
@Singleton
class RecommendationRepository @Inject constructor(
    private val progressDao: ProgressDao,
    private val sessionDao: PlaySessionDao,
    private val libraryDao: LibraryDao,
    private val snapshotDao: TitleSnapshotDao,
    private val stateDao: RecommendationStateDao,
    private val cacheDao: RecommendationCacheDao,
    private val catalog: CatalogRepository,
    private val json: Json,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    private val serializer = ListSerializer(TitleSummary.serializer())
    private val mutex = Mutex()

    fun rails(profileId: Long): Flow<List<RecommendationRail>> = cacheDao.observe(profileId).map { rows ->
        rows.sortedBy { if (it.railKey == RecommendationPlanner.RECOMMENDED_RAIL) 0 else 1 }.map {
            RecommendationRail(it.railKey, it.title, runCatching { json.decodeFromString(serializer, it.json) }.getOrDefault(emptyList()))
        }.filter { it.items.isNotEmpty() }
    }

    suspend fun isStale(profileId: Long): Boolean = withContext(io) {
        val last = cacheDao.lastGenerated(profileId) ?: return@withContext true
        clock.nowMillis() - last > 12 * HOUR_MS
    }

    suspend fun reset(profileId: Long) = withContext(io) {
        stateDao.clearAffinity(profileId)
        cacheDao.clear(profileId)
    }

    /** Recomputes affinity and rebuilds both rail types for [profile]. Safe to call often. */
    suspend fun rebuild(profile: Profile) = mutex.withLock {
        withContext(io) {
            val now = clock.nowMillis()
            val progress = progressDao.allFor(profile.id)
            val sessions = sessionDao.allFor(profile.id)
            val liked = libraryDao.likedFor(profile.id)
            val myList = libraryDao.myListFor(profile.id)
            val seeds = stateDao.seeds(profile.id).mapNotNull { UnifiedGenre.fromId(it.unifiedGenreId) }.toSet()
            val qualifyingSessions = sessions.count { it.watchedMs >= 10 * 60_000 }

            if (qualifyingSessions == 0 && liked.isEmpty() && myList.isEmpty() && seeds.isEmpty()) {
                cacheDao.clear(profile.id) // cold start: rails hidden (spec §5.7)
                return@withContext
            }

            val keys = (progress.map { it.tmdbId to it.mediaType } + liked.map { it.tmdbId to it.mediaType } + myList.map { it.tmdbId to it.mediaType }).toSet()
            val signals = keys.mapNotNull { key ->
                val snapshot = snapshotDao.get(key.first, key.second) ?: return@mapNotNull null
                val type = MediaType.fromApi(key.second) ?: return@mapNotNull null
                val rows = progress.filter { it.tmdbId == key.first && it.mediaType == key.second }
                val completion = rows.takeIf { it.isNotEmpty() }?.map { if (it.durationMs > 0) it.positionMs.toFloat() / it.durationMs else 0f }
                    ?.average()?.toFloat()?.coerceAtMost(1f)
                val lastPlayed = rows.maxOfOrNull { it.updatedAt }?.takeIf { it > 0 }
                    ?: sessions.filter { it.tmdbId == key.first && it.mediaType == key.second }.maxOfOrNull { it.startedAt }
                val like = liked.firstOrNull { it.tmdbId == key.first && it.mediaType == key.second }
                val listed = myList.firstOrNull { it.tmdbId == key.first && it.mediaType == key.second }
                TitleSignal(
                    mediaType = type,
                    genreIds = snapshot.genreIds,
                    completion = completion,
                    daysSinceLastPlayed = lastPlayed?.let { (now - it).toDouble() / DAY_MS },
                    liked = like != null,
                    daysSinceLiked = like?.let { (now - it.likedAt).toDouble() / DAY_MS },
                    inMyList = listed != null,
                    daysSinceAddedToList = listed?.let { (now - it.addedAt).toDouble() / DAY_MS },
                )
            }

            val affinity = AffinityCalculator.compute(signals, seeds, qualifyingSessions)
            stateDao.replaceAffinity(profile.id, affinity.map { GenreAffinityEntity(profile.id, it.first.id, it.second, now) })
            if (affinity.isEmpty()) {
                cacheDao.clear(profile.id)
                return@withContext
            }
            val affinityMap = affinity.toMap()
            val played = signals.count { it.completion != null }
            val movieShare = if (played == 0) null else signals.count { it.completion != null && it.mediaType == MediaType.MOVIE }.toDouble() / played
            val split = RecommendationPlanner.mediaSplit(movieShare)

            // Exclusions: watched titles, Continue Watching, anything played in the last 30 days.
            val excluded = buildSet {
                progress.filter { it.completed || it.updatedAt > now - 30 * DAY_MS }.forEach { row ->
                    MediaType.fromApi(row.mediaType)?.let { add(row.tmdbId to it) }
                }
                sessions.filter { it.startedAt > now - 30 * DAY_MS }.forEach { s -> MediaType.fromApi(s.mediaType)?.let { add(s.tmdbId to it) } }
            }

            val top = affinity.map { it.first }
            val recommendedCandidates = RecommendationPlanner.recommendedQueries(top, split).flatMap { q ->
                (1..q.pages).flatMap { page -> runCatching { catalog.loadPage(RailSource.Discover(q.mediaType, q.filters), page, profile).items }.getOrDefault(emptyList()) }
            }
            val recommended = RecommendationPlanner.rank(recommendedCandidates, affinityMap, excluded, split, RecommendationPlanner.RECOMMENDED_LIMIT)
            val rows = mutableListOf(
                RecommendationCacheEntity(profile.id, RecommendationPlanner.RECOMMENDED_RAIL, "Recommended for You", json.encodeToString(serializer, recommended), now),
            )
            val recommendedKeys = recommended.map { it.tmdbId to it.mediaType }.toSet()
            top.take(2).forEach { genre ->
                val candidates = RecommendationPlanner.genreRailQueries(genre, split).flatMap { q ->
                    runCatching { catalog.loadPage(RailSource.Discover(q.mediaType, q.filters), 1, profile).items }.getOrDefault(emptyList())
                }
                var overlap = 0
                val items = RecommendationPlanner.rank(candidates, affinityMap, excluded, split, RecommendationPlanner.GENRE_RAIL_LIMIT * 2)
                    .filter { (it.tmdbId to it.mediaType) !in recommendedKeys || overlap++ < RecommendationPlanner.MAX_OVERLAP_WITH_RECOMMENDED }
                    .take(RecommendationPlanner.GENRE_RAIL_LIMIT)
                rows += RecommendationCacheEntity(profile.id, "because_${genre.id}", "Because you watch ${genre.label}", json.encodeToString(serializer, items), now)
            }
            cacheDao.replace(profile.id, rows)
        }
    }
}
