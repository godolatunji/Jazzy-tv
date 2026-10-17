package com.nexient.jazzy.core.data.catalog

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.nexient.jazzy.core.common.Clock
import com.nexient.jazzy.core.common.HOUR_MS
import com.nexient.jazzy.core.common.IoDispatcher
import com.nexient.jazzy.core.data.Cached
import com.nexient.jazzy.core.data.cacheFirstOnce
import com.nexient.jazzy.core.database.app.SearchHistoryDao
import com.nexient.jazzy.core.database.app.SearchHistoryEntity
import com.nexient.jazzy.core.database.cache.TmdbCacheDao
import com.nexient.jazzy.core.database.cache.TmdbSearchEntity
import com.nexient.jazzy.core.model.MaturityLevel
import com.nexient.jazzy.core.model.MediaType
import com.nexient.jazzy.core.model.Profile
import com.nexient.jazzy.core.model.SearchResult
import com.nexient.jazzy.core.model.TitleSummary
import com.nexient.jazzy.core.network.TmdbApi
import com.nexient.jazzy.core.network.toSearchResult
import com.nexient.jazzy.core.network.toSummary
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

enum class SearchFilter { ALL, MOVIES, TV, PEOPLE }

/** `/search/multi` with a 1 h SQLite cache, plus per-profile recent searches (spec §5.4). */
@Singleton
class SearchRepository @Inject constructor(
    private val api: TmdbApi,
    private val cache: TmdbCacheDao,
    private val history: SearchHistoryDao,
    private val json: Json,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    private val serializer = ListSerializer(SearchResult.serializer())

    fun search(query: String, filter: SearchFilter, profile: Profile?): Flow<PagingData<SearchResult>> = Pager(
        // initialLoadSize defaults to 3×pageSize (=60); TMDB serves 20/page, so the default fires
        // three sequential fetches before the first result shows. Match pageSize for fast first paint.
        PagingConfig(pageSize = 20, initialLoadSize = 20, prefetchDistance = 10, enablePlaceholders = false),
    ) {
        object : PagingSource<Int, SearchResult>() {
            override fun getRefreshKey(state: PagingState<Int, SearchResult>): Int? = null
            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SearchResult> {
                val page = params.key ?: 1
                return try {
                    val (items, total) = loadPage(query, page, profile?.language ?: "en-US")
                    val restricted = (profile?.maturity ?: MaturityLevel.ALL) != MaturityLevel.ALL
                    val filtered = items.filter { r ->
                        when (r) {
                            is SearchResult.Person -> filter == SearchFilter.ALL || filter == SearchFilter.PEOPLE
                            is SearchResult.Title -> {
                                val t = r.title
                                val typeOk = when (filter) {
                                    SearchFilter.ALL -> true
                                    SearchFilter.MOVIES -> t.mediaType == MediaType.MOVIE
                                    SearchFilter.TV -> t.mediaType == MediaType.TV
                                    SearchFilter.PEOPLE -> false
                                }
                                // Restricted profiles only see clearly family-friendly genres in search (SR-5).
                                typeOk && !t.adult && (!restricted || t.genreIds.any { it in KIDS_GENRES })
                            }
                        }
                    }.filter { !(restricted && it is SearchResult.Person) }
                    LoadResult.Page(filtered, null, if (page < total && items.isNotEmpty()) page + 1 else null)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    LoadResult.Error(e)
                }
            }
        }
    }.flow

    private suspend fun loadPage(query: String, page: Int, language: String): Pair<List<SearchResult>, Int> = withContext(io) {
        val key = "$language:${query.trim().lowercase()}"
        cacheFirstOnce(
            clock::nowMillis, HOUR_MS,
            read = { cache.search(key, page)?.let { Cached(json.decodeFromString(serializer, it.json) to it.totalPages, it.fetchedAt) } },
            fetch = {
                val dto = api.searchMulti(query.trim(), language, page)
                dto.results.mapNotNull { it.toSearchResult() } to dto.totalPages
            },
            write = { (items, total) -> cache.putSearch(TmdbSearchEntity(key, page, json.encodeToString(serializer, items), total, clock.nowMillis())) },
        )
    }

    /** Filmography for a person result (SR-1). */
    suspend fun personCredits(personId: Int, language: String): List<TitleSummary> = withContext(io) {
        val credits = api.personCredits(personId, language)
        (credits.cast + credits.crew).mapNotNull { it.toSummary() }
            .filter { !it.adult }
            .distinctBy { it.tmdbId to it.mediaType }
            .sortedByDescending { it.popularity }
    }

    fun recentSearches(profileId: Long): Flow<List<String>> = history.observeRecent(profileId, 10).map { rows -> rows.map { it.query } }

    suspend fun recordSearch(profileId: Long, query: String) {
        val q = query.trim()
        if (q.length >= 2) history.upsert(SearchHistoryEntity(profileId, q, clock.nowMillis()))
    }

    suspend fun removeRecent(profileId: Long, query: String) = history.delete(profileId, query)

    suspend fun clearRecent(profileId: Long) = history.clear(profileId)

    private companion object {
        val KIDS_GENRES = setOf(16, 10751, 10762)
    }
}
