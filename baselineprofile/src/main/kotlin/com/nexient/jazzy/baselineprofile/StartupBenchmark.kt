package com.nexient.jazzy.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Startup (plan E16-02, spec §9: cold start → Home ≤ 1.5 s from cache on a mid-range phone).
 *
 * Compares no AOT compilation with the shipped Baseline Profile. Metrics:
 * - `timeToInitialDisplayMs`: first frame.
 * - `timeToFullDisplayMs`: Home has rails + hero (the app calls reportFullyDrawn via ReportDrawnWhen).
 * - `HomeFirstRailFirstMs`: MainActivity.onCreate → Home ready (app trace section).
 *
 * Before measuring, the first setup makes sure a launch lands on Home (profile remembered) and the
 * TMDB cache is warm, so this is the "from cache" number.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test fun startupColdNone() = startup(StartupMode.COLD, CompilationMode.None())

    @Test fun startupColdBaselineProfile() = startup(StartupMode.COLD, CompilationMode.Partial(BaselineProfileMode.Require))

    @Test fun startupWarmNone() = startup(StartupMode.WARM, CompilationMode.None())

    @Test fun startupWarmBaselineProfile() = startup(StartupMode.WARM, CompilationMode.Partial(BaselineProfileMode.Require))

    @OptIn(ExperimentalMetricApi::class)
    private fun startup(mode: StartupMode, compilation: CompilationMode) {
        var prepared = false
        rule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(
                StartupTimingMetric(),
                TraceSectionMetric("HomeFirstRail", TraceSectionMetric.Mode.First),
            ),
            compilationMode = compilation,
            startupMode = mode,
            iterations = ITERATIONS,
            setupBlock = {
                if (!prepared) {
                    ensureStartsAtHome()
                    prepared = true
                }
                pressHome()
            },
        ) {
            startActivityAndWait()
            waitForHomeContent()
        }
    }

    companion object {
        const val ITERATIONS = 10
    }
}
