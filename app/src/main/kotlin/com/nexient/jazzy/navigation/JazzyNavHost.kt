package com.nexient.jazzy.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.nexient.jazzy.AppViewModel
import com.nexient.jazzy.core.data.catalog.HomeTab
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.core.model.TitleRef
import com.nexient.jazzy.feature.details.DetailsActions
import com.nexient.jazzy.feature.details.DetailsArgs
import com.nexient.jazzy.feature.details.DetailsRoute as DetailsScreenRoute
import com.nexient.jazzy.feature.discover.DiscoverRoute as DiscoverScreenRoute
import com.nexient.jazzy.feature.home.HomeActions
import com.nexient.jazzy.feature.home.HomeRoute as HomeScreenRoute
import com.nexient.jazzy.feature.library.LibraryRoute as LibraryScreenRoute
import com.nexient.jazzy.feature.player.PlayerRoute as PlayerScreenRoute
import com.nexient.jazzy.feature.profiles.ProfileEditRoute as ProfileEditScreenRoute
import com.nexient.jazzy.feature.profiles.ProfilePickerRoute as ProfilePickerScreenRoute
import com.nexient.jazzy.feature.search.PersonRoute as PersonScreenRoute
import com.nexient.jazzy.feature.search.SearchRoute as SearchScreenRoute
import com.nexient.jazzy.feature.settings.AboutRoute as AboutScreenRoute
import com.nexient.jazzy.feature.settings.SettingsRoute as SettingsScreenRoute
import com.nexient.jazzy.feature.settings.SitesSettingsRoute as SitesSettingsScreenRoute

@Composable
fun JazzyNavHost(startKey: NavKey, appViewModel: AppViewModel, pendingLink: DetailsRoute?, onLinkConsumed: () -> Unit) {
    val backStack = rememberNavBackStack(startKey)
    val nav = remember(backStack) { Navigator(backStack) }
    val current = backStack.lastOrNull()
    val profile by appViewModel.activeProfile.collectAsStateWithLifecycle()
    var profileMenu by remember { mutableStateOf(false) }

    // Open deep links (Watch Next) once a profile is in use.
    androidx.compose.runtime.LaunchedEffect(pendingLink, current, profile) {
        val link = pendingLink ?: return@LaunchedEffect
        if (profile != null && current != null && current !is ProfilePickerRoute && current !is ProfileEditRoute) {
            nav.push(link)
            onLinkConsumed()
        }
    }

    val content: @Composable (Modifier) -> Unit = { modifier ->
        NavDisplay(
            backStack = backStack,
            modifier = modifier,
            onBack = { nav.back() },
            entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()),
            entryProvider = entryProvider {
                entry<ProfilePickerRoute> {
                    ProfilePickerScreenRoute(
                        onProfileSelected = { nav.resetTo(HomeRoute) },
                        onAddProfile = { nav.push(ProfileEditRoute()) },
                        onEditProfile = { id -> nav.push(ProfileEditRoute(profileId = id)) },
                    )
                }
                entry<ProfileEditRoute> { key ->
                    ProfileEditScreenRoute(
                        profileId = key.profileId,
                        firstRun = key.firstRun,
                        onDone = { if (key.firstRun) nav.resetTo(HomeRoute) else nav.back() },
                        onCancel = { nav.back() },
                    )
                }
                entry<HomeRoute> { HomeScreenRoute(homeActions(nav) { profileMenu = true }) }
                entry<HomeTabRoute> { key -> HomeScreenRoute(homeActions(nav) { profileMenu = true }, tab = HomeTab.valueOf(key.tab)) }
                entry<SearchRoute> {
                    SearchScreenRoute(
                        onOpenTitle = { ref -> nav.push(DetailsRoute(ref.tmdbId, ref.mediaType)) },
                        onOpenPerson = { id, name -> nav.push(PersonRoute(id, name)) },
                    )
                }
                entry<PersonRoute> { key ->
                    PersonScreenRoute(key.personId, key.name, onOpenTitle = { ref -> nav.push(DetailsRoute(ref.tmdbId, ref.mediaType)) }, onBack = { nav.back() })
                }
                entry<DiscoverRoute> { DiscoverScreenRoute(onOpenTitle = { ref -> nav.push(DetailsRoute(ref.tmdbId, ref.mediaType)) }) }
                entry<LibraryRoute> { key ->
                    LibraryScreenRoute(initialTab = key.tab, onOpenTitle = { ref, season, episode, focus -> nav.push(DetailsRoute(ref.tmdbId, ref.mediaType, season, episode, focus)) })
                }
                entry<DetailsRoute> { key ->
                    DetailsScreenRoute(
                        DetailsArgs(TitleRef(key.tmdbId, key.mediaType), key.season, key.episode, key.focusSources),
                        DetailsActions(
                            back = { nav.back() },
                            openTitle = { ref -> nav.push(DetailsRoute(ref.tmdbId, ref.mediaType)) },
                            play = { request -> nav.push(PlayerRoute(request)) },
                            openSitesSettings = { nav.push(SitesSettingsRoute) },
                        ),
                    )
                }
                entry<PlayerRoute> { key -> PlayerScreenRoute(key.request, onBack = { nav.back() }) }
                entry<SettingsRoute> {
                    SettingsScreenRoute(
                        onOpenSites = { nav.push(SitesSettingsRoute) },
                        onOpenAbout = { nav.push(AboutRoute) },
                        onManageProfiles = { nav.push(ProfilePickerRoute) },
                    )
                }
                entry<SitesSettingsRoute> { SitesSettingsScreenRoute(onBack = { nav.back() }) }
                entry<AboutRoute> { AboutScreenRoute(onBack = { nav.back() }) }
            },
        )
    }

    if (JazzyTheme.isTv) {
        TvShell(current, nav, profile) { content(Modifier.fillMaxSize()) }
    } else {
        PhoneShell(current, nav, content)
    }

    if (profileMenu) ProfileMenu(onDismiss = { profileMenu = false }, nav = nav)
}

