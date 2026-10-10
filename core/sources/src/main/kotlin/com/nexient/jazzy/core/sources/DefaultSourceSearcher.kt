package com.nexient.jazzy.core.sources

import com.nexient.jazzy.core.model.FailureReason
import com.nexient.jazzy.core.model.MediaType
import com.nexient.jazzy.core.model.ProviderAuthKind
import com.nexient.jazzy.core.model.ProviderInfo
import com.nexient.jazzy.core.model.ProviderSearchState
import com.nexient.jazzy.core.model.ProviderSettings
import com.nexient.jazzy.core.model.ProviderStatus
import com.nexient.jazzy.core.model.ReleaseInfo
import com.nexient.jazzy.core.model.SourceQuery
import com.nexient.jazzy.core.model.SourceSearchResult
import com.nexient.jazzy.core.model.TorrentSource
import com.nexient.jazzy.core.sources.di.SourcesHttpClient
import com.nexient.jazzy.core.sources.engine.BlockedException
import com.nexient.jazzy.core.sources.engine.HttpStatusException
import com.nexient.jazzy.core.sources.engine.LoginRequiredException
import com.nexient.jazzy.core.sources.engine.Magnets
import com.nexient.jazzy.core.sources.engine.ParseException
import com.nexient.jazzy.core.sources.engine.ProviderAuth
import com.nexient.jazzy.core.sources.engine.ProviderDefinition
import com.nexient.jazzy.core.sources.engine.ProviderRegistry
import com.nexient.jazzy.core.sources.engine.Torrent
import com.nexient.jazzy.core.sources.engine.TorrentProvider
import com.nexient.jazzy.core.sources.parse.QueryBuilder
import com.nexient.jazzy.core.sources.parse.ReleaseParser
import com.nexient.jazzy.core.sources.parse.ResultMatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerializationException
import okhttp3.OkHttpClient
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLException
import kotlin.time.TimeSource

/**
 * [SourceSearcher] over the built-in torrent-site providers (spec §5.8).
 *
 * For each search it reads [ProviderSettingsStore.settings] (enable flag, mirror URL,
 * credentials/cookies), runs the [QueryBuilder] queries against every enabled provider that has
 * the media type, in parallel, each provider bounded by a 15 s timeout, and emits after every
 * provider finishes. Rows go through [ReleaseParser] and [ResultMatcher], then [SourceMerger].
 * Each provider's outcome is reported to [ProviderSettingsStore.recordResult].
 */
