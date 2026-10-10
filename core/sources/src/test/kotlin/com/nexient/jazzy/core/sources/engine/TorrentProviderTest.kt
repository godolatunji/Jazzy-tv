package com.nexient.jazzy.core.sources.engine

import kotlinx.coroutines.runBlocking
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Ports of torrent-search-api's TorrentProvider.test.js plus pagination/auth behaviour. */
class TorrentProviderTest {
    private val definition = ProviderDefinition(
        id = "torrent9",
        name = "Torrent9",
        baseUrl = "http://www.torrent.com",
        searchUrl = "/engine/search?q={query}&order=desc&sort=seed&category={cat}",
        categories = linkedMapOf("All" to "everything", "Movies" to "films", "Url" to "url:/query={query}"),
        defaultCategory = "Movies",
    )
    private fun provider(def: ProviderDefinition = definition) = TorrentProvider(def, TestSupport.client)

    private lateinit var server: MockWebServer

    @Before fun setUp() {
        server = MockWebServer().apply { start() }
    }

    @After fun tearDown() = server.close()

    @Test fun `getUrl substitutes category and query`() = assertEquals(
        "http://www.torrent.com/engine/search?q=1080&order=desc&sort=seed&category=films",
        provider().getUrl("Movies", "1080"),
    )

    @Test fun `getUrl uses the default category when none is given`() = assertEquals(
        "http://www.torrent.com/engine/search?q=1080&order=desc&sort=seed&category=films",
        provider().getUrl(null, "1080"),
    )

    @Test fun `getUrl matches categories case-insensitively`() = assertEquals(
        "http://www.torrent.com/engine/search?q=1080&order=desc&sort=seed&category=everything",
        provider().getUrl("aLL", "1080"),
    )

    @Test fun `getUrl with url category replaces the search path`() =
        assertEquals("http://www.torrent.com/query=1080", provider().getUrl("Url", "1080"))

    @Test fun `getUrl returns null for an unknown category`() = assertNull(provider().getUrl("Nope", "1080"))

    @Test fun `getUrl encodes like encodeURIComponent`() =
        assertEquals("http://www.torrent.com/query=big%20buck%20bunny%20(2008)", provider().getUrl("Url", "big buck bunny (2008)"))

    @Test fun `page count follows the requested limit`() {
        assertEquals(1, TorrentProvider.computePageCount(null, 20))
        assertEquals(1, TorrentProvider.computePageCount(20, 20))
        assertEquals(2, TorrentProvider.computePageCount(21, 20))
    }

    @Test fun `public provider enables without credentials`() {
        val p = provider()
        p.enable()
        assertTrue(p.isActive)
        p.disable()
        assertFalse(p.isActive)
    }

    @Test fun `private provider refuses to enable without matching auth`() {
        val p = provider(definition.copy(requireAuthentication = true, supportCookiesAuthentication = true))
        assertThrows(IllegalArgumentException::class.java) { p.enable() }
        assertThrows(IllegalArgumentException::class.java) { p.enable(ProviderAuth.Credentials("u", "p")) }
        p.enable(ProviderAuth.Cookies(listOf("uid=1;", "pass=2;")))
        assertTrue(p.isActive)
        assertTrue(p.isLogged())
        assertEquals("uid=1; pass=2", p.cookieHeader("http://www.torrent.com/download/1"))
    }

    @Test fun `auth toString never reveals secrets`() {
        assertFalse(ProviderAuth.Credentials("me", "hunter2").toString().contains("hunter2"))
        assertFalse(ProviderAuth.Cookies(listOf("uid=secret")).toString().contains("secret"))
    }

    @Test fun `scraping follows pagination up to the page limit`() = runBlocking {
        server.enqueueBody(fixture("paginated-page1.html"))
        server.enqueueBody(fixture("paginated-page2.html"))
        val p = provider(paginatedDefinition())

        val results = p.search("x", null, limit = 4)!!

        assertEquals(listOf("First result", "Second result", "Third result"), results.map { it.title })
        assertEquals(1234, results[1].seeds)
        assertEquals("1.5 GB", results[0].size)
        assertEquals(server.url("/torrent/1/first").toString(), results[0].desc)
        assertEquals(2, server.requestCount)
    }

