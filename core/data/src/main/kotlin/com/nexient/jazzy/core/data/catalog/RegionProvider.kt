package com.nexient.jazzy.core.data.catalog

import com.nexient.jazzy.core.common.ApplicationScope
import com.nexient.jazzy.core.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TMDB `region`: user override, else device locale country, else US (spec §6.2).
 *
 * Cached as a [StateFlow] so [region] is an in-memory read: it was previously re-reading the whole
 * `app_setting` table (rebuilding ~20 keys) on every catalog page of every rail. Still reactive, so
 * a user changing the region in Settings takes effect without a restart.
 */
@Singleton
class RegionProvider @Inject constructor(
    settings: SettingsRepository,
    @ApplicationScope scope: CoroutineScope,
) {
    private val fallback: String = Locale.getDefault().country.takeIf { it.length == 2 } ?: "US"

    private val region: StateFlow<String> = settings.appSettings
        .map { it.region ?: fallback }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, fallback)

    suspend fun region(): String = region.value
}
