package com.nexient.jazzy.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.nexient.jazzy.core.common.formatBytes
import com.nexient.jazzy.core.data.settings.SettingKeys
import com.nexient.jazzy.core.designsystem.component.SettingsActionRow
import com.nexient.jazzy.core.designsystem.component.SettingsSwitchRow
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.model.SourceSort
import com.nexient.jazzy.feature.settings.SettingsUiState
import com.nexient.jazzy.feature.settings.SettingsViewModel

/** Settings sections (spec §5.11). */
internal enum class SettingsSection(val title: String, val icon: ImageVector) {
    PROFILES("Profiles & parental controls", Icons.Filled.SupervisorAccount),
    PLAYBACK("Playback", Icons.Filled.PlayCircle),
    SOURCES("Sources", Icons.Filled.Public),
    HISTORY("History & recommendations", Icons.Filled.History),
    NETWORK("Network & storage", Icons.Filled.NetworkCheck),
    LOCAL_DATA("Local data", Icons.Filled.Storage),
    PRIVACY("Privacy", Icons.Filled.PrivacyTip),
    ABOUT("About", Icons.Filled.Info),
}

/** Sections visible for this profile: Kids profiles don't see Sources (PR-4). */
internal fun SettingsUiState.visibleSections(): List<SettingsSection> =
    SettingsSection.entries.filter { it != SettingsSection.SOURCES || !isKids }

internal enum class SettingsDialog {
    SORT, UPLOAD, CONNECTIONS, CACHE_CAP, GENRES,
    CONFIRM_CLEAR_HISTORY, CONFIRM_CLEAR_SEARCH, CONFIRM_RESET_RECS, CONFIRM_CLEAR_TORRENT, CONFIRM_CLEAR_TMDB,
}

internal class SettingsNav(
    val openSites: () -> Unit,
    val openAbout: () -> Unit,
    val manageProfiles: () -> Unit,
    val openDialog: (SettingsDialog) -> Unit,
)

// --- Choice lists --------------------------------------------------------------------------

internal val UploadLimitOptions = listOf(250_000L, 500_000L, 1_000_000L, 2_000_000L, 5_000_000L, 10_000_000L)
internal val MaxConnectionOptions = listOf(50, 100, 200, 300, 500)
private const val GIB = 1024L * 1024 * 1024
internal val CacheCapOptions = listOf(1 * GIB, 2 * GIB, 4 * GIB, 8 * GIB, 16 * GIB, 32 * GIB)

internal fun uploadLabel(bps: Long): String = when {
    bps <= 0 -> "Minimal"
    bps % 1_000_000 == 0L -> "${bps / 1_000_000} MB/s"
    bps % 1_000 == 0L -> "${bps / 1_000} KB/s"
    else -> formatBytes(bps) + "/s"
}

internal fun capLabel(bytes: Long): String = if (bytes % GIB == 0L) "${bytes / GIB} GB" else formatBytes(bytes)

internal fun sortLabel(sort: SourceSort): String = when (sort) {
    SourceSort.SEEDERS -> "Seeders (most first)"
    SourceSort.SIZE -> "Size"
    SourceSort.QUALITY -> "Quality"
}

// --- Section content -------------------------------------------------------------------------

