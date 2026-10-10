package com.nexient.jazzy.feature.settings

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.StringRes
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
}.getOrDefault(context.getString(R.string.settings_about_version_unknown))

/** [name] and [licence] are product / SPDX names and stay as data; [note] is user-facing text. */
internal data class Licence(val name: String, val licence: String, @StringRes val note: Int)

internal object AboutText {
    val LICENCES = listOf(
        Licence("libtorrent", "BSD 3-Clause", R.string.settings_about_licence_libtorrent),
        Licence("libtorrent4j", "MIT", R.string.settings_about_licence_libtorrent4j),
        Licence("torrent-search-api", "MIT", R.string.settings_about_licence_torrent_search_api),
        Licence("AndroidX Media3 (ExoPlayer)", "Apache-2.0", R.string.settings_about_licence_androidx_media3),
        Licence("FFmpeg 7.1.5 (Media3 decoder extension)", "LGPL-2.1 or later", R.string.settings_about_licence_ffmpeg),
        Licence("OkHttp", "Apache-2.0", R.string.settings_about_licence_okhttp),
        Licence("Retrofit", "Apache-2.0", R.string.settings_about_licence_retrofit),
        Licence("jsoup", "MIT", R.string.settings_about_licence_jsoup),
        Licence("NanoHTTPD", "BSD 3-Clause", R.string.settings_about_licence_nanohttpd),
        Licence("Coil", "Apache-2.0", R.string.settings_about_licence_coil),
        Licence("Jetpack Compose, Room, WorkManager and AndroidX", "Apache-2.0", R.string.settings_about_licence_jetpack_compose),
        Licence("Dagger / Hilt", "Apache-2.0", R.string.settings_about_licence_dagger),
        Licence("Kotlin, kotlinx.coroutines and kotlinx.serialization", "Apache-2.0", R.string.settings_about_licence_kotlin),
    )
}
