package com.nexient.jazzy.feature.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexient.jazzy.core.common.Clock
import com.nexient.jazzy.core.common.Resource
import com.nexient.jazzy.core.data.catalog.TitleRepository
import com.nexient.jazzy.core.data.device.DeviceCapabilities
import com.nexient.jazzy.core.data.library.LibraryRepository
import com.nexient.jazzy.core.data.profile.ProfileRepository
import com.nexient.jazzy.core.data.settings.SettingsRepository
import com.nexient.jazzy.core.data.sources.SourceRepository
import com.nexient.jazzy.core.designsystem.component.SourceRowFlags
import com.nexient.jazzy.core.designsystem.text.UiText
import com.nexient.jazzy.core.model.Episode
import com.nexient.jazzy.core.model.MaturityLevel
import com.nexient.jazzy.core.model.MediaType
import com.nexient.jazzy.core.model.NO_EPISODE
import com.nexient.jazzy.core.model.PlayableRef
import com.nexient.jazzy.core.model.PlaybackRequest
import com.nexient.jazzy.core.model.Profile
import com.nexient.jazzy.core.model.Resolution
import com.nexient.jazzy.core.model.Season
import com.nexient.jazzy.core.model.SourceQuery
import com.nexient.jazzy.core.model.SourceSearchResult
import com.nexient.jazzy.core.model.SourceSort
import com.nexient.jazzy.core.model.TitleDetails
import com.nexient.jazzy.core.model.TitleRef
import com.nexient.jazzy.core.model.TorrentSource
import com.nexient.jazzy.core.model.WatchProgress
import com.nexient.jazzy.core.model.toSnapshot
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DetailsArgs(
    val ref: TitleRef,
    val season: Int? = null,
    val episode: Int? = null,
    val focusSources: Boolean = false,
)

data class SourceFilters(
    val resolutions: Set<Resolution> = emptySet(),
    val hideCam: Boolean = false,
    val hideUnsupported: Boolean = false,
    val showDead: Boolean = false,
    val sort: SourceSort = SourceSort.SEEDERS,
)

data class SourceRowUi(val source: TorrentSource, val flags: SourceRowFlags)

data class SourcesUi(
    val query: SourceQuery? = null,
    val result: SourceSearchResult? = null,
    val rows: List<SourceRowUi> = emptyList(),
    val hiddenCount: Int = 0,
    val filters: SourceFilters = SourceFilters(),
    val searching: Boolean = false,
    val unreleasedWarning: Boolean = false,
)

data class DetailsUiState(
    val profile: Profile? = null,
    val details: Resource<TitleDetails> = Resource.loading(),
    val blockedByMaturity: Boolean = false,
    val inMyList: Boolean = false,
    val liked: Boolean = false,
    val selectedSeason: Int? = null,
    val season: Resource<Season>? = null,
    val selectedEpisode: Episode? = null,
    val episodeProgress: Map<Pair<Int, Int>, WatchProgress> = emptyMap(),
    /** Progress for the current movie or selected episode (resume banner, DT-3). */
    val progress: WatchProgress? = null,
    val sources: SourcesUi = SourcesUi(),
    val focusSources: Boolean = false,
)

sealed interface DetailsEvent {
    data class Play(val request: PlaybackRequest) : DetailsEvent
    data class AskResume(val source: TorrentSource, val positionMs: Long) : DetailsEvent
    data class Message(val text: UiText) : DetailsEvent
    data class OpenTrailer(val youtubeKey: String) : DetailsEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = DetailsViewModel.Factory::class)
