package com.nexient.jazzy.core.database.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TmdbCacheDao {
    @Query("SELECT * FROM tmdb_config WHERE `key` = :key")
    suspend fun config(key: String): TmdbConfigEntity?

    @Upsert
    suspend fun putConfig(entity: TmdbConfigEntity)

    @Query("SELECT * FROM genre WHERE mediaType = :mediaType AND language = :language ORDER BY name")
    suspend fun genres(mediaType: String, language: String): List<GenreEntity>

    @Query("SELECT * FROM genre WHERE language = :language")
    fun observeGenres(language: String): Flow<List<GenreEntity>>

    @Transaction
    suspend fun replaceGenres(mediaType: String, language: String, rows: List<GenreEntity>) {
        deleteGenres(mediaType, language)
        insertGenres(rows)
    }

    @Query("DELETE FROM genre WHERE mediaType = :mediaType AND language = :language")
    suspend fun deleteGenres(mediaType: String, language: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGenres(rows: List<GenreEntity>)

    @Query("SELECT * FROM tmdb_list_page WHERE listKey = :listKey AND page = :page AND language = :language")
    suspend fun listPage(listKey: String, page: Int, language: String): TmdbListPageEntity?

    @Upsert
    suspend fun putListPage(entity: TmdbListPageEntity)

    @Query("UPDATE tmdb_list_page SET lastAccessedAt = :now WHERE listKey = :listKey AND page = :page AND language = :language")
    suspend fun touchListPage(listKey: String, page: Int, language: String, now: Long)

    @Query("SELECT * FROM tmdb_title WHERE tmdbId = :tmdbId AND mediaType = :mediaType AND language = :language")
    suspend fun title(tmdbId: Int, mediaType: String, language: String): TmdbTitleEntity?

    @Upsert
    suspend fun putTitle(entity: TmdbTitleEntity)

    @Query("UPDATE tmdb_title SET lastAccessedAt = :now WHERE tmdbId = :tmdbId AND mediaType = :mediaType AND language = :language")
    suspend fun touchTitle(tmdbId: Int, mediaType: String, language: String, now: Long)

    @Query("SELECT * FROM tmdb_season WHERE tmdbId = :tmdbId AND season = :season AND language = :language")
    suspend fun season(tmdbId: Int, season: Int, language: String): TmdbSeasonEntity?

    @Upsert
    suspend fun putSeason(entity: TmdbSeasonEntity)

    @Query("SELECT * FROM tmdb_search WHERE queryKey = :queryKey AND page = :page")
    suspend fun search(queryKey: String, page: Int): TmdbSearchEntity?

    @Upsert
    suspend fun putSearch(entity: TmdbSearchEntity)

    // Eviction (spec §6.3): idle rows first, then LRU until under the size cap.
    @Query("DELETE FROM tmdb_list_page WHERE lastAccessedAt < :before")
    suspend fun evictListPages(before: Long): Int

    @Query("DELETE FROM tmdb_title WHERE lastAccessedAt < :before")
    suspend fun evictTitles(before: Long): Int

    @Query("DELETE FROM tmdb_season WHERE lastAccessedAt < :before")
    suspend fun evictSeasons(before: Long): Int

    @Query("DELETE FROM tmdb_search WHERE fetchedAt < :before")
    suspend fun evictSearches(before: Long): Int

    @Query(
        """SELECT COALESCE(SUM(LENGTH(json)), 0) FROM (
            SELECT json FROM tmdb_list_page UNION ALL SELECT json FROM tmdb_title UNION ALL
            SELECT json FROM tmdb_season UNION ALL SELECT json FROM tmdb_search UNION ALL SELECT json FROM source_result)""",
    )
    suspend fun approximateBytes(): Long

    @Query("SELECT MIN(lastAccessedAt) FROM (SELECT lastAccessedAt FROM tmdb_list_page UNION ALL SELECT lastAccessedAt FROM tmdb_title UNION ALL SELECT lastAccessedAt FROM tmdb_season)")
    suspend fun oldestAccess(): Long?
}

@Dao
interface SourceCacheDao {
    @Query("SELECT * FROM source_result WHERE queryKey = :queryKey")
    suspend fun get(queryKey: String): SourceResultEntity?

    @Upsert
    suspend fun put(entity: SourceResultEntity)

    @Query("DELETE FROM source_result WHERE fetchedAt < :before")
    suspend fun evict(before: Long): Int
}

@Dao
interface RecommendationCacheDao {
    @Query("SELECT * FROM recommendation_cache WHERE profileId = :profileId")
    fun observe(profileId: Long): Flow<List<RecommendationCacheEntity>>

    @Query("SELECT MAX(generatedAt) FROM recommendation_cache WHERE profileId = :profileId")
    suspend fun lastGenerated(profileId: Long): Long?

    @Query("DELETE FROM recommendation_cache WHERE profileId = :profileId")
    suspend fun clear(profileId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rows: List<RecommendationCacheEntity>)

    @Transaction
    suspend fun replace(profileId: Long, rows: List<RecommendationCacheEntity>) {
        clear(profileId)
        insert(rows)
    }
}
