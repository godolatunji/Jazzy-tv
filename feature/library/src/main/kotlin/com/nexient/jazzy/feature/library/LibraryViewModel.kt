package com.nexient.jazzy.feature.library

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexient.jazzy.core.common.Clock
import com.nexient.jazzy.core.data.library.LibraryRepository
import com.nexient.jazzy.core.data.profile.ProfileRepository
import com.nexient.jazzy.core.designsystem.text.UiText
import com.nexient.jazzy.core.model.LibraryItem
import com.nexient.jazzy.core.model.LibrarySort
import com.nexient.jazzy.core.model.Profile
import com.nexient.jazzy.core.model.TitleRef
import com.nexient.jazzy.core.model.TitleSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CollectionOptions(val filter: TypeFilter = TypeFilter.ALL, val sort: LibrarySort = LibrarySort.DATE_ADDED)

data class CollectionState(
    val kind: CollectionKind,
    /** Filtered and sorted items. */
    val items: List<LibraryItem> = emptyList(),
    /** Unfiltered size, to tell "empty list" from "nothing matches the chip". */
    val total: Int = 0,
    val options: CollectionOptions = CollectionOptions(),
    val loading: Boolean = true,
)

data class HistoryState(
    val sections: List<HistorySection> = emptyList(),
    /** Entries before the title search is applied. */
    val totalEntries: Int = 0,
    val loading: Boolean = true,
)

enum class ClearHistoryStep { HIDDEN, CONFIRM, PIN }

data class LibraryUiState(
    val profile: Profile? = null,
    val myList: CollectionState = CollectionState(CollectionKind.MY_LIST),
    val liked: CollectionState = CollectionState(CollectionKind.LIKED),
    val history: HistoryState = HistoryState(),
    val myListKeys: Set<TitleRef> = emptySet(),
    val likedKeys: Set<TitleRef> = emptySet(),
    val clearStep: ClearHistoryStep = ClearHistoryStep.HIDDEN,
    @StringRes val pinError: Int? = null,
) {
    fun collection(kind: CollectionKind) = if (kind == CollectionKind.MY_LIST) myList else liked
}

