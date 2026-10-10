package com.nexient.jazzy.feature.settings

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexient.jazzy.core.data.catalog.SearchRepository
import com.nexient.jazzy.core.data.library.LibraryRepository
import com.nexient.jazzy.core.data.profile.ProfileRepository
import com.nexient.jazzy.core.data.recommendations.RecommendationRepository
import com.nexient.jazzy.core.data.settings.AppSettings
import com.nexient.jazzy.core.data.settings.LocalDataSizes
import com.nexient.jazzy.core.data.settings.SettingKeys
import com.nexient.jazzy.core.data.settings.SettingsRepository
import com.nexient.jazzy.core.data.settings.TmdbCacheCleaner
import com.nexient.jazzy.core.designsystem.text.UiText
import com.nexient.jazzy.core.model.Profile
import com.nexient.jazzy.core.model.SourceSort
import com.nexient.jazzy.core.torrent.TorrentEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** PIN prompts on the Settings screen. */
sealed interface SettingsPinFlow {
    @get:StringRes val error: Int?

    /** HS-6: clearing play history on a protected profile needs its PIN. */
    data class ProfilePinForHistory(@StringRes override val error: Int? = null) : SettingsPinFlow

    /** Changing or removing the parental PIN needs the current one. */
    data class VerifyParental(val remove: Boolean, @StringRes override val error: Int? = null) : SettingsPinFlow

    data object NewParental : SettingsPinFlow {
        override val error: Int? = null
    }
}

