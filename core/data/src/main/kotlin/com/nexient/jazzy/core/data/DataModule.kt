package com.nexient.jazzy.core.data

import com.nexient.jazzy.core.data.settings.DbEngineSettingsStore
import com.nexient.jazzy.core.data.sources.DbProviderSettingsStore
import com.nexient.jazzy.core.data.sources.DbTrackerListStore
import com.nexient.jazzy.core.torrent.TrackerListStore
import com.nexient.jazzy.core.sources.ProviderSettingsStore
import com.nexient.jazzy.core.torrent.EngineSettingsStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    abstract fun providerSettingsStore(impl: DbProviderSettingsStore): ProviderSettingsStore

    @Binds
    abstract fun engineSettingsStore(impl: DbEngineSettingsStore): EngineSettingsStore

    @Binds
    abstract fun trackerListStore(impl: DbTrackerListStore): TrackerListStore
}
