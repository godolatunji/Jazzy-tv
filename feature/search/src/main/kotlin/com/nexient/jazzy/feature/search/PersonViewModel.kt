package com.nexient.jazzy.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexient.jazzy.core.data.catalog.SearchRepository
import com.nexient.jazzy.core.data.library.LibraryRepository
import com.nexient.jazzy.core.data.profile.ProfileRepository
import com.nexient.jazzy.core.model.MaturityLevel
import com.nexient.jazzy.core.model.Profile
import com.nexient.jazzy.core.model.TitleRef
import com.nexient.jazzy.core.model.TitleSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PersonUiState(
    val personId: Int? = null,
    val loading: Boolean = true,
    val error: Boolean = false,
    val credits: List<TitleSummary> = emptyList(),
    val myList: Set<TitleRef> = emptySet(),
    val liked: Set<TitleRef> = emptySet(),
)

/** A person's filmography (SR-1). The person is set by [load] from the route arguments. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PersonViewModel @Inject constructor(
    profiles: ProfileRepository,
    private val search: SearchRepository,
    library: LibraryRepository,
) : ViewModel() {
    private val content = MutableStateFlow(PersonUiState())
    private var job: Job? = null

    private val profile = profiles.activeProfile.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val keys: Flow<Pair<Set<TitleRef>, Set<TitleRef>>> = profile.filterNotNull()
        .flatMapLatest { p -> combine(library.myListKeys(p.id), library.likedKeys(p.id)) { a, b -> a to b } }
        .onStart { emit(emptySet<TitleRef>() to emptySet()) }

    val state: StateFlow<PersonUiState> = combine(content, keys) { c, (myList, liked) -> c.copy(myList = myList, liked = liked) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PersonUiState())

    fun load(personId: Int, force: Boolean = false) {
        if (!force && content.value.personId == personId && !content.value.error) return
        job?.cancel()
        content.value = PersonUiState(personId = personId, loading = true)
        job = viewModelScope.launch {
            val p = profile.filterNotNull().first()
            content.value = runCatching { search.personCredits(personId, p.language) }
                .map { credits -> credits.filter { it.isAllowedFor(p) } }
                .fold(
                    onSuccess = { PersonUiState(personId, loading = false, credits = it) },
                    onFailure = { PersonUiState(personId, loading = false, error = true) },
                )
        }
    }

    fun retry() {
        content.value.personId?.let { load(it, force = true) }
    }

    /** Credits carry no certification, so restricted profiles only see family genres (as Search does, SR-5). */
    private fun TitleSummary.isAllowedFor(profile: Profile): Boolean =
        !adult && (profile.maturity == MaturityLevel.ALL || genreIds.any { it in KIDS_GENRES })

    private companion object {
        val KIDS_GENRES = setOf(16, 10751, 10762)
    }
}
