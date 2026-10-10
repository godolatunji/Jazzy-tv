package com.nexient.jazzy.feature.settings

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexient.jazzy.core.data.profile.ProfileRepository
import com.nexient.jazzy.core.data.sources.DbProviderSettingsStore
import com.nexient.jazzy.core.data.sources.SourceRepository
import com.nexient.jazzy.core.designsystem.text.UiText
import com.nexient.jazzy.core.model.FailureReason
import com.nexient.jazzy.core.model.ProviderHealth
import com.nexient.jazzy.core.model.ProviderInfo
import com.nexient.jazzy.core.model.ProviderSearchState
import com.nexient.jazzy.core.model.ProviderSettings
import com.nexient.jazzy.core.model.ProviderStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URI
import javax.inject.Inject

sealed interface SiteTest {
    data object Running : SiteTest
    data class Finished(val status: ProviderStatus, val tookMs: Long) : SiteTest
}

/** One built-in provider with its effective configuration (SRC-5). */
data class SiteItem(
    val info: ProviderInfo,
    /** Explicit setting, else [ProviderInfo.enabledByDefault]. */
    val enabled: Boolean,
    val settings: ProviderSettings?,
    val health: ProviderHealth?,
    val test: SiteTest?,
) {
    val hasAuth: Boolean get() = info.authKinds.isNotEmpty()
    val hasMirror: Boolean get() = !settings?.baseUrlOverride.isNullOrBlank()
    val effectiveBaseUrl: String get() = settings?.baseUrlOverride?.takeIf { it.isNotBlank() } ?: info.defaultBaseUrl

    /** "Not responding" after repeated failures; never disabled silently (SRC-5). */
    val notResponding: Boolean get() = (health?.errorCount ?: 0) >= NOT_RESPONDING_ERRORS

    companion object {
        const val NOT_RESPONDING_ERRORS = 3
    }
}

data class SitesUiState(
    val loading: Boolean = true,
    val items: List<SiteItem> = emptyList(),
    val isKids: Boolean = false,
    val message: UiText? = null,
)

@HiltViewModel
class SitesSettingsViewModel @Inject constructor(
    private val sources: SourceRepository,
    private val store: DbProviderSettingsStore,
    profiles: ProfileRepository,
) : ViewModel() {
    private val tests = MutableStateFlow<Map<String, SiteTest>>(emptyMap())
    private val message = MutableStateFlow<UiText?>(null)
    private val providers: List<ProviderInfo> by lazy { runCatching { sources.providers }.getOrDefault(emptyList()) }

    val state: StateFlow<SitesUiState> = combine(
        store.settings,
        store.health,
        tests,
        profiles.activeProfile,
        message,
    ) { settings, health, running, profile, msg ->
        SitesUiState(
            loading = false,
            items = providers.map { info ->
                val explicit = settings[info.id]
                SiteItem(
                    info = info,
                    enabled = explicit?.enabled ?: info.enabledByDefault,
                    settings = explicit,
                    health = health[info.id],
                    test = running[info.id],
                )
            },
            isKids = profile?.isKids == true,
            message = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SitesUiState())

    fun clearMessage() = message.update { null }

    fun setEnabled(providerId: String, enabled: Boolean) {
        viewModelScope.launch { store.setEnabled(providerId, enabled) }
    }

    /**
     * Saves the mirror URL and login/cookies. Blank values reset to the default / remove the secret.
     * @return an error message (string resource) when the URL isn't valid, else null.
     */
    @StringRes
    fun saveConnection(providerId: String, baseUrl: String, username: String, password: String, cookies: String): Int? {
        val url = baseUrl.trim()
        if (url.isNotEmpty() && !isValidBaseUrl(url)) return R.string.settings_sites_invalid_url
        val current = state.value.items.firstOrNull { it.info.id == providerId }
        viewModelScope.launch {
            // Rows without an explicit enable flag are ignored by the store, so pin it first.
            store.ensureExplicit(providerId, current?.enabled ?: false)
            store.setConnection(
                providerId,
                baseUrl = url.trimEnd('/').ifEmpty { null },
                username = username.ifBlank { null },
                password = password.ifEmpty { null },
                cookies = cookies.ifBlank { null },
            )
            message.value = UiText.Res(
                R.string.settings_sites_saved,
                current?.info?.name ?: UiText.Res(R.string.settings_sites_saved_fallback_name),
            )
        }
        return null
    }

    fun test(providerId: String) {
        if (tests.value[providerId] == SiteTest.Running) return
        tests.update { it + (providerId to SiteTest.Running) }
        viewModelScope.launch {
            val start = System.currentTimeMillis()
            val status = try {
                sources.testProvider(providerId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val name = providers.firstOrNull { it.id == providerId }?.name ?: providerId
                ProviderStatus(providerId, name, ProviderSearchState.Failed(FailureReason.OTHER, e.message))
            }
            tests.update { it + (providerId to SiteTest.Finished(status, System.currentTimeMillis() - start)) }
        }
    }

    private fun isValidBaseUrl(url: String): Boolean = runCatching {
        val uri = URI(url)
        (uri.scheme == "https" || uri.scheme == "http") && !uri.host.isNullOrBlank()
    }.getOrDefault(false)
}
