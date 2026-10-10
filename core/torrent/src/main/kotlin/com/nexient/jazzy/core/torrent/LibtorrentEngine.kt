package com.nexient.jazzy.core.torrent

import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import com.nexient.jazzy.core.model.EngineSettings
import com.nexient.jazzy.core.model.StreamRequest
import com.nexient.jazzy.core.model.StreamState
import com.nexient.jazzy.core.model.StreamStats
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.libtorrent4j.AddTorrentParams
import org.libtorrent4j.AlertListener
import org.libtorrent4j.SessionHandle
import org.libtorrent4j.SessionManager
import org.libtorrent4j.SessionParams
import org.libtorrent4j.SettingsPack
import org.libtorrent4j.Sha1Hash
import org.libtorrent4j.TorrentFlags
import org.libtorrent4j.TorrentHandle
import org.libtorrent4j.TorrentInfo
import org.libtorrent4j.Vectors
import org.libtorrent4j.alerts.Alert
import org.libtorrent4j.alerts.AlertType
import org.libtorrent4j.alerts.CacheFlushedAlert
import org.libtorrent4j.alerts.FileErrorAlert
import org.libtorrent4j.alerts.FilePrioAlert
import org.libtorrent4j.alerts.MetadataReceivedAlert
import org.libtorrent4j.alerts.PieceFinishedAlert
import org.libtorrent4j.alerts.SaveResumeDataAlert
import org.libtorrent4j.alerts.TorrentAlert
import org.libtorrent4j.alerts.TorrentCheckedAlert
import org.libtorrent4j.alerts.TorrentErrorAlert
import org.libtorrent4j.swig.alert
import org.libtorrent4j.swig.error_code
import org.libtorrent4j.swig.libtorrent
import org.libtorrent4j.swig.settings_pack
import java.io.File
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [TorrentEngine] on libtorrent (libtorrent4j). See spec §5.9.1.
 *
 * Threading: every native call runs on [Dispatchers.IO] or on libtorrent4j's alert thread;
 * nothing touches the main thread. Alert callbacks only copy data out and signal waiters.
 */
