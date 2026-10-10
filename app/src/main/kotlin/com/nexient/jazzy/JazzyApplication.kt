package com.nexient.jazzy

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.nexient.jazzy.core.common.ApplicationScope
import com.nexient.jazzy.core.data.SeedData
import com.nexient.jazzy.core.data.recommendations.RecommendationTriggers
import com.nexient.jazzy.core.data.work.JazzyWork
import com.nexient.jazzy.tv.WatchNextPublisher
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class JazzyApplication : Application(), Configuration.Provider, SingletonImageLoader.Factory {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var seedData: SeedData
    @Inject lateinit var recommendationTriggers: RecommendationTriggers
    @Inject lateinit var watchNext: WatchNextPublisher
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        appScope.launch { seedData.ensureSeeded() }
        recommendationTriggers.start()
        JazzyWork.schedule(this)
        watchNext.start()
    }

    /** Coil disk cache: 300 MB on TV, 150 MB on phones (spec §6.3). */
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val tv = FormFactors.isTv(this)
        return ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory()) }
            .memoryCache { MemoryCache.Builder().maxSizePercent(context, if (tv) 0.15 else 0.25).build() }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("images"))
                    .maxSizeBytes(if (tv) 300L * 1024 * 1024 else 150L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
