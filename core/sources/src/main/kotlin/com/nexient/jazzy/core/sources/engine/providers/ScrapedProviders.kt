package com.nexient.jazzy.core.sources.engine.providers

import com.nexient.jazzy.core.sources.engine.ProviderDefinition
import com.nexient.jazzy.core.sources.engine.Selector
import com.nexient.jazzy.core.sources.engine.Torrent
import com.nexient.jazzy.core.sources.engine.TorrentProvider
import okhttp3.OkHttpClient
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// HTML providers that override part of the base behaviour.

/** Cloudflare challenge as of 2026-10; ships disabled. Magnets come from the detail page. */
internal class X1337x(client: OkHttpClient) : TorrentProvider(
    ProviderDefinition(
        id = "1337x",
        name = "1337x",
        baseUrl = "https://www.1337x.to",
        enabledByDefault = false,
        movieCategory = "Movies",
        tvCategory = "TV",
        searchUrl = "/category-search/{query}/{cat}/1/",
        categories = linkedMapOf(
            "All" to "url:/search/{query}/1/",
            "Movies" to "Movies",
            "TV" to "TV",
            "Games" to "Games",
            "Music" to "Music",
            "Anime" to "Anime",
            "Applications" to "Apps",
            "Documentaries" to "Documentaries",
            "Xxx" to "XXX",
            "Other" to "Other",
            "Top100" to "url:/top-100",
        ),
        defaultCategory = "All",
        resultsPerPageCount = 20,
        itemsSelector = "tbody > tr",
        itemSelectors = linkedMapOf(
            "title" to "a:nth-child(2)",
            "time" to ".coll-date",
            "seeds" to ".seeds | int",
            "peers" to ".leeches | int",
            "size" to ".size@html | until:<sp",
            "desc" to "a:nth-child(2)@href",
        ),
        paginateSelector = ".pagination li:nth-last-child(2) a@href",
        torrentDetailsSelector = ".torrent-detail-page@html",
        behindCloudflare = true,
    ),
    client,
) {
    /** The original built an itorrents.org cache URL from the info hash on the detail page. */
    override suspend fun getTorrentFileUrl(torrent: Torrent): String? {
        val desc = torrent.desc ?: return null
        val hash = Selector.parse(".infohash-box span").evaluate(fetchDocument(desc)) ?: return null
        return "https://itorrents.org/torrent/${hash.trim()}.torrent"
    }
}

/**
 * French site; DNS gone as of 2026-10, ships disabled. Its Movies/TV "categories" are browse pages
 * that ignore the query, so both media types search "All".
 */
internal class Torrent9(client: OkHttpClient) : TorrentProvider(
    ProviderDefinition(
        id = "torrent9",
        name = "Torrent9",
        baseUrl = "https://www.torrent9.ninja",
        enabledByDefault = false,
        movieCategory = "All",
        tvCategory = "All",
        behindCloudflare = true,
        searchUrl = "/recherche/{query}",
        categories = linkedMapOf(
            "All" to "",
            "Movies" to "url:/torrents/films",
            "TV" to "url:/torrents/series",
            "Music" to "url:/torrents/musique",
            "Apps" to "url:/torrents/logiciels",
            "Books" to "url:/torrents/ebook",
            "Top100" to "url:/top",
        ),
        defaultCategory = "All",
        resultsPerPageCount = 60,
        itemsSelector = "table tr",
        itemSelectors = linkedMapOf(
            "title" to "a",
            "seeds" to ".seed_ok | int",
            "peers" to "td:nth-child(4) | int",
            "size" to "td:nth-child(2)",
            "desc" to "a@href | replace:t//,t/",
        ),
        paginateSelector = "a:contains(Suivant ►)@href",
        torrentDetailsSelector = ".movie-detail > .row:nth-child(1)@html",
    ),
    client,
) {
    override suspend fun getTorrentFileUrl(torrent: Torrent): String? {
        val desc = torrent.desc ?: return null
        return Selector.parse(".download-btn > a@href").evaluate(fetchDocument(desc))
    }
}

/** Private tracker; searching is anonymous but magnets and downloads need a session. */
internal class Yggtorrent(
    client: OkHttpClient,
    private val clock: () -> Long = System::currentTimeMillis,
) : TorrentProvider(
    ProviderDefinition(
        id = "yggtorrent",
        name = "YggTorrent",
        baseUrl = "https://www4.yggtorrent.li",
        enabledByDefault = false,
        movieCategory = "Movies",
        tvCategory = "TV",
        requireAuthentication = true,
        supportCookiesAuthentication = true,
        supportCredentialsAuthentication = true,
        behindCloudflare = true,
        loginUrl = "/user/login",
        loginQueryString = "id={username}&pass={password}",
        searchUrl = "/engine/search?name={query}&do=search&order=desc&sort=seed&category={cat}",
        categories = linkedMapOf(
            "All" to "",
            "Videos" to "2145",
            "Movies" to "2145&sub_category=2183",
            "TV" to "2145&sub_category=2184",
            "Emulation" to "2141",
            "Games" to "2142",
            "Applications" to "2144",
            "Music" to "2139",
            "Books" to "2140",
            "GPS" to "2143",
            "XXX" to "2188",
        ),
        defaultCategory = "All",
        resultsPerPageCount = 25,
        itemsSelector = ".results tr",
        itemSelectors = linkedMapOf(
            "title" to "td:nth-child(2) a@text | replace:\"\n\" | replace:\"\r\" | trim",
            "time" to "td:nth-child(5) div@text | int",
            "seeds" to "td:nth-child(8)@text | int",
            "peers" to "td:nth-child(9)@text | int",
            "size" to "td:nth-child(6)@text",
            "desc" to "td:nth-child(2) a@href",
            "id" to "td:nth-child(3) a@target",
        ),
        paginateSelector = """a[rel="next"]@href""",
        torrentDetailsSelector = ".content > .default@html",
    ),
    client,
) {
    @Volatile private var lastLoginTime: Long? = null

    override val searchRequiresLogin: Boolean get() = false

    override fun isLogged(): Boolean {
        val last = lastLoginTime ?: return false
        return super.isLogged() && clock() - last < SESSION_MS
    }

    override fun onLoggedOut() {
        lastLoginTime = null
    }

    override suspend fun login() {
        request(baseUrl, ensureLogin = false)
        super.login()
        lastLoginTime = clock()
    }

    override fun postProcess(results: List<Torrent>): List<Torrent> = super.postProcess(results).map { torrent ->
        val id = torrent.extras["id"]
        torrent.copy(
            time = torrent.time?.toLongOrNull()?.let { HTTP_DATE.format(Instant.ofEpochSecond(it)) } ?: torrent.time,
            link = id?.let { "$baseUrl/engine/download_torrent?id=$it" } ?: torrent.link,
        )
    }

    private companion object {
        const val SESSION_MS = 600_000L
        val HTTP_DATE: DateTimeFormatter = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC)
    }
}
