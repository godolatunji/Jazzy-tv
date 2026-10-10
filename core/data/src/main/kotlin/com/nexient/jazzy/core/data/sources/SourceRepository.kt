package com.nexient.jazzy.core.data.sources

import com.nexient.jazzy.core.common.Clock
import com.nexient.jazzy.core.common.DAY_MS
import com.nexient.jazzy.core.common.IoDispatcher
import com.nexient.jazzy.core.common.MINUTE_MS
import com.nexient.jazzy.core.data.settings.SecretCipher
import com.nexient.jazzy.core.database.app.ProviderDao
import com.nexient.jazzy.core.database.app.ProviderSettingEntity
import com.nexient.jazzy.core.database.app.SourceFlagEntity
import com.nexient.jazzy.core.database.cache.SourceCacheDao
import com.nexient.jazzy.core.database.cache.SourceResultEntity
import com.nexient.jazzy.core.model.ProviderHealth
import com.nexient.jazzy.core.model.ProviderInfo
import com.nexient.jazzy.core.model.ProviderSettings
import com.nexient.jazzy.core.model.ProviderStatus
import com.nexient.jazzy.core.model.SourceQuery
import com.nexient.jazzy.core.model.SourceSearchResult
import com.nexient.jazzy.core.model.TorrentSource
import com.nexient.jazzy.core.sources.ProviderSettingsStore
import com.nexient.jazzy.core.sources.SourceSearcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Torrent source search with the 15-minute result cache (SL-9) and bad-source flags (SL-7).
 * Also stores per-site settings and health for `:core:sources` (SRC-5).
 */
@Singleton
class SourceRepository @Inject constructor(
    private val searcher: SourceSearcher,
    private val cacheDao: SourceCacheDao,
    private val providerDao: ProviderDao,
    private val json: Json,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    private val serializer = ListSerializer(TorrentSource.serializer())

    val providers: List<ProviderInfo> get() = searcher.providers

    /**
     * Emits cached rows first (marked `fromCache`) when they're under 15 minutes old, then runs
     * the live search unless the cache was fresh and [force] is false. Live results are cached.
     */
    fun search(query: SourceQuery, force: Boolean = false): Flow<SourceSearchResult> = flow {
        val cached = cacheDao.get(query.cacheKey)
        val fresh = cached != null && clock.nowMillis() - cached.fetchedAt < CACHE_TTL
        if (cached != null) {
            val rows = runCatching { json.decodeFromString(serializer, cached.json) }.getOrDefault(emptyList())
            emit(SourceSearchResult(query, rows, emptyList(), isComplete = fresh && !force, fromCache = true))
            if (fresh && !force) return@flow
        }
        searcher.search(query).collect { result ->
            emit(result)
            if (result.isComplete) {
                cacheDao.put(SourceResultEntity(query.cacheKey, json.encodeToString(serializer, result.sources), clock.nowMillis()))
            }
        }
    }.flowOn(io)

    suspend fun resolveMagnet(source: TorrentSource): String = withContext(io) {
        source.magnet ?: searcher.resolveMagnet(source)
    }

    /** Info-hashes that failed recently (SL-7), refreshed as flags expire. */
    fun activeFlags(): Flow<Set<String>> = providerDao.observeActiveFlags(clock.nowMillis()).map { rows -> rows.map { it.infoHash }.toSet() }

    suspend fun flagBad(infoHash: String, reason: String) = withContext(io) {
        providerDao.flag(SourceFlagEntity(infoHash.lowercase(), clock.nowMillis() + DAY_MS, reason))
    }

    suspend fun testProvider(providerId: String): ProviderStatus = withContext(io) { searcher.testProvider(providerId) }

    companion object {
        const val CACHE_TTL = 15 * MINUTE_MS
    }
}

/** jazzy.db-backed [ProviderSettingsStore]; secrets are Keystore-encrypted (SRC-3). */
@Singleton
class DbProviderSettingsStore @Inject constructor(
    private val dao: ProviderDao,
    private val cipher: SecretCipher,
    @IoDispatcher private val io: CoroutineDispatcher,
) : ProviderSettingsStore {

    /** Only rows with an explicit enable flag; providers missing from the map use their defaults. */
    override val settings: Flow<Map<String, ProviderSettings>> = dao.observeAll().map { rows ->
        rows.filter { it.enabled != null }.associate { e ->
            e.providerId to ProviderSettings(
                providerId = e.providerId,
                enabled = e.enabled == true,
                baseUrlOverride = e.baseUrlOverride,
                username = e.username,
                password = cipher.decrypt(e.passwordEncrypted),
                cookies = cipher.decrypt(e.cookiesEncrypted),
            )
        }
    }

    val health: Flow<Map<String, ProviderHealth>> = dao.observeAll().map { rows ->
        rows.associate { it.providerId to ProviderHealth(it.providerId, it.lastOkAt, it.avgLatencyMs, it.errorCount, it.lastError) }
    }

    override suspend fun recordResult(providerId: String, ok: Boolean, latencyMs: Long, error: String?) = withContext(io) {
        val existing = dao.get(providerId) ?: ProviderSettingEntity(providerId, null, null, null, null, null, null, null, 0, null)
        val avg = existing.avgLatencyMs?.let { (it * 3 + latencyMs) / 4 } ?: latencyMs
        dao.upsert(
            if (ok) existing.copy(lastOkAt = System.currentTimeMillis(), avgLatencyMs = avg, errorCount = 0, lastError = null)
            else existing.copy(avgLatencyMs = avg, errorCount = existing.errorCount + 1, lastError = error?.take(200)),
        )
    }

    suspend fun setEnabled(providerId: String, enabled: Boolean) = update(providerId) { it.copy(enabled = enabled) }

    suspend fun setConnection(providerId: String, baseUrl: String?, username: String?, password: String?, cookies: String?) =
        update(providerId) {
            it.copy(
                baseUrlOverride = baseUrl?.trim()?.takeIf(String::isNotEmpty),
                username = username?.trim()?.takeIf(String::isNotEmpty),
                passwordEncrypted = cipher.encrypt(password),
                cookiesEncrypted = cipher.encrypt(cookies?.trim()),
            )
        }

    /** Call before [setConnection] so the row carries an explicit enable flag and is honoured by [settings]. */
    suspend fun ensureExplicit(providerId: String, currentEnabled: Boolean) = update(providerId) {
        if (it.enabled == null) it.copy(enabled = currentEnabled) else it
    }

    private suspend fun update(providerId: String, change: (ProviderSettingEntity) -> ProviderSettingEntity) = withContext(io) {
        val existing = dao.get(providerId) ?: ProviderSettingEntity(providerId, null, null, null, null, null, null, null, 0, null)
        dao.upsert(change(existing))
    }
}
