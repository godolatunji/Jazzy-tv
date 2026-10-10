package com.nexient.jazzy.feature.settings.ui

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nexient.jazzy.core.common.formatBytes
import com.nexient.jazzy.core.data.settings.SettingKeys
import com.nexient.jazzy.core.designsystem.component.SettingsActionRow
import com.nexient.jazzy.core.designsystem.component.SettingsSwitchRow
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.model.SourceSort
import com.nexient.jazzy.feature.settings.R
import com.nexient.jazzy.feature.settings.SettingsUiState
import com.nexient.jazzy.feature.settings.SettingsViewModel

/** Settings sections (spec §5.11). */
internal enum class SettingsSection(@StringRes val title: Int, val icon: ImageVector) {
    PROFILES(R.string.settings_section_profiles, Icons.Filled.SupervisorAccount),
    PLAYBACK(R.string.settings_section_playback, Icons.Filled.PlayCircle),
    SUBTITLES(R.string.settings_section_subtitles, Icons.Filled.Subtitles),
    SOURCES(R.string.settings_section_sources, Icons.Filled.Public),
    HISTORY(R.string.settings_section_history, Icons.Filled.History),
    NETWORK(R.string.settings_section_network, Icons.Filled.NetworkCheck),
    LOCAL_DATA(R.string.settings_section_local_data, Icons.Filled.Storage),
    PRIVACY(R.string.settings_section_privacy, Icons.Filled.PrivacyTip),
    ABOUT(R.string.settings_section_about, Icons.Filled.Info),
}

/** Sections visible for this profile: Kids profiles don't see Sources (PR-4). */
internal fun SettingsUiState.visibleSections(): List<SettingsSection> =
    SettingsSection.entries.filter { it != SettingsSection.SOURCES || !isKids }

