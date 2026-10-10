package com.nexient.jazzy.core.sources.engine.providers

import com.nexient.jazzy.core.sources.engine.Filters
import com.nexient.jazzy.core.sources.engine.Magnets
import com.nexient.jazzy.core.sources.engine.ParseException
import com.nexient.jazzy.core.sources.engine.ProviderDefinition
import com.nexient.jazzy.core.sources.engine.ProviderException
import com.nexient.jazzy.core.sources.engine.Sizes
import com.nexient.jazzy.core.sources.engine.Torrent
import com.nexient.jazzy.core.sources.engine.TorrentProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.OkHttpClient
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// Providers backed by JSON APIs instead of HTML scraping.

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

private fun parseJson(provider: String, body: String): JsonElement =
    runCatching { json.parseToJsonElement(body) }
        .getOrElse { throw ParseException("$provider returned an unexpected response", it) }

private operator fun JsonElement?.get(key: String): JsonElement? = (this as? JsonObject)?.get(key)
private val JsonElement?.string: String? get() = (this as? JsonPrimitive)?.contentOrNull
private val JsonElement?.int: Int? get() = string?.let(Filters::parseInt)
private val JsonElement?.long: Long? get() = string?.toDoubleOrNull()?.toLong()
private val JsonElement?.array: List<JsonElement> get() = (this as? JsonArray).orEmpty()

private val HTTP_DATE: DateTimeFormatter = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC)

/**
 * apibay JSON. Movies and TV both search the "Video" category (200): its sub-categories can't be
 * combined in one request, and title/episode matching drops the other kind anyway.
 */
internal class ThePirateBay(client: OkHttpClient) : TorrentProvider(
    ProviderDefinition(
        id = "thepiratebay",
        name = "ThePirateBay",
        baseUrl = "https://apibay.org",
        enabledByDefault = true,
        movieCategory = "Movies",
        tvCategory = "TV",
        searchUrl = "/q.php?q={query}&cat={cat}",
        categories = linkedMapOf(
            "All" to "",
            "Audio" to "100",
            "Video" to "200",
            "Movies" to "200",
            "TV" to "200",
            "Applications" to "300",
            "Games" to "400",
            "Porn" to "500",
            "Other" to "600",
            "Top100" to "url:/top/all",
        ),
        defaultCategory = "All",
        resultsPerPageCount = 100,
    ),
    client,
) {
    override suspend fun search(query: String, category: String?, limit: Int?): List<Torrent>? {
        val url = getUrl(category, query) ?: return null
        val response = parseJson(name, request(url))
        // Lenient parsing turns an HTML error page into a string primitive; apibay always answers an array.
        val results = (response as? JsonArray) ?: throw ParseException("$name returned an unexpected response")
        return postProcess(results.mapNotNull { r ->
            val hash = r["info_hash"].string
            // apibay answers an empty search with a single placeholder row with id 0.
            if (r["id"].string == "0" || hash == null || hash.all { it == '0' }) return@mapNotNull null
            val title = r["name"].string ?: return@mapNotNull null
            Torrent(
                provider = name,
                title = title,
                time = r["added"].long?.let { HTTP_DATE.format(Instant.ofEpochSecond(it)) },
                seeds = r["seeders"].int,
                peers = r["leechers"].int,
                size = r["size"].long?.let(Sizes::humanizeSi),
                magnet = Magnets.build(hash, title),
                extras = buildMap {
                    r["id"].string?.let { put("id", it) }
                    r["size"].long?.let { put("sizeBytes", it.toString()) }
                    r["num_files"].string?.let { put("numFiles", it) }
                    r["status"].string?.let { put("status", it) }
                    r["category"].string?.let { put("category", it) }
                    r["imdb"].string?.takeIf { it.isNotEmpty() }?.let { put("imdb", it) }
                    r["username"].string?.let { put("uploader", it) }
                },
            )
        })
    }

    /** The original returned the raw `t.php` JSON; it's rendered as simple HTML here. */
    override suspend fun getTorrentDetails(torrent: Torrent): String? {
        val id = torrent.extras["id"] ?: throw ProviderException("Missing torrent id")
        val details = parseJson(name, request("$baseUrl/t.php?id=$id"))
        val description = details["descr"].string.orEmpty()
            .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>")
        val fields = listOf("username" to "Uploader", "num_files" to "Files", "imdb" to "IMDb")
            .mapNotNull { (key, label) -> details[key].string?.takeIf { it.isNotEmpty() }?.let { "<b>$label:</b> $it" } }
        return (fields + description).filter { it.isNotEmpty() }.joinToString("<br>")
    }

    override suspend fun getTorrentFileUrl(torrent: Torrent): String? = null
}

