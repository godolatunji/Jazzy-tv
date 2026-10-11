package com.nexient.jazzy.feature.settings

import com.nexient.jazzy.core.designsystem.component.dpadFocusEscape
import com.nexient.jazzy.core.designsystem.text.genreLabelRes
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexient.jazzy.core.designsystem.component.ChoiceDialog
import com.nexient.jazzy.core.designsystem.component.ConfirmDialog
import com.nexient.jazzy.core.designsystem.component.PinDialog
import com.nexient.jazzy.core.designsystem.component.PinSetupDialog
import com.nexient.jazzy.core.designsystem.component.ToggleChipGroup
import com.nexient.jazzy.core.designsystem.text.asString
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
    val message = state.message?.asString()
    LaunchedEffect(message) {
        message?.let {
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
                title = stringResource(R.string.settings_pin_new_parental_title),
                onDismiss = viewModel::dismissPin,
                onPinSet = viewModel::submitPin,
            )
            is SettingsPinFlow.VerifyParental -> PinDialog(
                title = stringResource(R.string.settings_pin_verify_parental_title),
                onDismiss = viewModel::dismissPin,
                onPinEntered = viewModel::submitPin,
                error = flow.error?.let { stringResource(it) },
            )
            is SettingsPinFlow.ProfilePinForHistory -> PinDialog(
                title = stringResource(
                    R.string.settings_pin_profile_title,
                    state.profile?.name ?: stringResource(R.string.settings_common_this_profile),
                ),
                onDismiss = viewModel::dismissPin,
                onPinEntered = viewModel::submitPin,
                error = flow.error?.let { stringResource(it) },
            )
        }
    }
}

@Composable
private fun SettingsDialogs(dialog: SettingsDialog?, state: SettingsUiState, viewModel: SettingsViewModel, onDismiss: () -> Unit) {
    val engine = state.settings.engine
    val profileName = state.profile?.name ?: stringResource(R.string.settings_common_this_profile)
    val resources = LocalResources.current
    when (dialog) {
        SettingsDialog.SORT -> ChoiceDialog(
            title = stringResource(R.string.settings_playback_sort_title),
            options = SourceSort.entries,
            selected = state.settings.defaultSourceSort,
            label = { sortLabel(it, resources) },
            onSelect = { viewModel.setSourceSort(it); onDismiss() },
            onDismiss = onDismiss,
        )
        SettingsDialog.UPLOAD -> ChoiceDialog(
            title = stringResource(R.string.settings_network_upload_title),
            options = (UploadLimitOptions + engine.uploadLimitBps).distinct().sorted(),
            selected = engine.uploadLimitBps,
            label = { uploadLabel(it, resources) },
            onSelect = { viewModel.setUploadLimit(it); onDismiss() },
            onDismiss = onDismiss,
        )
        SettingsDialog.CONNECTIONS -> ChoiceDialog(
            title = stringResource(R.string.settings_network_connections_title),
            options = (MaxConnectionOptions + engine.maxConnections).distinct().sorted(),
            selected = engine.maxConnections,
            label = { it.toString() },
            onSelect = { viewModel.setMaxConnections(it); onDismiss() },
            onDismiss = onDismiss,
        )
        SettingsDialog.CACHE_CAP -> ChoiceDialog(
            title = stringResource(R.string.settings_network_cache_size_title),
            options = (CacheCapOptions + engine.cacheCapBytes).distinct().sorted(),
            selected = engine.cacheCapBytes,
            label = { capLabel(it, resources) },
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
            title = stringResource(R.string.settings_history_clear_dialog_title),
            message = stringResource(R.string.settings_history_clear_dialog_message, profileName),
            confirmLabel = stringResource(R.string.settings_history_clear_dialog_confirm),
            onConfirm = { onDismiss(); viewModel.requestClearHistory() },
            onDismiss = onDismiss,
        )
        SettingsDialog.CONFIRM_CLEAR_SEARCH -> ConfirmDialog(
            title = stringResource(R.string.settings_history_clear_search_dialog_title),
            message = stringResource(R.string.settings_history_clear_search_dialog_message, profileName),
            confirmLabel = stringResource(R.string.settings_history_clear_search_dialog_confirm),
            onConfirm = { onDismiss(); viewModel.clearSearchHistory() },
            onDismiss = onDismiss,
        )
        SettingsDialog.CONFIRM_RESET_RECS -> ConfirmDialog(
            title = stringResource(R.string.settings_history_reset_recs_dialog_title),
            message = stringResource(R.string.settings_history_reset_recs_dialog_message, profileName),
            confirmLabel = stringResource(R.string.settings_history_reset_recs_dialog_confirm),
            onConfirm = { onDismiss(); viewModel.resetRecommendations() },
            onDismiss = onDismiss,
        )
        SettingsDialog.CONFIRM_CLEAR_TORRENT -> ConfirmDialog(
            title = stringResource(R.string.settings_network_clear_torrent_dialog_title),
            message = stringResource(R.string.settings_network_clear_torrent_dialog_message),
            confirmLabel = stringResource(R.string.settings_common_clear_cache),
            onConfirm = { onDismiss(); viewModel.clearTorrentCache() },
            onDismiss = onDismiss,
        )
        SettingsDialog.CONFIRM_CLEAR_TMDB -> ConfirmDialog(
            title = stringResource(R.string.settings_local_clear_tmdb_dialog_title),
            message = stringResource(R.string.settings_local_clear_tmdb_dialog_message),
            confirmLabel = stringResource(R.string.settings_common_clear_cache),
            onConfirm = { onDismiss(); viewModel.clearTmdbCache() },
            onDismiss = onDismiss,
        )
        SettingsDialog.OPENSUBTITLES_LOGIN -> OpenSubtitlesLoginDialog(
            onLogin = { u, pw -> onDismiss(); viewModel.loginOpenSubtitles(u, pw) },
            onDismiss = onDismiss,
        )
        null -> Unit
    }
}

@Composable
private fun OpenSubtitlesLoginDialog(onLogin: (String, String) -> Unit, onDismiss: () -> Unit) {
    var user by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_subtitles_login_dialog_title)) },
        text = {
            androidx.compose.foundation.layout.Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.OutlinedTextField(user, { user = it }, label = { Text(stringResource(R.string.settings_common_username)) }, singleLine = true, modifier = Modifier.dpadFocusEscape(),)
                androidx.compose.material3.OutlinedTextField(
                    password, { password = it }, label = { Text(stringResource(R.string.settings_common_password)) }, singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), modifier = Modifier.dpadFocusEscape(),)
                Text(stringResource(R.string.settings_subtitles_login_dialog_note), style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { onLogin(user, password) }, enabled = user.isNotBlank() && password.isNotBlank()) { Text(stringResource(R.string.settings_subtitles_login)) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_common_cancel)) } },
    )
}

@Composable
private fun GenresDialog(initial: Set<String>, profileName: String, onSave: (Set<String>) -> Unit, onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf(initial) }
    val firstChip = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstChip.requestFocus() } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_profiles_genres_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.settings_genres_dialog_message, profileName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = JazzyColors.TextSecondary,
                )
                ToggleChipGroup(
                    options = UnifiedGenre.pickable.map { it.id to (genreLabelRes(it.id)?.let { r -> stringResource(r) } ?: it.label) },
                    selected = selected,
                    onToggle = { id -> selected = if (id in selected) selected - id else selected + id },
                    firstChipFocusRequester = firstChip,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(selected) }) { Text(stringResource(R.string.settings_common_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_common_cancel)) } },
        containerColor = JazzyColors.SurfaceRaised,
    )
}
