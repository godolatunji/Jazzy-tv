package com.nexient.jazzy.baselineprofile

import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Details → first source row (plan E16-02, spec §9: ≤ 3 s).
 *
 * `SourcesFirstRowFirstMs` is the app's async trace section from the tap (Details pushed) to the first
 * SourceRow composed. The UiAutomator wait (tap → row visible in the accessibility tree) is also
 * logged under tag `JazzyBench` and reported as instrumentation status `details_first_row_ms_*`, as a
 * cross-check that doesn't depend on the app's trace points. On phones it includes one short scroll
 * (the Sources list starts below the fold), so it reads a little higher than the trace section.
 *
 * Source search hits live sites, so this number includes network time and varies with the network.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class DetailsBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test fun detailsFirstSourceRowNone() = details(CompilationMode.None())

    @Test fun detailsFirstSourceRowBaselineProfile() = details(CompilationMode.Partial(BaselineProfileMode.Require))

    @OptIn(ExperimentalMetricApi::class)
    private fun details(compilation: CompilationMode) {
        var prepared = false
        val waits = mutableListOf<Long>()
        rule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(
                TraceSectionMetric("SourcesFirstRow", TraceSectionMetric.Mode.First),
                FrameTimingMetric(),
            ),
            compilationMode = compilation,
            startupMode = StartupMode.WARM,
            iterations = ITERATIONS,
            setupBlock = {
                if (!prepared) {
                    ensureStartsAtHome()
                    prepared = true
                }
                pressHome()
                startActivityAndWait()
                waitForHomeContent()
                selectMoviesTab()
            },
        ) {
            val start = SystemClock.uptimeMillis()
            check(openFirstTitle()) { "No title to open on Home" }
            val found = waitForFirstSourceRow()
            val ms = SystemClock.uptimeMillis() - start
            if (iteration != null) {
                if (found) waits += ms
                Log.i(TAG, "Details → first source row: ${if (found) "$ms ms" else "none within timeout"}")
            }
            scrollDetails()
        }
        report(waits)
    }

    private fun report(waits: List<Long>) {
        if (waits.isEmpty()) return
        val sorted = waits.sorted()
        val p50 = sorted[sorted.size / 2]
        val p90 = sorted[((sorted.size - 1) * 9) / 10]
        Log.i(TAG, "Details → first source row (UiAutomator): n=${sorted.size} min=${sorted.first()} p50=$p50 p90=$p90 max=${sorted.last()} ms")
        InstrumentationRegistry.getInstrumentation().sendStatus(
            0,
            Bundle().apply {
                putLong("details_first_row_ms_p50", p50)
                putLong("details_first_row_ms_p90", p90)
                putInt("details_first_row_n", sorted.size)
            },
        )
    }

    companion object {
        const val ITERATIONS = 5
    }
}
