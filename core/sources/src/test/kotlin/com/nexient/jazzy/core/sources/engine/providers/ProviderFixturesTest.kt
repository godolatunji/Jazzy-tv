package com.nexient.jazzy.core.sources.engine.providers

import com.nexient.jazzy.core.sources.engine.ProviderAuth
import com.nexient.jazzy.core.sources.engine.ProviderRegistry
import com.nexient.jazzy.core.sources.engine.TestSupport
import com.nexient.jazzy.core.sources.engine.Torrent
import com.nexient.jazzy.core.sources.engine.TorrentProvider
import com.nexient.jazzy.core.sources.engine.baseUrl
import com.nexient.jazzy.core.sources.engine.enqueueBody
import com.nexient.jazzy.core.sources.engine.fixture
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Every provider parsed against a synthetic fixture served by MockWebServer (ported from
 * TorrentSearch's ProviderFixturesTest; see fixtures/README.md).
 */
class ProviderFixturesTest {
    private lateinit var server: MockWebServer
    private val client = TestSupport.client

    @Before fun setUp() {
        server = MockWebServer().apply { start() }
    }

    @After fun tearDown() = server.close()

    private fun <T : TorrentProvider> T.pointedAtServer(auth: ProviderAuth = ProviderAuth.None): T = apply {
        overrideBaseUrl(server.baseUrl())
        enable(auth)
    }

    /** Port of the original snapshot assertion: results exist and the first has every selector field. */
    private fun assertSnapshot(provider: TorrentProvider, results: List<Torrent>?) {
        assertNotNull(results)
        assertTrue("${provider.name} returned no results", results!!.isNotEmpty())
        val first = results.first()
        provider.definition.itemSelectors.keys.forEach { key ->
            val value: Any? = when (key) {
                "title" -> first.title
                "time" -> first.time
                "seeds" -> first.seeds
                "peers" -> first.peers
                "size" -> first.size
                "magnet" -> first.magnet
                "link" -> first.link
                "desc" -> first.desc
                else -> first.extras[key]
            }
            assertNotNull("$key is missing in ${provider.name}: $first", value)
        }
    }

    @Test fun `1337x snapshot`() = runBlocking {
        server.enqueueBody(fixture("1337x-search.html"))
        val provider = X1337x(client).pointedAtServer()
        val results = provider.search("1080")
        assertSnapshot(provider, results)
        assertEquals("/search/1080/1/", server.takeRequest().url.encodedPath)
        assertEquals(listOf("Sintel.2010.1080p.BluRay.x264-OPENFILM", "Tears of Steel (2012) [720p] [WEB-DL] [AAC2.0]"), results!!.map { it.title })
        assertEquals("1.2 GB", results.first().size)
        assertEquals(1276, results[1].seeds)
    }

    @Test fun `1337x movie search uses the Movies category`() = runBlocking {
        server.enqueueBody(fixture("1337x-search.html"))
        X1337x(client).pointedAtServer().search("sintel 2010", "Movies")
        assertEquals("/category-search/sintel%202010/Movies/1/", server.takeRequest().url.encodedPath)
    }

    @Test fun `torrent9 snapshot`() = runBlocking {
        server.enqueueBody(fixture("torrent9-search.html"))
        val provider = Torrent9(client).pointedAtServer()
        val results = provider.search("1080")
        assertSnapshot(provider, results)
        assertEquals("/recherche/1080", server.takeRequest().url.encodedPath)
        assertEquals("Sintel FRENCH WEBRIP 1080p 2010", results!!.first().title)
        assertEquals(1_200_000_000L, results.first().sizeBytes)
    }

    @Test fun `yggtorrent snapshot searches anonymously and builds download links`() = runBlocking {
        server.enqueueBody(fixture("yggtorrent-search.html"))
        val provider = Yggtorrent(client).pointedAtServer(ProviderAuth.Credentials("user", "pass"))
        val results = provider.search("1080")
        assertSnapshot(provider, results)
        val first = results!!.first()
        assertEquals("Sintel 2010 MULTi 1080p BluRay x264", first.title)
        assertEquals("${server.baseUrl()}/engine/download_torrent?id=${first.extras["id"]}", first.link)
        assertTrue("time should be a date: ${first.time}", first.time!!.endsWith("GMT"))
        assertEquals("GET", server.takeRequest().method) // no login before searching
    }

    @Test fun `kickasstorrents parses the mirror's results and scrapes the magnet`() = runBlocking {
        server.enqueueBody(fixture("kickasstorrents-search.html"))
        server.enqueueBody(fixture("kickasstorrents-detail.html"))
        val provider = kickassTorrents(client).pointedAtServer()
        val results = provider.search("big buck bunny", "Movies")!!
        assertSnapshot(provider, results)
        assertEquals(
            "/search?query=big%20buck%20bunny&cat=1&sort_by=seeders&sort_direction=desc",
            server.takeRequest().target,
        )
        assertEquals(listOf("Big Buck Bunny 1080p", "Big Buck Bunny 720p"), results.map { it.title })
        val first = results.first()
        assertEquals(1204, first.seeds)
        assertEquals(31, first.peers)
        assertEquals("276.4 MB", first.size)
        assertEquals("7 months", first.time)
        assertEquals(server.url("/torrent/101").toString(), first.desc)
        assertNull(first.magnet)

        // The decoy magnet above the download buttons must not be picked.
        val magnet = provider.getMagnet(first)!!
        assertEquals("/torrent/101", server.takeRequest().target)
        assertTrue(magnet, magnet.startsWith("magnet:?xt=urn:btih:DD8255ECDC7CA55FB0BBF81323D87062DB1F6D1C&dn="))
    }

    @Test fun `kickasstorrents tv search uses the TV category`() = runBlocking {
        server.enqueueBody(fixture("kickasstorrents-search.html"))
        kickassTorrents(client).pointedAtServer().search("show s01e02", "TV")
        assertTrue(server.takeRequest().target.contains("&cat=2&"))
    }

    @Test fun `kickasstorrents doesn't follow pagination links that drop the query`() = runBlocking {
        server.enqueueBody(fixture("kickasstorrents-search.html"))
        kickassTorrents(client).pointedAtServer().search("big buck bunny", limit = 100)
        assertEquals(1, server.requestCount)
    }

    @Test fun `magnetdl reads magnets straight from the result rows`() = runBlocking {
        server.enqueueBody(fixture("magnetdl-search.html"))
        val provider = magnetDl(client).pointedAtServer()
        val results = provider.search("big buck bunny")!!
        assertSnapshot(provider, results)
        assertEquals("/search?q=big%20buck%20bunny&orderby=DESC&order=seeders", server.takeRequest().target)
        assertEquals(listOf("Big Buck Bunny 1080p", "Big Buck Bunny 720p"), results.map { it.title })
        val first = results.first()
        assertEquals(1204, first.seeds)
        assertEquals(31, first.peers)
        assertEquals("276.4 MB", first.size)
        assertEquals("2 Years+", first.time)
        assertEquals("movies", first.extras["type"])
        assertEquals("dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c", first.infoHash)
        assertEquals(server.url("/single/101").toString(), first.desc)
        assertEquals(first.magnet, provider.getMagnet(first))
        assertEquals(0, results[1].seeds) // 0-seeder rows are kept
        assertEquals(1, server.requestCount) // no detail page fetch for the magnet
    }

    @Test fun `limetorrents parses rows and scrapes the magnet from the detail page`() = runBlocking {
        server.enqueueBody(fixture("limetorrents-search.html"))
        server.enqueueBody(fixture("limetorrents-detail.html"))
        val provider = limetorrents(client).pointedAtServer()
        val results = provider.search("big buck bunny 2008", "Movies")!!
        assertSnapshot(provider, results)
        assertEquals("/search/movies/big%20buck%20bunny%202008/seeds/1/", server.takeRequest().target)
        val first = results.first()
        assertEquals("Big Buck Bunny 2008 1080p BluRay x264", first.title)
        assertEquals(1204, first.seeds)
        assertEquals("2 years ago", first.time)
        assertNull(first.magnet)
        val magnet = provider.getMagnet(first)!!
        assertTrue(magnet, magnet.startsWith("magnet:?xt=urn:btih:DD8255ECDC7CA55FB0BBF81323D87062DB1F6D1C"))
    }

    @Test fun `torrentproject parses rows and decodes the detail page magnet`() = runBlocking {
        server.enqueueBody(fixture("torrentproject-search.html"))
        server.enqueueBody(fixture("torrentproject-detail.html"))
        val provider = torrentProject(client).pointedAtServer()
        val results = provider.search("big buck bunny 2008")!!
        assertSnapshot(provider, results)
        assertEquals("/?t=big%20buck%20bunny%202008&orderby=seeders", server.takeRequest().target)
        assertEquals(listOf("Big Buck Bunny 2008 1080p BluRay x264", "Big Buck Bunny 2008 2160p WEB-DL HEVC"), results.map { it.title })
        assertEquals(1204, results.first().seeds)
        assertEquals("276.4 MB", results.first().size)
        val magnet = provider.getMagnet(results.first())!!
        assertTrue(magnet, magnet.startsWith("magnet:?xt=urn:btih:DD8255ECDC7CA55FB0BBF81323D87062DB1F6D1C&dn="))
        assertEquals(2, server.requestCount)
    }

    @Test fun `eztv parses episode rows with magnets`() = runBlocking {
        server.enqueueBody(fixture("eztv-search.html"))
        val provider = eztv(client).pointedAtServer()
        val results = provider.search("open movie weekly s01e02", "All")!!
        assertSnapshot(provider, results)
        assertEquals("/search/open%20movie%20weekly%20s01e02", server.takeRequest().target)
        val row = results.single()
        assertEquals("Open Movie Weekly S01E02 1080p WEB H264 [eztv]", row.title)
        assertEquals(412, row.seeds)
        assertEquals("08ada5a7a6183aae1e09d831df6748d566095a10", row.infoHash)
        // EZTV has no movies.
        assertNull(provider.definition.movieCategory)
    }

    @Test fun `thepiratebay parses apibay json and builds magnets`() = runBlocking {
        server.enqueueBody(fixture("thepiratebay-search.json"))
        val results = ThePirateBay(client).pointedAtServer().search("big buck bunny", "Movies")!!
        assertEquals("/q.php?q=big%20buck%20bunny&cat=200", server.takeRequest().target)
        assertEquals(2, results.size)
        val first = results.first()
        assertEquals("Big Buck Bunny 1080p", first.title)
        assertEquals(120, first.seeds)
        assertEquals(4, first.peers)
        assertEquals("276.4 MB", first.size)
        assertEquals("276445467", first.extras["sizeBytes"])
        assertEquals("Tue, 3 Oct 2023 12:00:00 GMT", first.time)
        assertTrue(first.magnet!!.startsWith("magnet:?xt=urn:btih:DD8255ECDC7CA55FB0BBF81323D87062DB1F6D1C&dn=Big%20Buck%20Bunny%201080p&tr="))
        assertEquals("dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c", first.infoHash)
        assertEquals("101", first.extras["id"])
        assertEquals("tt1254207", first.extras["imdb"])
        assertNull(results[1].extras["imdb"])
    }

    @Test fun `thepiratebay placeholder row means no results`() = runBlocking {
        server.enqueueBody(fixture("thepiratebay-empty.json"))
        assertEquals(emptyList<Torrent>(), ThePirateBay(client).pointedAtServer().search("zzzz"))
    }

    @Test fun `thepiratebay details are rendered as escaped html`() = runBlocking {
        server.enqueueBody(fixture("thepiratebay-details.json"))
        val provider = ThePirateBay(client).pointedAtServer()
        val html = provider.getTorrentDetails(Torrent("ThePirateBay", "x", extras = mapOf("id" to "101")))!!
        assertEquals("/t.php?id=101", server.takeRequest().target)
        assertTrue(html.contains("<b>Uploader:</b> blender"))
        assertTrue(html.contains("&lt;script&gt;"))
    }

    @Test fun `thepiratebay html instead of json is a parse error`() = runBlocking {
        server.enqueueBody("<html>maintenance</html>")
        val error = runCatching { ThePirateBay(client).pointedAtServer().search("x") }.exceptionOrNull()
        assertTrue(error.toString(), error is com.nexient.jazzy.core.sources.engine.ParseException)
    }

    @Test fun `yts flattens each movie quality into a release-style result`() = runBlocking {
        server.enqueueBody(fixture("yts-search.json"))
        val provider = Yts(client).pointedAtServer()
        val results = provider.search("tt1254207", "Movies")!!
        val request = server.takeRequest()
        assertEquals("curl/7.37.0", request.headers["User-Agent"])
        assertEquals("/api/v2/list_movies.json?query_term=tt1254207&sort=seeds&order=desc&set=1", request.target)
        assertEquals(
            listOf(
                "Big Buck Bunny (2008) [720p] [BluRay] [x264] [2.0] [YTS]",
                "Big Buck Bunny (2008) [1080p] [WEBRip] [x265] [5.1] [YTS]",
            ),
            results.map { it.title },
        )
        assertEquals("https://yts.example/torrent/download/BBB", results[1].link)
        assertEquals("https://yts.example/movies/big-buck-bunny-2008", results[1].desc)
        assertEquals("dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c", results[1].infoHash)
        assertEquals("tt1254207", results[1].extras["imdb"])
        assertEquals("1288490189", results[1].extras["sizeBytes"])
        // YTS has no TV.
        assertNull(provider.definition.tvCategory)
    }

    @Test fun `rarbg fetches a token, then searches with it`() = runBlocking {
        server.enqueueBody(fixture("rarbg-token.json"))
        server.enqueueBody(fixture("rarbg-search.json"))
        val results = Rarbg(client, tokenDelayMs = 0).pointedAtServer().search("bunny", "Movies")!!
        assertTrue(server.takeRequest().target.contains("get_token=get_token"))
        val search = server.takeRequest().target
        assertTrue(search, search.endsWith("&token=tok123"))
        assertTrue(search, search.contains("category=14;17;42;44;45;46;47;48;50;51;52"))
        assertEquals(1, results.size)
        assertEquals("276.4 MB", results[0].size)
        assertEquals("dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c", results[0].infoHash)
    }

    @Test fun `torrentleech sends cookies and builds the download url`() = runBlocking {
        server.enqueueBody(fixture("torrentleech-search.json"))
        val provider = TorrentLeech(client).pointedAtServer(ProviderAuth.Cookies(listOf("tluid=1;", "tlpass=2;")))
        val results = provider.search("bunny")!!
        assertEquals("tluid=1; tlpass=2", server.takeRequest().headers["Cookie"])
        val first = results.single()
        assertEquals("263.6 MiB", first.size)
        assertEquals("tt1254207", first.extras["imdb"])
        assertEquals("${server.baseUrl()}/download/777/Big.Buck.Bunny.1080p.torrent", provider.getTorrentFileUrl(first))
    }

    @Test fun `1337x builds an itorrents link from the detail page hash`() = runBlocking {
        server.enqueueBody(fixture("detail-page.html"))
        val provider = X1337x(client).pointedAtServer()
        val url = provider.getTorrentFileUrl(Torrent("1337x", "x", desc = server.url("/torrent/1").toString()))
        assertEquals("https://itorrents.org/torrent/ABCDEF0123456789ABCDEF0123456789ABCDEF01.torrent", url)
    }

    @Test fun `every provider from the original library is registered, plus MagnetDL`() {
        val providers = ProviderRegistry.allProviders(client)
        assertEquals(
            listOf(
                "thepiratebay", "yts", "limetorrents", "torrentproject", "kickasstorrents", "magnetdl",
                "1337x", "eztv", "torrent9", "torrentz2", "rarbg", "iptorrents", "torrentleech", "yggtorrent",
            ),
            providers.map { it.id },
        )
        assertEquals(
            setOf("thepiratebay", "yts", "limetorrents", "torrentproject", "kickasstorrents", "magnetdl"),
            providers.filter { it.definition.enabledByDefault }.map { it.id }.toSet(),
        )
        assertEquals(
            setOf("iptorrents", "torrentleech", "yggtorrent"),
            providers.filter { it.definition.requireAuthentication }.map { it.id }.toSet(),
        )
        providers.forEach { p ->
            listOfNotNull(p.definition.movieCategory, p.definition.tvCategory).forEach { cat ->
                assertNotNull("${p.id} has no category $cat", p.getCategoryValue(cat.ifEmpty { null }))
            }
            assertFalse("${p.id} must not be private and enabled by default", p.definition.requireAuthentication && p.definition.enabledByDefault)
        }
    }
}
