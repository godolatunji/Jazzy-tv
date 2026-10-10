package com.nexient.jazzy.core.sources.engine

import com.nexient.jazzy.core.sources.engine.providers.Rarbg
import com.nexient.jazzy.core.sources.engine.providers.ThePirateBay
import com.nexient.jazzy.core.sources.engine.providers.Torrent9
import com.nexient.jazzy.core.sources.engine.providers.TorrentLeech
import com.nexient.jazzy.core.sources.engine.providers.X1337x
import com.nexient.jazzy.core.sources.engine.providers.Yggtorrent
import com.nexient.jazzy.core.sources.engine.providers.Yts
import com.nexient.jazzy.core.sources.engine.providers.eztv
import com.nexient.jazzy.core.sources.engine.providers.ipTorrents
import com.nexient.jazzy.core.sources.engine.providers.kickassTorrents
import com.nexient.jazzy.core.sources.engine.providers.limetorrents
import com.nexient.jazzy.core.sources.engine.providers.magnetDl
import com.nexient.jazzy.core.sources.engine.providers.torrentProject
import com.nexient.jazzy.core.sources.engine.providers.torrentz2
import okhttp3.OkHttpClient

/**
 * The built-in providers: every provider shipped by torrent-search-api 2.1.4 plus MagnetDL, as in
 * TorrentSearch's `ProviderManager.allProviders`. Ordered with the default-enabled public sites
 * first, then the disabled public sites, then the private trackers. (TorrentSearch's
 * `ProviderManager` aggregation is replaced by `DefaultSourceSearcher`.)
 */
internal object ProviderRegistry {
    fun allProviders(client: OkHttpClient): List<TorrentProvider> = listOf(
        ThePirateBay(client),
        Yts(client),
        limetorrents(client),
        torrentProject(client),
        kickassTorrents(client),
        magnetDl(client),
        X1337x(client),
        eztv(client),
        Torrent9(client),
        torrentz2(client),
        Rarbg(client),
        ipTorrents(client),
        TorrentLeech(client),
        Yggtorrent(client),
    ).also { providers ->
        check(providers.map { it.id }.toSet().size == providers.size) { "Duplicate provider id" }
    }
}