class DetailsViewModel @AssistedInject constructor(
    @Assisted private val args: DetailsArgs,
    private val profiles: ProfileRepository,
    private val titles: TitleRepository,
    private val library: LibraryRepository,
    private val sourcesRepo: SourceRepository,
    private val settings: SettingsRepository,
    private val capabilities: DeviceCapabilities,
    private val clock: Clock,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(args: DetailsArgs): DetailsViewModel
    }

    private val profile = profiles.activeProfile.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val refreshDetails = MutableStateFlow(0)
    private val selectedSeason = MutableStateFlow(args.season)
    private val selectedEpisodeNumber = MutableStateFlow(args.episode)
    private val filters = MutableStateFlow(SourceFilters())
    private val sourceResult = MutableStateFlow<SourceSearchResult?>(null)
    private val searching = MutableStateFlow(false)
    private val currentQuery = MutableStateFlow<SourceQuery?>(null)
    private val resolvingKey = MutableStateFlow<String?>(null)
    private var searchJob: Job? = null

    private val _events = Channel<DetailsEvent>(Channel.BUFFERED)
    val events: Flow<DetailsEvent> = _events.receiveAsFlow()

    private val details: StateFlow<Resource<TitleDetails>> = combine(profile.filterNotNull(), refreshDetails) { p, n -> p to n }
        .flatMapLatest { (p, n) -> titles.details(args.ref, p.language, force = n > 0) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Resource.loading())

    private val season: StateFlow<Resource<Season>?> = combine(profile.filterNotNull(), details, selectedSeason) { p, d, s -> Triple(p, d.data, s) }
        .flatMapLatest { (p, d, s) ->
            if (d == null || d.mediaType != MediaType.TV || s == null) flowOf(null)
            else titles.season(d.tmdbId, s, p.language, stillAiring = d.inProduction)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val titleProgress: Flow<List<WatchProgress>> = profile.filterNotNull()
        .flatMapLatest { p -> library.progressForTitle(p.id, args.ref) }

    val state: StateFlow<DetailsUiState> = combine(
        combine(profile, details, season, selectedEpisodeNumber) { p, d, s, e -> Quad(p, d, s, e) },
        profile.filterNotNull().flatMapLatest { p ->
            combine(library.isInMyList(p.id, args.ref), library.isLiked(p.id, args.ref)) { a, b -> a to b }
        },
        titleProgress,
        combine(sourceResult, filters, searching, currentQuery, resolvingKey) { r, f, s, q, k -> SourcePart(r, f, s, q, k) },
        sourcesRepo.activeFlags(),
    ) { head, (inList, liked), progressRows, sp, flags ->
        val d = head.details.data
        val episode = head.season?.data?.episodes?.firstOrNull { it.episodeNumber == head.episode }
        val playable = currentPlayable(d, head.season?.data?.seasonNumber, head.episode)
        val progress = progressRows.firstOrNull { it.playable == playable }
        DetailsUiState(
            profile = head.profile,
            details = head.details,
            blockedByMaturity = d != null && head.profile != null && head.profile.maturity != MaturityLevel.ALL &&
                !head.profile.maturity.allows(d.mediaType, d.certification),
            inMyList = inList,
            liked = liked,
            selectedSeason = head.season?.data?.seasonNumber ?: selectedSeason.value,
            season = head.season,
            selectedEpisode = episode,
            episodeProgress = progressRows.filter { it.playable.isEpisode }.associateBy { it.playable.season to it.playable.episode },
            progress = progress?.takeIf { !it.completed && it.positionMs > 0 },
            sources = buildSources(sp, flags, progress, d),
            focusSources = args.focusSources,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailsUiState())

    init {
        // Default filters from settings (SL-5 off by default; user prefs can turn them on).
        viewModelScope.launch {
            val s = settings.appSettings.first()
            filters.update { it.copy(hideCam = s.hideCamByDefault, hideUnsupported = s.hideUnsupportedByDefault, showDead = s.showDeadLinks, sort = s.defaultSourceSort) }
        }
        // Movies: search as soon as details arrive. Shows: pick the season/episode, then search per episode.
        viewModelScope.launch {
            val d = details.filterNotNull().map { it.data }.filterNotNull().first()
            if (d.mediaType == MediaType.MOVIE) {
                startSearch(queryFor(d, null, null))
            } else if (selectedSeason.value == null || selectedEpisodeNumber.value == null) {
                val (s, e) = defaultEpisode(d)
                if (selectedSeason.value == null) selectedSeason.value = s
                if (selectedEpisodeNumber.value == null) selectedEpisodeNumber.value = e
            }
        }
        viewModelScope.launch {
            combine(details, selectedSeason, selectedEpisodeNumber) { d, s, e -> Triple(d.data, s, e) }.collect { (d, s, e) ->
                if (d != null && d.mediaType == MediaType.TV && s != null && e != null) {
                    val q = queryFor(d, s, e)
                    if (q.cacheKey != currentQuery.value?.cacheKey) startSearch(q)
                }
            }
        }
    }

    fun retryDetails() = refreshDetails.update { it + 1 }

    fun selectSeason(number: Int) {
        if (selectedSeason.value == number) return
        selectedSeason.value = number
        selectedEpisodeNumber.value = null
        sourceResult.value = null
        currentQuery.value = null
    }

    fun selectEpisode(episode: Episode) {
        selectedSeason.value = episode.seasonNumber
        selectedEpisodeNumber.value = episode.episodeNumber
    }

    fun refreshSources() = currentQuery.value?.let { startSearch(it, force = true) }

    fun setFilters(update: (SourceFilters) -> SourceFilters) = filters.update(update)

    fun toggleMyList() = viewModelScope.launch {
        val p = profile.value ?: return@launch
        val d = details.value.data ?: return@launch
        library.setInMyList(p.id, d.summary.toSnapshot(d.imdbId), !state.value.inMyList)
    }

    fun toggleLike() = viewModelScope.launch {
        val p = profile.value ?: return@launch
        val d = details.value.data ?: return@launch
        library.setLiked(p.id, d.summary.toSnapshot(d.imdbId), !state.value.liked)
    }

    fun markEpisodeWatched(episode: Episode, watched: Boolean) = viewModelScope.launch {
        val p = profile.value ?: return@launch
        val d = details.value.data ?: return@launch
        library.saveSnapshot(d.summary.toSnapshot(d.imdbId))
        library.markWatched(p.id, PlayableRef(d.tmdbId, MediaType.TV, episode.seasonNumber, episode.episodeNumber), watched, (episode.runtimeMinutes ?: 45) * 60_000L)
    }

    fun openTrailer() {
        details.value.data?.trailerYoutubeKey?.let { _events.trySend(DetailsEvent.OpenTrailer(it)) }
    }

    /** ▶ Play on a row. RS-5: a different source than last time asks "Resume / Start over". */
    fun play(source: TorrentSource) {
        val progress = state.value.progress
        if (progress != null && progress.lastInfoHash?.lowercase() != source.infoHash?.lowercase()) {
            _events.trySend(DetailsEvent.AskResume(source, progress.positionMs))
        } else {
            launchPlayback(source, 0)
        }
    }

    fun resume(source: TorrentSource) = launchPlayback(source, state.value.progress?.positionMs ?: 0)

    fun launchPlayback(source: TorrentSource, startPositionMs: Long) {
        val d = details.value.data ?: return
        val s = state.value
        viewModelScope.launch {
            val magnet = try {
                resolvingKey.value = source.key
                sourcesRepo.resolveMagnet(source)
            } catch (e: Exception) {
                _events.send(
                    DetailsEvent.Message(
                        UiText.Res(R.string.details_error_resolve_link, source.providerNames.firstOrNull() ?: UiText.Res(R.string.details_error_the_site)),
                    ),
                )
                return@launch
            } finally {
                resolvingKey.value = null
            }
            val playable = currentPlayable(d, s.selectedSeason, s.selectedEpisode?.episodeNumber)
            _events.send(
                DetailsEvent.Play(
                    PlaybackRequest(
                        snapshot = d.summary.toSnapshot(d.imdbId),
                        playable = playable,
                        episodeTitle = s.selectedEpisode?.name,
                        source = source.copy(magnet = magnet),
                        startPositionMs = startPositionMs,
                        seasonNumbers = d.seasons.map { it.seasonNumber },
                    ),
                ),
            )
        }
    }

    private fun startSearch(query: SourceQuery, force: Boolean = false) {
        searchJob?.cancel()
        currentQuery.value = query
        sourceResult.value = null
        searching.value = true
        searchJob = viewModelScope.launch {
            try {
                sourcesRepo.search(query, force).collect { sourceResult.value = it }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _events.send(DetailsEvent.Message(UiText.Res(R.string.details_error_source_search_failed, e.message ?: UiText.Res(R.string.details_error_unknown))))
            } finally {
                searching.value = false
            }
        }
    }

    private fun queryFor(d: TitleDetails, season: Int?, episode: Int?) = SourceQuery(
        tmdbId = d.tmdbId,
        mediaType = d.mediaType,
        title = d.title,
        originalTitle = d.summary.originalTitle.takeIf { it != d.title },
        year = if (d.mediaType == MediaType.MOVIE) d.year else null,
        imdbId = d.imdbId,
        season = season,
        episode = episode,
    )

    /** Next episode to watch: continue the latest one, the one after a finished one, else S1E1. */
    private suspend fun defaultEpisode(d: TitleDetails): Pair<Int, Int> {
        val firstSeason = d.seasons.firstOrNull { it.seasonNumber > 0 }?.seasonNumber ?: d.seasons.firstOrNull()?.seasonNumber ?: 1
        val p = profile.value ?: return firstSeason to 1
        val latest = library.latestProgress(p.id, args.ref) ?: return firstSeason to 1
        val pl = latest.playable
        if (!pl.isEpisode) return firstSeason to 1
        if (!latest.completed) return pl.season to pl.episode
        val season = runCatching { titles.seasonOnce(d.tmdbId, pl.season, p.language) }.getOrNull()
        val next = season?.episodes?.firstOrNull { it.episodeNumber == pl.episode + 1 }
        return when {
            next != null -> pl.season to next.episodeNumber
            d.seasons.any { it.seasonNumber == pl.season + 1 } -> (pl.season + 1) to 1
            else -> pl.season to pl.episode
        }
    }

    private fun currentPlayable(d: TitleDetails?, season: Int?, episode: Int?): PlayableRef =
        if (d?.mediaType == MediaType.TV && season != null && episode != null) PlayableRef(args.ref.tmdbId, MediaType.TV, season, episode)
        else PlayableRef(args.ref.tmdbId, args.ref.mediaType, NO_EPISODE, NO_EPISODE)

    private fun buildSources(sp: SourcePart, flagged: Set<String>, progress: WatchProgress?, d: TitleDetails?): SourcesUi {
        val all = sp.result?.sources.orEmpty()
        val f = sp.filters
        val visible = all.filter { s ->
            (f.showDead || s.seeders > 0) &&
                (f.resolutions.isEmpty() || (s.release.resolution ?: Resolution.SD) in f.resolutions) &&
                !(f.hideCam && s.release.source?.isLowQuality == true) &&
                !(f.hideUnsupported && capabilities.mayNotPlay(s))
        }
        val sorted = when (f.sort) {
            SourceSort.SEEDERS -> visible // already seeders desc → resolution → size (SL-2)
            SourceSort.SIZE -> visible.sortedByDescending { it.sizeBytes ?: 0 }
            SourceSort.QUALITY -> visible.sortedWith(compareByDescending<TorrentSource> { it.release.resolution?.rank ?: 0 }.thenByDescending { it.seeders })
        }
        val lastHash = progress?.lastInfoHash?.lowercase()
        val resumeMs = progress?.positionMs?.takeIf { progress.completed.not() && it > 0 }
        return SourcesUi(
            query = sp.query,
            result = sp.result,
            rows = sorted.map { s ->
                val isLast = lastHash != null && s.infoHash?.lowercase() == lastHash
                SourceRowUi(
                    s,
                    SourceRowFlags(
                        lastPlayed = isLast,
                        resumePositionMs = if (isLast) resumeMs else null,
                        failedRecently = s.infoHash?.lowercase() in flagged,
                        mayNotPlay = capabilities.mayNotPlay(s),
                        resolving = sp.resolving == s.key,
                    ),
                )
            },
            hiddenCount = all.size - visible.size,
            filters = f,
            searching = sp.searching && sp.result?.isComplete != true,
            unreleasedWarning = d?.isUnreleased(LocalDate.ofEpochDay(clock.nowMillis() / 86_400_000).toString()) == true,
        )
    }

    private data class Quad(val profile: Profile?, val details: Resource<TitleDetails>, val season: Resource<Season>?, val episode: Int?)
    private data class SourcePart(val result: SourceSearchResult?, val filters: SourceFilters, val searching: Boolean, val query: SourceQuery?, val resolving: String?)
}
