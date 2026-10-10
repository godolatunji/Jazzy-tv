package com.nexient.jazzy.feature.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexient.jazzy.core.data.profile.LastProfileDeletion
import com.nexient.jazzy.core.data.profile.ProfileDraft
import com.nexient.jazzy.core.data.profile.ProfileLimitReached
import com.nexient.jazzy.core.data.profile.ProfileRepository
import com.nexient.jazzy.core.data.profile.defaultLanguage
import com.nexient.jazzy.core.data.settings.SettingsRepository
import com.nexient.jazzy.core.model.AVATAR_COUNT
import com.nexient.jazzy.core.model.MaturityLevel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class EditStep { FORM, GENRES }

/** PIN changes on an existing profile need the current PIN first. */
sealed interface PinFlow {
    data class VerifyCurrent(val thenClear: Boolean, val error: String? = null) : PinFlow
    data object SetNew : PinFlow
}

data class ProfileEditUiState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val firstRun: Boolean = false,
    val step: EditStep = EditStep.FORM,
    val name: String = "",
    val avatarId: Int = 0,
    val isKids: Boolean = false,
    val maturity: MaturityLevel = MaturityLevel.ALL,
    val language: String = defaultLanguage(),
    val subtitleLanguage: String? = null,
    val audioLanguage: String? = null,
    val autoplayNext: Boolean = true,
    /** Saved PIN on an existing profile. */
    val hasPin: Boolean = false,
    /** PIN chosen for a profile that isn't created yet. */
    val pendingPin: String? = null,
    val genres: Set<String> = emptySet(),
    val profileCount: Int = 0,
    val saving: Boolean = false,
    val showNameError: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val pinFlow: PinFlow? = null,
) {
    val trimmedName: String get() = name.trim()
    val nameValid: Boolean get() = trimmedName.length in 1..MAX_NAME
    val pinEnabled: Boolean get() = hasPin || pendingPin != null
    val canDelete: Boolean get() = !isNew && profileCount > 1
    val maturityOptions: List<MaturityLevel>
        get() = if (isKids) MaturityLevel.entries.filter { it != MaturityLevel.ALL } else MaturityLevel.entries

    companion object {
        const val MAX_NAME = 20
        const val MIN_GENRES = 3
    }
}

sealed interface EditEvent {
    data object Done : EditEvent
}

