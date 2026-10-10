package com.nexient.jazzy.core.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nexient.jazzy.core.common.Clock
import com.nexient.jazzy.core.common.DAY_MS
import com.nexient.jazzy.core.data.profile.ProfileRepository
import com.nexient.jazzy.core.data.recommendations.RecommendationRepository
import com.nexient.jazzy.core.database.app.TitleSnapshotDao
import com.nexient.jazzy.core.database.cache.SourceCacheDao
import com.nexient.jazzy.core.database.cache.TmdbCacheDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/** Keeps jazzy_cache.db under its cap: 30-day idle purge, then LRU (spec §6.3, E3-07). */
@HiltWorker
class CacheEvictionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val tmdb: TmdbCacheDao,
    private val sources: SourceCacheDao,
    private val snapshots: TitleSnapshotDao,
    private val clock: Clock,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val now = clock.nowMillis()
        val idleCutoff = now - 30 * DAY_MS
        tmdb.evictListPages(idleCutoff)
        tmdb.evictTitles(idleCutoff)
        tmdb.evictSeasons(idleCutoff)
        tmdb.evictSearches(now - 7 * DAY_MS)
        sources.evict(now - DAY_MS)
        // LRU: walk the cutoff forward a day at a time until under the cap.
        var cutoff = tmdb.oldestAccess() ?: now
        while (tmdb.approximateBytes() > CAP_BYTES && cutoff < now) {
            cutoff += DAY_MS
            tmdb.evictListPages(cutoff)
            tmdb.evictTitles(cutoff)
            tmdb.evictSeasons(cutoff)
        }
        snapshots.deleteUnreferenced()
        return Result.success()
    }

    companion object {
        const val CAP_BYTES = 150L * 1024 * 1024
    }
}

/** Rebuilds stale recommendation rails for every profile (spec §5.7: 12 h refresh). */
@HiltWorker
class RecommendationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val profiles: ProfileRepository,
    private val recommendations: RecommendationRepository,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        profiles.profiles.first().forEach { p ->
            if (recommendations.isStale(p.id)) runCatching { recommendations.rebuild(p) }
        }
        return Result.success()
    }
}

object JazzyWork {
    fun schedule(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.enqueueUniquePeriodicWork(
            "cache_eviction",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<CacheEvictionWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build(),
        )
        wm.enqueueUniquePeriodicWork(
            "recommendations",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<RecommendationWorker>(12, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build(),
        )
    }
}
