package com.nexient.jazzy.core.database.cache

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * `jazzy_cache.db`: TMDB responses, source search results and recommendation output.
 * Excluded from backups and safe to wipe; destructive migrations are allowed here only.
 */
@Database(
    entities = [
        TmdbConfigEntity::class, GenreEntity::class, TmdbListPageEntity::class, TmdbTitleEntity::class,
        TmdbSeasonEntity::class, TmdbSearchEntity::class, SourceResultEntity::class, RecommendationCacheEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class CacheDatabase : RoomDatabase() {
    abstract fun tmdbCacheDao(): TmdbCacheDao
    abstract fun sourceCacheDao(): SourceCacheDao
    abstract fun recommendationCacheDao(): RecommendationCacheDao

    companion object {
        const val NAME = "jazzy_cache.db"
    }
}