/** One-off snackbar messages. [undo] re-adds the removed title with the same snapshot (ML-4, LK-4). */
data class LibraryMessage(val text: UiText, val undo: Undo? = null) {
    data class Undo(val kind: CollectionKind, val snapshot: TitleSnapshot, val addedAt: Long? = null)
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val profiles: ProfileRepository,
    private val library: LibraryRepository,
    private val clock: Clock,
) : ViewModel() {

    /** History title search (HS-7); Compose state so the text field updates synchronously. */
    var historyQuery by mutableStateOf("")
        private set

    private val myListOptions = MutableStateFlow(CollectionOptions())
    private val likedOptions = MutableStateFlow(CollectionOptions())
    private val clearStep = MutableStateFlow(ClearHistoryStep.HIDDEN)
    private val pinError = MutableStateFlow<Int?>(null)
    private val _messages = Channel<LibraryMessage>(Channel.BUFFERED)
    val messages: Flow<LibraryMessage> = _messages.receiveAsFlow()

    private val profile = profiles.activeProfile.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val myList: Flow<CollectionState> = collection(CollectionKind.MY_LIST, myListOptions) { library.myList(it) }
    private val liked: Flow<CollectionState> = collection(CollectionKind.LIKED, likedOptions) { library.liked(it) }

    private val history: Flow<HistoryState> = combine(
        profile.filterNotNull().flatMapLatest { library.history(it.id) },
        snapshotFlow { historyQuery },
    ) { days, query ->
        HistoryState(
            sections = days.toSections(query, clock.today()),
            totalEntries = days.sumOf { it.entries.size },
            loading = false,
        )
    }.onStart { emit(HistoryState()) }

    private val keys: Flow<Pair<Set<TitleRef>, Set<TitleRef>>> = profile.filterNotNull()
        .flatMapLatest { p -> combine(library.myListKeys(p.id), library.likedKeys(p.id)) { a, b -> a to b } }
        .onStart { emit(emptySet<TitleRef>() to emptySet()) }

    val state: StateFlow<LibraryUiState> = combine(
        combine(profile, clearStep, pinError) { p, step, err -> Triple(p, step, err) },
        myList,
        liked,
        history,
        keys,
    ) { (p, step, err), my, lk, hist, (myKeys, likedKeys) ->
        LibraryUiState(
            profile = p,
            myList = my,
            liked = lk,
            history = hist,
            myListKeys = myKeys,
            likedKeys = likedKeys,
            clearStep = step,
            pinError = err,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    private fun collection(
        kind: CollectionKind,
        options: MutableStateFlow<CollectionOptions>,
        source: (Long) -> Flow<List<LibraryItem>>,
    ): Flow<CollectionState> = combine(profile.filterNotNull().flatMapLatest { source(it.id) }, options) { items, o ->
        CollectionState(kind, items.arrange(o.filter, o.sort), items.size, o, loading = false)
    }.onStart { emit(CollectionState(kind, options = options.value)) }

    private fun options(kind: CollectionKind) = if (kind == CollectionKind.MY_LIST) myListOptions else likedOptions

    // ---- My List / Liked ----

    fun setFilter(kind: CollectionKind, filter: TypeFilter) = options(kind).update { it.copy(filter = filter) }

    fun setSort(kind: CollectionKind, sort: LibrarySort) = options(kind).update { it.copy(sort = sort) }

    /** Remove from My List / unlike, with a 5 s Undo (ML-4, LK-4). */
    fun remove(kind: CollectionKind, item: LibraryItem) {
        val p = profile.value ?: return
        viewModelScope.launch {
            set(kind, p.id, item.snapshot, false)
            val text = UiText.Res(
                if (kind == CollectionKind.MY_LIST) R.string.library_collection_removed_from_my_list else R.string.library_collection_unliked,
                item.snapshot.title,
            )
            _messages.send(LibraryMessage(text, LibraryMessage.Undo(kind, item.snapshot, item.addedAt)))
        }
    }

    fun undo(undo: LibraryMessage.Undo) {
        val p = profile.value ?: return
        viewModelScope.launch { set(undo.kind, p.id, undo.snapshot, true, undo.addedAt) }
    }

    /** Card-menu toggle for the other list (e.g. "Like" from My List). */
    fun toggle(kind: CollectionKind, snapshot: TitleSnapshot) {
        val p = profile.value ?: return
        val current = if (kind == CollectionKind.MY_LIST) state.value.myListKeys else state.value.likedKeys
        viewModelScope.launch { set(kind, p.id, snapshot, snapshot.ref !in current) }
    }

    private suspend fun set(kind: CollectionKind, profileId: Long, snapshot: TitleSnapshot, value: Boolean, at: Long? = null) = when (kind) {
        CollectionKind.MY_LIST -> library.setInMyList(profileId, snapshot, value, at)
        CollectionKind.LIKED -> library.setLiked(profileId, snapshot, value, at)
    }

    // ---- History (HS-3 … HS-7) ----

    fun onHistoryQueryChange(value: String) {
        historyQuery = value
    }

    /** Removes an entry and every session folded into it (HS-5, HS-6). */
    fun removeHistoryItem(item: HistoryItem) {
        viewModelScope.launch {
            library.deleteSessions(item.sessionIds)
            _messages.send(LibraryMessage(UiText.Res(R.string.library_history_removed, item.snapshot.title)))
        }
    }

    /** Removes one session from a folded entry. */
    fun removeSession(sessionId: Long) {
        viewModelScope.launch { library.deleteSessions(listOf(sessionId)) }
    }

    fun removeAllForTitle(item: HistoryItem) {
        val p = profile.value ?: return
        viewModelScope.launch {
            library.deleteHistoryForTitle(p.id, item.snapshot.ref)
            _messages.send(LibraryMessage(UiText.Res(R.string.library_history_removed_all, item.snapshot.title)))
        }
    }

    fun requestClearHistory() {
        pinError.value = null
        clearStep.value = ClearHistoryStep.CONFIRM
    }

    /** After the confirmation, protected profiles must enter their PIN (HS-6). */
    fun confirmClearHistory() {
        val p = profile.value ?: return
        if (p.hasPin) {
            pinError.value = null
            clearStep.value = ClearHistoryStep.PIN
        } else {
            clearNow(p)
        }
    }

    fun submitPin(pin: String) {
        val p = profile.value ?: return
        viewModelScope.launch {
            if (profiles.verifyPin(p.id, pin)) clearNow(p) else pinError.value = R.string.library_history_pin_wrong
        }
    }

    fun dismissClearHistory() {
        clearStep.value = ClearHistoryStep.HIDDEN
        pinError.value = null
    }

    private fun clearNow(p: Profile) {
        clearStep.value = ClearHistoryStep.HIDDEN
        pinError.value = null
        viewModelScope.launch {
            library.clearHistory(p.id)
            historyQuery = ""
            _messages.send(LibraryMessage(UiText.Res(R.string.library_history_cleared)))
        }
    }
}
