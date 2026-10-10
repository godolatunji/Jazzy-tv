package com.nexient.jazzy.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.nexient.jazzy.core.data.NetworkMonitor
import com.nexient.jazzy.core.data.catalog.CatalogRepository
import com.nexient.jazzy.core.data.catalog.HomeTab
import com.nexient.jazzy.core.data.catalog.RailDefinition
import com.nexient.jazzy.core.data.catalog.RailStyle
import com.nexient.jazzy.core.data.catalog.TitleRepository
import com.nexient.jazzy.core.data.catalog.tmdbRails
import com.nexient.jazzy.core.data.library.LibraryRepository
import com.nexient.jazzy.core.data.library.NextEpisodeResolver
import com.nexient.jazzy.core.data.profile.ProfileRepository
import com.nexient.jazzy.core.data.recommendations.RecommendationRail
import com.nexient.jazzy.core.data.recommendations.RecommendationRepository
import com.nexient.jazzy.core.designsystem.component.HeroContent
import com.nexient.jazzy.core.designsystem.text.UiText
import com.nexient.jazzy.core.designsystem.text.railTitle
import com.nexient.jazzy.core.model.ContinueWatchingItem
import com.nexient.jazzy.core.model.LibraryItem
import com.nexient.jazzy.core.model.Profile
import com.nexient.jazzy.core.model.TitleRef
import com.nexient.jazzy.core.model.TitleSnapshot
import com.nexient.jazzy.core.model.TitleSummary
import com.nexient.jazzy.core.model.toSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

/** A Home rail, in spec HM-3 order. */
sealed interface HomeRail {
    val key: String
    val title: UiText

    data class ContinueWatching(val items: List<ContinueWatchingItem>) : HomeRail {
        override val key = "continue"
        override val title: UiText = UiText.Res(R.string.home_rail_continue_watching)
    }

    data class Fixed(override val key: String, override val title: UiText, val items: List<TitleSummary>) : HomeRail

    data class Library(override val key: String, override val title: UiText, val items: List<LibraryItem>) : HomeRail

    data class Paged(val definition: RailDefinition, val items: Flow<PagingData<TitleSummary>>) : HomeRail {
        override val key get() = definition.key
        override val title: UiText get() = railTitle(definition.key, definition.title, definition.title.removeSuffix(" Originals").takeIf { definition.key.startsWith("originals_") })
        val style: RailStyle get() = definition.style
    }
}

data class HeroState(val summary: TitleSummary, val content: HeroContent)

