package com.nexient.jazzy.core.data.catalog

import com.nexient.jazzy.core.common.Clock
import com.nexient.jazzy.core.common.DAY_MS
import com.nexient.jazzy.core.common.HOUR_MS
import com.nexient.jazzy.core.common.IoDispatcher
import com.nexient.jazzy.core.common.Resource
import com.nexient.jazzy.core.data.Cached
import com.nexient.jazzy.core.data.cacheFirst
import com.nexient.jazzy.core.data.cacheFirstOnce
import com.nexient.jazzy.core.database.cache.GenreEntity
import com.nexient.jazzy.core.database.cache.TmdbCacheDao
import com.nexient.jazzy.core.database.cache.TmdbSeasonEntity
import com.nexient.jazzy.core.database.cache.TmdbTitleEntity
import com.nexient.jazzy.core.model.Genre
import com.nexient.jazzy.core.model.MediaType
import com.nexient.jazzy.core.model.Season
import com.nexient.jazzy.core.model.TitleDetails
import com.nexient.jazzy.core.model.TitleRef
import com.nexient.jazzy.core.network.TmdbApi
import com.nexient.jazzy.core.network.toDetails
import com.nexient.jazzy.core.network.toSeason
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Title details, seasons and genre lists, cache-first (spec §5.3, §6.3). */
@Singleton
class TitleRepository @Inject constructor(
    private val api: TmdbApi,
    private val cache: TmdbCacheDao,
    private val json: Json,
    private val clock: Clock,
    private val region: RegionProvider,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    fun details(ref: TitleRef, language: String, force: Boolean = false): Flow<Resource<TitleDetails>> = cacheFirst(
        now = clock::nowMillis,
        ttlMs = DAY_MS,
        force = force,
        read = { readDetails(ref, language) },
        fetch = { fetchDetails(ref, language) },
        write = { writeDetails(ref, language, it) },
    ).flowOn(io)

    /** Details for background work (recommendations, snapshots); stale cache accepted on failure. */
    suspend fun detailsOnce(ref: TitleRef, language: String): TitleDetails = withContext(io) {
        cacheFirstOnce(clock::nowMillis, DAY_MS, { readDetails(ref, language) }, { fetchDetails(ref, language) }, { writeDetails(ref, language, it) })
    }

    private suspend fun readDetails(ref: TitleRef, language: String): Cached<TitleDetails>? =
        cache.title(ref.tmdbId, ref.mediaType.apiValue, language)?.let {
            cache.touchTitle(ref.tmdbId, ref.mediaType.apiValue, language, clock.nowMillis())
            runCatching { Cached(json.decodeFromString(TitleDetails.serializer(), it.json), it.fetchedAt) }.getOrNull()
        }

    private suspend fun fetchDetails(ref: TitleRef, language: String): TitleDetails {
        val append = if (ref.mediaType == MediaType.MOVIE) TmdbApi.MOVIE_APPEND else TmdbApi.TV_APPEND
        val lang = language.substringBefore('-')
        return api.details(ref.mediaType.apiValue, ref.tmdbId, language, append, "$lang,en,null")
            .toDetails(ref.mediaType, region.region(), language)
    }

    private suspend fun writeDetails(ref: TitleRef, language: String, details: TitleDetails) {
        val now = clock.nowMillis()
        cache.putTitle(TmdbTitleEntity(ref.tmdbId, ref.mediaType.apiValue, language, json.encodeToString(TitleDetails.serializer(), details), now, now))
    }

    fun season(showId: Int, seasonNumber: Int, language: String, stillAiring: Boolean, force: Boolean = false): Flow<Resource<Season>> = cacheFirst(
        now = clock::nowMillis,
        ttlMs = if (stillAiring) 6 * HOUR_MS else DAY_MS,
        force = force,
        read = {
            cache.season(showId, seasonNumber, language)?.let {
                runCatching { Cached(json.decodeFromString(Season.serializer(), it.json), it.fetchedAt) }.getOrNull()
            }
        },
        fetch = { api.season(showId, seasonNumber, language).toSeason(showId) },
        write = {
            val now = clock.nowMillis()
            cache.putSeason(TmdbSeasonEntity(showId, seasonNumber, language, json.encodeToString(Season.serializer(), it), now, now))
        },
    ).flowOn(io)

    suspend fun seasonOnce(showId: Int, seasonNumber: Int, language: String): Season = withContext(io) {
        cacheFirstOnce(
            clock::nowMillis, 6 * HOUR_MS,
            read = { cache.season(showId, seasonNumber, language)?.let { Cached(json.decodeFromString(Season.serializer(), it.json), it.fetchedAt) } },
            fetch = { api.season(showId, seasonNumber, language).toSeason(showId) },
            write = {
                val now = clock.nowMillis()
                cache.putSeason(TmdbSeasonEntity(showId, seasonNumber, language, json.encodeToString(Season.serializer(), it), now, now))
            },
        )
    }

    /** TMDB genre list for a media type, cached 7 days (spec §5.7). */
    suspend fun genres(mediaType: MediaType, language: String): List<Genre> = withContext(io) {
        val cached = cache.genres(mediaType.apiValue, language)
        val fresh = cached.isNotEmpty() && clock.nowMillis() - cached.first().fetchedAt < 7 * DAY_MS
        if (fresh) return@withContext cached.map { Genre(it.id, it.name) }
        runCatching {
            val now = clock.nowMillis()
            val list = api.genres(mediaType.apiValue, language).genres
            cache.replaceGenres(mediaType.apiValue, language, list.map { GenreEntity(it.id, mediaType.apiValue, language, it.name, now) })
            list.map { Genre(it.id, it.name) }
        }.getOrElse { cached.map { Genre(it.id, it.name) } }
    }
}
