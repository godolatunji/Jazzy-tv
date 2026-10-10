package com.nexient.jazzy.core.data.catalog

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.nexient.jazzy.core.common.Clock
import com.nexient.jazzy.core.common.DAY_MS
import com.nexient.jazzy.core.common.HOUR_MS
import com.nexient.jazzy.core.common.IoDispatcher
import com.nexient.jazzy.core.data.Cached
import com.nexient.jazzy.core.data.cacheFirstOnce
import com.nexient.jazzy.core.database.cache.TmdbCacheDao
import com.nexient.jazzy.core.database.cache.TmdbListPageEntity
import com.nexient.jazzy.core.model.MaturityLevel
import com.nexient.jazzy.core.model.MediaType
import com.nexient.jazzy.core.model.Profile
import com.nexient.jazzy.core.model.TitleSummary
import com.nexient.jazzy.core.network.TmdbApi
import com.nexient.jazzy.core.network.toSummary
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

data class TitlePage(val items: List<TitleSummary>, val totalPages: Int)

/**
 * TMDB lists, trending and discover pages, cache-first in `jazzy_cache.db` (spec §6.3) with
 * maturity filtering for restricted profiles (spec §6.4).
 */
@Singleton
class CatalogRepository @Inject constructor(
    private val api: TmdbApi,
    private val cache: TmdbCacheDao,
    private val json: Json,
    private val clock: Clock,
    private val region: RegionProvider,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    private val listSerializer = ListSerializer(TitleSummary.serializer())

    fun pager(rail: RailDefinition, profile: Profile?): Flow<PagingData<TitleSummary>> = Pager(
        config = PagingConfig(pageSize = 20, prefetchDistance = 6, enablePlaceholders = false, initialLoadSize = 20),
        pagingSourceFactory = { RailPagingSource(rail.maxPages) { page -> loadRailPage(rail, page, profile) } },
    ).flow

    /** One page across all of a rail's sources, interleaved movie/tv. */
    suspend fun loadRailPage(rail: RailDefinition, page: Int, profile: Profile?, force: Boolean = false): TitlePage = coroutineScope {
        val pages = rail.sources.map { source -> async { loadPage(source, page, profile, force) } }.awaitAll()
        TitlePage(interleave(pages.map { it.items }).distinctBy { it.tmdbId to it.mediaType }, pages.maxOf { it.totalPages })
    }

    suspend fun loadPage(source: RailSource, page: Int, profile: Profile?, force: Boolean = false): TitlePage = withContext(io) {
        val language = profile?.language ?: "en-US"
        val reg = region.region()
        val effective = restrict(source, profile?.maturity ?: MaturityLevel.ALL, reg)
        val key = effective.cacheKey + ":" + reg
        val ttl = ttlFor(effective)
        val result = cacheFirstOnce(
            now = clock::nowMillis,
            ttlMs = if (force) -1 else ttl,
            read = {
                cache.listPage(key, page, language)?.let {
                    cache.touchListPage(key, page, language, clock.nowMillis())
                    Cached(TitlePage(json.decodeFromString(listSerializer, it.json), it.totalPages), it.fetchedAt)
                }
            },
            fetch = { fetch(effective, page, language, reg) },
            write = { p ->
                val now = clock.nowMillis()
                cache.putListPage(TmdbListPageEntity(key, page, language, json.encodeToString(listSerializer, p.items), p.totalPages, now, now))
            },
        )
        val restricted = (profile?.maturity ?: MaturityLevel.ALL) != MaturityLevel.ALL
        result.copy(items = result.items.filter { !it.adult && (!restricted || isKidSafeGenre(it)) })
    }

    private suspend fun fetch(source: RailSource, page: Int, language: String, region: String): TitlePage {
        val dto = when (source) {
            is RailSource.Trending -> api.trending(source.mediaType?.apiValue ?: "all", source.window, language, page)
            is RailSource.TmdbList -> api.list(source.mediaType.apiValue, source.list, language, region.takeIf { source.mediaType == MediaType.MOVIE }, page)
            is RailSource.Discover -> api.discover(source.mediaType.apiValue, language, page, source.filters + ("include_adult" to "false"))
        }
        val fallback = when (source) {
            is RailSource.Trending -> source.mediaType
            is RailSource.TmdbList -> source.mediaType
            is RailSource.Discover -> source.mediaType
        }
        return TitlePage(dto.results.mapNotNull { it.toSummary(fallback) }, dto.totalPages.coerceAtMost(500))
    }

    private fun ttlFor(source: RailSource): Long = when (source) {
        is RailSource.Trending -> if (source.window == "day") 2 * HOUR_MS else 6 * HOUR_MS
        is RailSource.TmdbList -> 6 * HOUR_MS
        is RailSource.Discover -> 12 * HOUR_MS
    }

    /**
     * Restricted profiles never use unfiltered list endpoints: lists become Discover queries with
     * certification limits (movies) or kids genres (TV), so nothing above the ceiling is fetched.
     */
    private fun restrict(source: RailSource, maturity: MaturityLevel, region: String): RailSource {
        if (maturity == MaturityLevel.ALL) return source
        fun movie(sort: String, extra: Map<String, String> = emptyMap()) = RailSource.Discover(
            MediaType.MOVIE,
            extra + mapOf("certification_country" to "US", "certification.lte" to maturity.movieCeiling, "sort_by" to sort),
        )
        fun tv(sort: String, extra: Map<String, String> = emptyMap()) = RailSource.Discover(
            MediaType.TV,
            extra + mapOf("with_genres" to KIDS_TV_GENRES, "without_genres" to "80,10768,9648", "sort_by" to sort),
        )
        val type = when (source) {
            is RailSource.Trending -> source.mediaType ?: MediaType.MOVIE
            is RailSource.TmdbList -> source.mediaType
            is RailSource.Discover -> source.mediaType
        }
        val sort = when (source) {
            is RailSource.TmdbList -> if (source.list == "top_rated") "vote_average.desc" else "popularity.desc"
            is RailSource.Discover -> source.filters["sort_by"] ?: "popularity.desc"
            else -> "popularity.desc"
        }
        val extra = mutableMapOf<String, String>()
        if (sort == "vote_average.desc") extra["vote_count.gte"] = "300"
        // Keep the user's own Discover filters (year, rating, votes, language, runtime); the maturity
        // limits below always win because they're applied last.
        (source as? RailSource.Discover)?.filters?.forEach { (k, v) ->
            if (k !in RESTRICTED_KEYS && !(k == "with_genres" && type == MediaType.TV)) extra[k] = v
        }
        return if (type == MediaType.MOVIE) movie(sort, extra) else tv(sort, extra)
    }

    private fun isKidSafeGenre(item: TitleSummary): Boolean =
        item.mediaType == MediaType.MOVIE || item.genreIds.any { it in KIDS_TV_GENRE_IDS }

    private fun interleave(lists: List<List<TitleSummary>>): List<TitleSummary> {
        if (lists.size == 1) return lists.first()
        val out = mutableListOf<TitleSummary>()
        val max = lists.maxOfOrNull { it.size } ?: 0
        for (i in 0 until max) lists.forEach { l -> l.getOrNull(i)?.let(out::add) }
        return out
    }

    companion object {
        const val KIDS_TV_GENRES = "10762|10751|16"
        private val RESTRICTED_KEYS = setOf("certification_country", "certification", "certification.lte", "certification.gte", "include_adult", "without_genres", "sort_by", "with_networks")
        val KIDS_TV_GENRE_IDS = setOf(10762, 10751, 16)
        val LIST_TTL = 6 * HOUR_MS
        val DETAILS_TTL = DAY_MS
    }
}

/** Pages 1…[maxPages] of a rail, read through [load]. */
class RailPagingSource(
    private val maxPages: Int,
    private val load: suspend (Int) -> TitlePage,
) : PagingSource<Int, TitleSummary>() {
    override fun getRefreshKey(state: PagingState<Int, TitleSummary>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TitleSummary> {
        val page = params.key ?: 1
        return try {
            val result = load(page)
            LoadResult.Page(
                data = result.items,
                prevKey = null,
                nextKey = if (page < minOf(maxPages, result.totalPages) && result.items.isNotEmpty()) page + 1 else null,
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