@HiltViewModel(assistedFactory = ProfileEditViewModel.Factory::class)
class ProfileEditViewModel @AssistedInject constructor(
    @Assisted private val profileId: Long?,
    @Assisted private val firstRun: Boolean,
    private val repository: ProfileRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(profileId: Long?, firstRun: Boolean): ProfileEditViewModel
    }

    private val _state = MutableStateFlow(ProfileEditUiState(isNew = profileId == null, firstRun = firstRun))
    val state: StateFlow<ProfileEditUiState> = _state.asStateFlow()

    private val _events = Channel<EditEvent>(Channel.BUFFERED)
    val events: Flow<EditEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val all = repository.profiles.first()
        val existing = profileId?.let { repository.get(it) }
        if (existing != null) {
            val genres = repository.seedGenres(existing.id).first().toSet()
            _state.update {
                it.copy(
                    loading = false, isNew = false, name = existing.name, avatarId = existing.avatarId,
                    isKids = existing.isKids, maturity = existing.maturity, language = existing.language,
                    subtitleLanguage = existing.subtitleLanguage, audioLanguage = existing.audioLanguage,
                    autoplayNext = existing.autoplayNext, hasPin = existing.hasPin, genres = genres,
                    profileCount = all.size,
                )
            }
        } else {
            val used = all.map { it.avatarId }.toSet()
            val autoplay = settings.appSettings.first().autoplayNextDefault
            _state.update {
                it.copy(
                    loading = false, isNew = true,
                    avatarId = (0 until AVATAR_COUNT).firstOrNull { id -> id !in used } ?: 0,
                    autoplayNext = autoplay, profileCount = all.size,
                    error = if (profileId != null) "That profile no longer exists." else null,
                )
            }
        }
    }

    fun setName(name: String) = _state.update { it.copy(name = name.take(ProfileEditUiState.MAX_NAME), showNameError = false) }
    fun setAvatar(id: Int) = _state.update { it.copy(avatarId = id) }
    fun setLanguage(tag: String) = _state.update { it.copy(language = tag) }
    fun setSubtitleLanguage(code: String?) = _state.update { it.copy(subtitleLanguage = code) }
    fun setAutoplayNext(on: Boolean) = _state.update { it.copy(autoplayNext = on) }
    fun setMaturity(level: MaturityLevel) = _state.update { it.copy(maturity = level) }
    fun clearMessage() = _state.update { it.copy(message = null, error = null) }

    /** A Kids profile can't use the "All maturity ratings" ceiling (PR-4). */
    fun setKids(kids: Boolean) = _state.update {
        it.copy(isKids = kids, maturity = if (kids && it.maturity == MaturityLevel.ALL) MaturityLevel.OLDER_KIDS else it.maturity)
    }

    fun toggleGenre(id: String) = _state.update { s ->
        s.copy(genres = if (id in s.genres) s.genres - id else s.genres + id)
    }

    // --- Steps -----------------------------------------------------------------------------

    /** Form "Next"/"Save": new profiles go on to the optional genre step (PR-3). */
    fun onPrimary() {
        val s = _state.value
        if (!s.nameValid) {
            _state.update { it.copy(showNameError = true) }
            return
        }
        if (s.isNew) _state.update { it.copy(step = EditStep.GENRES) } else save()
    }

    private var genresBeforeEdit: Set<String> = emptySet()

    fun openGenres() {
        genresBeforeEdit = _state.value.genres
        _state.update { it.copy(step = EditStep.GENRES) }
    }

    /** Leaves the genre step; unsaved picks on an existing profile are discarded. */
    fun backToForm() = _state.update {
        it.copy(step = EditStep.FORM, genres = if (it.isNew) it.genres else genresBeforeEdit)
    }

    /** Genre step finished. [skip] drops any picks for a new profile. */
    fun finishGenres(skip: Boolean) {
        val s = _state.value
        if (s.isNew) {
            if (skip) _state.update { it.copy(genres = emptySet()) }
            save()
        } else {
            val id = profileId ?: return
            viewModelScope.launch {
                repository.setSeedGenres(id, s.genres.toList())
                _state.update { it.copy(step = EditStep.FORM, message = "Favourite genres saved") }
            }
        }
    }

    private fun save() {
        val s = _state.value
        if (s.saving) return
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val draft = ProfileDraft(
                name = s.trimmedName, avatarId = s.avatarId, isKids = s.isKids,
                maturity = if (s.isKids && s.maturity == MaturityLevel.ALL) MaturityLevel.OLDER_KIDS else s.maturity,
                language = s.language, subtitleLanguage = s.subtitleLanguage, audioLanguage = s.audioLanguage,
                autoplayNext = s.autoplayNext,
            )
            try {
                if (s.isNew) {
                    val id = repository.create(draft, s.genres.toList())
                    s.pendingPin?.let { repository.setPin(id, it) }
                    if (firstRun) repository.select(id)
                } else {
                    repository.update(profileId!!, draft)
                }
                _events.send(EditEvent.Done)
            } catch (e: ProfileLimitReached) {
                _state.update { it.copy(saving = false, step = EditStep.FORM, error = "This device already has the maximum number of profiles.") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(saving = false, step = EditStep.FORM, error = "Couldn't save the profile. ${e.message.orEmpty()}".trim()) }
            }
        }
    }

    fun delete() {
        val id = profileId ?: return
        viewModelScope.launch {
            try {
                repository.delete(id)
                _events.send(EditEvent.Done)
            } catch (e: LastProfileDeletion) {
                _state.update { it.copy(error = "The last remaining profile can't be deleted.") }
            }
        }
    }

    // --- Profile PIN (PR-5) ----------------------------------------------------------------

    fun startSetPin() = _state.update { it.copy(pinFlow = PinFlow.SetNew) }

    fun startChangePin() = _state.update { it.copy(pinFlow = if (it.hasPin) PinFlow.VerifyCurrent(thenClear = false) else PinFlow.SetNew) }

    fun startRemovePin() {
        val s = _state.value
        if (s.hasPin) {
            _state.update { it.copy(pinFlow = PinFlow.VerifyCurrent(thenClear = true)) }
        } else {
            _state.update { it.copy(pendingPin = null) }
        }
    }

    fun dismissPinFlow() = _state.update { it.copy(pinFlow = null) }

    fun verifyCurrentPin(pin: String) {
        val flow = _state.value.pinFlow as? PinFlow.VerifyCurrent ?: return
        val id = profileId ?: return
        viewModelScope.launch {
            if (!repository.verifyPin(id, pin)) {
                _state.update { it.copy(pinFlow = flow.copy(error = "Incorrect PIN. Try again.")) }
            } else if (flow.thenClear) {
                repository.setPin(id, null)
                _state.update { it.copy(pinFlow = null, hasPin = false, message = "Profile PIN removed") }
            } else {
                _state.update { it.copy(pinFlow = PinFlow.SetNew) }
            }
        }
    }

    fun setNewPin(pin: String) {
        val id = profileId
        if (id == null) {
            _state.update { it.copy(pinFlow = null, pendingPin = pin) }
        } else {
            viewModelScope.launch {
                repository.setPin(id, pin)
                _state.update { it.copy(pinFlow = null, hasPin = true, message = "Profile PIN saved") }
            }
        }
    }
}
