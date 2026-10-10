package com.nexient.jazzy.feature.profiles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexient.jazzy.core.designsystem.component.PinDialog
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.feature.profiles.ui.ProfilePickerPhoneScreen
import com.nexient.jazzy.feature.profiles.ui.ProfilePickerTvScreen

/**
 * "Who's watching?" (spec §4, PR-1…PR-8). Selecting a profile makes it active and calls
 * [onProfileSelected]; [onAddProfile] / [onEditProfile] open the profile editor.
 */
@Composable
fun ProfilePickerRoute(
    onProfileSelected: () -> Unit,
    onAddProfile: () -> Unit,
    onEditProfile: (Long) -> Unit,
    viewModel: ProfilePickerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selected by rememberUpdatedState(onProfileSelected)
    val add by rememberUpdatedState(onAddProfile)
    val edit by rememberUpdatedState(onEditProfile)

    LaunchedEffect(viewModel) {
        viewModel.refresh()
        viewModel.events.collect { event ->
            when (event) {
                PickerEvent.ProfileSelected -> selected()
                PickerEvent.AddProfile -> add()
                is PickerEvent.EditProfile -> edit(event.profileId)
            }
        }
    }

    if (JazzyTheme.isTv) ProfilePickerTvScreen(state, viewModel) else ProfilePickerPhoneScreen(state, viewModel)

    state.prompt?.let { prompt ->
        PinDialog(
            title = when (prompt) {
                is PinPrompt.ProfilePin -> stringResource(R.string.profiles_picker_pin_profile, prompt.profile.name)
                is PinPrompt.Parental -> when (prompt.action) {
                    is GuardedAction.LeaveKids -> stringResource(R.string.profiles_picker_pin_leave_kids)
                    else -> stringResource(R.string.profiles_picker_pin_parental)
                }
            },
            onDismiss = viewModel::dismissPrompt,
            onPinEntered = viewModel::submitPin,
            error = prompt.error?.let { stringResource(it) },
        )
    }
}
