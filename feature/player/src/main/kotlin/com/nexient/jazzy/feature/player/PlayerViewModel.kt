package com.nexient.jazzy.feature.player

import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.nexient.jazzy.core.common.formatDuration
import com.nexient.jazzy.core.data.catalog.TitleRepository
import com.nexient.jazzy.core.data.library.LibraryRepository
import com.nexient.jazzy.core.data.library.NextEpisodeResolver
import com.nexient.jazzy.core.data.library.SourceRef
import com.nexient.jazzy.core.data.profile.ProfileRepository
import com.nexient.jazzy.core.data.sources.SourceRepository
import com.nexient.jazzy.core.model.Episode
import com.nexient.jazzy.core.model.MediaType
import com.nexient.jazzy.core.model.PlayableRef
import com.nexient.jazzy.core.model.PlaybackEndReason
import com.nexient.jazzy.core.model.PlaybackRequest
import com.nexient.jazzy.core.model.Profile
import com.nexient.jazzy.core.model.SourceQuery
import com.nexient.jazzy.core.model.StreamRequest
import com.nexient.jazzy.core.model.StreamState
import com.nexient.jazzy.core.model.StreamStats
import com.nexient.jazzy.core.model.TmdbImages
import com.nexient.jazzy.core.model.TorrentSource
import com.nexient.jazzy.core.model.WatchRules
import com.nexient.jazzy.core.player.PlayerHolder
import com.nexient.jazzy.core.torrent.StreamException
import com.nexient.jazzy.core.torrent.StreamFailure
import com.nexient.jazzy.core.torrent.TorrentEngine
import com.nexient.jazzy.core.torrent.TorrentFileProvider
import com.nexient.jazzy.core.torrent.TorrentStream
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class PlayerPhase { CONNECTING, BUFFERING, PLAYING, ENDED, ERROR }

enum class PlayerPanel { NONE, SOURCES, EPISODES, TRACKS }

data class PlayerError(val message: String, val canTryNext: Boolean, val suggestHideUnsupported: Boolean = false)

data class NextEpisodeCountdown(val playable: PlayableRef, val title: String?, val secondsLeft: Int, val sameTorrent: Boolean)

data class EpisodesPanelState(
    val seasonNumbers: List<Int> = emptyList(),
    val season: Int? = null,
    val episodes: List<Episode> = emptyList(),
    val loading: Boolean = false,
    /** Episode whose sources are listed in the panel, or null for the episode list. */
    val chosen: Episode? = null,
    val sources: List<TorrentSource> = emptyList(),
    val sourcesLoading: Boolean = false,
)

data class PlayerUiState(
    val request: PlaybackRequest,
    val phase: PlayerPhase = PlayerPhase.CONNECTING,
    val stats: StreamStats? = null,
    val fileName: String? = null,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val bufferedMs: Long = 0,
    val isPlaying: Boolean = false,
    val speed: Float = 1f,
    val liked: Boolean = false,
    val panel: PlayerPanel = PlayerPanel.NONE,
    val sources: List<TorrentSource> = emptyList(),
    val episodes: EpisodesPanelState = EpisodesPanelState(),
    val stalledSuggestion: TorrentSource? = null,
    val countdown: NextEpisodeCountdown? = null,
    val error: PlayerError? = null,
    val finished: Boolean = false,
    val metadataMs: Long? = null,
    val firstFrameMs: Long? = null,
    val onlineSubtitles: OnlineSubtitlesState = OnlineSubtitlesState(),
) {
    val title: String get() = request.snapshot.title
    val subtitle: String? get() = request.playable.label?.let { listOfNotNull(it, request.episodeTitle).joinToString(" · ") }
}

/** OpenSubtitles search/apply state for the Tracks panel (PL-8). */
data class OnlineSubtitlesState(
    val configured: Boolean = false,
    val searching: Boolean = false,
    val results: List<com.nexient.jazzy.core.data.subtitles.OnlineSubtitle>? = null,
    val applying: Int? = null,
    val applied: com.nexient.jazzy.core.data.subtitles.OnlineSubtitle? = null,
    val offsetMs: Long = 0,
    val error: String? = null,
)