@Singleton
class LibtorrentEngine @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsStore: EngineSettingsStore,
    private val trackerStore: Optional<TrackerListStore>,
) : TorrentEngine, TorrentFileProvider {

    /** TE-2: metadata timeout. */
    var metadataTimeoutMs: Long = 30_000

    /** The session stops this long after the last stream is released (unless seeding). */
    var idleSessionStopMs: Long = 60_000

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("JazzyTorrentEngine"))
    private val cache = TorrentCache(File(context.cacheDir, "torrents"))
    private val sessionStateFile = File(context.noBackupFilesDir, "jazzy-torrent-session.dat")

    private val sessionLock = Mutex()
    @Volatile private var session: SessionManager? = null
    private var settingsJob: Job? = null
    private var idleStopJob: Job? = null
    private val opening = AtomicInteger(0)

    private val streams = MutableStateFlow<Map<String, ActiveStream>>(emptyMap())
    private val seeding = ConcurrentHashMap<String, TorrentHandle>()
    private val resumeWaiters = ConcurrentHashMap<String, CompletableDeferred<Unit>>()

    // region TorrentEngine

    override suspend fun open(request: StreamRequest): TorrentStream = withContext(Dispatchers.IO) {
        opening.incrementAndGet()
        try {
            openInternal(request)
        } finally {
            opening.decrementAndGet()
        }
    }

    override fun stats(infoHash: String): Flow<StreamStats> = flow {
        val key = Magnets.normalizeInfoHash(infoHash)
        val stream = streams.mapNotNull { it[key] }.first()
        emitAll(stream.stats.takeWhile { it != null }.filterNotNull())
    }

    override suspend fun release(infoHash: String, deleteData: Boolean) = withContext(Dispatchers.IO + NonCancellable) {
        val hash = Magnets.normalizeInfoHash(infoHash)
        val settings = currentSettings()
        var stream: ActiveStream? = null
        streams.update { stream = it[hash]; it - hash }
        val released = stream
        if (released != null) {
            released.close()
            val handle = released.handle?.takeIf { it.isValid }
            when {
                handle == null -> if (deleteData) cache.delete(hash)
                deleteData -> removeFromSession(handle, deleteFiles = true).also { cache.delete(hash) }
                else -> {
                    saveResumeData(hash, handle)
                    if (settings.stopSeedingOnStop) removeFromSession(handle, deleteFiles = false) else seeding[hash] = handle
                    cache.touch(hash)
                }
            }
        } else {
            seeding.remove(hash)?.let { removeFromSession(it, deleteFiles = deleteData) }
            if (deleteData) cache.delete(hash)
        }
        evictIfNeeded(settings)
        scheduleIdleStop()
    }

    override suspend fun cacheSizeBytes(): Long = withContext(Dispatchers.IO) { cache.sizeBytes() }

    /** Deletes every cached torrent that isn't currently streaming (seeding ones are stopped first). */
    override suspend fun clearCache() = withContext(Dispatchers.IO) {
        val active = streams.value.keys
        seeding.keys.toList().forEach { hash ->
            seeding.remove(hash)?.let { removeFromSession(it, deleteFiles = true) }
        }
        cache.entries().filter { it.infoHash !in active }.forEach { cache.delete(it.infoHash) }
        scheduleIdleStop()
    }

    // endregion

    // region TorrentFileProvider

    override fun openFile(infoHash: String, fileIndex: Int): TorrentFileAccess? {
        val stream = streams.value[Magnets.normalizeInfoHash(infoHash)] ?: return null
        val selection = stream.selection?.takeIf { it.fileIndex == fileIndex } ?: return null
        return StreamFileAccess(stream, selection)
    }

    override fun reportPlayerBuffer(infoHash: String, bufferedAheadMs: Long) {
        streams.value[Magnets.normalizeInfoHash(infoHash)]?.playerBufferMs = bufferedAheadMs.coerceAtLeast(0)
    }

    override fun setDurationHint(infoHash: String, durationMs: Long) {
        val stream = streams.value[Magnets.normalizeInfoHash(infoHash)] ?: return
        stream.durationHintMs = durationMs.takeIf { it > 0 }
        stream.selection?.let { it.scheduler.bitrateBps = bitrateOf(stream, it) }
    }

    // endregion

    private suspend fun openInternal(request: StreamRequest): TorrentStream {
        val settings = currentSettings()
        if (settings.wifiOnly && isActiveNetworkMetered()) {
            throw StreamException(StreamFailure.NETWORK_NOT_ALLOWED, "Wi-Fi only is on and the active network is metered")
        }
        val magnetParams = try {
            AddTorrentParams.parseMagnetUri(request.magnet)
        } catch (e: IllegalArgumentException) {
            throw StreamException(StreamFailure.ENGINE_ERROR, "Invalid magnet: ${e.message}", e)
        }
        val hash = magnetParams.infoHashes.best.toHex().lowercase()
        idleStopJob?.cancel()
        evictIfNeeded(settings)
        val sm = ensureSession(settings)

        val fresh = ActiveStream(hash, request)
        val stream = streams.updateAndGet { if (hash in it) it else it + (hash to fresh) }.getValue(hash)
        val created = stream === fresh
        stream.request = request
        try {
            val handle = stream.handle?.takeIf { it.isValid }
                ?: addTorrent(sm, stream, magnetParams).also { stream.handle = it }
            if (stream.ticker == null) stream.ticker = scope.launch { tickLoop(stream) }

            val metadataStart = System.currentTimeMillis()
            if (!handle.status(true).hasMetadata()) awaitMetadata(stream, handle)
            Log.i(TAG, "metadata for $hash ready in ${System.currentTimeMillis() - metadataStart} ms")
            if (stream.closed) throw StreamException(StreamFailure.ENGINE_ERROR, "Stream released while opening")

            val info = handle.torrentFile() ?: throw StreamException(StreamFailure.ENGINE_ERROR, "Metadata missing after metadata alert")
            stream.torrentName = info.name()
            val chosen = chooseFile(info, request)
            val selection = applySelection(stream, handle, info, chosen)
            // Persist the info dict so reopening skips the metadata fetch.
            handle.saveResumeData(TorrentHandle.SAVE_INFO_DICT)
            return StreamHandleImpl(
                infoHash = hash,
                torrentName = info.name(),
                fileIndex = chosen.index,
                fileName = chosen.path.substringAfterLast('/'),
                fileSize = chosen.size,
                mimeType = selection.mimeType,
                uri = streamUri(hash, chosen.index),
            )
        } catch (t: Throwable) {
            if (created) {
                withContext(NonCancellable) {
                    val deleteData = t is StreamException
                    streams.update { it - hash }
                    stream.close()
                    stream.handle?.takeIf { it.isValid }?.let { removeFromSession(it, deleteFiles = deleteData) }
                    if (deleteData && !cache.resumeFile(hash).exists()) cache.delete(hash)
                    scheduleIdleStop()
                }
            }
            throw when (t) {
                is StreamException, is CancellationException -> t
                else -> StreamException(StreamFailure.ENGINE_ERROR, t.message, t)
            }
        }
    }

    private suspend fun awaitMetadata(stream: ActiveStream, handle: TorrentHandle) {
        val received = withTimeoutOrNull(metadataTimeoutMs) {
            while (!handle.status(true).hasMetadata()) {
                // The alert normally wakes us; polling covers a dropped alert.
                withTimeoutOrNull(METADATA_POLL_MS) { stream.metadata.await() }
            }
        }
        if (received == null) {
            throw StreamException(StreamFailure.METADATA_TIMEOUT, "No metadata within $metadataTimeoutMs ms")
        }
    }

    private fun chooseFile(info: TorrentInfo, request: StreamRequest): TorrentFileEntry {
        val files = info.files()
        val entries = (0 until files.numFiles())
            .filterNot { files.padFileAt(it) }
            .map { TorrentFileEntry(it, files.filePath(it), files.fileSize(it)) }
        val chosen = FileSelector.select(entries, request.season, request.episode)
            ?: throw StreamException(StreamFailure.NO_VIDEO_FILE, "No matching video file in ${info.name()}")
        val isEpisode = request.season != null && request.episode != null
        if (!TitleVerifier.matches(request.expectedTitle, request.expectedYear, info.name(), chosen.path, isEpisode)) {
            throw StreamException(
                StreamFailure.TITLE_MISMATCH,
                "\"${info.name()}\" / \"${chosen.path}\" doesn't look like \"${request.expectedTitle}\"",
            )
        }
        return chosen
    }

    private suspend fun applySelection(stream: ActiveStream, handle: TorrentHandle, info: TorrentInfo, chosen: TorrentFileEntry): Selection {
        val files = info.files()
        stream.filePriorities = CompletableDeferred()
        handle.prioritizeFiles(
            Array(files.numFiles()) { if (it == chosen.index) org.libtorrent4j.Priority.DEFAULT else org.libtorrent4j.Priority.IGNORE },
        )
        val span = FileSpan(info.pieceLength(), files.fileOffset(chosen.index), chosen.size, info.totalSize())
        val hasV1 = info.hasV1()
        stream.syncHave(handle.status(TorrentHandle.QUERY_PIECES).pieces(), span)
        val scheduler = PieceScheduler(
            span = span,
            controller = HandleController(handle),
            have = stream::hasPiece,
            bitrateBps = BitrateEstimator.estimate(chosen.size, stream.durationHintMs, stream.isEpisode),
        )
        val selection = Selection(
            fileIndex = chosen.index,
            path = chosen.path,
            file = File(handle.savePath(), chosen.path),
            span = span,
            scheduler = scheduler,
            mimeType = FileSelector.mimeTypeFor(chosen.path),
            pieceHash = { piece ->
                if (hasV1) Vectors.byte_vector2bytes(info.hashForPiece(piece).swig().to_bytes()) else null
            },
        )
        // libtorrent applies file priorities asynchronously and then re-derives piece priorities;
        // set ours after that (the ticker re-asserts them as well).
        withTimeoutOrNull(FILE_PRIORITY_WAIT_MS) { stream.filePriorities.await() }
        stream.selection = selection
        scheduler.start()
        Log.i(TAG, "selected ${chosen.path} (${chosen.size} B, piece ${span.pieceLength} B, pieces ${span.firstPiece}..${span.lastPiece})")
        return selection
    }

    private suspend fun addTorrent(sm: SessionManager, stream: ActiveStream, magnetParams: AddTorrentParams): TorrentHandle {
        val hash = stream.infoHash
        stream.webSeeds = magnetParams.urlSeeds.orEmpty()
        sm.find(Sha1Hash.parseHex(hash))?.takeIf { it.isValid }?.let { existing ->
            seeding.remove(hash)
            existing.urlSeeds().orEmpty().forEach(existing::removeUrlSeed)
            return existing
        }
        val dir = cache.dirFor(hash).apply { mkdirs() }
        cache.touch(hash)
        val params = loadResumeData(hash) ?: magnetParams
        params.savePath = dir.absolutePath
        params.trackers = Magnets.mergeTrackers(params.trackers.orEmpty(), currentTrackers())
        // Web seeds (BEP 19) are held back: libtorrent treats one as a peer, and a slow HTTP
        // server handed a time-critical piece stalls playback for its whole urlseed_timeout.
        // They are added back only when the swarm can't feed the stream (see tick()).
        stream.webSeeds = (stream.webSeeds + params.urlSeeds.orEmpty()).distinct()
        params.urlSeeds = emptyList()
        params.flags = params.flags
            .and_(TorrentFlags.AUTO_MANAGED.inv())
            .and_(TorrentFlags.PAUSED.inv())
            .and_(TorrentFlags.UPLOAD_MODE.inv())
        val ec = error_code()
        val handle = sm.swig().add_torrent(params.swig(), ec)
        if (ec.value() != 0 || handle == null) {
            throw StreamException(StreamFailure.ENGINE_ERROR, "add_torrent failed: ${ec.message()}")
        }
        return TorrentHandle(handle)
    }

    // region session lifecycle

    private suspend fun ensureSession(settings: EngineSettings): SessionManager = sessionLock.withLock {
        session?.takeIf { it.isRunning }?.let { return it }
        val sm = SessionManager(false)
        sm.addListener(alertListener)
        val saved = runCatching { sessionStateFile.takeIf { it.isFile }?.readBytes() }.getOrNull()
        val params = saved?.let { runCatching { SessionParams(it) }.getOrNull() } ?: SessionParams()
        params.settings = buildSettings(settings)
        sm.start(params)
        // SessionManager.start() forces "all categories but logs"; drop the per-block and upload
        // alerts we don't need (thousands per second at full speed).
        sm.applySettings(SettingsPack().setInteger(settings_pack.int_types.alert_mask.swigValue(), ALERT_MASK.to_int()))
        session = sm
        settingsJob?.cancel()
        settingsJob = scope.launch {
            settingsStore.settings.drop(1).distinctUntilChanged().collect { s ->
                session?.takeIf { it.isRunning }?.applySettings(buildSettings(s))
            }
        }
        Log.i(TAG, "session started (resumed state: ${saved != null})")
        sm
    }

    private fun scheduleIdleStop() {
        if (streams.value.isNotEmpty() || seeding.isNotEmpty() || opening.get() > 0) return
        idleStopJob?.cancel()
        idleStopJob = scope.launch {
            delay(idleSessionStopMs)
            sessionLock.withLock {
                if (streams.value.isEmpty() && seeding.isEmpty() && opening.get() == 0) stopSessionLocked()
            }
        }
    }

    /** Stops the session now (used by tests and app shutdown). */
    suspend fun shutdown() = withContext(Dispatchers.IO + NonCancellable) {
        streams.value.keys.forEach { release(it) }
        sessionLock.withLock { stopSessionLocked() }
    }

    private fun stopSessionLocked() {
        val sm = session ?: return
        session = null
        settingsJob?.cancel()
        runCatching {
            val state = sm.saveState()
            if (state != null) {
                sessionStateFile.parentFile?.mkdirs()
                sessionStateFile.writeBytes(state)
            }
        }.onFailure { Log.w(TAG, "saving session state failed", it) }
        sm.removeListener(alertListener)
        sm.stop()
        Log.i(TAG, "session stopped")
    }

    private fun buildSettings(settings: EngineSettings): SettingsPack {
        val sp = SettingsPack()
        sp.connectionsLimit(settings.maxConnections.coerceAtLeast(10))
        // libtorrent treats 0 as "unlimited"; the app means "don't upload", so use 1 B/s.
        sp.uploadRateLimit(if (settings.uploadLimitBps <= 0) 1 else settings.uploadLimitBps.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
        sp.setEnableDht(true)
        sp.setEnableLsd(true)
        sp.setBoolean(settings_pack.bool_types.enable_upnp.swigValue(), true)
        sp.setBoolean(settings_pack.bool_types.enable_natpmp.swigValue(), true)
        if (settings.listenPort in 1..65535) {
            sp.listenInterfaces("0.0.0.0:${settings.listenPort},[::]:${settings.listenPort}")
        }
        val enabled = settings_pack.enc_policy.pe_enabled.swigValue()
        sp.setInteger(settings_pack.int_types.out_enc_policy.swigValue(), enabled)
        sp.setInteger(settings_pack.int_types.in_enc_policy.swigValue(), enabled)
        sp.setInteger(
            settings_pack.int_types.allowed_enc_level.swigValue(),
            (if (settings.preferEncryption) settings_pack.enc_level.pe_rc4 else settings_pack.enc_level.pe_both).swigValue(),
        )
        sp.setBoolean(settings_pack.bool_types.prefer_rc4.swigValue(), settings.preferEncryption)
        sp.alertQueueSize(ALERT_QUEUE_SIZE)
        // Streaming: no torrent is queued behind another.
        sp.activeDownloads(-1)
        sp.activeLimit(-1)
        applyStreamingTuning(sp)
        return sp
    }

    /**
     * Latency over throughput (spike findings, DEVELOPMENT_PLAN §10.5 option 1):
     * - announce to every tracker and tier at once; by default libtorrent tries tiers one by one,
     *   so a dead first tracker in a magnet costs a full UDP timeout before peers (and metadata) arrive;
     * - shorter per-peer request queues and block timeouts: libtorrent's time-critical picker
     *   skips peers whose queue holds ≥ 2 s of requests, and a seek waits behind whatever is queued;
     * - connect to peers faster while the torrent starts.
     */
    private fun applyStreamingTuning(sp: SettingsPack) {
        fun bool(key: settings_pack.bool_types, value: Boolean) = sp.setBoolean(key.swigValue(), value)
        fun int(key: settings_pack.int_types, value: Int) = sp.setInteger(key.swigValue(), value)
        bool(settings_pack.bool_types.announce_to_all_trackers, true)
        bool(settings_pack.bool_types.announce_to_all_tiers, true)
        bool(settings_pack.bool_types.strict_end_game_mode, false)
        bool(settings_pack.bool_types.smooth_connects, false)
        int(settings_pack.int_types.tracker_receive_timeout, 5)
        int(settings_pack.int_types.tracker_completion_timeout, 15)
        int(settings_pack.int_types.request_queue_time, 1)
        int(settings_pack.int_types.request_timeout, 10)
        int(settings_pack.int_types.piece_timeout, 5)
        int(settings_pack.int_types.connection_speed, 100)
        int(settings_pack.int_types.torrent_connect_boost, 80)
        int(settings_pack.int_types.min_reconnect_time, 20)
    }

    private suspend fun currentSettings(): EngineSettings = settingsStore.settings.first()

    private suspend fun currentTrackers(): List<String> =
        if (trackerStore.isPresent) trackerStore.get().trackers.first() else DefaultTrackers.LIST

    private fun isActiveNetworkMetered(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        return cm.isActiveNetworkMetered
    }

    // endregion

    // region stats, eviction, resume data

    private suspend fun tickLoop(stream: ActiveStream) {
        var tick = 0
        while (scope.isActive && !stream.closed) {
            runCatching { tick(stream, tick++) }.onFailure { Log.w(TAG, "stats tick failed for ${stream.infoHash}", it) }
            delay(TICK_MS)
        }
    }

    private suspend fun tick(stream: ActiveStream, tick: Int) {
        val handle = stream.handle?.takeIf { it.isValid } ?: return
        val status = handle.status(TorrentHandle.QUERY_PIECES)
        val rate = status.downloadPayloadRate().toLong()
        stream.downloadRateBps = rate
        val selection = stream.selection
        if (selection == null) {
            if (stream.closed) return
            stream.stats.value = StreamStats(
                stream.infoHash, StreamState.FETCHING_METADATA, rate, status.uploadPayloadRate().toLong(),
                status.numPeers(), status.numSeeds(), 0f, 0,
            )
            return
        }
        stream.syncHave(status.pieces(), selection.span)
        maybeEnableWebSeeds(stream, handle, status.numPeers())
        val bitrate = bitrateOf(stream, selection)
        selection.scheduler.bitrateBps = bitrate
        selection.scheduler.refresh(rate)
        if (tick % REASSERT_EVERY_TICKS == 1) selection.scheduler.reassert()

        val span = selection.span
        val position = stream.lastReadPosition.coerceIn(0, span.fileSize)
        val engineAhead = stream.contiguousEnd(span, position) - position
        val playerBufferMs = stream.playerBufferMs
        val buffered = BufferHealth.bufferedAheadBytes(engineAhead, playerBufferMs, bitrate)
        val now = SystemClock.elapsedRealtime()
        val waitingMs = stream.waitingSinceMs.let { if (it < 0) 0 else now - it }
        val effective = BufferHealth.effectiveBufferBytes(buffered, playerBufferMs, waitingMs)
        val downloaded = stream.downloadedBytes(span)
        val complete = downloaded >= span.fileSize
        val stalled = stream.stall.update(now, rate, bitrate, effective, complete)
        val state = when {
            stream.error != null -> StreamState.ERROR
            complete -> StreamState.FINISHED
            stalled -> StreamState.STALLED
            BufferHealth.isHealthy(effective, bitrate, reachedEndOfFile = position + engineAhead >= span.fileSize) -> StreamState.STREAMING
            else -> StreamState.BUFFERING
        }
        if (stream.closed) return
        stream.stats.value = StreamStats(
            infoHash = stream.infoHash,
            state = state,
            downloadRateBps = rate,
            uploadRateBps = status.uploadPayloadRate().toLong(),
            peers = status.numPeers(),
            seeds = status.numSeeds(),
            fileProgress = (downloaded.toDouble() / span.fileSize).toFloat().coerceIn(0f, 1f),
            bufferedAheadBytes = buffered,
        )
        if (tick > 0 && tick % EVICT_EVERY_TICKS == 0) evictIfNeeded(currentSettings())
    }

    /** Falls back to the magnet's web seeds when fewer than [MIN_PEERS_WITHOUT_WEB_SEEDS] peers stay connected. */
    private fun maybeEnableWebSeeds(stream: ActiveStream, handle: TorrentHandle, peers: Int) {
        if (stream.webSeedsEnabled || stream.webSeeds.isEmpty()) return
        val now = SystemClock.elapsedRealtime()
        if (peers >= MIN_PEERS_WITHOUT_WEB_SEEDS) {
            stream.fewPeersSinceMs = -1
            return
        }
        if (stream.fewPeersSinceMs < 0) stream.fewPeersSinceMs = now
        if (now - stream.fewPeersSinceMs >= WEB_SEED_FALLBACK_MS) {
            stream.webSeedsEnabled = true
            stream.webSeeds.forEach(handle::addUrlSeed)
            Log.i(TAG, "only $peers peers for ${stream.infoHash}: enabling ${stream.webSeeds.size} web seed(s)")
        }
    }

    private fun bitrateOf(stream: ActiveStream, selection: Selection): Long =
        BitrateEstimator.estimate(selection.span.fileSize, stream.durationHintMs, stream.isEpisode)

    private fun evictIfNeeded(settings: EngineSettings) {
        val victims = CacheEvictionPolicy.selectVictims(
            entries = cache.entries(),
            active = streams.value.keys,
            capBytes = settings.cacheCapBytes,
            freeBytes = cache.freeBytes(),
        )
        victims.forEach { hash ->
            seeding.remove(hash)?.let { removeFromSession(it, deleteFiles = true) }
            cache.delete(hash)
            Log.i(TAG, "evicted $hash from the torrent cache")
        }
    }

    private suspend fun saveResumeData(hash: String, handle: TorrentHandle) {
        val waiter = CompletableDeferred<Unit>()
        resumeWaiters[hash] = waiter
        runCatching { handle.saveResumeData(TorrentHandle.SAVE_INFO_DICT) }
        withTimeoutOrNull(RESUME_WAIT_MS) { waiter.await() }
        resumeWaiters.remove(hash)
    }

    private fun loadResumeData(hash: String): AddTorrentParams? {
        val file = cache.resumeFile(hash).takeIf { it.isFile } ?: return null
        return runCatching {
            val ec = error_code()
            val params = libtorrent.read_resume_data_ex(Vectors.bytes2byte_vector(file.readBytes()), ec)
            if (ec.value() != 0) null else AddTorrentParams(params)
        }.getOrNull()?.takeIf { it.infoHashes.best.toHex().equals(hash, ignoreCase = true) }
    }

    private fun removeFromSession(handle: TorrentHandle, deleteFiles: Boolean) {
        val sm = session ?: return
        runCatching {
            if (deleteFiles) sm.remove(handle, SessionHandle.DELETE_FILES) else sm.remove(handle)
        }.onFailure { Log.w(TAG, "remove failed", it) }
    }

    // endregion

    // region alerts (libtorrent4j's alert thread: copy data out, never block)

    private val alertListener = object : AlertListener {
        override fun types(): IntArray = LISTENED_ALERTS

        override fun alert(alert: Alert<*>) {
            try {
                val torrentAlert = alert as? TorrentAlert<*> ?: return
                val hash = torrentAlert.handle().infoHash().toHex()
                val stream = streams.value[hash]
                when (alert) {
                    is PieceFinishedAlert -> stream?.markHave(alert.pieceIndex())
                    is MetadataReceivedAlert -> stream?.metadata?.complete(Unit)
                    is FilePrioAlert -> stream?.filePriorities?.complete(Unit)
                    is TorrentCheckedAlert -> stream?.wake()
                    is CacheFlushedAlert -> stream?.onCacheFlushed()
                    is TorrentErrorAlert -> {
                        Log.w(TAG, "torrent error $hash: ${alert.error().message} (${alert.filename()})")
                        stream?.error = alert.error().message
                        stream?.wake()
                    }
                    is FileErrorAlert -> {
                        Log.w(TAG, "file error $hash: ${alert.error().message} (${alert.filename()})")
                        stream?.error = alert.error().message
                        stream?.wake()
                    }
                    is SaveResumeDataAlert -> {
                        // The alert is only valid inside this callback: serialise now, write later.
                        val bytes = AddTorrentParams.writeResumeDataBuf(alert.params())
                        scope.launch {
                            runCatching { writeAtomically(cache.resumeFile(hash), bytes) }
                                .onFailure { Log.w(TAG, "writing resume data failed", it) }
                            resumeWaiters[hash]?.complete(Unit)
                        }
                    }
                    else -> if (alert.type() == AlertType.SAVE_RESUME_DATA_FAILED) resumeWaiters[hash]?.complete(Unit)
                }
            } catch (t: Throwable) {
                Log.w(TAG, "alert handling failed for ${alert.type()}", t)
            }
        }
    }

    // endregion

    private class StreamHandleImpl(
        override val infoHash: String,
        override val torrentName: String,
        override val fileIndex: Int,
        override val fileName: String,
        override val fileSize: Long,
        override val mimeType: String?,
        override val uri: Uri,
    ) : TorrentStream {
        override fun toString() = "TorrentStream($infoHash/$fileIndex $fileName, $fileSize B)"
    }

    companion object {
        const val TAG = "JazzyTorrent"
        private const val TICK_MS = 1_000L
        private const val METADATA_POLL_MS = 1_000L
        private const val FILE_PRIORITY_WAIT_MS = 1_000L
        private const val RESUME_WAIT_MS = 3_000L
        private const val REASSERT_EVERY_TICKS = 5
        private const val EVICT_EVERY_TICKS = 60
        private const val ALERT_QUEUE_SIZE = 10_000
        private const val MIN_PEERS_WITHOUT_WEB_SEEDS = 3
        private const val WEB_SEED_FALLBACK_MS = 10_000L

        // Lazy: these touch SWIG classes, whose static init loads the native library.
        private val LISTENED_ALERTS by lazy(LazyThreadSafetyMode.PUBLICATION) {
            intArrayOf(
                AlertType.PIECE_FINISHED.swig(),
                AlertType.METADATA_RECEIVED.swig(),
                AlertType.FILE_PRIO.swig(),
                AlertType.TORRENT_CHECKED.swig(),
                AlertType.CACHE_FLUSHED.swig(),
                AlertType.TORRENT_ERROR.swig(),
                AlertType.FILE_ERROR.swig(),
                AlertType.SAVE_RESUME_DATA.swig(),
                AlertType.SAVE_RESUME_DATA_FAILED.swig(),
            )
        }

        private val ALERT_MASK by lazy(LazyThreadSafetyMode.PUBLICATION) {
            alert.all_categories.and_(
            alert.block_progress_notification
                .or_(alert.upload_notification)
                .or_(alert.incoming_request_notification)
                .or_(alert.session_log_notification)
                .or_(alert.torrent_log_notification)
                .or_(alert.peer_log_notification)
                .or_(alert.dht_log_notification)
                .or_(alert.port_mapping_log_notification)
                .or_(alert.picker_log_notification)
                .inv(),
            )
        }

        fun streamUri(infoHash: String, fileIndex: Int): Uri =
            Uri.Builder().scheme(TORRENT_URI_SCHEME).authority(infoHash).appendPath(fileIndex.toString()).build()

        private fun writeAtomically(target: File, bytes: ByteArray) {
            target.parentFile?.mkdirs()
            val tmp = File(target.parentFile, target.name + ".tmp")
            tmp.writeBytes(bytes)
            if (!tmp.renameTo(target)) {
                target.delete()
                tmp.renameTo(target)
            }
        }
    }
}
