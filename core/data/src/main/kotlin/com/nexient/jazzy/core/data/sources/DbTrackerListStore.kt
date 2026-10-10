package com.nexient.jazzy.core.data.sources

import com.nexient.jazzy.core.data.SeedData
import com.nexient.jazzy.core.database.app.ProviderDao
import com.nexient.jazzy.core.torrent.TrackerListStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/** Public trackers appended to magnets (TE-2), from jazzy.db `public_tracker` (seeded on first run). */
@Singleton
class DbTrackerListStore @Inject constructor(private val dao: ProviderDao) : TrackerListStore {
    override val trackers: Flow<List<String>> = flow {
        emit(dao.enabledTrackers().map { it.url }.ifEmpty { SeedData.PUBLIC_TRACKERS })
    }
}
