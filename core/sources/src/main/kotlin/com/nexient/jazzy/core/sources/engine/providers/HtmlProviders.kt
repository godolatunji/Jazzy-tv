package com.nexient.jazzy.core.sources.engine.providers

import com.nexient.jazzy.core.sources.engine.ProviderDefinition
import com.nexient.jazzy.core.sources.engine.TorrentProvider
import okhttp3.OkHttpClient

// Providers that the original library defined purely as data (its .json provider files, plus
// .js providers that only passed a definition to the base class). Selectors are copied verbatim
// from TorrentSearch; ids, default enablement and movie/TV categories are Jazzy additions.

/** TV-only site: movie searches report Unsupported. Behind a Cloudflare challenge as of 2026-10. */
internal fun eztv(client: OkHttpClient) = TorrentProvider(
    ProviderDefinition(
        id = "eztv",
        name = "EZTV",
        baseUrl = "https://eztv.re",
        enabledByDefault = false,
        movieCategory = null,
        tvCategory = "All",
        behindCloudflare = true,
        searchUrl = "/search/{query}",
        categories = linkedMapOf("All" to ""),
        defaultCategory = "All",
        resultsPerPageCount = 50,
        itemsSelector = "tr.forum_header_border",
        itemSelectors = linkedMapOf(
            "title" to "a.epinfo@text",
            "link" to "a.download_1@href",
            "magnet" to "a.magnet@href",
            "time" to "td.forum_thread_post:nth-child(5)@text",
            "seeds" to "td.forum_thread_post_end | int",
            "size" to "td.forum_thread_post:nth-child(4)@text",
            "desc" to "a.epinfo@href",
        ),
        paginateSelector = "a:contains( next page)@href",
        torrentDetailsSelector = ".section_thread_post@html",
    ),
    client,
)

/** Private tracker: credentials or cookies. Gives .torrent links only (no magnets). */
internal fun ipTorrents(client: OkHttpClient) = TorrentProvider(
    ProviderDefinition(
        id = "iptorrents",
        name = "IPTorrents",
        baseUrl = "https://iptorrents.eu",
        enabledByDefault = false,
        movieCategory = "Movies",
        tvCategory = "TV",
        requireAuthentication = true,
        supportCookiesAuthentication = true,
        supportCredentialsAuthentication = true,
        loginUrl = "/take_login.php",
        loginQueryString = "username={username}&password={password}",
        searchUrl = "/t?{cat}q={query};o=seeders",
        categories = linkedMapOf(
            "All" to "",
            "Movies" to "7;68;54;77;20;89;90;96;6;48;62;38;",
            "TV" to "73;",
            "Games" to "74;",
            "Music" to "75;",
        ),
        defaultCategory = "All",
        resultsPerPageCount = 50,
        itemsSelector = """table[id="torrents"] tr:not(:first-child)""",
        itemSelectors = linkedMapOf(
            "title" to "td:nth-child(2) a",
            "time" to "tr:nth-child(2) div.t_ctime | split:%SPECIAL_CHAR%,1 | trim",
            "downloaded" to "td:nth-child(7) | int",
            "seeds" to "td:nth-child(8) | int",
            "peers" to "td:nth-child(9) | int",
            "size" to "td:nth-child(6)",
            "link" to "td:nth-child(4) a@href",
            "desc" to "td:nth-child(2) a@href",
        ),
        paginateSelector = "a:contains(Next)@href",
        torrentDetailsSelector = ".desWrap@html",
    ),
    client,
)

/**
 * Not the original's katcr.co (parked as of 2026-10): selectors target the kickasstorrents.cc mirror
 * listed on kat.cc. Its pagination links drop the query, so only page one is parsed, and magnets
 * come from the detail page (resolved lazily on Play).
 */
internal fun kickassTorrents(client: OkHttpClient) = TorrentProvider(
    ProviderDefinition(
        id = "kickasstorrents",
        name = "KickassTorrents",
        baseUrl = "https://kickasstorrents.cc",
        enabledByDefault = true,
        movieCategory = "Movies",
        tvCategory = "TV",
        searchUrl = "/search?query={query}&cat={cat}&sort_by=seeders&sort_direction=desc",
        categories = linkedMapOf(
            "All" to "0",
            "Movies" to "1",
            "TV" to "2",
            "Music" to "3",
            "Games" to "4",
            "Documentaries" to "5",
            "Anime" to "6",
            "Applications" to "7",
            "XXX" to "8",
            "Other" to "9",
        ),
        defaultCategory = "All",
        resultsPerPageCount = 30,
        itemsSelector = "table.data tr",
        itemSelectors = linkedMapOf(
            "title" to "a.cellMainLink",
            "time" to "td:nth-child(3)",
            "seeds" to "td.green | int",
            "peers" to "td.red | int",
            "size" to "td:nth-child(2)",
            "desc" to "a.cellMainLink@href",
        ),
        torrentDetailsSelector = "#tab-main@html",
        magnetSelector = """.downloadButtonGroup a[href^="magnet:"]@href""",
    ),
    client,
)

