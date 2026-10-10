package com.nexient.jazzy.core.data

import com.nexient.jazzy.core.data.catalog.DefaultOriginals
import com.nexient.jazzy.core.data.settings.SettingKeys
import com.nexient.jazzy.core.data.settings.SettingsRepository
import com.nexient.jazzy.core.database.app.OriginalsNetworkEntity
import com.nexient.jazzy.core.database.app.ProviderDao
import com.nexient.jazzy.core.database.app.PublicTrackerEntity
import javax.inject.Inject

/** First-run seed data for jazzy.db (E2-08): Originals networks and public trackers. */
class SeedData @Inject constructor(
    private val providerDao: ProviderDao,
    private val settings: SettingsRepository,
) {
    suspend fun ensureSeeded() {
        if (settings.get(SettingKeys.SEEDED) != null) return
        providerDao.insertOriginals(DefaultOriginals.mapIndexed { i, n -> OriginalsNetworkEntity(n.id, n.name, "tv", true, i) })
        providerDao.insertTrackers(PUBLIC_TRACKERS.map { PublicTrackerEntity(it, true) })
        settings.put(SettingKeys.SEEDED, true)
    }

    companion object {
        val PUBLIC_TRACKERS = listOf(
            "udp://tracker.opentrackr.org:1337/announce",
            "udp://open.demonii.com:1337/announce",
            "udp://open.stealth.si:80/announce",
            "udp://tracker.torrent.eu.org:451/announce",
            "udp://exodus.desync.com:6969/announce",
            "udp://tracker.openbittorrent.com:6969/announce",
            "udp://explodie.org:6969/announce",
            "udp://tracker.dler.org:6969/announce",
        )
    }
}
