package com.nexient.jazzy.feature.settings

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.feature.settings.ui.AboutPhoneScreen
import com.nexient.jazzy.feature.settings.ui.AboutTvScreen

/** About: version, TMDB attribution, open-source licences, disclaimer and privacy (spec §5.11, §11). */
@Composable
fun AboutRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    val version = remember(context) { appVersion(context) }
    val modifier = Modifier.fillMaxSize().background(JazzyColors.Background)
    if (JazzyTheme.isTv) AboutTvScreen(version, modifier) else AboutPhoneScreen(version, onBack, modifier)
}

private fun appVersion(context: Context): String = runCatching {
    val pm = context.packageManager
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        pm.getPackageInfo(context.packageName, 0)
    }
    "${info.versionName} (${info.longVersionCode})"
}.getOrDefault("Unknown")

internal data class Licence(val name: String, val licence: String, val note: String)

internal object AboutText {
    const val TMDB_ATTRIBUTION = "This product uses the TMDB API but is not endorsed or certified by TMDB."
    const val TMDB_DETAIL = "Film and TV information and artwork come from The Movie Database (themoviedb.org)."

    const val DISCLAIMER =
        "Jazzy doesn't host, upload or store any films or TV shows. It searches third-party torrent sites and " +
            "streams the file you choose directly from other people's computers (peer to peer), which also shares " +
            "pieces of that file with them while you watch. You are responsible for what you search for, stream and " +
            "share, and for following the copyright law where you live. Only watch content you have the right to watch."

    const val PRIVACY =
        "Jazzy has no account and no servers. Profiles, play history, likes, My List, search history and " +
            "recommendations are stored only in a local database on this device and never leave it. TMDB receives " +
            "the look-ups needed to show title information, torrent sites receive the searches Jazzy runs for you, " +
            "and other peers in a torrent swarm can see your IP address. Crash reports and usage metrics stay off " +
            "unless you turn them on in Settings → Privacy."

    val LICENCES = listOf(
        Licence("libtorrent", "BSD 3-Clause", "Copyright Arvid Norberg. BitTorrent engine."),
        Licence("libtorrent4j", "MIT", "Java bindings for libtorrent."),
        Licence(
            "torrent-search-api",
            "MIT",
            "Copyright Jimmy Laurent. Torrent-site search engine, ported to Kotlin through the TorrentSearch project.",
        ),
        Licence("AndroidX Media3 (ExoPlayer)", "Apache-2.0", "Copyright The Android Open Source Project."),
        Licence(
            "FFmpeg (Media3 decoder extension)",
            "LGPL-2.1 or later",
            "Built in an LGPL-only configuration and linked dynamically. Source code is available from ffmpeg.org.",
        ),
        Licence("OkHttp", "Apache-2.0", "Copyright Square, Inc."),
        Licence("Retrofit", "Apache-2.0", "Copyright Square, Inc."),
        Licence("jsoup", "MIT", "Copyright Jonathan Hedley."),
        Licence("NanoHTTPD", "BSD 3-Clause", "Copyright Paul S. Hawke, Jarno Elonen and contributors."),
        Licence("Coil", "Apache-2.0", "Copyright Coil Contributors."),
        Licence("Jetpack Compose, Room, WorkManager and AndroidX", "Apache-2.0", "Copyright The Android Open Source Project."),
        Licence("Dagger / Hilt", "Apache-2.0", "Copyright The Dagger Authors."),
        Licence("Kotlin, kotlinx.coroutines and kotlinx.serialization", "Apache-2.0", "Copyright JetBrains s.r.o. and contributors."),
    )
}