/** No magnets in the listing; the detail page has one (resolved lazily on Play). */
internal fun limetorrents(client: OkHttpClient) = TorrentProvider(
    ProviderDefinition(
        id = "limetorrents",
        name = "LimeTorrents",
        baseUrl = "https://www.limetorrents.info",
        enabledByDefault = true,
        movieCategory = "Movies",
        tvCategory = "TV",
        behindCloudflare = true,
        searchUrl = "/search/{cat}/{query}/seeds/1/",
        categories = linkedMapOf(
            "All" to "all",
            "Movies" to "movies",
            "TV" to "tv",
            "Games" to "games",
            "Music" to "music",
            "Applications" to "applications",
            "Anime" to "anime",
        ),
        defaultCategory = "All",
        resultsPerPageCount = 40,
        itemsSelector = ".table2 tr",
        itemSelectors = linkedMapOf(
            "title" to ".tt-name > a:nth-child(2)@text",
            "link" to "a.csprite_dl14@href",
            "seeds" to ".tdseed | int",
            "peers" to ".tdleech | int",
            "time" to """td.tdnormal:nth-child(2)@text  | match:"(.+?) -"""",
            "size" to "td.tdnormal:nth-child(3)@text",
            "desc" to ".tt-name > a:nth-child(2)@href",
        ),
        paginateSelector = "a:contains(Next page)@href",
        torrentDetailsSelector = "#content@html",
    ),
    client,
)

/**
 * Not in the original. magnetdl.pro (listed on kat.cc) puts the magnet in every result row. Further
 * pages load through JavaScript, so only page one is parsed. No category filter.
 */
internal fun magnetDl(client: OkHttpClient) = TorrentProvider(
    ProviderDefinition(
        id = "magnetdl",
        name = "MagnetDL",
        baseUrl = "https://magnetdl.pro",
        enabledByDefault = true,
        movieCategory = "All",
        tvCategory = "All",
        searchUrl = "/search?q={query}&orderby=DESC&order=seeders",
        categories = linkedMapOf("All" to ""),
        defaultCategory = "All",
        resultsPerPageCount = 10,
        itemsSelector = "table.download tbody tr",
        itemSelectors = linkedMapOf(
            "title" to "td.n a:not(.csprite_dl14)",
            "magnet" to "td.m a@href",
            "time" to "td:nth-child(3)",
            "type" to "td:nth-child(4)",
            "size" to "td:nth-child(5)",
            "seeds" to "td.s | int",
            "peers" to "td.l | int",
            "desc" to "td.n a:not(.csprite_dl14)@href",
        ),
        torrentDetailsSelector = "#content .fill-content@html",
    ),
    client,
)

/** No category filter; magnets come from the detail page. */
internal fun torrentProject(client: OkHttpClient) = TorrentProvider(
    ProviderDefinition(
        id = "torrentproject",
        name = "TorrentProject",
        baseUrl = "https://torrentproject.cc",
        enabledByDefault = true,
        movieCategory = "All",
        tvCategory = "All",
        searchUrl = "/?t={query}&orderby=seeders",
        categories = linkedMapOf("All" to ""),
        defaultCategory = "All",
        resultsPerPageCount = 50,
        itemsSelector = "#similarfiles > div:not(.gac_bb)",
        itemSelectors = linkedMapOf(
            "title" to "a@text",
            "time" to "span:nth-child(4)@text",
            "seeds" to "span:nth-child(3)@text | int",
            "peers" to "span:nth-child(4)@text | int",
            "size" to "span:nth-child(5)@text",
            "desc" to "a@href",
        ),
        paginateSelector = "td.cur + td > a@href",
        torrentDetailsSelector = "#res2@text",
        magnetSelector = """.usite a@href | match:"url=(.+)" | decodeURIComponent""",
    ),
    client,
)

/** Answered HTTP 400 in the 2026-10-10 live check; ships disabled. */
internal fun torrentz2(client: OkHttpClient) = TorrentProvider(
    ProviderDefinition(
        id = "torrentz2",
        name = "Torrentz2",
        baseUrl = "https://torrentz2.eu",
        enabledByDefault = false,
        movieCategory = "All",
        tvCategory = "All",
        searchUrl = "/?f={query}",
        categories = linkedMapOf("All" to ""),
        defaultCategory = "All",
        resultsPerPageCount = 50,
        itemsSelector = "div.results dl",
        itemSelectors = linkedMapOf(
            "title" to "a",
            "hash" to "a@href | substr:21,",
            "time" to "span:nth-child(2)",
            "size" to "span:nth-child(3)",
            "seeds" to "span:nth-child(5) | int",
            "peers" to "span:nth-child(4) | int",
            "magnet" to "a@href | substr:21, | format:magnet:?xt=urn:btih:{0}",
            "link" to "a@href | substr:21, | format:https://itorrents.org/torrent/{0}.torrent",
        ),
        paginateSelector = "a:contains(»)@href",
        torrentDetailsSelector = """td[class*="frame"]:nth-child(1)@html""",
        behindCloudflare = true,
    ),
    client,
)
