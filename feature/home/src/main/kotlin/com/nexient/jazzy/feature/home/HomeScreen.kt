package com.nexient.jazzy.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.nexient.jazzy.core.data.catalog.HomeTab
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.core.model.TitleRef
import com.nexient.jazzy.feature.home.ui.HomePhoneScreen
import com.nexient.jazzy.feature.home.ui.HomeTvScreen

/** Navigation callbacks out of Home; the app module wires them to routes. */
data class HomeActions(
    val openTitle: (ref: TitleRef, season: Int?, episode: Int?, focusSources: Boolean) -> Unit,
    val openProfiles: () -> Unit,
    val openSearch: () -> Unit,
    /** Opens the full page of a rail that has one (an Originals network), by rail key. */
    val openNetwork: (railKey: String) -> Unit,
)

@Composable
fun HomeRoute(actions: HomeActions, tab: HomeTab? = null, viewModel: HomeViewModel = hiltViewModel(key = "home_${tab ?: "all"}")) {
    LaunchedEffect(tab) { tab?.let(viewModel::selectTab) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (JazzyTheme.isTv) HomeTvScreen(state, viewModel, actions) else HomePhoneScreen(state, viewModel, actions)
}
