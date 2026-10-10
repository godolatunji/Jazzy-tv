package com.nexient.jazzy.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexient.jazzy.core.designsystem.component.ChoiceDialog
import com.nexient.jazzy.core.designsystem.component.ConfirmDialog
import com.nexient.jazzy.core.designsystem.component.PinDialog
import com.nexient.jazzy.core.designsystem.component.PinSetupDialog
import com.nexient.jazzy.core.designsystem.component.ToggleChipGroup
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.core.model.SourceSort
import com.nexient.jazzy.core.recommendations.UnifiedGenre
import com.nexient.jazzy.feature.settings.ui.CacheCapOptions
import com.nexient.jazzy.feature.settings.ui.MaxConnectionOptions
import com.nexient.jazzy.feature.settings.ui.SettingsDialog
import com.nexient.jazzy.feature.settings.ui.SettingsNav
import com.nexient.jazzy.feature.settings.ui.SettingsPhoneScreen
import com.nexient.jazzy.feature.settings.ui.SettingsTvScreen
import com.nexient.jazzy.feature.settings.ui.UploadLimitOptions
import com.nexient.jazzy.feature.settings.ui.capLabel
import com.nexient.jazzy.feature.settings.ui.sortLabel
import com.nexient.jazzy.feature.settings.ui.uploadLabel

/**
 * App settings (spec §5.11). [onOpenSites] opens the per-site settings (SRC-5), [onOpenAbout] the
 * About screen and [onManageProfiles] the profile picker in manage mode.
 */
@Composable
fun SettingsRoute(
    onOpenSites: () -> Unit,
    onOpenAbout: () -> Unit,
    onManageProfiles: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var dialog by rememberSaveable { mutableStateOf<SettingsDialog?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val nav = remember(onOpenSites, onOpenAbout, onManageProfiles) {
        SettingsNav(onOpenSites, onOpenAbout, onManageProfiles) { dialog = it }
    }

    LaunchedEffect(viewModel) { viewModel.refresh() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Box(Modifier.fillMaxSize().background(JazzyColors.Background)) {
        if (JazzyTheme.isTv) SettingsTvScreen(state, viewModel, nav) else SettingsPhoneScreen(state, viewModel, nav)
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars))
    }

    SettingsDialogs(dialog, state, viewModel, onDismiss = { dialog = null })

    state.pinFlow?.let { flow ->
        when (flow) {
            SettingsPinFlow.NewParental -> PinSetupDialog(
                title = "Choose a 4-digit parental PIN",
                onDismiss = viewModel::dismissPin,
                onPinSet = viewModel::submitPin,
            )
            is SettingsPinFlow.VerifyParental -> PinDialog(
                title = "Enter the current parental PIN",
                onDismiss = viewModel::dismissPin,
                onPinEntered = viewModel::submitPin,
                error = flow.error,
            )
            is SettingsPinFlow.ProfilePinForHistory -> PinDialog(
                title = "Enter the PIN for ${state.profile?.name ?: "this profile"}",
                onDismiss = viewModel::dismissPin,
                onPinEntered = viewModel::submitPin,
                error = flow.error,
            )
        }
    }
}