internal enum class SettingsDialog {
    SORT, UPLOAD, CONNECTIONS, CACHE_CAP, GENRES, OPENSUBTITLES_LOGIN,
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

internal fun uploadLabel(bps: Long, res: Resources): String = when {
    bps <= 0 -> res.getString(R.string.settings_network_upload_minimal)
    bps % 1_000_000 == 0L -> res.getString(R.string.settings_network_upload_mbps, bps / 1_000_000)
    bps % 1_000 == 0L -> res.getString(R.string.settings_network_upload_kbps, bps / 1_000)
    else -> res.getString(R.string.settings_network_upload_per_second, formatBytes(bps))
}

internal fun capLabel(bytes: Long, res: Resources): String =
    if (bytes % GIB == 0L) res.getString(R.string.settings_network_cache_gb, bytes / GIB) else formatBytes(bytes)

internal fun sortLabel(sort: SourceSort, res: Resources): String = res.getString(
    when (sort) {
        SourceSort.SEEDERS -> R.string.settings_playback_sort_seeders
        SourceSort.SIZE -> R.string.settings_playback_sort_size
        SourceSort.QUALITY -> R.string.settings_playback_sort_quality
    },
)

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
    val res = LocalResources.current
    val pendingSize = stringResource(R.string.settings_common_size_pending)
    Column(Modifier.fillMaxWidth()) {
        when (section) {
            SettingsSection.PROFILES -> {
                SettingsActionRow(
                    title = stringResource(R.string.settings_profiles_manage_title),
                    subtitle = stringResource(R.string.settings_profiles_manage_subtitle),
                    icon = Icons.Filled.ManageAccounts,
                    showChevron = true,
                    onClick = nav.manageProfiles,
                    focusRequester = firstItemFocus,
                )
                if (!state.hasParentalPin) {
                    SettingsActionRow(
                        title = stringResource(R.string.settings_profiles_parental_pin_title),
                        subtitle = stringResource(R.string.settings_profiles_parental_pin_subtitle),
                        value = stringResource(R.string.settings_common_off),
                        icon = Icons.Filled.LockOpen,
                        onClick = viewModel::startSetParentalPin,
                    )
                } else {
                    SettingsActionRow(
                        title = stringResource(R.string.settings_profiles_change_parental_pin_title),
                        subtitle = stringResource(R.string.settings_profiles_change_parental_pin_subtitle),
                        icon = Icons.Filled.Lock,
                        onClick = viewModel::startChangeParentalPin,
                    )
                    SettingsActionRow(title = stringResource(R.string.settings_profiles_remove_parental_pin_title), icon = Icons.Filled.LockOpen, onClick = viewModel::startRemoveParentalPin)
                }
                FavouriteGenresRow(state, nav)
            }

            SettingsSection.PLAYBACK -> {
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_playback_autoplay_title),
                    subtitle = stringResource(R.string.settings_playback_autoplay_subtitle),
                    checked = s.autoplayNextDefault,
                    onCheckedChange = { viewModel.setFlag(SettingKeys.AUTOPLAY_NEXT, it) },
                    focusRequester = firstItemFocus,
                )
                SettingsActionRow(
                    title = stringResource(R.string.settings_playback_sort_title),
                    value = sortLabel(s.defaultSourceSort, res),
                    icon = Icons.AutoMirrored.Filled.Sort,
                    onClick = { nav.openDialog(SettingsDialog.SORT) },
                )
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_playback_hide_cam_title),
                    subtitle = stringResource(R.string.settings_playback_hide_cam_subtitle),
                    checked = s.hideCamByDefault,
                    onCheckedChange = { viewModel.setFlag(SettingKeys.HIDE_CAM, it) },
                )
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_playback_hide_unsupported_title),
                    subtitle = stringResource(R.string.settings_playback_hide_unsupported_subtitle),
                    checked = s.hideUnsupportedByDefault,
                    onCheckedChange = { viewModel.setFlag(SettingKeys.HIDE_UNSUPPORTED, it) },
                )
            }

            SettingsSection.SOURCES -> {
                SettingsActionRow(
                    title = stringResource(R.string.settings_sources_sites_title),
                    subtitle = stringResource(R.string.settings_sources_sites_subtitle),
                    icon = Icons.Filled.Public,
                    showChevron = true,
                    onClick = nav.openSites,
                    focusRequester = firstItemFocus,
                )
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_sources_show_dead_title),
                    subtitle = stringResource(R.string.settings_sources_show_dead_subtitle),
                    checked = s.showDeadLinks,
                    onCheckedChange = { viewModel.setFlag(SettingKeys.SHOW_DEAD, it) },
                )
            }

            SettingsSection.SUBTITLES -> {
                if (!state.openSubtitlesConfigured) {
                    SettingsActionRow(
                        title = stringResource(R.string.settings_subtitles_online_title),
                        subtitle = stringResource(R.string.settings_subtitles_online_not_set_up),
                        icon = Icons.Filled.Subtitles,
                        onClick = {},
                        focusRequester = firstItemFocus,
                    )
                } else if (state.openSubtitlesUser == null) {
                    SettingsActionRow(
                        title = stringResource(R.string.settings_subtitles_login_title),
                        subtitle = stringResource(R.string.settings_subtitles_login_subtitle),
                        icon = Icons.Filled.Login,
                        onClick = { nav.openDialog(SettingsDialog.OPENSUBTITLES_LOGIN) },
                        focusRequester = firstItemFocus,
                    )
                } else {
                    SettingsActionRow(
                        title = stringResource(R.string.settings_subtitles_logout_title),
                        subtitle = stringResource(R.string.settings_subtitles_logged_in_as, state.openSubtitlesUser),
                        icon = Icons.Filled.Logout,
                        onClick = viewModel::logoutOpenSubtitles,
                        focusRequester = firstItemFocus,
                    )
                }
                Text(
                    stringResource(R.string.settings_subtitles_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = JazzyColors.TextTertiary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            SettingsSection.HISTORY -> {
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_history_remove_watched_title),
                    subtitle = stringResource(R.string.settings_history_remove_watched_subtitle),
                    checked = s.removeFromListWhenWatched,
                    onCheckedChange = { viewModel.setFlag(SettingKeys.REMOVE_WHEN_WATCHED, it) },
                    focusRequester = firstItemFocus,
                )
                FavouriteGenresRow(state, nav, title = stringResource(R.string.settings_history_edit_genres_title))
                SettingsActionRow(
                    title = stringResource(R.string.settings_history_clear_title),
                    subtitle = stringResource(
                        if (state.profile?.hasPin == true) R.string.settings_history_clear_subtitle_pin else R.string.settings_history_clear_subtitle,
                        profileName ?: stringResource(R.string.settings_common_this_profile),
                    ),
                    icon = Icons.Filled.Delete,
                    destructive = true,
                    enabled = state.profile != null && !state.busy,
                    onClick = { nav.openDialog(SettingsDialog.CONFIRM_CLEAR_HISTORY) },
                )
                SettingsActionRow(
                    title = stringResource(R.string.settings_history_clear_search_title),
                    icon = Icons.Filled.SearchOff,
                    enabled = state.profile != null && !state.busy,
                    onClick = { nav.openDialog(SettingsDialog.CONFIRM_CLEAR_SEARCH) },
                )
                SettingsActionRow(
                    title = stringResource(R.string.settings_history_reset_recs_title),
                    subtitle = stringResource(R.string.settings_history_reset_recs_subtitle),
                    icon = Icons.Filled.Refresh,
                    enabled = state.profile != null && !state.busy,
                    onClick = { nav.openDialog(SettingsDialog.CONFIRM_RESET_RECS) },
                )
            }

            SettingsSection.NETWORK -> {
                val e = s.engine
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_network_wifi_only_title),
                    subtitle = stringResource(R.string.settings_network_wifi_only_subtitle),
                    checked = e.wifiOnly,
                    onCheckedChange = { viewModel.setFlag(SettingKeys.WIFI_ONLY, it) },
                    focusRequester = firstItemFocus,
                )
                SettingsActionRow(
                    title = stringResource(R.string.settings_network_upload_title),
                    value = uploadLabel(e.uploadLimitBps, res),
                    icon = Icons.Filled.Speed,
                    onClick = { nav.openDialog(SettingsDialog.UPLOAD) },
                )
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_network_stop_seeding_title),
                    subtitle = stringResource(R.string.settings_network_stop_seeding_subtitle),
                    checked = e.stopSeedingOnStop,
                    onCheckedChange = { viewModel.setFlag(SettingKeys.STOP_SEEDING, it) },
                )
                SettingsActionRow(
                    title = stringResource(R.string.settings_network_connections_title),
                    value = e.maxConnections.toString(),
                    onClick = { nav.openDialog(SettingsDialog.CONNECTIONS) },
                )
                SettingsActionRow(
                    title = stringResource(R.string.settings_network_cache_size_title),
                    subtitle = stringResource(
                        R.string.settings_network_cache_size_subtitle,
                        state.torrentCacheBytes?.let(::formatBytes) ?: pendingSize,
                    ),
                    value = capLabel(e.cacheCapBytes, res),
                    icon = Icons.Filled.Storage,
                    onClick = { nav.openDialog(SettingsDialog.CACHE_CAP) },
                )
                SettingsActionRow(
                    title = stringResource(R.string.settings_network_clear_torrent_title),
                    subtitle = stringResource(
                        R.string.settings_network_clear_torrent_subtitle,
                        state.torrentCacheBytes?.let(::formatBytes) ?: pendingSize,
                    ),
                    icon = Icons.Filled.Delete,
                    destructive = true,
                    enabled = !state.busy,
                    onClick = { nav.openDialog(SettingsDialog.CONFIRM_CLEAR_TORRENT) },
                )
            }

            SettingsSection.LOCAL_DATA -> {
                SettingsActionRow(
                    title = stringResource(R.string.settings_local_clear_tmdb_title),
                    subtitle = stringResource(R.string.settings_local_clear_tmdb_subtitle),
                    icon = Icons.Filled.DeleteSweep,
                    destructive = true,
                    enabled = !state.busy,
                    onClick = { nav.openDialog(SettingsDialog.CONFIRM_CLEAR_TMDB) },
                    focusRequester = firstItemFocus,
                )
                InfoRow(stringResource(R.string.settings_local_app_data), state.localData?.appDbBytes?.let(::formatBytes) ?: pendingSize)
                InfoRow(stringResource(R.string.settings_local_tmdb_cache), state.localData?.cacheDbBytes?.let(::formatBytes) ?: pendingSize)
            }

            SettingsSection.PRIVACY -> {
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_privacy_crash_title),
                    subtitle = stringResource(R.string.settings_privacy_crash_subtitle),
                    checked = s.crashReportingOptIn,
                    onCheckedChange = { viewModel.setFlag(SettingKeys.CRASH_OPT_IN, it) },
                    focusRequester = firstItemFocus,
                )
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_privacy_metrics_title),
                    subtitle = stringResource(R.string.settings_privacy_metrics_subtitle),
                    checked = s.metricsOptIn,
                    onCheckedChange = { viewModel.setFlag(SettingKeys.METRICS_OPT_IN, it) },
                )
            }

            SettingsSection.ABOUT -> {
                SettingsActionRow(
                    title = stringResource(R.string.settings_about_row_title),
                    subtitle = stringResource(R.string.settings_about_row_subtitle),
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
private fun FavouriteGenresRow(
    state: SettingsUiState,
    nav: SettingsNav,
    title: String = stringResource(R.string.settings_profiles_genres_title),
) {
    val p = state.profile ?: return
    val count = state.favouriteGenres.size
    SettingsActionRow(
        title = title,
        subtitle = stringResource(R.string.settings_profiles_genres_subtitle, p.name),
        value = if (count == 0) {
            stringResource(R.string.settings_common_none)
        } else {
            pluralStringResource(R.plurals.settings_common_genres_selected, count, count)
        },
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
