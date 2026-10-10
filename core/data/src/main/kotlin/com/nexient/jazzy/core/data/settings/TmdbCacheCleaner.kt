package com.nexient.jazzy.core.data.settings

import android.content.Context
import com.nexient.jazzy.core.common.IoDispatcher
import com.nexient.jazzy.core.database.app.AppDatabase
import com.nexient.jazzy.core.database.cache.CacheDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** On-disk size of the two SQLite databases (spec §5.11 "Local data"). */
data class LocalDataSizes(val appDbBytes: Long, val cacheDbBytes: Long)

/**
 * Settings → Local data → "Clear TMDB cache". Wipes `jazzy_cache.db` only (TMDB responses, source
 * result cache and recommendation output); it never touches `jazzy.db`, so history, My List, likes,
 * resume points and title snapshots are kept (spec §5.11, §6.3).
 */
@Singleton
class TmdbCacheCleaner @Inject constructor(
    private val cacheDatabase: CacheDatabase,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    suspend fun clear() = withContext(io) { cacheDatabase.clearAllTables() }

    suspend fun sizes(): LocalDataSizes = withContext(io) {
        LocalDataSizes(appDbBytes = dbSize(AppDatabase.NAME), cacheDbBytes = dbSize(CacheDatabase.NAME))
    }

    /** The main file plus its WAL and shared-memory side files. */
    private fun dbSize(name: String): Long {
        val main = context.getDatabasePath(name)
        return listOf(main, File(main.path + "-wal"), File(main.path + "-shm")).sumOf { if (it.exists()) it.length() else 0L }
    }
}