@Singleton
class DefaultSourceSearcher internal constructor(
    private val engineProviders: List<TorrentProvider>,
    private val settingsStore: ProviderSettingsStore,
    private val dispatcher: CoroutineDispatcher,
    private val providerTimeoutMs: Long = PROVIDER_TIMEOUT_MS,
    private val magnetTimeoutMs: Long = MAGNET_TIMEOUT_MS,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) : SourceSearcher {

    @Inject
    constructor(
        @SourcesHttpClient client: OkHttpClient,
        settingsStore: ProviderSettingsStore,
    ) : this(ProviderRegistry.allProviders(client), settingsStore, Dispatchers.IO)

    override val providers: List<ProviderInfo> = engineProviders.map { it.toProviderInfo() }

    override fun search(query: SourceQuery): Flow<SourceSearchResult> = channelFlow {
        val settings = currentSettings()
        val queries = QueryBuilder.build(query)
        val statuses = LinkedHashMap<String, ProviderStatus>()
        val active = mutableListOf<TorrentProvider>()
        for (provider in engineProviders) {
            if (!isEnabled(provider, settings[provider.id])) continue
            val supported = when (query.mediaType) {
                MediaType.MOVIE -> provider.supportsMovies
                MediaType.TV -> provider.supportsTv
            }
            statuses[provider.id] = provider.status(if (supported) ProviderSearchState.Searching else ProviderSearchState.Unsupported)
            if (supported) active += provider
        }

        val rows = mutableListOf<TorrentSource>()
        val mutex = Mutex()
        send(SourceSearchResult(query, emptyList(), statuses.values.toList(), isComplete = active.isEmpty()))

        for (provider in active) {
            launch {
                val start = timeSource.markNow()
                val outcome = searchProvider(provider, settings[provider.id], query, queries)
                val latencyMs = start.elapsedNow().inWholeMilliseconds
                mutex.withLock {
                    rows += outcome.rows
                    statuses[provider.id] = provider.status(outcome.state)
                    val complete = statuses.values.none { it.state == ProviderSearchState.Searching }
                    send(SourceSearchResult(query, SourceMerger.mergeAndSort(rows), statuses.values.toList(), complete))
                }
                record(provider.id, outcome.state, latencyMs)
            }
        }
        // channelFlow completes once every provider coroutine has finished.
    }.flowOn(dispatcher)

    override suspend fun resolveMagnet(source: TorrentSource): String = withContext(dispatcher) {
        source.magnet?.takeIf(Magnets::isMagnet)?.let { return@withContext it }
        source.infoHash?.let { return@withContext Magnets.build(it, source.name) }
        val settings = currentSettings()
        var lastError: Throwable? = null
        var triedAll = false
        val magnet = withTimeoutOrNull(magnetTimeoutMs) {
            for (providerId in source.providerIds) {
                val provider = provider(providerId) ?: continue
                val desc = source.resolveHint[hintKey(providerId, HINT_DESC)] ?: continue
                try {
                    configure(provider, settings[providerId])
                    val found = provider.getMagnet(Torrent(provider = provider.name, title = source.name, desc = desc))
                    if (Magnets.isMagnet(found)) return@withTimeoutOrNull found
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    lastError = e
                }
            }
            triedAll = true
            null
        }
        if (magnet != null) return@withContext magnet
        val error = lastError
        val failure = when {
            !triedAll -> ProviderSearchState.Failed(FailureReason.TIMEOUT, "Timed out getting the magnet link")
            error != null -> failureOf(error)
            else -> ProviderSearchState.Failed(FailureReason.PARSE_ERROR, "No magnet link found for this source")
        }
        throw MagnetResolutionException(failure.reason, failure.message ?: "Couldn't get a magnet link", error)
    }

    override suspend fun testProvider(providerId: String): ProviderStatus = withContext(dispatcher) {
        val provider = provider(providerId)
            ?: return@withContext ProviderStatus(providerId, providerId, ProviderSearchState.Failed(FailureReason.OTHER, "Unknown site"))
        val settings = currentSettings()[providerId]
        val start = timeSource.markNow()
        val state = try {
            val auth = configure(provider, settings)
            if (provider.definition.requireAuthentication && auth == ProviderAuth.None) {
                ProviderSearchState.Failed(FailureReason.LOGIN_REQUIRED, LOGIN_REQUIRED_MESSAGE)
            } else {
                val results = withTimeout(providerTimeoutMs) { provider.search(TEST_QUERY) }
                if (results == null) ProviderSearchState.Unsupported else ProviderSearchState.Done(results.size)
            }
        } catch (e: TimeoutCancellationException) {
            failureOf(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failureOf(e)
        }
        record(providerId, state, start.elapsedNow().inWholeMilliseconds)
        provider.status(state)
    }

    // region Search internals

    private class Outcome(val state: ProviderSearchState, val rows: List<TorrentSource> = emptyList())

    private suspend fun searchProvider(
        provider: TorrentProvider,
        settings: ProviderSettings?,
        query: SourceQuery,
        queries: List<String>,
    ): Outcome {
        try {
            val auth = configure(provider, settings)
            provider.enable(auth)
        } catch (_: IllegalArgumentException) {
            return Outcome(ProviderSearchState.Failed(FailureReason.LOGIN_REQUIRED, LOGIN_REQUIRED_MESSAGE))
        }
        val category = when (query.mediaType) {
            MediaType.MOVIE -> provider.definition.movieCategory
            MediaType.TV -> provider.definition.tvCategory
        } ?: return Outcome(ProviderSearchState.Unsupported)
        val imdbId = query.imdbId?.takeIf { it.isNotBlank() }
        val providerQueries = if (provider.definition.supportsImdbSearch && imdbId != null && query.mediaType == MediaType.MOVIE) {
            listOf(imdbId)
        } else {
            queries
        }

        // Queries run in parallel; each is bounded by the provider timeout, so the provider as a
        // whole answers within it too. Any successful query makes the provider "Done".
        val attempts: List<Result<List<Torrent>?>> = coroutineScope {
            providerQueries.map { text ->
                async {
                    try {
                        Result.success(withTimeout(providerTimeoutMs) { provider.search(text, category.ifEmpty { null }) })
                    } catch (e: TimeoutCancellationException) {
                        Result.failure(e)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                }
            }.awaitAll()
        }

        val found = attempts.mapNotNull { it.getOrNull() }
        if (found.isEmpty()) {
            val error = attempts.firstNotNullOfOrNull { it.exceptionOrNull() }
            return Outcome(if (error == null) ProviderSearchState.Unsupported else failureOf(error))
        }
        val rows = found.flatten()
            .distinctBy { it.key }
            .mapNotNull { torrent ->
                val parsed = ReleaseParser.parseDetailed(torrent.title)
                if (ResultMatcher.matches(query, parsed, torrent.extras["imdb"])) toSource(provider, torrent, parsed.info) else null
            }
        val count = SourceMerger.merge(rows).size
        return Outcome(ProviderSearchState.Done(count), rows)
    }

    /** Applies mirror URL and auth from [settings]; returns the auth that was set. */
    private fun configure(provider: TorrentProvider, settings: ProviderSettings?): ProviderAuth {
        provider.overrideBaseUrl(settings?.baseUrlOverride)
        val auth = authFor(provider.definition, settings)
        provider.setAuth(auth)
        return auth
    }

    private fun isEnabled(provider: TorrentProvider, settings: ProviderSettings?): Boolean =
        settings?.enabled ?: provider.definition.enabledByDefault

    private suspend fun currentSettings(): Map<String, ProviderSettings> = try {
        settingsStore.settings.first()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        emptyMap() // fall back to defaults rather than failing the whole search
    }

    private suspend fun record(providerId: String, state: ProviderSearchState, latencyMs: Long) {
        val (ok, error) = when (state) {
            is ProviderSearchState.Done -> true to null
            is ProviderSearchState.Failed -> false to listOfNotNull(state.reason.name, state.message).joinToString(": ")
            else -> return
        }
        try {
            settingsStore.recordResult(providerId, ok, latencyMs, error)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Health bookkeeping must never break a search.
        }
    }

    private fun provider(id: String): TorrentProvider? = engineProviders.firstOrNull { it.id == id }

    private fun TorrentProvider.status(state: ProviderSearchState) = ProviderStatus(id, name, state)

    // endregion

    internal companion object {
        const val PROVIDER_TIMEOUT_MS = 15_000L

        /** SRC-7: "Getting link…" gives up after 5 s, then the UI offers a retry. */
        const val MAGNET_TIMEOUT_MS = 5_000L

        /** A small, legal, well-seeded search (Blender open movie). YTS answers it too. */
        const val TEST_QUERY = "big buck bunny"

        const val HINT_DESC = "desc"
        const val HINT_LINK = "link"
        private const val LOGIN_REQUIRED_MESSAGE = "Add a login or cookies for this site"

        fun hintKey(providerId: String, field: String) = "$providerId:$field"

        fun authFor(definition: ProviderDefinition, settings: ProviderSettings?): ProviderAuth {
            settings ?: return ProviderAuth.None
            val cookies = settings.cookies.orEmpty().split(';', '\n').map { it.trim() }.filter { '=' in it }
            val username = settings.username?.trim().orEmpty()
            val password = settings.password.orEmpty()
            return when {
                cookies.isNotEmpty() && definition.supportCookiesAuthentication -> ProviderAuth.Cookies(cookies)
                username.isNotEmpty() && password.isNotEmpty() && definition.supportCredentialsAuthentication ->
                    ProviderAuth.Credentials(username, password)
                else -> ProviderAuth.None
            }
        }

        fun toSource(provider: TorrentProvider, torrent: Torrent, release: ReleaseInfo): TorrentSource {
            val listedMagnet = torrent.magnet?.takeIf(Magnets::isMagnet)
            val hash = torrent.infoHash
            val siteKey = torrent.desc ?: torrent.link ?: torrent.title
            return TorrentSource(
                key = hash ?: "${provider.id}:$siteKey",
                name = torrent.title,
                infoHash = hash,
                magnet = listedMagnet ?: hash?.let { Magnets.build(it, torrent.title) },
                sizeBytes = torrent.extras["sizeBytes"]?.toLongOrNull()
                    ?: torrent.sizeBytes
                    ?: listedMagnet?.let(Magnets::exactLength),
                seeders = (torrent.seeds ?: 0).coerceAtLeast(0),
                leechers = (torrent.peers ?: 0).coerceAtLeast(0),
                providerIds = listOf(provider.id),
                providerNames = listOf(provider.name),
                release = release,
                resolveHint = buildMap {
                    torrent.desc?.let { put(hintKey(provider.id, HINT_DESC), it) }
                    torrent.link?.let { put(hintKey(provider.id, HINT_LINK), it) }
                },
                uploadedAt = torrent.time,
            )
        }

        /** Maps an engine/network failure to a user-facing reason. Messages never include URLs. */
        fun failureOf(error: Throwable): ProviderSearchState.Failed = when (error) {
            is TimeoutCancellationException -> ProviderSearchState.Failed(FailureReason.TIMEOUT, "Timed out")
            is BlockedException -> ProviderSearchState.Failed(FailureReason.BLOCKED, "Blocked by the site")
            is LoginRequiredException -> ProviderSearchState.Failed(FailureReason.LOGIN_REQUIRED, LOGIN_REQUIRED_MESSAGE)
            is HttpStatusException -> ProviderSearchState.Failed(FailureReason.HTTP_ERROR, "HTTP ${error.code}")
            is ParseException, is SerializationException ->
                ProviderSearchState.Failed(FailureReason.PARSE_ERROR, "Unexpected response from the site")
            is InterruptedIOException -> ProviderSearchState.Failed(FailureReason.TIMEOUT, "Timed out")
            is UnknownHostException -> ProviderSearchState.Failed(FailureReason.UNREACHABLE, "Site not found (DNS)")
            is ConnectException, is NoRouteToHostException ->
                ProviderSearchState.Failed(FailureReason.UNREACHABLE, "Site unreachable")
            is SSLException -> ProviderSearchState.Failed(FailureReason.UNREACHABLE, "Secure connection failed")
            is IOException -> ProviderSearchState.Failed(FailureReason.UNREACHABLE, "Network error")
            else -> ProviderSearchState.Failed(FailureReason.OTHER, error::class.java.simpleName)
        }

        private fun TorrentProvider.toProviderInfo() = ProviderInfo(
            id = id,
            name = name,
            isPublic = !definition.requireAuthentication,
            enabledByDefault = definition.enabledByDefault,
            defaultBaseUrl = definition.baseUrl,
            supportsMovies = supportsMovies,
            supportsTv = supportsTv,
            authKinds = buildSet {
                if (definition.supportCredentialsAuthentication) add(ProviderAuthKind.CREDENTIALS)
                if (definition.supportCookiesAuthentication) add(ProviderAuthKind.COOKIES)
            },
        )
    }
}

/** [SourceSearcher.resolveMagnet] couldn't produce a magnet. [reason] says why (timeout, blocked...). */
class MagnetResolutionException(
    val reason: FailureReason,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