@Composable
internal fun SettingsSectionContent(
    section: SettingsSection,
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    nav: SettingsNav,
    firstItemFocus: FocusRequester? = null,
) {
    val s = state.settings
    val profileName = state.profile?.name
    Column(Modifier.fillMaxWidth()) {
        when (section) {
            SettingsSection.PROFILES -> {
                SettingsActionRow(
                    title = "Manage profiles",
                    subtitle = "Add, edit or delete profiles, Kids profiles and maturity ratings.",
                    icon = Icons.Filled.ManageAccounts,
                    showChevron = true,
                    onClick = nav.manageProfiles,
                    focusRequester = firstItemFocus,
                )
                if (!state.hasParentalPin) {
                    SettingsActionRow(
                        title = "Parental PIN",
                        subtitle = "Protects adding, editing and deleting profiles, and leaving a Kids profile.",
                        value = "Off",
                        icon = Icons.Filled.LockOpen,
                        onClick = viewModel::startSetParentalPin,
                    )
                } else {
                    SettingsActionRow(
                        title = "Change parental PIN",
                        subtitle = "Protects profile management and leaving a Kids profile.",
                        icon = Icons.Filled.Lock,
                        onClick = viewModel::startChangeParentalPin,
                    )
                    SettingsActionRow(title = "Remove parental PIN", icon = Icons.Filled.LockOpen, onClick = viewModel::startRemoveParentalPin)
                }
                FavouriteGenresRow(state, nav)
            }

            SettingsSection.PLAYBACK -> {
                SettingsSwitchRow(
                    title = "Autoplay next episode",
                    subtitle = "Default for new profiles. Each profile can change it in its own settings.",
                    checked = s.autoplayNextDefault,
                    onCheckedChange = { viewModel.setFlag(SettingKeys.AUTOPLAY_NEXT, it) },
                    focusRequester = firstItemFocus,
                )
                SettingsActionRow(
                    title = "Default Sources sort",
                    value = sortLabel(s.defaultSourceSort),
                    icon = Icons.AutoMirrored.Filled.Sort,
                    onClick = { nav.openDialog(SettingsDialog.SORT) },
                )
                SettingsSwitchRow(
                    title = "Hide CAM, TS and TC releases",
                    subtitle = "Low-quality camera recordings are hidden from the Sources list by default.",
                    checked = s.hideCamByDefault,
                    onCheckedChange = { viewModel.setFlag(SettingKeys.HIDE_CAM, it) },
                )
                SettingsSwitchRow(
                    title = "Hide releases that may not play",
                    subtitle = "Codecs or HDR formats this device can't decode.",
                    checked = s.hideUnsupportedByDefault,
                    onCheckedChange = { viewModel.setFlag(SettingKeys.HIDE_UNSUPPORTED, it) },
                )
            }

            SettingsSection.SOURCES -> {
                SettingsActionRow(
                    title = "Torrent sites",
                    subtitle = "Enable or disable sites, mirror URLs, logins, health and Test.",
                    icon = Icons.Filled.Public,
                    showChevron = true,
                    onClick = nav.openSites,
                    focusRequester = firstItemFocus,
                )
                SettingsSwitchRow(
                    title = "Show dead links",
                    subtitle = "Include sources with no seeders in the Sources list by default.",
                    checked = s.showDeadLinks,
                    onCheckedChange = { viewModel.setFlag(SettingKeys.SHOW_DEAD, it) },
                )
            }

            SettingsSection.HISTORY -> {
                SettingsSwitchRow(
                    title = "Remove from My List when watched",
                    subtitle = "Movies leave My List once watched; shows after their last aired episode.",
                    checked = s.removeFromListWhenWatched,
                    onCheckedChange = { viewModel.setFlag(SettingKeys.REMOVE_WHEN_WATCHED, it) },
                    focusRequester = firstItemFocus,
                )
                FavouriteGenresRow(state, nav, title = "Edit favourite genres")
                SettingsActionRow(
                    title = "Clear play history",
                    subtitle = buildString {
                        append("Erases ${profileName ?: "this profile"}'s history and resets genre affinity.")
                        if (state.profile?.hasPin == true) append(" Needs the profile PIN.")
                    },
                    icon = Icons.Filled.Delete,
                    destructive = true,
                    enabled = state.profile != null && !state.busy,
                    onClick = { nav.openDialog(SettingsDialog.CONFIRM_CLEAR_HISTORY) },
                )
                SettingsActionRow(
                    title = "Clear search history",
                    icon = Icons.Filled.SearchOff,
                    enabled = state.profile != null && !state.busy,
                    onClick = { nav.openDialog(SettingsDialog.CONFIRM_CLEAR_SEARCH) },
                )
                SettingsActionRow(
                    title = "Reset recommendations",
                    subtitle = "Clears genre affinity and saved recommendations. Watch history is kept.",
                    icon = Icons.Filled.Refresh,
                    enabled = state.profile != null && !state.busy,
                    onClick = { nav.openDialog(SettingsDialog.CONFIRM_RESET_RECS) },
                )
            }

            SettingsSection.NETWORK -> {
                val e = s.engine
                SettingsSwitchRow(
                    title = "Stream on Wi-Fi only",
                    subtitle = "Never use mobile data for torrent streaming.",
                    checked = e.wifiOnly,
                    onCheckedChange = { viewModel.setFlag(SettingKeys.WIFI_ONLY, it) },
                    focusRequester = firstItemFocus,
                )
                SettingsActionRow(
                    title = "Upload limit",
                    value = uploadLabel(e.uploadLimitBps),
                    icon = Icons.Filled.Speed,
                    onClick = { nav.openDialog(SettingsDialog.UPLOAD) },
                )
                SettingsSwitchRow(
                    title = "Stop seeding when playback stops",
                    subtitle = "No torrent activity after you close the player.",
                    checked = e.stopSeedingOnStop,
                    onCheckedChange = { viewModel.setFlag(SettingKeys.STOP_SEEDING, it) },
                )
                SettingsActionRow(
                    title = "Max connections",
                    value = e.maxConnections.toString(),
                    onClick = { nav.openDialog(SettingsDialog.CONNECTIONS) },
                )
                SettingsActionRow(
                    title = "Torrent cache size",
                    subtitle = "Using ${state.torrentCacheBytes?.let(::formatBytes) ?: "…"}. Oldest data is removed first.",
                    value = capLabel(e.cacheCapBytes),
                    icon = Icons.Filled.Storage,
                    onClick = { nav.openDialog(SettingsDialog.CACHE_CAP) },
                )
                SettingsActionRow(
                    title = "Clear torrent cache",
                    subtitle = "Deletes downloaded pieces (${state.torrentCacheBytes?.let(::formatBytes) ?: "…"}).",
                    icon = Icons.Filled.Delete,
                    destructive = true,
                    enabled = !state.busy,
                    onClick = { nav.openDialog(SettingsDialog.CONFIRM_CLEAR_TORRENT) },
                )
            }

            SettingsSection.LOCAL_DATA -> {
                SettingsActionRow(
                    title = "Clear TMDB cache",
                    subtitle = "Keeps history, My List, likes and resume points. Titles and artwork info reload from TMDB.",
                    icon = Icons.Filled.DeleteSweep,
                    destructive = true,
                    enabled = !state.busy,
                    onClick = { nav.openDialog(SettingsDialog.CONFIRM_CLEAR_TMDB) },
                    focusRequester = firstItemFocus,
                )
                InfoRow("App data (profiles, lists, history)", state.localData?.appDbBytes?.let(::formatBytes) ?: "…")
                InfoRow("TMDB & search cache", state.localData?.cacheDbBytes?.let(::formatBytes) ?: "…")
            }

            SettingsSection.PRIVACY -> {
                SettingsSwitchRow(
                    title = "Send crash reports",
                    subtitle = "Off by default. Crash details only, no viewing data.",
                    checked = s.crashReportingOptIn,
                    onCheckedChange = { viewModel.setFlag(SettingKeys.CRASH_OPT_IN, it) },
                    focusRequester = firstItemFocus,
                )
                SettingsSwitchRow(
                    title = "Share anonymous usage metrics",
                    subtitle = "Aggregate counts only. History, likes and recommendations never leave this device.",
                    checked = s.metricsOptIn,
                    onCheckedChange = { viewModel.setFlag(SettingKeys.METRICS_OPT_IN, it) },
                )
            }

            SettingsSection.ABOUT -> {
                SettingsActionRow(
                    title = "About Jazzy",
                    subtitle = "Version, TMDB attribution, open-source licences, privacy and disclaimer.",
                    icon = Icons.Filled.Info,
                    showChevron = true,
                    onClick = nav.openAbout,
                    focusRequester = firstItemFocus,
                )
            }
        }
    }
}

@Composable
private fun FavouriteGenresRow(state: SettingsUiState, nav: SettingsNav, title: String = "Favourite genres") {
    val p = state.profile ?: return
    SettingsActionRow(
        title = title,
        subtitle = "For ${p.name}. Seeds recommendations until there's enough watch history.",
        value = if (state.favouriteGenres.isEmpty()) "None" else "${state.favouriteGenres.size} selected",
        icon = Icons.Filled.Favorite,
        onClick = { nav.openDialog(SettingsDialog.GENRES) },
    )
}

@Composable
private fun InfoRow(title: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = JazzyColors.TextSecondary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
