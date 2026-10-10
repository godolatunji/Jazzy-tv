package com.nexient.jazzy.feature.profiles

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexient.jazzy.core.data.profile.ProfileRepository
import com.nexient.jazzy.core.data.settings.SettingsRepository
import com.nexient.jazzy.core.model.MAX_PROFILES
import com.nexient.jazzy.core.model.Profile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Something the parental PIN guards on the picker (PR-5). */
sealed interface GuardedAction {
    data object Manage : GuardedAction
    data object Add : GuardedAction
    data class Edit(val profileId: Long) : GuardedAction
    /** Leaving the active Kids profile for [target]. */
    data class LeaveKids(val target: Profile) : GuardedAction
}

sealed interface PinPrompt {
    @get:StringRes val error: Int?

    data class ProfilePin(val profile: Profile, @StringRes override val error: Int? = null) : PinPrompt
    data class Parental(val action: GuardedAction, @StringRes override val error: Int? = null) : PinPrompt
}

sealed interface PickerEvent {
    data object ProfileSelected : PickerEvent
    data object AddProfile : PickerEvent
    data class EditProfile(val profileId: Long) : PickerEvent
}

data class ProfilePickerUiState(
    val loading: Boolean = true,
    val profiles: List<Profile> = emptyList(),
    val activeProfile: Profile? = null,
    val rememberProfile: Boolean = false,
    val hasParentalPin: Boolean = false,
    val editMode: Boolean = false,
    val prompt: PinPrompt? = null,
) {
    val canAdd: Boolean get() = profiles.size < MAX_PROFILES
}

@HiltViewModel
class ProfilePickerViewModel @Inject constructor(
    private val repository: ProfileRepository,
    settings: SettingsRepository,
) : ViewModel() {
    private data class Local(val editMode: Boolean = false, val prompt: PinPrompt? = null, val hasParentalPin: Boolean = false)

    private val local = MutableStateFlow(Local())
    private val _events = Channel<PickerEvent>(Channel.BUFFERED)
    val events: Flow<PickerEvent> = _events.receiveAsFlow()

    /** The parental PIN was entered on this visit; don't ask again until a profile is chosen. */
    private var parentalUnlocked = false

    val state: StateFlow<ProfilePickerUiState> = combine(
        repository.profiles,
        repository.activeProfile,
        settings.appSettings.map { it.rememberProfile },
        local,
    ) { profiles, active, remember, l ->
        ProfilePickerUiState(
            loading = false,
            profiles = profiles,
            activeProfile = active,
            rememberProfile = remember,
            hasParentalPin = l.hasParentalPin,
            editMode = l.editMode,
            prompt = l.prompt,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfilePickerUiState())

    init {
        refresh()
    }

    /** Re-reads whether a parental PIN exists (it can change in Settings while this screen is in the back stack). */
    fun refresh() = viewModelScope.launch {
        val has = repository.hasParentalPin()
        local.update { it.copy(hasParentalPin = has) }
    }

    fun onProfileClick(profile: Profile) {
        if (local.value.editMode) guard(GuardedAction.Edit(profile.id)) else requestSelect(profile)
    }

    fun onAddClick() = guard(GuardedAction.Add)

    fun toggleManage() {
        if (local.value.editMode) local.update { it.copy(editMode = false) } else guard(GuardedAction.Manage)
    }

    fun setRememberProfile(remember: Boolean) = viewModelScope.launch { repository.setRememberProfile(remember) }

    fun dismissPrompt() = local.update { it.copy(prompt = null) }

    fun submitPin(pin: String) = viewModelScope.launch {
        when (val prompt = local.value.prompt) {
            is PinPrompt.ProfilePin -> {
                if (repository.verifyPin(prompt.profile.id, pin)) {
                    local.update { it.copy(prompt = null) }
                    select(prompt.profile)
                } else {
                    local.update { it.copy(prompt = prompt.copy(error = R.string.profiles_pin_incorrect)) }
                }
            }
            is PinPrompt.Parental -> {
                if (repository.verifyParentalPin(pin)) {
                    parentalUnlocked = true
                    local.update { it.copy(prompt = null) }
                    perform(prompt.action)
                } else {
                    local.update { it.copy(prompt = prompt.copy(error = R.string.profiles_picker_parental_pin_incorrect)) }
                }
            }
            null -> Unit
        }
    }

    private fun requestSelect(profile: Profile) {
        val active = state.value.activeProfile
        // PR-5: leaving a Kids profile needs the parental PIN.
        if (active != null && active.isKids && active.id != profile.id) {
            guard(GuardedAction.LeaveKids(profile))
        } else {
            askProfilePinOrSelect(profile)
        }
    }

    private fun askProfilePinOrSelect(profile: Profile) {
        if (profile.hasPin) local.update { it.copy(prompt = PinPrompt.ProfilePin(profile)) } else viewModelScope.launch { select(profile) }
    }

    private fun guard(action: GuardedAction) = viewModelScope.launch {
        // Read the PIN state fresh so a quick tap can't slip past before refresh() lands.
        val hasPin = repository.hasParentalPin()
        local.update { it.copy(hasParentalPin = hasPin) }
        if (hasPin && !parentalUnlocked) {
            local.update { it.copy(prompt = PinPrompt.Parental(action)) }
        } else {
            perform(action)
        }
    }

    private suspend fun perform(action: GuardedAction) {
        when (action) {
            GuardedAction.Manage -> local.update { it.copy(editMode = true) }
            GuardedAction.Add -> _events.send(PickerEvent.AddProfile)
            is GuardedAction.Edit -> _events.send(PickerEvent.EditProfile(action.profileId))
            is GuardedAction.LeaveKids -> askProfilePinOrSelect(action.target)
        }
    }

    private suspend fun select(profile: Profile) {
        repository.select(profile.id)
        parentalUnlocked = false
        local.update { it.copy(editMode = false) }
        _events.send(PickerEvent.ProfileSelected)
    }
}
