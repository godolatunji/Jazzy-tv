package com.nexient.jazzy.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates the app's Baseline Profile (plan E16-01) and its startup (dex layout) profile.
 *
 *   ./gradlew :app:generateReleaseBaselineProfile
 *
 * Output: `app/src/release/generated/baselineProfiles/{baseline,startup}-prof.txt`. Needs API 33+
 * (or a rooted device). Journeys never press Play, so no torrent traffic happens while profiling.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    /** Cold start → Home with its first rails. Also feeds the startup profile for dex layout. */
    @Test
    fun startup() = rule.collect(PACKAGE_NAME, maxIterations = MAX_ITERATIONS, stableIterations = STABLE_ITERATIONS, includeInStartupProfile = true) {
        pressHome()
        startActivityAndWait()
        goToHome(rememberProfile = true)
        waitForHomeContent()
    }

    /** The main browse journeys: Home rails, Details + Sources list, Search typing. */
    @Test
    fun journeys() = rule.collect(PACKAGE_NAME, maxIterations = MAX_ITERATIONS, stableIterations = STABLE_ITERATIONS, includeInStartupProfile = false) {
        pressHome()
        startActivityAndWait()
        goToHome(rememberProfile = true)
        waitForHomeContent()

        // Home: rails vertically and horizontally.
        scrollHomeRails()

        // Details: a movie from the first rail, wait for Sources, scroll the list.
        selectMoviesTab()
        if (openFirstTitle()) {
            waitForFirstSourceRow()
            scrollDetails()
            backToHome()
        }

        // Search: open and type.
        searchAndType("star")
        backToHome()
    }

    private companion object {
        // Compose apps rarely reach a byte-identical profile; a few passes capture the journeys.
        const val MAX_ITERATIONS = 5
        const val STABLE_ITERATIONS = 2
    }
}