/**
 * RARBG's torrentapi. Needs a token (valid ~15 minutes) and at most one request per two
 * seconds. RARBG shut down in 2023; kept for parity with the original library, ships disabled.
 */
internal class Rarbg(
    client: OkHttpClient,
    private val tokenDelayMs: Long = 2_000,
    private val clock: () -> Long = System::currentTimeMillis,
) : TorrentProvider(
    ProviderDefinition(
        id = "rarbg",
        name = "RARBG",
        baseUrl = "https://torrentapi.org",
        enabledByDefault = false,
        movieCategory = "Movies",
        tvCategory = "TV",
        searchUrl = "/pubapi_v2.php?app_id=NodeTorrentSearchApi&search_string={query}&category={cat}" +
            "&mode=search&format=json_extended&sort=seeders&limit=100&token=",
        categories = linkedMapOf(
            "All" to "1;4;14;15;16;17;21;22;42;18;19;41;27;28;29;30;31;32;40;23;24;25;26;33;34;43;44;45;46;47;48;49;50;51;52",
            "Movies" to "14;17;42;44;45;46;47;48;50;51;52",
            "XXX" to "1;4",
            "Games" to "1;27;28;29;30;31;32;40",
            "TV" to "1;18;41;49",
            "Music" to "1;23;24;25;26",
            "Apps" to "1;33;34;43",
            "Books" to "35",
        ),
        defaultCategory = "All",
        resultsPerPageCount = 100,
        headers = mapOf("User-Agent" to "curl/7.37.0"),
    ),
    client,
) {
    private val tokenMutex = Mutex()
    @Volatile private var token: String? = null
    @Volatile private var lastLoginTime: Long = 0

    override suspend fun ensureLogin() = tokenMutex.withLock {
        if (token == null || clock() - lastLoginTime > TOKEN_TTL_MS) {
            val response = parseJson(name, request("$baseUrl$TOKEN_PATH", ensureLogin = false))
            delay(tokenDelayMs) // the API allows one request per two seconds
            token = response["token"].string ?: throw ParseException("$name didn't return a token")
            lastLoginTime = clock()
        }
    }

    override fun onLoggedOut() {
        token = null
    }

    override suspend fun search(query: String, category: String?, limit: Int?): List<Torrent>? {
        val url = getUrl(category, query) ?: return null
        ensureLogin()
        val response = parseJson(name, request(url + token, ensureLogin = false))
        return postProcess(response["torrent_results"].array.mapNotNull { r ->
            Torrent(
                provider = name,
                title = r["title"].string ?: return@mapNotNull null,
                time = r["pubdate"].string,
                seeds = r["seeders"].int,
                peers = r["leechers"].int,
                size = r["size"].long?.let(Sizes::humanizeSi),
                magnet = r["download"].string,
                desc = r["info_page"].string,
                extras = listOfNotNull(
                    r["size"].long?.let { "sizeBytes" to it.toString() },
                    r["episode_info"]["imdb"].string?.let { "imdb" to it },
                ).toMap(),
            )
        })
    }

    override suspend fun getTorrentDetails(torrent: Torrent): String? = null
    override suspend fun getTorrentFileUrl(torrent: Torrent): String? = null

    private companion object {
        const val TOKEN_PATH = "/pubapi_v2.php?get_token=get_token&app_id=NodeTorrentSearchApi"
        const val TOKEN_TTL_MS = 840_000L
    }
}

/**
 * YTS JSON API, movies only. One result per quality; the magnet is built from `hash` (an addition
 * over the original). `query_term` accepts an IMDb ID, which Jazzy uses when it has one. Result
 * titles are composed in YTS's own release-name style so they parse like any other release.
 */
