package com.nexient.jazzy.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.tv.material3.NavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import com.nexient.jazzy.R
import com.nexient.jazzy.core.data.catalog.HomeTab
import com.nexient.jazzy.core.designsystem.component.AvatarTile
import com.nexient.jazzy.core.model.Profile

private data class DrawerEntry(val key: NavKey, @StringRes val label: Int, val icon: ImageVector?)

/**
 * TV shell: a left navigation drawer that collapses to icons and expands on focus (spec §4),
 * shown only on top-level destinations.
 */
@Composable
internal fun TvShell(current: NavKey?, nav: Navigator, profile: Profile?, content: @Composable () -> Unit) {
    if (current?.isTopLevel() != true) {
        content()
        return
    }
    val entries = listOf(
        DrawerEntry(SearchRoute, R.string.app_nav_search, Icons.Filled.Search),
        DrawerEntry(HomeRoute, R.string.app_nav_home, Icons.Filled.Home),
        DrawerEntry(HomeTabRoute(HomeTab.MOVIES.name), R.string.app_nav_movies, Icons.Filled.Movie),
        DrawerEntry(HomeTabRoute(HomeTab.TV.name), R.string.app_nav_tv_shows, Icons.Filled.Tv),
        DrawerEntry(DiscoverRoute, R.string.app_nav_discover, Icons.Filled.Explore),
        DrawerEntry(LibraryRoute(0), R.string.app_nav_my_list, Icons.Filled.PlaylistAddCheck),
        DrawerEntry(LibraryRoute(1), R.string.app_nav_liked, Icons.Filled.Favorite),
        DrawerEntry(LibraryRoute(2), R.string.app_nav_history, Icons.Filled.History),
        DrawerEntry(SettingsRoute, R.string.app_nav_settings, Icons.Filled.Settings),
    )
    NavigationDrawer(
        drawerContent = {
            Column(Modifier.fillMaxHeight().padding(vertical = 24.dp, horizontal = 8.dp)) {
                NavigationDrawerItem(
                    selected = false,
                    onClick = { nav.resetTo(ProfilePickerRoute) },
                    leadingContent = { AvatarTile(profile?.avatarId ?: 0, Modifier.size(28.dp), size = 28.dp) },
                ) { Text(profile?.name ?: stringResource(R.string.app_nav_profiles)) }
                entries.forEach { e ->
                    NavigationDrawerItem(
                        selected = current == e.key,
                        onClick = { nav.navigateTopLevel(e.key) },
                        leadingContent = { e.icon?.let { Icon(it, null) } },
                    ) { Text(stringResource(e.label)) }
                }
            }
        },
    ) { content() }
}
