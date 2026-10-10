package com.nexient.jazzy.core.sources

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.nexient.jazzy.core.model.FailureReason
import com.nexient.jazzy.core.model.MediaType
import com.nexient.jazzy.core.model.ProviderAuthKind
import com.nexient.jazzy.core.model.ProviderSearchState
import com.nexient.jazzy.core.model.ProviderSettings
import com.nexient.jazzy.core.model.ReleaseInfo
import com.nexient.jazzy.core.model.SourceQuery
import com.nexient.jazzy.core.model.TorrentSource
import com.nexient.jazzy.core.sources.engine.BlockedException
import com.nexient.jazzy.core.sources.engine.FakeProvider
import com.nexient.jazzy.core.sources.engine.ProviderRegistry
import com.nexient.jazzy.core.sources.engine.TestSupport
import com.nexient.jazzy.core.sources.engine.Torrent
import com.nexient.jazzy.core.sources.engine.TorrentProvider
import com.nexient.jazzy.core.sources.engine.torrent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultSourceSearcherTest {

    private class Recorded(val providerId: String, val ok: Boolean, val latencyMs: Long, val error: String?)

    private class FakeSettingsStore(initial: Map<String, ProviderSettings> = emptyMap()) : ProviderSettingsStore {
        val state = MutableStateFlow(initial)
        override val settings = state
        val recorded = java.util.Collections.synchronizedList(mutableListOf<Recorded>())
        override suspend fun recordResult(providerId: String, ok: Boolean, latencyMs: Long, error: String?) {
            recorded += Recorded(providerId, ok, latencyMs, error)
        }
    }

    private val store = FakeSettingsStore()

    private fun TestScope.searcher(vararg providers: TorrentProvider, settings: FakeSettingsStore = store) =
        DefaultSourceSearcher(
            engineProviders = providers.toList(),
            settingsStore = settings,
            dispatcher = StandardTestDispatcher(testScheduler),
            timeSource = testScheduler.timeSource,
        )

    private val sintel = SourceQuery(
        tmdbId = 45745, mediaType = MediaType.MOVIE, title = "Sintel", originalTitle = "Sintel", year = 2010, imdbId = "tt1727587",
    )
    private val episode = SourceQuery(
        tmdbId = 7, mediaType = MediaType.TV, title = "Harbor Lights", originalTitle = null, year = 2019, imdbId = null,
        season = 1, episode = 2,
    )

    private val hashA = "dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c"
    private val hashB = "08ada5a7a6183aae1e09d831df6748d566095a10"
    private val hashC = "c87ba9523d37b0fbcceb30f0c4d11e917d9ed0dc"

    // region Progressive emission

    @Test fun `emits initially, after each provider, and completes last`() = runTest {
        val slow = CompletableDeferred<List<Torrent>>()
        val s = searcher(
            FakeProvider("fast") { _, _ -> listOf(torrent("fast", "Sintel.2010.720p.WEB-DL", 3, hashB)) },
            FakeProvider("slow") { _, _ -> slow.await() },
            FakeProvider("broken") { _, _ -> throw BlockedException("Broken") },
            FakeProvider("tvonly", movieCategory = null) { _, _ -> emptyList() },
            FakeProvider("off", enabledByDefault = false) { _, _ -> error("must not be searched") },
        )

        s.search(sintel).test {
            val initial = awaitItem()
            assertThat(initial.isComplete).isFalse()
            assertThat(initial.sources).isEmpty()
            assertThat(initial.statuses.map { it.providerId to it.state }).containsExactly(
                "fast" to ProviderSearchState.Searching,
                "slow" to ProviderSearchState.Searching,
                "broken" to ProviderSearchState.Searching,
                "tvonly" to ProviderSearchState.Unsupported,
            ).inOrder()

            val afterTwo = List(2) { awaitItem() }.last()
            assertThat(afterTwo.isComplete).isFalse()
            assertThat(afterTwo.state("fast")).isEqualTo(ProviderSearchState.Done(1))
            assertThat(afterTwo.state("broken")).isEqualTo(ProviderSearchState.Failed(FailureReason.BLOCKED, "Blocked by the site"))
            assertThat(afterTwo.state("slow")).isEqualTo(ProviderSearchState.Searching)
            assertThat(afterTwo.sources.map { it.name }).containsExactly("Sintel.2010.720p.WEB-DL")

            slow.complete(listOf(torrent("slow", "Sintel.2010.1080p.BluRay.x264", 50, hashA)))
            val done = awaitItem()
            assertThat(done.isComplete).isTrue()
            assertThat(done.state("slow")).isEqualTo(ProviderSearchState.Done(1))
            assertThat(done.sources.map { it.name }).containsExactly("Sintel.2010.1080p.BluRay.x264", "Sintel.2010.720p.WEB-DL").inOrder()
            assertThat(done.query).isEqualTo(sintel)
            awaitComplete()
        }
        assertThat(store.recorded.map { Triple(it.providerId, it.ok, it.error) }).containsExactly(
            Triple("fast", true, null),
            Triple("broken", false, "BLOCKED: Blocked by the site"),
            Triple("slow", true, null),
        )
    }

    @Test fun `a hanging provider times out after 15 s of virtual time without blocking others`() = runTest {
        val s = searcher(
            FakeProvider("ok") { _, _ -> listOf(torrent("ok", "Sintel 2010 1080p", 1, hashA)) },
            FakeProvider("hang") { _, _ -> awaitCancellation() },
        )
        s.search(sintel).test {
            skipItems(2) // initial + "ok" finished
            val last = awaitItem()
            assertThat(testScheduler.currentTime).isEqualTo(DefaultSourceSearcher.PROVIDER_TIMEOUT_MS)
            assertThat(last.state("hang")).isEqualTo(ProviderSearchState.Failed(FailureReason.TIMEOUT, "Timed out"))
            assertThat(last.sources.map { it.name }).containsExactly("Sintel 2010 1080p")
            assertThat(last.isComplete).isTrue()
            awaitComplete()
        }
        assertThat(store.recorded.single { it.providerId == "hang" }.latencyMs).isEqualTo(15_000)
    }

    @Test fun `latency is measured per provider`() = runTest {
        val s = searcher(FakeProvider("slowish") { _, _ -> delay(3_000); emptyList() })
        s.search(sintel).toList()
        assertThat(store.recorded.single().latencyMs).isEqualTo(3_000)
    }

    @Test fun `no enabled provider gives one complete emission`() = runTest {
        val s = searcher(FakeProvider("off", enabledByDefault = false) { _, _ -> emptyList() })
        s.search(sintel).test {
            val only = awaitItem()
            assertThat(only.isComplete).isTrue()
            assertThat(only.statuses).isEmpty()
            awaitComplete()
        }
    }

    @Test fun `unsupported media type only gives one complete emission`() = runTest {
        val s = searcher(FakeProvider("moviesonly", tvCategory = null) { _, _ -> emptyList() })
        s.search(episode).test {
            val only = awaitItem()
            assertThat(only.isComplete).isTrue()
            assertThat(only.state("moviesonly")).isEqualTo(ProviderSearchState.Unsupported)
            awaitComplete()
        }
        assertThat(store.recorded).isEmpty()
    }

    // endregion

    // region Queries, categories, matching and merging

    @Test fun `movie queries use the movie category, original title and imdb id where supported`() = runTest {
        val plain = FakeProvider("plain") { _, _ -> emptyList() }
        val imdb = FakeProvider("imdbsite", supportsImdbSearch = true, tvCategory = null) { _, _ -> emptyList() }
        val query = sintel.copy(title = "Amélie", originalTitle = "Le Fabuleux Destin d'Amélie Poulain", year = 2001, imdbId = "tt0211915")
        searcher(plain, imdb).search(query).toList()
        assertThat(plain.searches).containsExactly("Amelie 2001" to "Movies", "Le Fabuleux Destin dAmelie Poulain 2001" to "Movies")
        assertThat(imdb.searches).containsExactly("tt0211915" to "Movies")
    }

    @Test fun `providers without a movie category search their default category`() = runTest {
        val all = FakeProvider("allonly", movieCategory = "All", tvCategory = "All") { _, _ -> emptyList() }
        searcher(all).search(sintel).toList()
        assertThat(all.searches).containsExactly("Sintel 2010" to "All")
    }

    @Test fun `episode search runs three queries in the tv category and keeps only matching rows`() = runTest {
        val site = FakeProvider("site") { q, _ ->
            when (q) {
                "Harbor Lights S01E02" -> listOf(
                    torrent("site", "Harbor.Lights.S01E02.1080p.WEB.H264", 40, hashA),
                    torrent("site", "Harbor.Lights.S01E03.1080p.WEB.H264", 90, hashB),
                    torrent("site", "Other.Show.S01E02.720p", 99, hashC),
                )
                "Harbor Lights S01" -> listOf(torrent("site", "Harbor.Lights.S01.720p.BluRay", 12, "1".repeat(40)))
                else -> listOf(torrent("site", "Harbor Lights Complete Series 1080p", 5, "2".repeat(40)))
            }
        }
        val result = searcher(site).search(episode).toList().last()
        assertThat(site.searches).containsExactly(
            "Harbor Lights S01E02" to "TV",
            "Harbor Lights S01" to "TV",
            "Harbor Lights complete" to "TV",
        )
        assertThat(result.sources.map { it.name }).containsExactly(
            "Harbor.Lights.S01E02.1080p.WEB.H264",
            "Harbor.Lights.S01.720p.BluRay",
            "Harbor Lights Complete Series 1080p",
        ).inOrder()
        assertThat(result.state("site")).isEqualTo(ProviderSearchState.Done(3))
        val pack = result.sources[1]
        assertThat(pack.release.isSeasonPack).isTrue()
        assertThat(pack.release.season).isEqualTo(1)
    }

    @Test fun `the same hash from two sites becomes one row with both sites`() = runTest {
        val s = searcher(
            FakeProvider("thepiratebay") { _, _ -> listOf(torrent("thepiratebay", "Sintel.2010.1080p.BluRay.x264", 40, hashA, "1.2 GB", peers = 4)) },
            FakeProvider("yts") { _, _ -> listOf(torrent("yts", "Sintel (2010) [1080p] [BluRay] [YTS]", 120, hashA.uppercase(), peers = 9)) },
        )
        val last = s.search(sintel).toList().last()
        val row = last.sources.single()
        assertThat(row.key).isEqualTo(hashA)
        assertThat(row.infoHash).isEqualTo(hashA)
        assertThat(row.seeders).isEqualTo(120)
        assertThat(row.leechers).isEqualTo(9)
        assertThat(row.providerIds).containsExactly("thepiratebay", "yts")
        assertThat(row.providerNames).containsExactly("Thepiratebay", "Yts")
        assertThat(row.sizeBytes).isEqualTo(1_200_000_000)
        assertThat(row.release.resolution?.label).isEqualTo("1080p")
    }

    @Test fun `rows without a hash keep a provider-scoped key and a resolve hint`() = runTest {
        val kat = FakeProvider("kickasstorrents") { _, _ ->
            listOf(Torrent("Kickasstorrents", "Sintel 2010 1080p BluRay", seeds = 0, size = "1.2 GB", desc = "https://kat.invalid/torrent/1"))
        }
        val row = searcher(kat).search(sintel).toList().last().sources.single()
        assertThat(row.key).isEqualTo("kickasstorrents:https://kat.invalid/torrent/1")
        assertThat(row.infoHash).isNull()
        assertThat(row.magnet).isNull()
        assertThat(row.seeders).isEqualTo(0) // 0-seeder rows are emitted; the UI hides them
        assertThat(row.resolveHint).containsEntry("kickasstorrents:desc", "https://kat.invalid/torrent/1")
    }

    @Test fun `a provider is done when at least one of its queries succeeded`() = runTest {
        val flaky = FakeProvider("flaky") { q, _ ->
            if (q == "Harbor Lights S01E02") listOf(torrent("flaky", "Harbor.Lights.S01E02.720p", 5, hashA)) else throw IOException("reset")
        }
        val last = searcher(flaky).search(episode).toList().last()
        assertThat(last.state("flaky")).isEqualTo(ProviderSearchState.Done(1))
    }

    @Test fun `when every query fails the first failure is reported`() = runTest {
        val down = FakeProvider("down") { _, _ -> throw UnknownHostException("down.invalid") }
        val last = searcher(down).search(sintel).toList().last()
        assertThat(last.state("down")).isEqualTo(ProviderSearchState.Failed(FailureReason.UNREACHABLE, "Site not found (DNS)"))
        assertThat(store.recorded.single().error).isEqualTo("UNREACHABLE: Site not found (DNS)")
    }

    // endregion

    // region Settings

    @Test fun `settings override enablement, base url and auth`() = runTest {
        val defaultOn = FakeProvider("defaulton") { _, _ -> emptyList() }
        val defaultOff = FakeProvider("defaultoff", enabledByDefault = false) { _, _ -> emptyList() }
        val privateNoAuth = FakeProvider("privnoauth", enabledByDefault = false, requireAuthentication = true) { _, _ -> emptyList() }
        val privateCookies = FakeProvider("privcookies", enabledByDefault = false, requireAuthentication = true) { _, _ -> emptyList() }
        val settings = FakeSettingsStore(
            mapOf(
                "defaulton" to ProviderSettings("defaulton", enabled = false),
                "defaultoff" to ProviderSettings("defaultoff", enabled = true, baseUrlOverride = "https://mirror.invalid/"),
                "privnoauth" to ProviderSettings("privnoauth", enabled = true),
                "privcookies" to ProviderSettings("privcookies", enabled = true, cookies = "uid=1; pass=secret"),
            ),
        )
        val last = searcher(defaultOn, defaultOff, privateNoAuth, privateCookies, settings = settings).search(sintel).toList().last()
        assertThat(last.statuses.map { it.providerId }).containsExactly("defaultoff", "privnoauth", "privcookies").inOrder()
        assertThat(defaultOn.searches).isEmpty()
        assertThat(defaultOff.baseUrl).isEqualTo("https://mirror.invalid")
        assertThat(last.state("privnoauth")).isEqualTo(
            ProviderSearchState.Failed(FailureReason.LOGIN_REQUIRED, "Add a login or cookies for this site"),
        )
        assertThat(privateNoAuth.searches).isEmpty()
        assertThat(last.state("privcookies")).isEqualTo(ProviderSearchState.Done(0))
        assertThat(privateCookies.cookieHeader("https://privcookies.invalid/x")).isEqualTo("uid=1; pass=secret")
    }

    @Test fun `settings are re-read on every search`() = runTest {
        val site = FakeProvider("site") { _, _ -> emptyList() }
        val s = searcher(site)
        s.search(sintel).toList()
        store.state.value = mapOf("site" to ProviderSettings("site", enabled = false))
        assertThat(s.search(sintel).toList().single().statuses).isEmpty()
        assertThat(site.searches).hasSize(1)
    }

    @Test fun `auth prefers cookies and ignores blanks`() {
        val ygg = ProviderRegistry.allProviders(TestSupport.client).single { it.id == "yggtorrent" }.definition
        val tl = ProviderRegistry.allProviders(TestSupport.client).single { it.id == "torrentleech" }.definition
        fun auth(d: com.nexient.jazzy.core.sources.engine.ProviderDefinition, s: ProviderSettings) = DefaultSourceSearcher.authFor(d, s)
        assertThat(auth(ygg, ProviderSettings("yggtorrent", true, username = "me", password = "pw")).toString()).startsWith("Credentials")
        assertThat(auth(ygg, ProviderSettings("yggtorrent", true, username = "me", password = "pw", cookies = "a=1")).toString()).startsWith("Cookies")
        assertThat(auth(tl, ProviderSettings("torrentleech", true, username = "me", password = "pw")).toString()).isEqualTo("None")
        assertThat(auth(ygg, ProviderSettings("yggtorrent", true, username = " ", password = "pw", cookies = "  ")).toString()).isEqualTo("None")
    }

    // endregion

    // region Magnets and testing

    private fun source(magnet: String? = null, hash: String? = null, providers: List<String> = listOf("kickasstorrents"), hints: Map<String, String> = emptyMap()) =
        TorrentSource(
            key = hash ?: "kickasstorrents:x", name = "Sintel 2010 1080p", infoHash = hash, magnet = magnet, sizeBytes = null,
            seeders = 1, leechers = 0, providerIds = providers, providerNames = providers, release = ReleaseInfo(), resolveHint = hints,
        )

    @Test fun `resolveMagnet returns a listed magnet or builds one from the hash`() = runTest {
        val s = searcher()
        assertThat(s.resolveMagnet(source(magnet = "magnet:?xt=urn:btih:$hashA"))).isEqualTo("magnet:?xt=urn:btih:$hashA")
        assertThat(s.resolveMagnet(source(hash = hashA))).startsWith("magnet:?xt=urn:btih:$hashA&dn=Sintel%202010%201080p")
    }

    @Test fun `resolveMagnet scrapes through the owning provider, trying each site`() = runTest {
        val kat = FakeProvider("kickasstorrents", magnets = emptyMap()) { _, _ -> emptyList() }
        val lime = FakeProvider("limetorrents", magnets = mapOf("https://lime.invalid/1" to "magnet:?xt=urn:btih:$hashB")) { _, _ -> emptyList() }
        val magnet = searcher(kat, lime).resolveMagnet(
            source(
                providers = listOf("kickasstorrents", "limetorrents"),
                hints = mapOf("kickasstorrents:desc" to "https://kat.invalid/1", "limetorrents:desc" to "https://lime.invalid/1"),
            ),
        )
        assertThat(magnet).isEqualTo("magnet:?xt=urn:btih:$hashB")
    }

    @Test fun `resolveMagnet fails with a reason when nothing is found`() = runTest {
        val kat = FakeProvider("kickasstorrents") { _, _ -> emptyList() }
        val s = searcher(kat)
        val error = runCatching { s.resolveMagnet(source(hints = mapOf("kickasstorrents:desc" to "https://kat.invalid/1"))) }.exceptionOrNull()
        assertThat(error).isInstanceOf(MagnetResolutionException::class.java)
        assertThat((error as MagnetResolutionException).reason).isEqualTo(FailureReason.PARSE_ERROR)
    }

    @Test fun `resolveMagnet gives up after 5 s`() = runTest {
        val hang = object : TorrentProvider(FakeProvider("kickasstorrents") { _, _ -> null }.definition, TestSupport.client) {
            override suspend fun getMagnet(torrent: Torrent): String? = awaitCancellation()
        }
        val error = runCatching { searcher(hang).resolveMagnet(source(hints = mapOf("kickasstorrents:desc" to "https://kat.invalid/1"))) }
            .exceptionOrNull() as MagnetResolutionException
        assertThat(error.reason).isEqualTo(FailureReason.TIMEOUT)
        assertThat(testScheduler.currentTime).isEqualTo(DefaultSourceSearcher.MAGNET_TIMEOUT_MS)
    }

    @Test fun `testProvider runs the known search even for a disabled site`() = runTest {
        val site = FakeProvider("site", enabledByDefault = false) { q, _ -> listOf(torrent("site", "$q 1080p", 3)) }
        val status = searcher(site).testProvider("site")
        assertThat(status.state).isEqualTo(ProviderSearchState.Done(1))
        assertThat(site.searches).containsExactly(DefaultSourceSearcher.TEST_QUERY to null)
        assertThat(store.recorded.single().ok).isTrue()
    }

    @Test fun `testProvider reports failures, missing logins and unknown ids`() = runTest {
        val broken = FakeProvider("broken") { _, _ -> throw BlockedException("Broken") }
        val private = FakeProvider("private", requireAuthentication = true) { _, _ -> emptyList() }
        val s = searcher(broken, private)
        assertThat(s.testProvider("broken").state).isEqualTo(ProviderSearchState.Failed(FailureReason.BLOCKED, "Blocked by the site"))
        assertThat(s.testProvider("private").state).isEqualTo(
            ProviderSearchState.Failed(FailureReason.LOGIN_REQUIRED, "Add a login or cookies for this site"),
        )
        assertThat((s.testProvider("nope").state as ProviderSearchState.Failed).reason).isEqualTo(FailureReason.OTHER)
    }

    // endregion

    @Test fun `provider list describes every built-in site`() = runTest {
        val infos = DefaultSourceSearcher(ProviderRegistry.allProviders(TestSupport.client), store, StandardTestDispatcher(testScheduler)).providers
        assertThat(infos.map { it.id }).hasSize(14)
        val yts = infos.single { it.id == "yts" }
        assertThat(yts.supportsMovies).isTrue()
        assertThat(yts.supportsTv).isFalse()
        assertThat(yts.enabledByDefault).isTrue()
        val eztv = infos.single { it.id == "eztv" }
        assertThat(eztv.supportsMovies).isFalse()
        assertThat(eztv.enabledByDefault).isFalse()
        val tl = infos.single { it.id == "torrentleech" }
        assertThat(tl.isPublic).isFalse()
        assertThat(tl.authKinds).containsExactly(ProviderAuthKind.COOKIES)
        assertThat(infos.single { it.id == "iptorrents" }.authKinds).containsExactly(ProviderAuthKind.COOKIES, ProviderAuthKind.CREDENTIALS)
    }

    @Test fun `failure mapping never leaks urls`() {
        val failed = DefaultSourceSearcher.failureOf(IOException("GET https://site.invalid/search?q=secret failed"))
        assertThat(failed.message).doesNotContain("secret")
        assertThat(failed.reason).isEqualTo(FailureReason.UNREACHABLE)
        assertThat(DefaultSourceSearcher.failureOf(java.net.SocketTimeoutException("x")).reason).isEqualTo(FailureReason.TIMEOUT)
    }

    private fun com.nexient.jazzy.core.model.SourceSearchResult.state(id: String) = statuses.single { it.providerId == id }.state
}
