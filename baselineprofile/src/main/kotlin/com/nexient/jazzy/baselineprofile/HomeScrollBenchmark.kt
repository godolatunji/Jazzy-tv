package com.nexient.jazzy.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Home rail jank (plan E16-02, spec §9: rails at 60 fps). `frameDurationCpuMs` p50/p90 must stay
 * under 16.7 ms; `frameOverrunMs` (API 31+) > 0 means a missed deadline.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class HomeScrollBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test fun scrollHomeNone() = scroll(CompilationMode.None())

    @Test fun scrollHomeBaselineProfile() = scroll(CompilationMode.Partial(BaselineProfileMode.Require))

    private fun scroll(compilation: CompilationMode) {
        var prepared = false
        rule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(FrameTimingMetric()),
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
            },
        ) {
            flingHomeVertically()
            flingRailsHorizontally()
        }
    }

    companion object {
        const val ITERATIONS = 5
    }
}
