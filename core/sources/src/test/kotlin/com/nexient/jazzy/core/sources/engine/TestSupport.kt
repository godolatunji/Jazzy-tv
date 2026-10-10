package com.nexient.jazzy.core.sources.engine

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient

internal fun fixture(name: String): String =
    requireNotNull(TestSupport::class.java.classLoader!!.getResource("fixtures/$name")) { "Missing fixture $name" }
        .readText()

internal fun MockWebServer.enqueueBody(body: String, code: Int = 200, vararg headers: Pair<String, String>) {
    enqueue(
        MockResponse.Builder().code(code).body(body)
            .apply { headers.forEach { (k, v) -> addHeader(k, v) } }
            .build(),
    )
}

/** Base URL of [server] without the trailing slash, as provider definitions expect. */
internal fun MockWebServer.baseUrl(): String = url("/").toString().trimEnd('/')

internal object TestSupport {
    val client: OkHttpClient = OkHttpClient()
}

/** Provider whose search result is scripted; no network. */
internal class FakeProvider(
    id: String,
    enabledByDefault: Boolean = true,
    movieCategory: String? = "Movies",
    tvCategory: String? = "TV",
    supportsImdbSearch: Boolean = false,
    requireAuthentication: Boolean = false,
    categories: Map<String, String> = linkedMapOf("All" to "", "Movies" to "m", "TV" to "t"),
    private val magnets: Map<String, String> = emptyMap(),
    private val result: suspend (query: String, category: String?) -> List<Torrent>?,
) : TorrentProvider(
    ProviderDefinition(
        id = id,
        name = id.replaceFirstChar { it.uppercase() },
        baseUrl = "https://$id.invalid",
        enabledByDefault = enabledByDefault,
        movieCategory = movieCategory,
        tvCategory = tvCategory,
        supportsImdbSearch = supportsImdbSearch,
        requireAuthentication = requireAuthentication,
        supportCookiesAuthentication = requireAuthentication,
        categories = categories,
        defaultCategory = "All",
    ),
    TestSupport.client,
) {
    val searches = java.util.Collections.synchronizedList(mutableListOf<Pair<String, String?>>())

    override suspend fun search(query: String, category: String?, limit: Int?): List<Torrent>? {
        searches += query to category
        return if (getCategoryValue(category) == null) null else result(query, category)
    }

    override suspend fun getMagnet(torrent: Torrent): String? = torrent.magnet ?: magnets[torrent.desc]
}

internal fun torrent(
    provider: String,
    title: String,
    seeds: Int?,
    hash: String? = null,
    size: String? = null,
    peers: Int? = null,
) = Torrent(
    provider = provider,
    title = title,
    seeds = seeds,
    peers = peers,
    size = size,
    magnet = hash?.let { Magnets.build(it, title, trackers = emptyList()) },
    desc = "https://$provider.invalid/${title.replace(' ', '-')}",
)
