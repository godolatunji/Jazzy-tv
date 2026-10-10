package com.nexient.jazzy.perf

import android.content.Context
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.tracing.Trace
import com.nexient.jazzy.core.player.PlayerHolder
import com.nexient.jazzy.feature.home.HomeViewModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Trace sections read by the macrobenchmarks in `:baselineprofile` (plan E16-02, spec §9).
 * All three are async sections (begin and end can be on different frames/threads), so
 * `TraceSectionMetric(name, Mode.First)` reports the user-visible latency:
 *
 * - [HOME_FIRST_RAIL]: MainActivity.onCreate → Home has its rails and hero (cold start target ≤ 1.5 s).
 * - [SOURCES_FIRST_ROW]: tap on a title (Details pushed) → first source row composed (target ≤ 3 s).
 *   Ended in `feature/details` DetailsRoute.
 * - [PLAYER_FIRST_FRAME]: Player pushed → ExoPlayer renders its first frame (TTFF, target ≤ 15 s p50).
 *
 * Sections cost nothing unless a trace is being recorded.
 */
object PerfTrace {
    const val HOME_FIRST_RAIL = "HomeFirstRail"
    const val SOURCES_FIRST_ROW = "SourcesFirstRow"
    const val PLAYER_FIRST_FRAME = "PlayerFirstFrame"

    /** One cookie per section name is enough: only one of each is in flight at a time. */
    const val COOKIE = 0

    fun begin(name: String) = Trace.beginAsyncSection(name, COOKIE)

    fun end(name: String) = Trace.endAsyncSection(name, COOKIE)
}

/**
 * Ends [PerfTrace.HOME_FIRST_RAIL] and reports the activity fully drawn (TTFD in
 * `StartupTimingMetric`) once Home has rails and a hero, or is offline with what the cache has.
 */
@Composable
fun HomeReadyMarker(viewModel: HomeViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ready = state.rails.isNotEmpty() && (state.hero != null || state.offline)
    ReportDrawnWhen { ready }
    if (ready) LaunchedEffect(Unit) { PerfTrace.end(PerfTrace.HOME_FIRST_RAIL) }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface PerfEntryPoint {
    fun playerHolder(): PlayerHolder
}

/** Ends [PerfTrace.PLAYER_FIRST_FRAME] on the shared ExoPlayer's first rendered frame. */
@Composable
fun PlayerFirstFrameMarker() {
    val context: Context = LocalContext.current.applicationContext
    DisposableEffect(Unit) {
        val player = EntryPointAccessors.fromApplication(context, PerfEntryPoint::class.java).playerHolder().get()
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                PerfTrace.end(PerfTrace.PLAYER_FIRST_FRAME)
                player.removeListener(this)
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
}
