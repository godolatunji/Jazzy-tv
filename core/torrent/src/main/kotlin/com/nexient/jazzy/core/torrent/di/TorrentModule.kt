package com.nexient.jazzy.core.torrent.di

import com.nexient.jazzy.core.torrent.LibtorrentEngine
import com.nexient.jazzy.core.torrent.TorrentEngine
import com.nexient.jazzy.core.torrent.TorrentFileProvider
import com.nexient.jazzy.core.torrent.TrackerListStore
import dagger.Binds
import dagger.BindsOptionalOf
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds the engine. The app must also bind an `EngineSettingsStore` (`:core:data`); a
 * `TrackerListStore` binding is optional (defaults to [com.nexient.jazzy.core.torrent.DefaultTrackers]).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TorrentModule {
    @Binds
    abstract fun bindTorrentEngine(impl: LibtorrentEngine): TorrentEngine

    @Binds
    abstract fun bindTorrentFileProvider(impl: LibtorrentEngine): TorrentFileProvider

    @BindsOptionalOf
    abstract fun optionalTrackerListStore(): TrackerListStore
}
