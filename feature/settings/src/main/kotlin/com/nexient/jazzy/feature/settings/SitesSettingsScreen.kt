package com.nexient.jazzy.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexient.jazzy.core.designsystem.component.FullScreenLoading
import com.nexient.jazzy.core.designsystem.component.MessageState
import com.nexient.jazzy.core.designsystem.text.asString
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.feature.settings.ui.ConnectionDialog
import com.nexient.jazzy.feature.settings.ui.SitesPhoneScreen
import com.nexient.jazzy.feature.settings.ui.SitesTvScreen

/**
 * Settings → Sources → Torrent sites (SRC-2…SRC-6): enable/disable, health, Test, mirror URL and
 * login/cookies for private trackers. Not available on Kids profiles (PR-4).
 */
@Composable
fun SitesSettingsRoute(
    onBack: () -> Unit,
    viewModel: SitesSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }

    val message = state.message?.asString()
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Box(Modifier.fillMaxSize().background(JazzyColors.Background)) {
        when {
            state.loading -> FullScreenLoading()
            state.isKids -> MessageState(
                title = stringResource(R.string.settings_sites_kids_title),
                message = stringResource(R.string.settings_sites_kids_message),
                icon = Icons.Filled.ChildCare,
                actionLabel = stringResource(R.string.settings_common_back),
                onAction = onBack,
                modifier = Modifier.align(Alignment.Center),
            )
            JazzyTheme.isTv -> SitesTvScreen(state, viewModel, onEditConnection = { editing = it })
            else -> SitesPhoneScreen(state, viewModel, onBack = onBack, onEditConnection = { editing = it })
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars))
    }

    editing?.let { id ->
        val item = state.items.firstOrNull { it.info.id == id }
        if (item == null) {
            editing = null
        } else {
            ConnectionDialog(
                item = item,
                onSave = { url, user, pass, cookies ->
                    viewModel.saveConnection(id, url, user, pass, cookies).also { if (it == null) editing = null }
                },
                onDismiss = { editing = null },
            )
        }
    }
}
