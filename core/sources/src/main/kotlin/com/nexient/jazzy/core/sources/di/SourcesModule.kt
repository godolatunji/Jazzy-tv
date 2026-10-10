package com.nexient.jazzy.core.sources.di

import com.nexient.jazzy.core.sources.DefaultSourceSearcher
import com.nexient.jazzy.core.sources.SourceSearcher
import com.nexient.jazzy.core.sources.engine.EngineHttp
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * The OkHttp client used for torrent-site scraping: desktop-browser User-Agent, 10 s connect /
 * 15 s read timeouts, no logging (URLs and cookies must never be logged). Separate from the TMDB
 * client in `:core:network`.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SourcesHttpClient

@Module
@InstallIn(SingletonComponent::class)
abstract class SourcesModule {
    @Binds
    @Singleton
    abstract fun bindSourceSearcher(impl: DefaultSourceSearcher): SourceSearcher
}

@Module
@InstallIn(SingletonComponent::class)
object SourcesNetworkModule {
    @Provides
    @Singleton
    @SourcesHttpClient
    fun provideSourcesHttpClient(): OkHttpClient = EngineHttp.defaultClient()
}
