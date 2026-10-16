package com.nexient.jazzy.navigation

import androidx.navigation3.runtime.NavKey
import com.nexient.jazzy.core.model.MediaType
import com.nexient.jazzy.core.model.PlaybackRequest
import kotlinx.serialization.Serializable

@Serializable data object ProfilePickerRoute : NavKey
@Serializable data class ProfileEditRoute(val profileId: Long? = null, val firstRun: Boolean = false) : NavKey
@Serializable data object HomeRoute : NavKey
@Serializable data class HomeTabRoute(val tab: String) : NavKey
@Serializable data object SearchRoute : NavKey
@Serializable data object DiscoverRoute : NavKey
@Serializable data class LibraryRoute(val tab: Int = 0) : NavKey
@Serializable
data class DetailsRoute(
    val tmdbId: Int,
    val mediaType: MediaType,
    val season: Int? = null,
    val episode: Int? = null,
    val focusSources: Boolean = false,
) : NavKey
/** A network's page (e.g. Netflix Originals), identified by its Home rail key. */
@Serializable data class NetworkRoute(val railKey: String) : NavKey
@Serializable data class PersonRoute(val personId: Int, val name: String) : NavKey
@Serializable data class PlayerRoute(val request: PlaybackRequest) : NavKey
@Serializable data object SettingsRoute : NavKey
@Serializable data object SitesSettingsRoute : NavKey
@Serializable data object AboutRoute : NavKey

/** Destinations that show the bottom bar (phone) or the drawer (TV). */
fun NavKey.isTopLevel(): Boolean = this is HomeRoute || this is HomeTabRoute || this is SearchRoute ||
    this is DiscoverRoute || this is LibraryRoute || this is SettingsRoute
