package com.nexient.jazzy.core.database.cache

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Cached payloads are stored as JSON of the domain models; TTLs are applied by repositories (spec §6.3). */
@Entity(tableName = "tmdb_config")
data class TmdbConfigEntity(@PrimaryKey val key: String, val json: String, val fetchedAt: Long)

@Entity(tableName = "genre", primaryKeys = ["id", "mediaType", "language"])
data class GenreEntity(val id: Int, val mediaType: String, val language: String, val name: String, val fetchedAt: Long)

@Entity(tableName = "tmdb_list_page", primaryKeys = ["listKey", "page", "language"])
data class TmdbListPageEntity(
    val listKey: String,
    val page: Int,
    val language: String,
    val json: String,
    val totalPages: Int,
    val fetchedAt: Long,
    val lastAccessedAt: Long,
)

@Entity(tableName = "tmdb_title", primaryKeys = ["tmdbId", "mediaType", "language"])
data class TmdbTitleEntity(
    val tmdbId: Int,
    val mediaType: String,
    val language: String,
    val json: String,
    val fetchedAt: Long,
    val lastAccessedAt: Long,
)

@Entity(tableName = "tmdb_season", primaryKeys = ["tmdbId", "season", "language"])
data class TmdbSeasonEntity(
    val tmdbId: Int,
    val season: Int,
    val language: String,
    val json: String,
    val fetchedAt: Long,
    val lastAccessedAt: Long,
)

@Entity(tableName = "tmdb_search", primaryKeys = ["queryKey", "page"])
data class TmdbSearchEntity(val queryKey: String, val page: Int, val json: String, val totalPages: Int, val fetchedAt: Long)

@Entity(tableName = "source_result")
data class SourceResultEntity(@PrimaryKey val queryKey: String, val json: String, val fetchedAt: Long)

@Entity(tableName = "recommendation_cache", primaryKeys = ["profileId", "railKey"])
data class RecommendationCacheEntity(
    val profileId: Long,
    val railKey: String,
    val title: String,
    val json: String,
    val generatedAt: Long,
)
