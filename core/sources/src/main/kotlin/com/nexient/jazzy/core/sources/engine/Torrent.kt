package com.nexient.jazzy.core.sources.engine

/**
 * One search result as a site lists it. Ported from TorrentSearch (itself a port of
 * torrent-search-api): every field except [provider] and [title] is optional because each site
 * exposes a different subset.
 *
 * - [time] and [size] are kept as the site displays them; use [sizeBytes] for sorting.
 * - [magnet] may be null until resolved with [TorrentProvider.getMagnet] (fetches [desc]).
 * - [link] is a direct .torrent file URL when the listing exposes one.
 * - [desc] is the torrent's detail page URL.
 * - [extras] holds provider-specific fields (TPB `id`, YTS `hash`/`imdb`, TorrentLeech `fid`, ...).
 */
internal data class Torrent(
    /** Display name of the provider that listed this torrent. */
    val provider: String,
    val title: String,
    val time: String? = null,
    val seeds: Int? = null,
    val peers: Int? = null,
    val size: String? = null,
    val magnet: String? = null,
    val link: String? = null,
    val desc: String? = null,
    val extras: Map<String, String> = emptyMap(),
) {
    /**
     * Identity within one provider's results; used to drop duplicates (e.g. overlapping pages)
     * and as a list key. Includes the title because one page can list several torrents (YTS).
     */
    val key: String get() = "$provider|${desc.orEmpty()}|${magnet ?: link ?: ""}|$title"

    val sizeBytes: Long? by lazy(LazyThreadSafetyMode.NONE) { Sizes.parse(size) }

    /** Lowercase hex info-hash from the magnet or a `hash` extra; base32 hashes are converted. */
    val infoHash: String? get() = (magnet?.let(Magnets::infoHash) ?: extras["hash"]?.let(Magnets::normaliseHash))
}

/** Static description of an engine provider (the original's `getProviders()` entries). */
internal data class EngineProviderInfo(
    val id: String,
    val name: String,
    val isPublic: Boolean,
    val categories: List<String>,
    val defaultBaseUrl: String,
    val supportsCredentials: Boolean,
    val supportsCookies: Boolean,
    /** The original library only reached this site through a Cloudflare challenge solver. */
    val behindCloudflare: Boolean,
)

/** Mirrors the `enableProvider(name, ...args)` overloads of the original library. */
internal sealed interface ProviderAuth {
    data object None : ProviderAuth
    data class Credentials(val username: String, val password: String) : ProviderAuth {
        override fun toString(): String = "Credentials(username=$username, password=***)"
    }

    /** Raw cookie strings such as `uid=XXX;` — one per cookie. */
    data class Cookies(val cookies: List<String>) : ProviderAuth {
        override fun toString(): String = "Cookies(${cookies.size})"
    }

    data class Token(val token: String) : ProviderAuth {
        override fun toString(): String = "Token(***)"
    }
}