@OptIn(UnstableApi::class)
@HiltViewModel(assistedFactory = PlayerViewModel.Factory::class)
class PlayerViewModel @AssistedInject constructor(
    @Assisted initial: PlaybackRequest,
    private val engine: TorrentEngine,
    private val files: TorrentFileProvider,
    private val holder: PlayerHolder,
    private val library: LibraryRepository,
    private val sourcesRepo: SourceRepository,
    private val titles: TitleRepository,
    private val profiles: ProfileRepository,
    private val nextEpisodes: NextEpisodeResolver,
    private val loopback: com.nexient.jazzy.core.torrent.LoopbackStreamServer,
    private val subtitles: com.nexient.jazzy.core.data.subtitles.SubtitleRepository,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(request: PlaybackRequest): PlayerViewModel
    }

    private val _state = MutableStateFlow(PlayerUiState(initial, onlineSubtitles = OnlineSubtitlesState(configured = subtitles.isConfigured)))
    private var subtitleFile: java.io.File? = null
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    val player: Player get() = holder.get()

    private var profile: Profile? = null
    private var stream: TorrentStream? = null
    private var sessionId: Long? = null
    private var watchedMs = 0L
    private var lastTickAt = 0L
    private var lastProgressSaveAt = 0L
    private var openJob: Job? = null
    private var statsJob: Job? = null
    private var openStartedAt = 0L
    private var nextPrefetched: Pair<PlayableRef, String?>? = null
    private var countdownJob: Job? = null
    private var stalledSince: Long? = null
    private var dismissedStallFor: String? = null

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    if (_state.value.firstFrameMs == null) _state.update { it.copy(firstFrameMs = SystemClock.elapsedRealtime() - openStartedAt) }
                    _state.update { it.copy(phase = PlayerPhase.PLAYING, durationMs = player.duration.coerceAtLeast(0)) }
                    stream?.let { s -> if (player.duration > 0) files.setDurationHint(s.infoHash, player.duration) }
                }
                Player.STATE_BUFFERING -> if (_state.value.phase == PlayerPhase.PLAYING) Unit
                Player.STATE_ENDED -> onEnded()
                else -> Unit
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
            if (!isPlaying) saveProgress() // RS-2: write on pause
        }

        override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
            if (reason == Player.DISCONTINUITY_REASON_SEEK) saveProgress()
        }

        override fun onPlayerError(error: PlaybackException) = onPlaybackError(error)
    }

    init {
        player.addListener(listener)
        viewModelScope.launch {
            profile = profiles.activeProfile.first()
            // Profile language preferences (PL-8): ISO 639-1 codes; null subtitles = off.
            profile?.let { com.nexient.jazzy.core.player.TrackHelper.setPreferredLanguages(player, it.audioLanguage, it.subtitleLanguage) }
            profile?.let { p ->
                launch { library.isLiked(p.id, initial.snapshot.ref).collect { liked -> _state.update { it.copy(liked = liked) } } }
            }
            open(initial, initial.startPositionMs)
        }
        viewModelScope.launch { tickLoop() }
        loadSourcesFor(initial.playable, initial.episodeTitle) { list -> _state.update { it.copy(sources = list) } }
    }

    // ---- Opening and switching streams -------------------------------------------------------

    private fun open(request: PlaybackRequest, startPositionMs: Long) {
        openJob?.cancel()
        openJob = viewModelScope.launch {
            _state.update { it.copy(request = request, phase = PlayerPhase.CONNECTING, error = null, stats = null, countdown = null, stalledSuggestion = null, firstFrameMs = null, metadataMs = null) }
            openStartedAt = SystemClock.elapsedRealtime()
            nextPrefetched = null
            stalledSince = null
            startSession(request, startPositionMs)
            val source = request.source
            val magnet = source.magnet ?: runCatching { sourcesRepo.resolveMagnet(source) }.getOrElse {
                fail(PlayerError("Couldn't get the link from ${source.providerNames.firstOrNull() ?: "the site"}.", canTryNext = true)); return@launch
            }
            try {
                val s = engine.open(
                    StreamRequest(
                        magnet = magnet,
                        infoHash = source.infoHash,
                        expectedTitle = request.snapshot.title,
                        expectedYear = request.snapshot.year,
                        season = request.playable.season.takeIf { request.playable.isEpisode },
                        episode = request.playable.episode.takeIf { request.playable.isEpisode },
                    ),
                )
                stream = s
                _state.update { it.copy(fileName = s.fileName, metadataMs = SystemClock.elapsedRealtime() - openStartedAt, phase = PlayerPhase.BUFFERING) }
                collectStats(s.infoHash)
                val item = MediaItem.Builder()
                    .setUri(s.uri)
                    .setMediaId("${request.playable.tmdbId}:${request.playable.season}:${request.playable.episode}")
                    .setMimeType(s.mimeType)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(request.snapshot.title)
                            .setSubtitle(_state.value.subtitle)
                            .setArtworkUri(TmdbImages.backdrop(request.snapshot.backdropPath)?.toUri())
                            .build(),
                    )
                    .build()
                player.setMediaItem(item, startPositionMs)
                player.prepare()
                player.playWhenReady = true
            } catch (e: StreamException) {
                when (e.failure) {
                    StreamFailure.TITLE_MISMATCH, StreamFailure.NO_VIDEO_FILE -> source.infoHash?.let { sourcesRepo.flagBad(it, e.failure.name) }
                    else -> Unit
                }
                fail(
                    PlayerError(
                        when (e.failure) {
                            StreamFailure.METADATA_TIMEOUT -> "This link isn't responding."
                            StreamFailure.TITLE_MISMATCH -> "This link doesn't match ${request.snapshot.title}. It has been flagged."
                            StreamFailure.NO_VIDEO_FILE -> "This link has no playable video."
                            StreamFailure.NETWORK_NOT_ALLOWED -> "Streaming is set to Wi-Fi only, and you're on a metered connection. Change it in Settings → Network & storage."
                            StreamFailure.ENGINE_ERROR -> "The streaming engine failed: ${e.message ?: "unknown error"}"
                        },
                        canTryNext = e.failure != StreamFailure.NETWORK_NOT_ALLOWED,
                    ),
                )
            }
        }
    }

    private fun collectStats(infoHash: String) {
        statsJob?.cancel()
        statsJob = viewModelScope.launch {
            engine.stats(infoHash).collect { stats ->
                _state.update { it.copy(stats = stats) }
                checkStall(stats)
            }
        }
    }

    /** TE-7: slow source → non-blocking "try the next one?" prompt. */
    private fun checkStall(stats: StreamStats) {
        val key = _state.value.request.source.key
        if (stats.state == StreamState.STALLED && dismissedStallFor != key) {
            val since = stalledSince ?: SystemClock.elapsedRealtime().also { stalledSince = it }
            if (SystemClock.elapsedRealtime() - since >= 20_000 && _state.value.stalledSuggestion == null) {
                nextSource()?.let { next -> _state.update { it.copy(stalledSuggestion = next) } }
            }
        } else if (stats.state == StreamState.STREAMING) {
            stalledSince = null
        }
    }

    fun dismissStall() {
        dismissedStallFor = _state.value.request.source.key
        _state.update { it.copy(stalledSuggestion = null) }
    }

    /** Switch to another source for the same episode/movie, keeping the position (PL-4). */
    fun switchSource(source: TorrentSource, reason: PlaybackEndReason = PlaybackEndReason.SOURCE_SWITCH) {
        val position = currentPositionForResume()
        endSession(reason)
        releaseStream()
        _state.update { it.copy(panel = PlayerPanel.NONE) }
        open(_state.value.request.copy(source = source, startPositionMs = position), position)
    }

    fun tryNextSource() {
        val next = nextSource() ?: return
        switchSource(next, if (_state.value.stats?.state == StreamState.STALLED) PlaybackEndReason.STALLED else PlaybackEndReason.SOURCE_SWITCH)
    }

    private fun nextSource(): TorrentSource? {
        val current = _state.value.request.source
        val list = _state.value.sources.filter { it.seeders > 0 }
        val index = list.indexOfFirst { it.key == current.key }
        return list.drop(index + 1).firstOrNull { it.key != current.key } ?: list.firstOrNull { it.key != current.key }
    }

    // ---- Episodes panel (PL-5) ----------------------------------------------------------------

    fun openPanel(panel: PlayerPanel) {
        _state.update { it.copy(panel = panel) }
        if (panel == PlayerPanel.EPISODES && _state.value.episodes.season == null) {
            val req = _state.value.request
            selectPanelSeason(if (req.playable.isEpisode) req.playable.season else req.seasonNumbers.firstOrNull { it > 0 } ?: 1)
        }
    }

    fun closePanel() = _state.update { it.copy(panel = PlayerPanel.NONE, episodes = it.episodes.copy(chosen = null)) }

    fun selectPanelSeason(season: Int) {
        val req = _state.value.request
        _state.update { it.copy(episodes = it.episodes.copy(seasonNumbers = req.seasonNumbers.filter { n -> n > 0 } + req.seasonNumbers.filter { n -> n == 0 }, season = season, loading = true, chosen = null)) }
        viewModelScope.launch {
            val lang = profile?.language ?: "en-US"
            val eps = runCatching { titles.seasonOnce(req.playable.tmdbId, season, lang).episodes }.getOrDefault(emptyList())
            _state.update { it.copy(episodes = it.episodes.copy(episodes = eps, loading = false)) }
        }
    }

    fun chooseEpisode(episode: Episode) {
        _state.update { it.copy(episodes = it.episodes.copy(chosen = episode, sources = emptyList(), sourcesLoading = true)) }
        val playable = PlayableRef(_state.value.request.playable.tmdbId, MediaType.TV, episode.seasonNumber, episode.episodeNumber)
        loadSourcesFor(playable, episode.name) { list ->
            _state.update { it.copy(episodes = it.episodes.copy(sources = list, sourcesLoading = false)) }
        }
    }

    fun backToEpisodeList() = _state.update { it.copy(episodes = it.episodes.copy(chosen = null)) }

    /** Play another episode from the panel (or the next-episode flow). */
    fun playEpisode(episode: Episode, source: TorrentSource, reason: PlaybackEndReason = PlaybackEndReason.USER_EXIT) {
        val req = _state.value.request
        val playable = PlayableRef(req.playable.tmdbId, MediaType.TV, episode.seasonNumber, episode.episodeNumber)
        startEpisode(playable, episode.name, source, reason)
    }

    private fun startEpisode(playable: PlayableRef, title: String?, source: TorrentSource, reason: PlaybackEndReason) {
        endSession(reason)
        releaseStream()
        viewModelScope.launch {
            val p = profile
            val resume = p?.let { library.progress(it.id, playable).first() }?.takeIf { !it.completed }?.positionMs ?: 0L
            _state.update { it.copy(panel = PlayerPanel.NONE, episodes = it.episodes.copy(chosen = null), sources = emptyList()) }
            val request = _state.value.request.copy(playable = playable, episodeTitle = title, source = source, startPositionMs = resume)
            open(request, resume)
            loadSourcesFor(playable, title) { list -> _state.update { it.copy(sources = list) } }
        }
    }

    private fun loadSourcesFor(playable: PlayableRef, @Suppress("UNUSED_PARAMETER") episodeTitle: String?, onLoaded: (List<TorrentSource>) -> Unit) {
        val snap = _state.value.request.snapshot
        val query = SourceQuery(
            tmdbId = playable.tmdbId,
            mediaType = playable.mediaType,
            title = snap.title,
            originalTitle = snap.originalTitle.takeIf { it != snap.title },
            year = if (playable.mediaType == MediaType.MOVIE) snap.year else null,
            imdbId = snap.imdbId,
            season = playable.season.takeIf { playable.isEpisode },
            episode = playable.episode.takeIf { playable.isEpisode },
        )
        viewModelScope.launch {
            runCatching { sourcesRepo.search(query).collect { r -> if (r.sources.isNotEmpty() || r.isComplete) onLoaded(r.sources) } }
                .onFailure { onLoaded(emptyList()) }
        }
    }

    // ---- Next episode (PL-10) -----------------------------------------------------------------

    private suspend fun maybeStartCountdown(position: Long, duration: Long) {
        val req = _state.value.request
        if (!req.playable.isEpisode || duration <= 0 || _state.value.countdown != null || countdownJob?.isActive == true) return
        val lang = profile?.language ?: "en-US"
        // Prefetch at 80%: resolve the next episode and warm its source search.
        if (position >= duration * 0.8 && nextPrefetched == null) {
            nextPrefetched = nextEpisodes.next(req.playable, lang)
            nextPrefetched?.let { (next, title) -> loadSourcesFor(next, title) { list -> _state.update { it.copy(episodes = it.episodes.copy(sources = list)) } } }
        }
        val remaining = duration - position
        if (remaining <= 10_000 || position >= duration * 0.95) {
            val (next, title) = nextPrefetched ?: nextEpisodes.next(req.playable, lang) ?: return
            val pack = req.source.release.isSeasonPack || req.source.release.isCompleteSeries
            val sameTorrent = pack && (req.source.release.season == null || req.source.release.season == next.season)
            countdownJob = viewModelScope.launch {
                for (s in 10 downTo 0) {
                    _state.update { it.copy(countdown = NextEpisodeCountdown(next, title, s, sameTorrent)) }
                    delay(1_000)
                }
                playNextNow()
            }
        }
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        _state.update { it.copy(countdown = null) }
    }

    /** "Next Episode": continue in the same season pack, else show the next episode's sources (PL-10). */
    fun playNextNow() {
        val c = _state.value.countdown ?: return
        countdownJob?.cancel()
        _state.update { it.copy(countdown = null) }
        val autoplay = profile?.autoplayNext ?: true
        if (c.sameTorrent && autoplay) {
            startEpisode(c.playable, c.title, _state.value.request.source, PlaybackEndReason.NEXT_EPISODE)
        } else {
            player.pause()
            _state.update { it.copy(panel = PlayerPanel.EPISODES) }
            viewModelScope.launch {
                selectPanelSeason(c.playable.season)
                val lang = profile?.language ?: "en-US"
                val ep = runCatching { titles.seasonOnce(c.playable.tmdbId, c.playable.season, lang).episodes.first { it.episodeNumber == c.playable.episode } }.getOrNull()
                ep?.let(::chooseEpisode)
            }
        }
    }

    // ---- Controls --------------------------------------------------------------------------------

    fun togglePlay() { if (player.isPlaying) player.pause() else player.play() }
    fun seekBy(deltaMs: Long) { player.seekTo((player.currentPosition + deltaMs).coerceIn(0, player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE)) }
    fun seekTo(positionMs: Long) = player.seekTo(positionMs)
    fun setSpeed(speed: Float) { player.setPlaybackSpeed(speed); _state.update { it.copy(speed = speed) } }

    fun toggleLike() = viewModelScope.launch {
        val p = profile ?: return@launch
        library.setLiked(p.id, _state.value.request.snapshot, !_state.value.liked)
    }

    fun retry() {
        val req = _state.value.request
        open(req, currentPositionForResume().takeIf { it > 0 } ?: req.startPositionMs)
    }

    // ---- Progress, sessions and history (RS-1…RS-3, HS-1) -------------------------------------

    private suspend fun tickLoop() {
        while (viewModelScope.isActive) {
            delay(1_000)
            val now = SystemClock.elapsedRealtime()
            if (player.isPlaying) {
                if (lastTickAt > 0) watchedMs += now - lastTickAt
                lastTickAt = now
            } else {
                lastTickAt = 0
            }
            val pos = player.currentPosition.coerceAtLeast(0)
            val dur = player.duration.takeIf { it > 0 } ?: 0
            _state.update { it.copy(positionMs = pos, durationMs = dur, bufferedMs = player.bufferedPosition) }
            stream?.let { files.reportPlayerBuffer(it.infoHash, (player.bufferedPosition - pos).coerceAtLeast(0)) }
            if (player.isPlaying && now - lastProgressSaveAt >= WatchRules.PROGRESS_TICK_MS) {
                lastProgressSaveAt = now
                saveProgress()
            }
            if (player.isPlaying) maybeStartCountdown(pos, dur)
        }
    }

    private suspend fun startSession(request: PlaybackRequest, startPositionMs: Long) {
        val p = profile ?: profiles.activeProfile.first().also { profile = it } ?: return
        watchedMs = 0
        sessionId = library.startSession(p.id, request.playable, request.episodeTitle, request.source.toRef(), startPositionMs, request.snapshot)
    }

    private fun saveProgress() {
        val p = profile ?: return
        val req = _state.value.request
        val pos = player.currentPosition.coerceAtLeast(0)
        val dur = player.duration.takeIf { it > 0 } ?: return
        library.saveProgressDetached(p.id, req.playable, pos, dur, req.source.toRef(), req.episodeTitle, sessionId, watchedMs)
    }

    private fun endSession(reason: PlaybackEndReason) {
        val id = sessionId ?: return
        sessionId = null
        val pos = player.currentPosition.coerceAtLeast(0)
        val dur = player.duration.takeIf { it > 0 } ?: 0
        val p = profile
        val req = _state.value.request
        // Final write runs on the application scope so it survives the screen closing.
        if (p != null && dur > 0) library.saveProgressDetached(p.id, req.playable, pos, dur, req.source.toRef(), req.episodeTitle)
        library.finishSession(id, pos, dur, watchedMs, reason)
    }

    private fun currentPositionForResume(): Long = player.currentPosition.coerceAtLeast(0)

    private fun onEnded() {
        _state.update { it.copy(phase = PlayerPhase.ENDED) }
        val req = _state.value.request
        if (req.playable.isEpisode && _state.value.countdown == null) {
            viewModelScope.launch { maybeStartCountdown(player.duration, player.duration) }
        } else if (!req.playable.isEpisode) {
            endSession(PlaybackEndReason.COMPLETED)
            _state.update { it.copy(finished = true) }
        }
    }

    private fun onPlaybackError(error: PlaybackException) {
        val decoder = error.errorCode in PlaybackException.ERROR_CODE_DECODER_INIT_FAILED..PlaybackException.ERROR_CODE_DECODING_RESOURCES_RECLAIMED
        val corrupt = error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED || error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED
        val source = _state.value.request.source
        if (corrupt) viewModelScope.launch { source.infoHash?.let { sourcesRepo.flagBad(it, "corrupt") } }
        endSession(PlaybackEndReason.ERROR)
        fail(
            PlayerError(
                when {
                    decoder -> "This device can't decode this video or audio format. Try a different source."
                    corrupt -> "This file looks broken. It has been flagged; try another source."
                    else -> "Playback failed: ${error.errorCodeName}"
                },
                canTryNext = true,
                suggestHideUnsupported = decoder,
            ),
        )
    }

    private fun fail(error: PlayerError) {
        _state.update { it.copy(phase = PlayerPhase.ERROR, error = error, panel = if (error.canTryNext) PlayerPanel.SOURCES else PlayerPanel.NONE) }
    }

    private fun releaseStream() {
        statsJob?.cancel()
        val s = stream ?: return
        stream = null
        player.stop()
        player.clearMediaItems()
        @Suppress("OPT_IN_USAGE")
        kotlinx.coroutines.GlobalScope.launch(NonCancellable) { runCatching { engine.release(s.infoHash) } }
    }

    /** Called when the user leaves the player. */
    fun exit() {
        endSession(if (_state.value.phase == PlayerPhase.ENDED) PlaybackEndReason.COMPLETED else PlaybackEndReason.USER_EXIT)
        releaseStream()
    }

    override fun onCleared() {
        player.removeListener(listener)
        if (sessionId != null) exit()
        holder.release()
        super.onCleared()
    }

    // ---- Online subtitles (OpenSubtitles, PL-8) ----------------------------------------------

    fun searchOnlineSubtitles() {
        val req = _state.value.request
        val imdb = req.snapshot.imdbId
        _state.update { it.copy(onlineSubtitles = it.onlineSubtitles.copy(searching = true, error = null)) }
        viewModelScope.launch {
            val langs = listOfNotNull(profile?.subtitleLanguage?.substringBefore('-'), profile?.language?.substringBefore('-'), "en").distinct()
            val result = runCatching {
                if (imdb == null) throw com.nexient.jazzy.core.data.subtitles.SubtitleException("This title has no IMDb ID, so subtitles can't be looked up.")
                subtitles.search(
                    imdb,
                    req.playable.season.takeIf { req.playable.isEpisode },
                    req.playable.episode.takeIf { req.playable.isEpisode },
                    langs,
                )
            }
            _state.update {
                it.copy(
                    onlineSubtitles = it.onlineSubtitles.copy(
                        searching = false,
                        results = result.getOrNull(),
                        error = result.exceptionOrNull()?.let { e -> e.message ?: "Subtitle search failed." },
                    ),
                )
            }
        }
    }

    fun applyOnlineSubtitle(sub: com.nexient.jazzy.core.data.subtitles.OnlineSubtitle) {
        _state.update { it.copy(onlineSubtitles = it.onlineSubtitles.copy(applying = sub.fileId, error = null)) }
        viewModelScope.launch {
            runCatching { subtitles.download(sub.fileId) }
                .onSuccess { file ->
                    subtitleFile = file
                    _state.update { it.copy(onlineSubtitles = it.onlineSubtitles.copy(applying = null, applied = sub, offsetMs = 0)) }
                    attachSubtitle(file, sub.language)
                }
                .onFailure { e -> _state.update { it.copy(onlineSubtitles = it.onlineSubtitles.copy(applying = null, error = e.message)) } }
        }
    }

    /** Sync offset control: positive delays subtitles, negative shows them earlier. */
    fun adjustSubtitleOffset(deltaMs: Long) {
        val file = subtitleFile ?: return
        val sub = _state.value.onlineSubtitles.applied ?: return
        val offset = _state.value.onlineSubtitles.offsetMs + deltaMs
        _state.update { it.copy(onlineSubtitles = it.onlineSubtitles.copy(offsetMs = offset)) }
        viewModelScope.launch {
            runCatching { subtitles.shifted(file, offset) }.onSuccess { attachSubtitle(it, sub.language) }
        }
    }

    private fun attachSubtitle(file: java.io.File, language: String) {
        val current = player.currentMediaItem ?: return
        val position = player.currentPosition
        val config = MediaItem.SubtitleConfiguration.Builder(android.net.Uri.fromFile(file))
            .setMimeType(androidx.media3.common.MimeTypes.APPLICATION_SUBRIP)
            .setLanguage(language)
            .setSelectionFlags(androidx.media3.common.C.SELECTION_FLAG_DEFAULT or androidx.media3.common.C.SELECTION_FLAG_FORCED)
            .setId("online:${file.name}")
            .build()
        val playWhenReady = player.playWhenReady
        player.setMediaItem(current.buildUpon().setSubtitleConfigurations(listOf(config)).build(), position)
        player.prepare()
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, false)
            .setPreferredTextLanguage(language)
            .build()
        player.playWhenReady = playWhenReady
    }

    /** "Open in external player" (spec §5.9.2): a tokenised 127.0.0.1 URL with Range support. Pauses our player. */
    fun externalPlayerUrl(): String? {
        val s = stream ?: return null
        player.pause()
        return runCatching { loopback.urlFor(s) }.getOrNull()
    }

    fun resumeLabel(): String = formatDuration(_state.value.positionMs)
}

private fun TorrentSource.toRef() = SourceRef(
    infoHash = infoHash,
    fileIndex = null,
    name = name,
    quality = release.qualityLabel,
    providerId = providerIds.firstOrNull(),
)