@Composable
private fun SettingsDialogs(dialog: SettingsDialog?, state: SettingsUiState, viewModel: SettingsViewModel, onDismiss: () -> Unit) {
    val engine = state.settings.engine
    val profileName = state.profile?.name ?: "this profile"
    when (dialog) {
        SettingsDialog.SORT -> ChoiceDialog(
            title = "Default Sources sort",
            options = SourceSort.entries,
            selected = state.settings.defaultSourceSort,
            label = ::sortLabel,
            onSelect = { viewModel.setSourceSort(it); onDismiss() },
            onDismiss = onDismiss,
        )
        SettingsDialog.UPLOAD -> ChoiceDialog(
            title = "Upload limit",
            options = (UploadLimitOptions + engine.uploadLimitBps).distinct().sorted(),
            selected = engine.uploadLimitBps,
            label = ::uploadLabel,
            onSelect = { viewModel.setUploadLimit(it); onDismiss() },
            onDismiss = onDismiss,
        )
        SettingsDialog.CONNECTIONS -> ChoiceDialog(
            title = "Max connections",
            options = (MaxConnectionOptions + engine.maxConnections).distinct().sorted(),
            selected = engine.maxConnections,
            label = { it.toString() },
            onSelect = { viewModel.setMaxConnections(it); onDismiss() },
            onDismiss = onDismiss,
        )
        SettingsDialog.CACHE_CAP -> ChoiceDialog(
            title = "Torrent cache size",
            options = (CacheCapOptions + engine.cacheCapBytes).distinct().sorted(),
            selected = engine.cacheCapBytes,
            label = ::capLabel,
            onSelect = { viewModel.setCacheCap(it); onDismiss() },
            onDismiss = onDismiss,
        )
        SettingsDialog.GENRES -> GenresDialog(
            initial = state.favouriteGenres,
            profileName = profileName,
            onSave = { viewModel.setFavouriteGenres(it); onDismiss() },
            onDismiss = onDismiss,
        )
        SettingsDialog.CONFIRM_CLEAR_HISTORY -> ConfirmDialog(
            title = "Clear play history?",
            message = "This erases every playback session for $profileName and resets their genre affinity, " +
                "so recommendations start over. Resume points, My List and likes are kept.",
            confirmLabel = "Clear history",
            onConfirm = { onDismiss(); viewModel.requestClearHistory() },
            onDismiss = onDismiss,
        )
        SettingsDialog.CONFIRM_CLEAR_SEARCH -> ConfirmDialog(
            title = "Clear search history?",
            message = "Recent searches for $profileName will be removed.",
            confirmLabel = "Clear",
            onConfirm = { onDismiss(); viewModel.clearSearchHistory() },
            onDismiss = onDismiss,
        )
        SettingsDialog.CONFIRM_RESET_RECS -> ConfirmDialog(
            title = "Reset recommendations?",
            message = "Genre affinity and saved recommendation rails for $profileName are cleared. " +
                "They rebuild from history, likes and My List.",
            confirmLabel = "Reset",
            onConfirm = { onDismiss(); viewModel.resetRecommendations() },
            onDismiss = onDismiss,
        )
        SettingsDialog.CONFIRM_CLEAR_TORRENT -> ConfirmDialog(
            title = "Clear torrent cache?",
            message = "Downloaded pieces are deleted. Anything playing now may need to buffer again.",
            confirmLabel = "Clear cache",
            onConfirm = { onDismiss(); viewModel.clearTorrentCache() },
            onDismiss = onDismiss,
        )
        SettingsDialog.CONFIRM_CLEAR_TMDB -> ConfirmDialog(
            title = "Clear TMDB cache?",
            message = "Cached titles, rails, searches, source results and recommendations are removed and reload " +
                "when needed. History, My List, likes and resume points are kept.",
            confirmLabel = "Clear cache",
            onConfirm = { onDismiss(); viewModel.clearTmdbCache() },
            onDismiss = onDismiss,
        )
        null -> Unit
    }
}

@Composable
private fun GenresDialog(initial: Set<String>, profileName: String, onSave: (Set<String>) -> Unit, onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf(initial) }
    val firstChip = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstChip.requestFocus() } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Favourite genres") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Pick 3 or more genres $profileName likes. They seed recommendations until there's enough watch history.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = JazzyColors.TextSecondary,
                )
                ToggleChipGroup(
                    options = UnifiedGenre.pickable.map { it.id to it.label },
                    selected = selected,
                    onToggle = { id -> selected = if (id in selected) selected - id else selected + id },
                    firstChipFocusRequester = firstChip,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(selected) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = JazzyColors.SurfaceRaised,
    )
}
