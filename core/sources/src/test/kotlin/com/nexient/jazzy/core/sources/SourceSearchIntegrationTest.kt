package com.nexient.jazzy.core.sources

import com.google.common.truth.Truth.assertThat
import com.nexient.jazzy.core.model.MediaType
import com.nexient.jazzy.core.model.ProviderSearchState
import com.nexient.jazzy.core.model.ProviderSettings
import com.nexient.jazzy.core.model.SourceQuery
import com.nexient.jazzy.core.sources.engine.ProviderRegistry
import com.nexient.jazzy.core.sources.engine.TestSupport
import com.nexient.jazzy.core.sources.engine.baseUrl
import com.nexient.jazzy.core.sources.engine.fixture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * The real provider definitions (ThePirateBay, YTS, KickassTorrents) pointed at MockWebServer
 * through mirror-URL settings. Uses runBlocking: real I/O must not run in virtual time.
 */
class SourceSearchIntegrationTest {
    private lateinit var server: MockWebServer

    @Before fun setUp() {
        server = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val target = request.target
                    val body = when {
                        target.startsWith("/q.php") -> fixture("thepiratebay-search.json")
                        target.startsWith("/api/v2/list_movies.json") -> fixture("yts-search.json")
                        target.startsWith("/search?query=") -> fixture("kickasstorrents-search.html")
                        target == "/torrent/102" -> fixture("kickasstorrents-detail.html").replace("DD8255ECDC7CA55FB0BBF81323D87062DB1F6D1C", "1111111111111111111111111111111111111111")
                        else -> return MockResponse.Builder().code(404).build()
                    }
                    return MockResponse.Builder().body(body).build()
                }
            }
            start()
        }
    }

    @After fun tearDown() = server.close()

    private class Store(initial: Map<String, ProviderSettings>) : ProviderSettingsStore {
        override val settings = MutableStateFlow(initial)
        val results = mutableListOf<Pair<String, Boolean>>()
        override suspend fun recordResult(providerId: String, ok: Boolean, latencyMs: Long, error: String?) {
            synchronized(results) { results += providerId to ok }
        }
    }

    private val store by lazy {
        Store(
            ProviderRegistry.allProviders(TestSupport.client).associate { p ->
                p.id to ProviderSettings(
                    providerId = p.id,
                    enabled = p.id in setOf("thepiratebay", "yts", "kickasstorrents"),
                    baseUrlOverride = server.baseUrl(),
                )
            },
        )
    }

    private val bunny = SourceQuery(
        tmdbId = 10378, mediaType = MediaType.MOVIE, title = "Big Buck Bunny", originalTitle = "Big Buck Bunny",
        year = 2008, imdbId = "tt1254207",
    )

    @Test fun `searches real definitions, matches, merges across sites and resolves lazily`() = runBlocking {
        val searcher = DefaultSourceSearcher(ProviderRegistry.allProviders(TestSupport.client), store, Dispatchers.IO)

        val emissions = searcher.search(bunny).toList()
        val last = emissions.last()

        assertThat(emissions.first().statuses.map { it.state }.distinct()).containsExactly(ProviderSearchState.Searching)
        assertThat(emissions).hasSize(4)
        assertThat(last.isComplete).isTrue()
        assertThat(last.statuses.map { it.providerId to it.state }).containsExactly(
            "thepiratebay" to ProviderSearchState.Done(1), // "Sintel 1080p" is dropped by the matcher
            "yts" to ProviderSearchState.Done(2),
            "kickasstorrents" to ProviderSearchState.Done(2),
        )
        // YTS was asked by IMDb ID; the others by title and year in their Movies category.
        val targets = (1..server.requestCount).map { server.takeRequest().target }
        assertThat(targets).contains("/api/v2/list_movies.json?query_term=tt1254207&sort=seeds&order=desc&set=1")
        assertThat(targets).contains("/q.php?q=Big%20Buck%20Bunny%202008&cat=200")
        assertThat(targets).contains("/search?query=Big%20Buck%20Bunny%202008&cat=1&sort_by=seeders&sort_direction=desc")

        // TPB and YTS list the same 1080p hash; KAT's hash-less "Big Buck Bunny 1080p" row joins
        // that group by normalised name + size within 1% (TPB: 276445467 B, KAT: 276.4 MB).
        val top = last.sources.first()
        assertThat(top.infoHash).isEqualTo("dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c")
        assertThat(top.seeders).isEqualTo(1204)
        assertThat(top.providerIds).containsExactly("thepiratebay", "yts", "kickasstorrents")
        assertThat(top.magnet).startsWith("magnet:?xt=urn:btih:")

        // KAT's 720p row has no hash and no YTS/TPB twin: it's resolved from the detail page on Play.
        val kat720 = last.sources.single { it.providerIds == listOf("kickasstorrents") }
        assertThat(kat720.magnet).isNull()
        assertThat(kat720.key).startsWith("kickasstorrents:")
        assertThat(searcher.resolveMagnet(kat720)).startsWith("magnet:?xt=urn:btih:1111111111111111111111111111111111111111")

        assertThat(store.results.map { it.second }.distinct()).containsExactly(true)
        Unit
    }
}
