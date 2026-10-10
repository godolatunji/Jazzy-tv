package com.nexient.jazzy.core.database.app

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nexient.jazzy.core.database.Converters

/**
 * `jazzy.db`: the user's data (profiles, lists, history, resume points, settings). Included in
 * Android backups. Every schema change MUST ship a tested migration (spec §7, plan §12.5).
 */
@Database(
    entities = [
        ProfileEntity::class, AppSettingEntity::class, ProfileSettingEntity::class, TitleSnapshotEntity::class,
        MyListItemEntity::class, LikedTitleEntity::class, WatchProgressEntity::class, PlaySessionEntity::class,
        SearchHistoryEntity::class, SeedGenreEntity::class, GenreAffinityEntity::class, ProviderSettingEntity::class,
        SourceFlagEntity::class, OriginalsNetworkEntity::class, PublicTrackerEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun settingsDao(): SettingsDao
    abstract fun titleSnapshotDao(): TitleSnapshotDao
    abstract fun libraryDao(): LibraryDao
    abstract fun progressDao(): ProgressDao
    abstract fun playSessionDao(): PlaySessionDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun recommendationStateDao(): RecommendationStateDao
    abstract fun providerDao(): ProviderDao

    companion object {
        const val NAME = "jazzy.db"
    }
}