private fun homeActions(nav: Navigator, openProfileMenu: () -> Unit) = HomeActions(
    openTitle = { ref, season, episode, focus -> nav.push(DetailsRoute(ref.tmdbId, ref.mediaType, season, episode, focus)) },
    openProfiles = openProfileMenu,
    openSearch = { nav.navigateTopLevel(SearchRoute) },
)

/** Thin wrapper over the Navigation 3 back stack. */
class Navigator(private val backStack: NavBackStack<NavKey>) {
    fun push(key: NavKey) {
        backStack.add(key)
    }

    fun back() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    fun resetTo(key: NavKey) {
        backStack.clear()
        backStack.add(key)
    }

    /** Top-level destinations replace each other above Home instead of stacking. */
    fun navigateTopLevel(key: NavKey) {
        if (backStack.lastOrNull() == key) return
        while (backStack.size > 1 && backStack.last().isTopLevel()) backStack.removeAt(backStack.lastIndex)
        if (backStack.lastOrNull() != HomeRoute && backStack.none { it == HomeRoute }) backStack.add(HomeRoute)
        if (key != HomeRoute) backStack.add(key)
    }
}

@Composable
private fun PhoneShell(current: NavKey?, nav: Navigator, content: @Composable (Modifier) -> Unit) {
    val showBar = current?.isTopLevel() == true
    Scaffold(
        containerColor = JazzyColors.Background,
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = JazzyColors.Surface) {
                    listOf(
                        Triple<NavKey, String, androidx.compose.ui.graphics.vector.ImageVector>(HomeRoute, "Home", Icons.Filled.Home),
                        Triple(SearchRoute, "Search", Icons.Filled.Search),
                        Triple(DiscoverRoute, "Discover", Icons.Filled.Explore),
                        Triple(LibraryRoute(), "Library", Icons.Filled.VideoLibrary),
                    ).forEach { (key, label, icon) ->
                        val selected = current == key || (key is LibraryRoute && current is LibraryRoute)
                        NavigationBarItem(selected = selected, onClick = { nav.navigateTopLevel(key) }, icon = { Icon(icon, null) }, label = { Text(label) })
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) { content(Modifier.fillMaxSize()) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileMenu(onDismiss: () -> Unit, nav: Navigator) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = JazzyColors.Surface) {
        Column(Modifier.padding(bottom = 24.dp)) {
            ListItem(headlineContent = { Text("Switch profile") }, modifier = Modifier.clickable { onDismiss(); nav.resetTo(ProfilePickerRoute) })
            ListItem(headlineContent = { Text("Settings") }, modifier = Modifier.clickable { onDismiss(); nav.navigateTopLevel(SettingsRoute) })
            ListItem(headlineContent = { Text("My List · Liked · History") }, modifier = Modifier.clickable { onDismiss(); nav.navigateTopLevel(LibraryRoute()) })
        }
    }
}
