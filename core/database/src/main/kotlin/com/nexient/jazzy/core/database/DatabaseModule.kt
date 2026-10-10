package com.nexient.jazzy.core.database

import android.content.Context
import androidx.room.Room
import com.nexient.jazzy.core.database.app.AppDatabase
import com.nexient.jazzy.core.database.cache.CacheDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun appDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            // jazzy.db: never destructive. Add Migration objects here with every version bump.
            .build()

    @Provides
    @Singleton
    fun cacheDatabase(@ApplicationContext context: Context): CacheDatabase =
        Room.databaseBuilder(context, CacheDatabase::class.java, CacheDatabase.NAME)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides fun profileDao(db: AppDatabase) = db.profileDao()
    @Provides fun settingsDao(db: AppDatabase) = db.settingsDao()
    @Provides fun titleSnapshotDao(db: AppDatabase) = db.titleSnapshotDao()
    @Provides fun libraryDao(db: AppDatabase) = db.libraryDao()
    @Provides fun progressDao(db: AppDatabase) = db.progressDao()
    @Provides fun playSessionDao(db: AppDatabase) = db.playSessionDao()
    @Provides fun searchHistoryDao(db: AppDatabase) = db.searchHistoryDao()
    @Provides fun recommendationStateDao(db: AppDatabase) = db.recommendationStateDao()
    @Provides fun providerDao(db: AppDatabase) = db.providerDao()
    @Provides fun tmdbCacheDao(db: CacheDatabase) = db.tmdbCacheDao()
    @Provides fun sourceCacheDao(db: CacheDatabase) = db.sourceCacheDao()
    @Provides fun recommendationCacheDao(db: CacheDatabase) = db.recommendationCacheDao()
}
