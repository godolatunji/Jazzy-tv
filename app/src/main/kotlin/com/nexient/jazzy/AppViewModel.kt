package com.nexient.jazzy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexient.jazzy.core.data.profile.ProfileRepository
import com.nexient.jazzy.core.data.settings.SettingKeys
import com.nexient.jazzy.core.data.settings.SettingsRepository
import com.nexient.jazzy.core.model.Profile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class StartDestination { FIRST_PROFILE, PICKER, HOME }

data class LaunchState(val start: StartDestination? = null, val disclaimerAccepted: Boolean = true)

/** Decides where the app opens (PR-1) and whether the first-run disclaimer is needed (E18-04). */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val profiles: ProfileRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val _launch = MutableStateFlow(LaunchState())
    val launch: StateFlow<LaunchState> = _launch.asStateFlow()

    val activeProfile: StateFlow<Profile?> = profiles.activeProfile.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            val all = profiles.profiles.first()
            val app = settings.appSettings.first()
            val start = when {
                all.isEmpty() -> StartDestination.FIRST_PROFILE
                app.rememberProfile && app.activeProfileId != null && all.any { it.id == app.activeProfileId } -> StartDestination.HOME
                else -> StartDestination.PICKER
            }
            _launch.value = LaunchState(start, app.disclaimerAccepted)
        }
    }

    fun acceptDisclaimer() {
        viewModelScope.launch { settings.put(SettingKeys.DISCLAIMER, true) }
        _launch.value = _launch.value.copy(disclaimerAccepted = true)
    }
}