data class HomeUiState(
    val profile: Profile? = null,
    val tab: HomeTab = HomeTab.ALL,
    val hero: HeroState? = null,
    val rails: List<HomeRail> = emptyList(),
    val myList: Set<TitleRef> = emptySet(),
    val liked: Set<TitleRef> = emptySet(),
    val offline: Boolean = false,
    val refreshing: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val profiles: ProfileRepository,
    private val catalog: CatalogRepository,
    private val titles: TitleRepository,
    private val library: LibraryRepository,
    private val recommendations: RecommendationRepository,
    private val nextEpisodes: NextEpisodeResolver,
    network: NetworkMonitor,
) : ViewModel() {
    private val tab = MutableStateFlow(HomeTab.ALL)
    private val hero = MutableStateFlow<HeroState?>(null)
    private val refreshing = MutableStateFlow(false)
    private val heroSeed = Random.nextInt(5)

    private val profile = profiles.activeProfile.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Paged TMDB rails are rebuilt only when the tab or profile changes, and cached in the VM scope. */
    private val pagedRails: StateFlow<List<HomeRail.Paged>> = combine(tab, profile.filterNotNull()) { t, p -> t to p }
        .map { (t, p) -> tmdbRails(t).map { def -> HomeRail.Paged(def, catalog.pager(def, p).cachedIn(viewModelScope)) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val localRails: Flow<LocalRails> = profile.filterNotNull().flatMapLatest { p ->
        combine(
            library.continueWatching(p.id) { progress -> nextEpisodes.next(progress, p.language) },
            library.myList(p.id),
            library.liked(p.id),
            recommendations.rails(p.id),
        ) { cw, myList, liked, recs -> LocalRails(cw, myList, liked, recs) }
    }

    private val keys: Flow<Pair<Set<TitleRef>, Set<TitleRef>>> = profile.filterNotNull().flatMapLatest { p ->
        combine(library.myListKeys(p.id), library.likedKeys(p.id)) { a, b -> a to b }
    }

    val state: StateFlow<HomeUiState> = combine(
        combine(profile, tab, hero, refreshing) { p, t, h, r -> HeaderPart(p, t, h, r) },
        localRails,
        pagedRails,
        keys,
        network.status,
    ) { header, local, paged, (myList, liked), net ->
        HomeUiState(
            profile = header.profile,
            tab = header.tab,
            hero = header.hero,
            rails = assemble(header.tab, local, paged),
            myList = myList,
            liked = liked,
            offline = !net.online,
            refreshing = header.refreshing,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        // Pick the hero once per tab/profile (spec §5.2: rotated per session).
        viewModelScope.launch {
            combine(tab, profile.filterNotNull()) { t, p -> t to p }.collect { (t, p) -> pickHero(t, p) }
        }
    }

    fun selectTab(newTab: HomeTab) = tab.update { newTab }

    fun refresh() {
        val p = profile.value ?: return
        viewModelScope.launch {
            refreshing.value = true
            runCatching { tmdbRails(tab.value).forEach { catalog.loadRailPage(it, 1, p, force = true) } }
            runCatching { recommendations.rebuild(p) }
            pickHero(tab.value, p)
            refreshing.value = false
        }
    }

    fun toggleMyList(snapshot: TitleSnapshot) = viewModelScope.launch {
        val p = profile.value ?: return@launch
        library.setInMyList(p.id, snapshot, snapshot.ref !in state.value.myList)
    }

    fun toggleLike(snapshot: TitleSnapshot) = viewModelScope.launch {
        val p = profile.value ?: return@launch
        library.setLiked(p.id, snapshot, snapshot.ref !in state.value.liked)
    }

    fun removeFromContinueWatching(ref: TitleRef) = viewModelScope.launch {
        val p = profile.value ?: return@launch
        library.removeFromContinueWatching(p.id, ref)
    }

    fun markWatched(item: ContinueWatchingItem) = viewModelScope.launch {
        val p = profile.value ?: return@launch
        library.markWatched(p.id, item.progress.playable, true, item.progress.durationMs)
    }

    /** TV dynamic backdrop: the hero follows the focused card (spec §5.2). */
    fun onCardFocused(summary: TitleSummary) {
        if (hero.value?.summary?.ref == summary.ref) return
        hero.value = HeroState(summary, summary.toHero(null, null, emptyList()))
    }

    private suspend fun pickHero(t: HomeTab, p: Profile) {
        val recs = recommendations.rails(p.id).first().firstOrNull()?.items.orEmpty()
        val trending = runCatching { catalog.loadRailPage(tmdbRails(t).first(), 1, p).items }.getOrDefault(emptyList())
        val candidates = (recs.filter { t.mediaType == null || it.mediaType == t.mediaType } + trending)
            .filter { it.backdropPath != null }
            .take(5)
        val pick = candidates.getOrNull(heroSeed.coerceAtMost(candidates.lastIndex.coerceAtLeast(0))) ?: return
        hero.value = HeroState(pick, pick.toHero(null, null, emptyList()))
        runCatching {
            val details = titles.detailsOnce(pick.ref, p.language)
            hero.value = HeroState(pick, pick.toHero(details.logoPath, details.certification, details.genres.map { it.name }, details.tagline))
        }
    }

    private fun assemble(t: HomeTab, local: LocalRails, paged: List<HomeRail.Paged>): List<HomeRail> {
        fun <T> List<T>.forTab(type: (T) -> com.nexient.jazzy.core.model.MediaType) =
            if (t.mediaType == null) this else filter { type(it) == t.mediaType }
        val rails = mutableListOf<HomeRail>()
        local.continueWatching.forTab { it.snapshot.mediaType }.takeIf { it.isNotEmpty() }?.let { rails += HomeRail.ContinueWatching(it) }
        local.recommendations.forEach { r ->
            r.items.forTab { it.mediaType }.takeIf { it.isNotEmpty() }?.let { rails += HomeRail.Fixed(r.key, railTitle(r.key, r.title), it) }
        }
        local.myList.forTab { it.snapshot.mediaType }.takeIf { it.isNotEmpty() }?.let { rails += HomeRail.Library("my_list", UiText.Res(R.string.home_rail_my_list), it) }
        local.liked.forTab { it.snapshot.mediaType }.takeIf { it.isNotEmpty() }?.let { rails += HomeRail.Library("liked", UiText.Res(R.string.home_rail_liked), it) }
        rails += paged
        return rails
    }

    private data class LocalRails(
        val continueWatching: List<ContinueWatchingItem>,
        val myList: List<LibraryItem>,
        val liked: List<LibraryItem>,
        val recommendations: List<RecommendationRail>,
    )

    private data class HeaderPart(val profile: Profile?, val tab: HomeTab, val hero: HeroState?, val refreshing: Boolean)
}

internal fun TitleSummary.toHero(logo: String?, certification: String?, genres: List<String>, tagline: String? = null) = HeroContent(
    key = "$tmdbId:${mediaType.apiValue}",
    title = title,
    overview = tagline ?: overview,
    backdropPath = backdropPath,
    logoPath = logo,
    genres = genres,
    certification = certification,
    meta = listOfNotNull(year?.toString(), if (voteAverage > 0) "★ ${"%.1f".format(voteAverage)}" else null).joinToString("  ").ifEmpty { null },
)

fun TitleSummary.snapshot(): TitleSnapshot = toSnapshot()