data class SettingsUiState(
    val loading: Boolean = true,
    val settings: AppSettings = AppSettings(),
    val profile: Profile? = null,
    val favouriteGenres: Set<String> = emptySet(),
    val hasParentalPin: Boolean = false,
    val torrentCacheBytes: Long? = null,
    val localData: LocalDataSizes? = null,
    val busy: Boolean = false,
    val message: UiText? = null,
    val pinFlow: SettingsPinFlow? = null,
    val openSubtitlesConfigured: Boolean = false,
    val openSubtitlesUser: String? = null,
) {
    val isKids: Boolean get() = profile?.isKids == true
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val profiles: ProfileRepository,
    private val library: LibraryRepository,
    private val search: SearchRepository,
    private val recommendations: RecommendationRepository,
    private val torrentEngine: TorrentEngine,
    private val tmdbCache: TmdbCacheCleaner,
    private val subtitles: com.nexient.jazzy.core.data.subtitles.SubtitleRepository,
) : ViewModel() {
    private data class Local(
        val openSubtitlesUser: String? = null,
        val hasParentalPin: Boolean = false,
        val torrentCacheBytes: Long? = null,
        val localData: LocalDataSizes? = null,
        val busy: Boolean = false,
        val message: UiText? = null,
        val pinFlow: SettingsPinFlow? = null,
    )

    private val local = MutableStateFlow(Local())
    private val profile = profiles.activeProfile.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val state: StateFlow<SettingsUiState> = combine(
        settings.appSettings,
        profile,
        profile.flatMapLatest { p -> if (p == null) flowOf(emptyList()) else profiles.seedGenres(p.id) },
        local,
    ) { app, p, genres, l ->
        SettingsUiState(
            loading = false,
            settings = app,
            profile = p,
            favouriteGenres = genres.toSet(),
            hasParentalPin = l.hasParentalPin,
            torrentCacheBytes = l.torrentCacheBytes,
            localData = l.localData,
            busy = l.busy,
            message = l.message,
            pinFlow = l.pinFlow,
            openSubtitlesConfigured = subtitles.isConfigured,
            openSubtitlesUser = l.openSubtitlesUser,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val has = profiles.hasParentalPin()
            val osUser = subtitles.loggedInUser()
            local.update { it.copy(hasParentalPin = has, openSubtitlesUser = osUser) }
        }
        refreshSizes()
    }

    private fun refreshSizes() {
        viewModelScope.launch {
            val torrent = runCatching { torrentEngine.cacheSizeBytes() }.getOrNull()
            val db = runCatching { tmdbCache.sizes() }.getOrNull()
            local.update { it.copy(torrentCacheBytes = torrent, localData = db) }
        }
    }

    fun clearMessage() = local.update { it.copy(message = null) }

    // --- OpenSubtitles account (PL-8) ---------------------------------------------------------

    fun loginOpenSubtitles(username: String, password: String) {
        viewModelScope.launch {
            local.update { it.copy(busy = true) }
            val result = runCatching { subtitles.login(username, password) }
            local.update {
                it.copy(
                    busy = false,
                    openSubtitlesUser = if (result.isSuccess) username.trim() else it.openSubtitlesUser,
                    message = result.exceptionOrNull()?.message?.let(UiText::Raw) ?: UiText.Res(R.string.settings_subtitles_logged_in),
                )
            }
        }
    }

    fun logoutOpenSubtitles() {
        viewModelScope.launch {
            subtitles.logout()
            local.update { it.copy(openSubtitlesUser = null, message = UiText.Res(R.string.settings_subtitles_logged_out)) }
        }
    }

    // --- Simple persisted settings ------------------------------------------------------------

    fun setFlag(key: String, value: Boolean) {
        viewModelScope.launch { settings.put(key, value) }
    }

    fun setSourceSort(sort: SourceSort) {
        viewModelScope.launch { settings.put(SettingKeys.SOURCE_SORT, sort.name) }
    }

    fun setUploadLimit(bytesPerSecond: Long) {
        viewModelScope.launch { settings.put(SettingKeys.UPLOAD_LIMIT, bytesPerSecond) }
    }

    fun setMaxConnections(count: Int) {
        viewModelScope.launch { settings.put(SettingKeys.MAX_CONNECTIONS, count) }
    }

    fun setCacheCap(bytes: Long) {
        viewModelScope.launch { settings.put(SettingKeys.CACHE_CAP, bytes) }
    }

    fun setFavouriteGenres(ids: Set<String>) {
        val p = profile.value ?: return
        viewModelScope.launch {
            profiles.setSeedGenres(p.id, ids.toList())
            notify(UiText.Res(R.string.settings_genres_saved))
        }
    }

    // --- History & recommendations ------------------------------------------------------------

    /** Called after the user confirmed; asks for the profile PIN first when there is one (HS-6). */
    fun requestClearHistory() {
        val p = profile.value ?: return
        if (p.hasPin) local.update { it.copy(pinFlow = SettingsPinFlow.ProfilePinForHistory()) } else clearHistory(p.id)
    }

    private fun clearHistory(profileId: Long) = runTask(R.string.settings_history_cleared) {
        library.clearHistory(profileId)
        // HS-6: clearing history also resets the profile's genre affinity.
        recommendations.reset(profileId)
    }

    fun clearSearchHistory() {
        val p = profile.value ?: return
        runTask(R.string.settings_history_search_cleared) { search.clearRecent(p.id) }
    }

    fun resetRecommendations() {
        val p = profile.value ?: return
        runTask(R.string.settings_history_recs_reset) { recommendations.reset(p.id) }
    }

    // --- Storage ----------------------------------------------------------------------------

    fun clearTorrentCache() = runTask(R.string.settings_network_torrent_cache_cleared) {
        torrentEngine.clearCache()
        refreshSizes()
    }

    fun clearTmdbCache() = runTask(R.string.settings_local_tmdb_cache_cleared) {
        tmdbCache.clear()
        refreshSizes()
    }

    // --- PINs -------------------------------------------------------------------------------

    fun startSetParentalPin() = local.update { it.copy(pinFlow = SettingsPinFlow.NewParental) }

    fun startChangeParentalPin() = local.update { it.copy(pinFlow = SettingsPinFlow.VerifyParental(remove = false)) }

    fun startRemoveParentalPin() = local.update { it.copy(pinFlow = SettingsPinFlow.VerifyParental(remove = true)) }

    fun dismissPin() = local.update { it.copy(pinFlow = null) }

    fun submitPin(pin: String) {
        viewModelScope.launch {
            when (val flow = local.value.pinFlow) {
                is SettingsPinFlow.ProfilePinForHistory -> {
                    val p = profile.value ?: return@launch
                    if (profiles.verifyPin(p.id, pin)) {
                        local.update { it.copy(pinFlow = null) }
                        clearHistory(p.id)
                    } else {
                        local.update { it.copy(pinFlow = flow.copy(error = R.string.settings_pin_incorrect)) }
                    }
                }
                is SettingsPinFlow.VerifyParental -> {
                    if (!profiles.verifyParentalPin(pin)) {
                        local.update { it.copy(pinFlow = flow.copy(error = R.string.settings_pin_parental_incorrect)) }
                    } else if (flow.remove) {
                        profiles.setParentalPin(null)
                        local.update { it.copy(pinFlow = null, hasParentalPin = false, message = UiText.Res(R.string.settings_pin_parental_removed)) }
                    } else {
                        local.update { it.copy(pinFlow = SettingsPinFlow.NewParental) }
                    }
                }
                SettingsPinFlow.NewParental -> {
                    profiles.setParentalPin(pin)
                    local.update { it.copy(pinFlow = null, hasParentalPin = true, message = UiText.Res(R.string.settings_pin_parental_saved)) }
                }
                null -> Unit
            }
        }
    }

    private fun notify(message: UiText) = local.update { it.copy(message = message) }

    private fun runTask(@StringRes success: Int, block: suspend () -> Unit) {
        viewModelScope.launch {
            local.update { it.copy(busy = true) }
            val result = try {
                block()
                UiText.Res(success)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UiText.Res(R.string.settings_common_error, (e.message ?: e::class.simpleName).toString())
            }
            local.update { it.copy(busy = false, message = result) }
        }
    }
}
