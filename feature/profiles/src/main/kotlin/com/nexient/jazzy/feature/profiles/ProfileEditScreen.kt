package com.nexient.jazzy.feature.profiles

import com.nexient.jazzy.core.designsystem.text.labelRes
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexient.jazzy.core.designsystem.component.ChoiceDialog
import com.nexient.jazzy.core.designsystem.component.ConfirmDialog
import com.nexient.jazzy.core.designsystem.component.FullScreenLoading
import com.nexient.jazzy.core.designsystem.component.PinDialog
import com.nexient.jazzy.core.designsystem.component.PinSetupDialog
import com.nexient.jazzy.core.designsystem.text.asString
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.feature.profiles.ui.AvatarPickerDialog
import com.nexient.jazzy.feature.profiles.ui.GenrePickerStep
import com.nexient.jazzy.feature.profiles.ui.ProfileEditPhoneScreen
import com.nexient.jazzy.feature.profiles.ui.ProfileEditTvScreen

internal enum class EditDialog { AVATAR, MATURITY, LANGUAGE, SUBTITLES, DELETE }

/**
 * Create ([profileId] == null) or edit a profile (PR-2…PR-7). New profiles get the optional
 * "Pick 3 or more genres you like" step. With [firstRun] the new profile is also made active.
 * [onDone] runs after a save or delete; [onCancel] when the user backs out.
 */
@Composable
fun ProfileEditRoute(
    profileId: Long?,
    firstRun: Boolean,
    onDone: () -> Unit,
    onCancel: () -> Unit,
) {
    val viewModel = hiltViewModel<ProfileEditViewModel, ProfileEditViewModel.Factory>(
        key = "profile-edit:$profileId:$firstRun",
    ) { factory -> factory.create(profileId, firstRun) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val done by rememberUpdatedState(onDone)
    var dialog by rememberSaveable { mutableStateOf<EditDialog?>(null) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { if (it == EditEvent.Done) done() }
    }
    val message = state.message?.asString()
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }
    BackHandler(enabled = state.step == EditStep.GENRES) { viewModel.backToForm() }

    Box(Modifier.fillMaxSize().background(JazzyColors.Background)) {
        when {
            state.loading -> FullScreenLoading()
            state.step == EditStep.GENRES -> GenrePickerStep(state, viewModel)
            JazzyTheme.isTv -> ProfileEditTvScreen(state, viewModel, onCancel = onCancel, openDialog = { dialog = it })
            else -> ProfileEditPhoneScreen(state, viewModel, onCancel = onCancel, openDialog = { dialog = it })
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars))
    }

    val off = stringResource(R.string.profiles_common_off)
    when (dialog) {
        EditDialog.AVATAR -> AvatarPickerDialog(
            selected = state.avatarId,
            onSelect = { viewModel.setAvatar(it); dialog = null },
            onDismiss = { dialog = null },
        )
        EditDialog.MATURITY -> {
            val maturityLabels = state.maturityOptions.associateWith { stringResource(it.labelRes) }
            ChoiceDialog(
            title = stringResource(R.string.profiles_edit_maturity_title),
            options = state.maturityOptions,
            selected = state.maturity,
            label = { maturityLabels.getValue(it) },
            onSelect = { viewModel.setMaturity(it); dialog = null },
            onDismiss = { dialog = null },
        )
        }
        EditDialog.LANGUAGE -> ChoiceDialog(
            title = stringResource(R.string.profiles_edit_content_language_title),
            options = ContentLanguages.withCurrent(state.language),
            selected = state.language,
            label = { languageLabel(it, off) },
            onSelect = { viewModel.setLanguage(it); dialog = null },
            onDismiss = { dialog = null },
        )
        EditDialog.SUBTITLES -> ChoiceDialog(
            title = stringResource(R.string.profiles_edit_subtitle_language_title),
            options = SubtitleLanguages.withCurrent(state.subtitleLanguage),
            selected = state.subtitleLanguage,
            label = { languageLabel(it, off) },
            onSelect = { viewModel.setSubtitleLanguage(it); dialog = null },
            onDismiss = { dialog = null },
        )
        EditDialog.DELETE -> ConfirmDialog(
            title = stringResource(
                R.string.profiles_edit_delete_dialog_title,
                state.trimmedName.ifEmpty { stringResource(R.string.profiles_edit_delete_dialog_this_profile) },
            ),
            message = stringResource(R.string.profiles_edit_delete_dialog_message),
            confirmLabel = stringResource(R.string.profiles_edit_delete_title),
            onConfirm = { dialog = null; viewModel.delete() },
            onDismiss = { dialog = null },
        )
        null -> Unit
    }

    when (val flow = state.pinFlow) {
        is PinFlow.VerifyCurrent -> PinDialog(
            title = stringResource(R.string.profiles_edit_pin_verify_title),
            onDismiss = viewModel::dismissPinFlow,
            onPinEntered = viewModel::verifyCurrentPin,
            error = flow.error?.let { stringResource(it) },
        )
        PinFlow.SetNew -> PinSetupDialog(
            title = stringResource(R.string.profiles_edit_pin_new_title),
            onDismiss = viewModel::dismissPinFlow,
            onPinSet = viewModel::setNewPin,
        )
        null -> Unit
    }
}
