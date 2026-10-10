package com.nexient.jazzy.core.sources.engine

import okhttp3.Cookie
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import kotlin.math.ceil

/**
 * Declarative provider description; a 1:1 port of the definition objects in
 * torrent-search-api's `lib/providers`, plus the Jazzy metadata at the top ([id],
 * [enabledByDefault], [movieCategory], [tvCategory], [supportsImdbSearch]).
 *
 * Category values are substituted into [searchUrl] as `{cat}`. A value starting with `url:`
 * replaces [searchUrl] entirely. `{query}` is the URI-encoded search term.
 */
internal data class ProviderDefinition(
    /** Stable lowercase slug, used as the settings/health key and in `TorrentSource.providerIds`. */
    val id: String,
    /** Display name. */
    val name: String,
    val baseUrl: String,
    val enabledByDefault: Boolean = false,
    /** Category used for movie searches; null when the site has no movies (e.g. EZTV). */
    val movieCategory: String? = DEFAULT_CATEGORY,
    /** Category used for episode searches; null when the site has no TV (e.g. YTS). */
    val tvCategory: String? = DEFAULT_CATEGORY,
    /** The search accepts an IMDb ID (`tt1234567`) as the query and matches it exactly (YTS). */
    val supportsImdbSearch: Boolean = false,
    val searchUrl: String = "",
    val categories: Map<String, String> = emptyMap(),
    val defaultCategory: String = "",
    val resultsPerPageCount: Int = 50,
    val itemsSelector: String = "",
    val itemSelectors: Map<String, String> = emptyMap(),
    val paginateSelector: String = "",
    val torrentDetailsSelector: String = "",
    val magnetSelector: String = """a[href*="magnet:?xt=urn:btih:"]@href""",
    /** The original used `cloudscraper` for these sites. We don't solve challenges; see [BlockedException]. */
    val behindCloudflare: Boolean = false,
    val requireAuthentication: Boolean = false,
    val supportTokenAuthentication: Boolean = false,
    val supportCookiesAuthentication: Boolean = false,
    val supportCredentialsAuthentication: Boolean = false,
    val loginUrl: String = "",
    /** Form body, with `{username}` and `{password}` placeholders. */
    val loginQueryString: String = "",
    val headers: Map<String, String> = emptyMap(),
) {
    companion object {
        /** Marker for "use [defaultCategory]" in [movieCategory]/[tvCategory]. */
        const val DEFAULT_CATEGORY = ""
    }
}

/**
 * Port of `lib/TorrentProvider.js`. A provider built from a [ProviderDefinition] alone scrapes
 * HTML with Jsoup; providers backed by a JSON API override [search].
 *
 * Instances are stateful (enabled flag, credentials, cookies, base URL override), like the
 * original. They are safe to search concurrently.
 */