    @Test fun `scraping fetches one page without a limit`() = runBlocking {
        server.enqueueBody(fixture("paginated-page1.html"))
        val results = provider(paginatedDefinition()).search("x")!!
        assertEquals(2, results.size)
        assertEquals(1, server.requestCount)
    }

    @Test fun `requests carry a desktop browser user agent`() = runBlocking {
        server.enqueueBody(fixture("paginated-page1.html"))
        provider(paginatedDefinition()).search("x")
        assertEquals(EngineHttp.USER_AGENT, server.takeRequest().headers["User-Agent"])
    }

    @Test fun `unsupported category returns null without a request`() = runBlocking {
        assertNull(provider(paginatedDefinition()).search("x", "Movies"))
        assertEquals(0, server.requestCount)
    }

    @Test fun `credentials login posts the form before searching`() = runBlocking {
        server.enqueueBody("You are logged", headers = arrayOf("Set-Cookie" to "uid=42; Path=/"))
        server.enqueueBody(fixture("paginated-page1.html"))
        val p = provider(
            paginatedDefinition().copy(
                requireAuthentication = true,
                supportCredentialsAuthentication = true,
                loginUrl = "/login",
                loginQueryString = "username={username}&password={password}",
            ),
        )
        p.enable(ProviderAuth.Credentials("me@x", "p&ss"))

        assertEquals(2, p.search("x")!!.size)

        val login = server.takeRequest()
        assertEquals("POST", login.method)
        assertEquals("/login", login.url.encodedPath)
        assertEquals("username=me%40x&password=p%26ss", login.body?.utf8())
        val search = server.takeRequest()
        assertEquals("uid=42", search.headers["Cookie"])
    }

    @Test fun `missing magnet is scraped from the detail page`() = runBlocking {
        server.enqueueBody(fixture("detail-page.html"))
        val magnet = provider(paginatedDefinition()).getMagnet(
            Torrent(provider = "Paginated", title = "x", desc = server.url("/torrent/1").toString()),
        )
        assertEquals("magnet:?xt=urn:btih:ABCDEF0123456789ABCDEF0123456789ABCDEF01&dn=x", magnet)
    }

    @Test fun `cloudflare challenge is reported as blocked`() = runBlocking {
        server.enqueueBody("<title>Just a moment...</title>", code = 403, "Server" to "cloudflare", "cf-mitigated" to "challenge")
        val error = runCatching { provider(paginatedDefinition()).search("x") }.exceptionOrNull()
        assertTrue(error is BlockedException)
    }

    @Test fun `a challenge page served with 200 is reported as blocked`() = runBlocking {
        server.enqueueBody(
            "<html><head><title>Just a moment...</title></head><body><script src=\"/cdn-cgi/challenge-platform/x.js\"></script></body></html>",
        )
        val error = runCatching { provider(paginatedDefinition()).search("x") }.exceptionOrNull()
        assertTrue(error is BlockedException)
    }

    @Test fun `http errors carry the status code and not the url`() = runBlocking {
        server.enqueueBody("gone", code = 404)
        val error = runCatching { provider(paginatedDefinition()).search("secret-query") }.exceptionOrNull()
        assertEquals(404, (error as HttpStatusException).code)
        assertFalse(error.message!!.contains("secret-query"))
    }

    private fun paginatedDefinition() = ProviderDefinition(
        id = "paginated",
        name = "Paginated",
        baseUrl = server.baseUrl(),
        searchUrl = "/search/{query}",
        categories = linkedMapOf("All" to ""),
        defaultCategory = "All",
        resultsPerPageCount = 2,
        itemsSelector = "#t tbody tr",
        itemSelectors = linkedMapOf(
            "title" to "a.name",
            "seeds" to ".seeds | int",
            "size" to ".size@html | until:<sp",
            "desc" to "a.name@href",
        ),
        paginateSelector = "a.next@href",
        torrentDetailsSelector = ".torrent-detail-page@html",
    )
}
