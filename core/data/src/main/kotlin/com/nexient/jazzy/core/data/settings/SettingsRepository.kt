package com.nexient.jazzy.core.data.settings

import com.nexient.jazzy.core.database.app.AppSettingEntity
import com.nexient.jazzy.core.database.app.SettingsDao
import com.nexient.jazzy.core.model.EngineSettings
import com.nexient.jazzy.core.model.SourceSort
import com.nexient.jazzy.core.torrent.EngineSettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** App-wide settings stored in jazzy.db `app_setting` (spec §5.11: no DataStore). */
data class AppSettings(
    val activeProfileId: Long? = null,
    val rememberProfile: Boolean = false,
    val disclaimerAccepted: Boolean = false,
    val crashReportingOptIn: Boolean = false,
    val metricsOptIn: Boolean = false,
    val autoplayNextDefault: Boolean = true,
    val defaultSourceSort: SourceSort = SourceSort.SEEDERS,
    val showDeadLinks: Boolean = false,
    val hideCamByDefault: Boolean = false,
    val hideUnsupportedByDefault: Boolean = false,
    val removeFromListWhenWatched: Boolean = false,
    val engine: EngineSettings = EngineSettings(),
    val region: String? = null,
)

object SettingKeys {
    const val ACTIVE_PROFILE = "active_profile_id"
    const val REMEMBER_PROFILE = "remember_profile"
    const val DISCLAIMER = "disclaimer_accepted"
    const val CRASH_OPT_IN = "crash_opt_in"
    const val METRICS_OPT_IN = "metrics_opt_in"
    const val AUTOPLAY_NEXT = "autoplay_next"
    const val SOURCE_SORT = "source_sort"
    const val SHOW_DEAD = "show_dead_links"
    const val HIDE_CAM = "hide_cam"
    const val HIDE_UNSUPPORTED = "hide_unsupported"
    const val REMOVE_WHEN_WATCHED = "remove_from_list_when_watched"
    const val WIFI_ONLY = "engine_wifi_only"
    const val UPLOAD_LIMIT = "engine_upload_limit"
    const val STOP_SEEDING = "engine_stop_seeding"
    const val MAX_CONNECTIONS = "engine_max_connections"
    const val CACHE_CAP = "engine_cache_cap"
    const val LISTEN_PORT = "engine_listen_port"
    const val ENCRYPTION = "engine_encryption"
    const val REGION = "region"
    const val SEEDED = "seeded_v1"
}

@Singleton
class SettingsRepository @Inject constructor(
    private val dao: SettingsDao,
) {
    val appSettings: Flow<AppSettings> = dao.observeAllApp().map { rows ->
        val m = rows.associate { it.key to it.value }
        val defaults = EngineSettings()
        AppSettings(
            activeProfileId = m[SettingKeys.ACTIVE_PROFILE]?.toLongOrNull(),
            rememberProfile = m[SettingKeys.REMEMBER_PROFILE].toBool(false),
            disclaimerAccepted = m[SettingKeys.DISCLAIMER].toBool(false),
            crashReportingOptIn = m[SettingKeys.CRASH_OPT_IN].toBool(false),
            metricsOptIn = m[SettingKeys.METRICS_OPT_IN].toBool(false),
            autoplayNextDefault = m[SettingKeys.AUTOPLAY_NEXT].toBool(true),
            defaultSourceSort = m[SettingKeys.SOURCE_SORT]?.let { runCatching { SourceSort.valueOf(it) }.getOrNull() }
                ?: SourceSort.SEEDERS,
            showDeadLinks = m[SettingKeys.SHOW_DEAD].toBool(false),
            hideCamByDefault = m[SettingKeys.HIDE_CAM].toBool(false),
            hideUnsupportedByDefault = m[SettingKeys.HIDE_UNSUPPORTED].toBool(false),
            removeFromListWhenWatched = m[SettingKeys.REMOVE_WHEN_WATCHED].toBool(false),
            engine = EngineSettings(
                wifiOnly = m[SettingKeys.WIFI_ONLY].toBool(defaults.wifiOnly),
                uploadLimitBps = m[SettingKeys.UPLOAD_LIMIT]?.toLongOrNull() ?: defaults.uploadLimitBps,
                stopSeedingOnStop = m[SettingKeys.STOP_SEEDING].toBool(defaults.stopSeedingOnStop),
                maxConnections = m[SettingKeys.MAX_CONNECTIONS]?.toIntOrNull() ?: defaults.maxConnections,
                cacheCapBytes = m[SettingKeys.CACHE_CAP]?.toLongOrNull() ?: defaults.cacheCapBytes,
                listenPort = m[SettingKeys.LISTEN_PORT]?.toIntOrNull() ?: defaults.listenPort,
                preferEncryption = m[SettingKeys.ENCRYPTION].toBool(defaults.preferEncryption),
            ),
            region = m[SettingKeys.REGION],
        )
    }.distinctUntilChanged()

    suspend fun put(key: String, value: Any?) {
        if (value == null) dao.removeApp(key) else dao.putApp(AppSettingEntity(key, value.toString()))
    }

    suspend fun get(key: String): String? = dao.getApp(key)

    private fun String?.toBool(default: Boolean) = this?.toBooleanStrictOrNull() ?: default
}

/** `:core:torrent` reads its settings through this view of [SettingsRepository]. */
@Singleton
class DbEngineSettingsStore @Inject constructor(repository: SettingsRepository) : EngineSettingsStore {
    override val settings: Flow<EngineSettings> = repository.appSettings.map { it.engine }.distinctUntilChanged()
}