internal open class TorrentProvider(
    val definition: ProviderDefinition,
    client: OkHttpClient,
) {
    private val cookieJar = MemoryCookieJar()
    protected val client: OkHttpClient = client.newBuilder().cookieJar(cookieJar).build()

    val id: String get() = definition.id
    val name: String get() = definition.name

    @Volatile var isActive: Boolean = false
        private set

    @Volatile private var baseUrlOverride: String? = null
    val baseUrl: String get() = baseUrlOverride ?: definition.baseUrl

    @Volatile protected var auth: ProviderAuth = ProviderAuth.None
        private set

    val supportsMovies: Boolean get() = definition.movieCategory != null
    val supportsTv: Boolean get() = definition.tvCategory != null

    val info: EngineProviderInfo
        get() = EngineProviderInfo(
            id = id,
            name = name,
            isPublic = !definition.requireAuthentication,
            categories = definition.categories.keys.toList(),
            defaultBaseUrl = definition.baseUrl,
            supportsCredentials = definition.supportCredentialsAuthentication,
            supportsCookies = definition.supportCookiesAuthentication,
            behindCloudflare = definition.behindCloudflare,
        )

    // region Enable / authentication

    /**
     * Activates the provider. Private providers need [ProviderAuth] matching one of their
     * supported mechanisms, otherwise this throws, as `enableProvider` does in the original.
     */
    fun enable(auth: ProviderAuth = ProviderAuth.None) {
        val d = definition
        val accepted = !d.requireAuthentication ||
            (auth is ProviderAuth.Credentials && d.supportCredentialsAuthentication) ||
            (auth is ProviderAuth.Token && d.supportTokenAuthentication) ||
            (auth is ProviderAuth.Cookies && d.supportCookiesAuthentication)
        require(accepted) { "Couldn't enable provider $name due to incorrect login information" }
        setAuth(auth)
        isActive = true
    }

    /** Stores [auth] without the private-provider check; used so a disabled provider can still be tested. */
    fun setAuth(auth: ProviderAuth) {
        if (auth != this.auth) {
            cookieJar.clear()
            onLoggedOut()
            this.auth = auth
            if (auth is ProviderAuth.Cookies) setCookies(auth.cookies)
        }
    }

    fun disable() {
        isActive = false
    }

    /** `overrideConfig({ baseUrl })` — the only override exposed, since domains move. */
    fun overrideBaseUrl(url: String?) {
        val normalised = url?.trim()?.trimEnd('/')?.ifEmpty { null }
        if (normalised != baseUrlOverride) {
            baseUrlOverride = normalised
            cookieJar.clear()
            onLoggedOut()
            (auth as? ProviderAuth.Cookies)?.let { setCookies(it.cookies) }
        }
    }

    private fun setCookies(cookies: List<String>) {
        val url = baseUrl.toHttpUrlOrNull() ?: return
        cookies.mapNotNull { Cookie.parse(url, it.trim()) }.forEach(cookieJar::add)
    }

    /** `Cookie` header for [url], for handing authenticated downloads to another component. */
    fun cookieHeader(url: String): String? {
        val httpUrl = url.toHttpUrlOrNull() ?: return null
        return cookieJar.loadForRequest(httpUrl)
            .joinToString("; ") { "${it.name}=${it.value}" }
            .ifEmpty { null }
    }

    open fun isLogged(): Boolean {
        val url = baseUrl.toHttpUrlOrNull() ?: return false
        return cookieJar.loadForRequest(url).isNotEmpty()
    }

    /** Called whenever cookies are dropped, so subclasses can reset login timers or tokens. */
    protected open fun onLoggedOut() = Unit

    /** Whether a search needs a logged-in session. Yggtorrent searches anonymously. */
    protected open val searchRequiresLogin: Boolean get() = definition.requireAuthentication

    protected open suspend fun ensureLogin() {
        if (!definition.requireAuthentication || isLogged()) return
        if (auth is ProviderAuth.Credentials) {
            login()
        } else {
            throw LoginRequiredException(name)
        }
    }

    protected open suspend fun login() {
        val credentials = auth as? ProviderAuth.Credentials ?: throw LoginRequiredException(name)
        val body = definition.loginQueryString
            .replace("{username}", Uris.encodeComponent(credentials.username))
            .replace("{password}", Uris.encodeComponent(credentials.password))
        request(
            url = baseUrl + definition.loginUrl,
            method = "POST",
            body = body.toRequestBody(FORM),
            ensureLogin = false,
        )
    }

    // endregion

    // region Search

    fun getCategories(): List<String> = definition.categories.keys.toList()

    /** Category value for [categoryName] (case-insensitive), the default when blank, null if unsupported. */
    fun getCategoryValue(categoryName: String?): String? {
        if (categoryName.isNullOrEmpty()) return definition.categories[definition.defaultCategory]
        return definition.categories.entries
            .firstOrNull { it.key.equals(categoryName, ignoreCase = true) }
            ?.value
    }

    /** Search URL for [category]/[query], or null when the provider doesn't have that category. */
    fun getUrl(category: String?, query: String): String? {
        val cat = getCategoryValue(category) ?: return null
        val path = if (cat.startsWith("url:")) cat.substring(4) else definition.searchUrl
        return (baseUrl + path)
            .replace("{cat}", cat)
            .replace("{query}", Uris.encodeComponent(query))
    }

    /**
     * Searches this provider. Returns null when it doesn't support [category] (the original
     * returned `undefined`). [limit] controls how many result pages are fetched.
     */
    open suspend fun search(query: String, category: String? = null, limit: Int? = null): List<Torrent>? {
        val url = getUrl(category, query) ?: return null
        val pageLimit = computePageCount(limit, definition.resultsPerPageCount)
        if (searchRequiresLogin) ensureLogin()
        return postProcess(fetchAndParseUrl(url, pageLimit).mapNotNull(::toTorrent))
    }

    protected open suspend fun fetchAndParseUrl(url: String, pageLimit: Int): List<Map<String, String>> {
        val items = mutableListOf<Map<String, String>>()
        val visited = mutableSetOf<String>()
        val paginate = definition.paginateSelector.takeIf { it.isNotEmpty() }?.let(Selector::parse)
        var next: String? = url
        while (next != null && visited.size < pageLimit && visited.add(next)) {
            val document = fetchDocument(next, ensureLogin = searchRequiresLogin)
            items += scrapeItems(document)
            next = paginate?.evaluate(document)
        }
        return items
    }

    fun scrapeItems(document: Document): List<Map<String, String>> {
        val fields = definition.itemSelectors.mapValues { Selector.parse(it.value) }
        return document.select(definition.itemsSelector).mapNotNull { item ->
            fields.mapNotNull { (key, selector) -> selector.evaluate(item)?.let { key to it } }
                .toMap()
                .ifEmpty { null }
        }
    }

    /** Maps scraped fields to a [Torrent]; rows without a title (headers, ads) are dropped. */
    protected open fun toTorrent(fields: Map<String, String>): Torrent? {
        val title = fields["title"]?.trim()?.ifEmpty { null } ?: return null
        return Torrent(
            provider = name,
            title = title,
            time = fields["time"],
            seeds = fields["seeds"]?.let(Filters::parseInt),
            peers = fields["peers"]?.let(Filters::parseInt),
            size = fields["size"]?.trim(),
            magnet = fields["magnet"],
            link = fields["link"],
            desc = fields["desc"],
            extras = fields - KNOWN_FIELDS,
        )
    }

    protected open fun postProcess(results: List<Torrent>): List<Torrent> =
        results.map { if (it.provider == name) it else it.copy(provider = name) }.distinctBy { it.key }

    // endregion

    // region Torrent actions

    /** Returns the magnet, scraping it from the detail page when the listing didn't include it. */
    open suspend fun getMagnet(torrent: Torrent): String? {
        torrent.magnet?.let { return it }
        val desc = torrent.desc ?: return null
        return Selector.parse(definition.magnetSelector).evaluate(fetchDocument(desc))
    }

    /** Raw scraped HTML describing the torrent, or null when the provider has no detail selector. */
    open suspend fun getTorrentDetails(torrent: Torrent): String? {
        if (definition.torrentDetailsSelector.isEmpty()) return null
        val desc = torrent.desc ?: return null
        return Selector.parse(definition.torrentDetailsSelector).evaluate(fetchDocument(desc))
    }

    /** URL of the .torrent file (the original's `downloadTorrent`), with [cookieHeader] for private sites. */
    open suspend fun getTorrentFileUrl(torrent: Torrent): String? = torrent.link

    // endregion

    // region HTTP

    protected suspend fun fetchDocument(url: String, ensureLogin: Boolean = true): Document {
        val html = request(url, ensureLogin = ensureLogin)
        return Jsoup.parse(html, url).apply { outputSettings().prettyPrint(false) }
    }

    protected suspend fun request(
        url: String,
        method: String = "GET",
        body: RequestBody? = null,
        ensureLogin: Boolean = true,
        headers: Map<String, String> = emptyMap(),
    ): String {
        if (ensureLogin) ensureLogin()
        // Never put the URL in the message: it can carry a query, a token or a passkey.
        val httpUrl = url.toHttpUrlOrNull() ?: throw ProviderException("$name: invalid URL")
        val request = Request.Builder()
            .url(httpUrl)
            .method(method, body ?: if (method == "POST") FormBody.Builder().build() else null)
            .header("User-Agent", EngineHttp.USER_AGENT)
            .apply { (definition.headers + headers).forEach { (k, v) -> header(k, v) } }
            .build()
        client.newCall(request).await().use { response ->
            val text = response.body.string()
            val challenge = isChallenge(response.code, response.header("server"), response.header("cf-mitigated"), text)
            if (challenge) throw BlockedException(name)
            if (!response.isSuccessful) throw HttpStatusException(response.code, httpUrl)
            return text
        }
    }

    // endregion

    companion object {
        private val KNOWN_FIELDS = setOf("title", "time", "seeds", "peers", "size", "magnet", "link", "desc")
        private val FORM = "application/x-www-form-urlencoded".toMediaType()
        private val CHALLENGE_TITLE = Regex("""<title>\s*(Just a moment|Attention Required|DDoS-Guard)""", RegexOption.IGNORE_CASE)

        fun computePageCount(askedCount: Int?, resultsPerPageCount: Int): Int =
            if (askedCount != null && askedCount > 0) ceil(askedCount / resultsPerPageCount.toDouble()).toInt() else 1

        /**
         * A bot challenge (Cloudflare "Just a moment...", DDoS-Guard). We report it as blocked and
         * never try to solve it. A 200 response only counts when it is a challenge page.
         */
        internal fun isChallenge(code: Int, server: String?, mitigated: String?, body: String): Boolean {
            if (mitigated.equals("challenge", ignoreCase = true)) return true
            val challengeBody = body.contains("challenge-platform") || CHALLENGE_TITLE.containsMatchIn(body.take(4_096))
            val challengeServer = server.orEmpty().let { it.contains("cloudflare", true) || it.contains("ddos-guard", true) }
            return when (code) {
                403, 429, 503 -> challengeServer && challengeBody
                in 200..299 -> challengeBody && CHALLENGE_TITLE.containsMatchIn(body.take(4_096))
                else -> false
            }
        }
    }
}