internal class Yts(client: OkHttpClient) : TorrentProvider(
    ProviderDefinition(
        id = "yts",
        name = "YTS",
        // yts.mx is gone; the API's own responses announce this host as its new base URL.
        baseUrl = "https://movies-api.accel.li",
        enabledByDefault = true,
        movieCategory = "Movies",
        tvCategory = null,
        supportsImdbSearch = true,
        searchUrl = "/api/v2/list_movies.json?query_term={query}&sort=seeds&order=desc&set=1",
        categories = linkedMapOf("All" to "", "Movies" to ""),
        defaultCategory = "All",
        resultsPerPageCount = 20,
        headers = mapOf("User-Agent" to "curl/7.37.0"),
    ),
    client,
) {
    override suspend fun search(query: String, category: String?, limit: Int?): List<Torrent>? {
        val url = getUrl(category, query) ?: return null
        val response = parseJson(name, request(url))
        if (response !is JsonObject || response["data"] == null) throw ParseException("$name returned an unexpected response")
        val movies = response["data"]["movies"].array
        return postProcess(movies.flatMap { movie ->
            val movieTitle = movie["title"].string ?: return@flatMap emptyList()
            val year = movie["year"].string
            movie["torrents"].array.map { t ->
                val title = releaseName(movieTitle, year, t)
                val hash = t["hash"].string
                Torrent(
                    provider = name,
                    title = title,
                    time = t["date_uploaded"].string,
                    seeds = t["seeds"].int,
                    peers = t["peers"].int,
                    size = t["size"].string,
                    magnet = hash?.let { Magnets.build(it, title) },
                    link = t["url"].string,
                    desc = movie["url"].string,
                    extras = listOfNotNull(
                        hash?.let { "hash" to it },
                        t["size_bytes"].long?.let { "sizeBytes" to it.toString() },
                        t["type"].string?.let { "type" to it },
                        year?.let { "year" to it },
                        movie["imdb_code"].string?.let { "imdb" to it },
                    ).toMap(),
                )
            }
        })
    }

    /** e.g. `Big Buck Bunny (2008) [1080p] [BluRay] [x265] [5.1] [YTS]`. */
    private fun releaseName(movieTitle: String, year: String?, t: JsonElement): String {
        val type = when (t["type"].string?.lowercase()) {
            "bluray" -> "BluRay"
            "web" -> "WEBRip"
            null, "" -> null
            else -> t["type"].string
        }
        val parts = listOfNotNull(
            year?.let { "$movieTitle ($it)" } ?: movieTitle,
            t["quality"].string?.let { "[$it]" },
            type?.let { "[$it]" },
            t["video_codec"].string?.let { "[$it]" },
            t["audio_channels"].string?.let { "[$it]" },
            "[YTS]",
        )
        return parts.joinToString(" ")
    }

    override suspend fun getTorrentDetails(torrent: Torrent): String? = null
}

/** Private tracker, cookie authentication only. Gives .torrent links only (no magnets). */
internal class TorrentLeech(client: OkHttpClient) : TorrentProvider(
    ProviderDefinition(
        id = "torrentleech",
        name = "TorrentLeech",
        baseUrl = "https://www.torrentleech.org",
        enabledByDefault = false,
        movieCategory = "Movies",
        tvCategory = "TV",
        requireAuthentication = true,
        supportCookiesAuthentication = true,
        supportCredentialsAuthentication = false,
        searchUrl = "/torrents/browse/list/query/{query}/categories/{cat}/orderby/seeders/order/desc",
        categories = linkedMapOf(
            "All" to "",
            "Movies" to "8,9,11,37,43,14,12,13,47,15,29",
            "TV" to "26,32,27",
            "Games" to "17,42,18,19,40,20,21,39,22,28,30,48",
            "Apps" to "23,24,25,33",
            "Education" to "38",
            "Animation" to "34,35",
            "Books" to "45,46",
            "Music" to "31,16",
            "Foreign" to "36,44",
        ),
        defaultCategory = "All",
    ),
    client,
) {
    override suspend fun search(query: String, category: String?, limit: Int?): List<Torrent>? {
        val url = getUrl(category, query) ?: return null
        val response = parseJson(name, request(url, ensureLogin = false))
        if (response !is JsonObject) throw ParseException("$name returned an unexpected response")
        val results = response["torrentList"].array
        return postProcess(results.mapNotNull { r ->
            Torrent(
                provider = name,
                title = r["name"].string ?: return@mapNotNull null,
                time = r["addedTimestamp"].string,
                seeds = r["seeders"].int,
                peers = r["leechers"].int,
                size = r["size"].long?.let(Sizes::humanizeBinary),
                extras = listOf("filename", "fid", "rating", "categoryID", "numComments", "imdbID", "igdbID", "tvmazeID")
                    .mapNotNull { key -> r[key].string?.takeIf { it.isNotEmpty() }?.let { key to it } }
                    .toMap() + listOfNotNull(
                    r["size"].long?.let { "sizeBytes" to it.toString() },
                    r["imdbID"].string?.takeIf { it.isNotEmpty() }?.let { "imdb" to it },
                ),
            )
        })
    }

    override suspend fun getTorrentDetails(torrent: Torrent): String? = null

    override suspend fun getTorrentFileUrl(torrent: Torrent): String? {
        val fid = torrent.extras["fid"] ?: return null
        val filename = torrent.extras["filename"] ?: return null
        return "$baseUrl/download/$fid/$filename"
    }
}
