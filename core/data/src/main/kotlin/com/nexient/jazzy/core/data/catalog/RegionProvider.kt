package com.nexient.jazzy.core.data.catalog

import com.nexient.jazzy.core.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** TMDB `region`: user override, else device locale country, else US (spec §6.2). */
@Singleton
class RegionProvider @Inject constructor(private val settings: SettingsRepository) {
    suspend fun region(): String = settings.appSettings.first().region
        ?: Locale.getDefault().country.takeIf { it.length == 2 }
        ?: "US"
}
