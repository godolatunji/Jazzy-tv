package com.nexient.jazzy.core.data

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.nexient.jazzy.core.model.WatchRules
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

class CacheFirstTest {
    private var now = 1_000_000L
    private var stored: Cached<String>? = null
    private var fetches = 0

    private fun flow(fetchResult: () -> String, force: Boolean = false) = cacheFirst(
        now = { now },
        ttlMs = 100,
        force = force,
        read = { stored },
        fetch = { fetches++; fetchResult() },
        write = { stored = Cached(it, now) },
    )

    @Test
    fun freshCacheIsServedWithoutFetching() = runTest {
        stored = Cached("cached", now - 10)
        flow({ "network" }).test {
            assertThat(awaitItem().data).isEqualTo("cached")
            awaitComplete()
        }
        assertThat(fetches).isEqualTo(0)
    }

    @Test
    fun staleCacheIsShownThenRefreshed() = runTest {
        stored = Cached("old", now - 1_000)
        flow({ "new" }).test {
            val first = awaitItem()
            assertThat(first.data).isEqualTo("old")
            assertThat(first.refreshing).isTrue()
            assertThat(first.stale).isTrue()
            assertThat(awaitItem().data).isEqualTo("new")
            awaitComplete()
        }
        assertThat(stored?.value).isEqualTo("new")
    }

    @Test
    fun failedRefreshKeepsStaleDataAndReportsError() = runTest {
        stored = Cached("old", now - 1_000)
        flow({ throw IOException("offline") }).test {
            awaitItem()
            val last = awaitItem()
            assertThat(last.data).isEqualTo("old")
            assertThat(last.error).isInstanceOf(IOException::class.java)
            assertThat(last.refreshing).isFalse()
            awaitComplete()
        }
    }

    @Test
    fun missingCacheAndFailureIsAnErrorWithoutData() = runTest {
        flow({ throw IOException("offline") }).test {
            assertThat(awaitItem().isLoadingWithoutData).isTrue()
            assertThat(awaitItem().isErrorWithoutData).isTrue()
            awaitComplete()
        }
    }

    @Test
    fun forceRefreshFetchesEvenWhenFresh() = runTest {
        stored = Cached("cached", now)
        flow({ "network" }, force = true).test {
            awaitItem()
            assertThat(awaitItem().data).isEqualTo("network")
            awaitComplete()
        }
        assertThat(fetches).isEqualTo(1)
    }

    @Test
    fun cacheFirstOnceFallsBackToStaleOnError() = runTest {
        stored = Cached("old", now - 1_000)
        val value = cacheFirstOnce({ now }, 100, { stored }, { throw IOException() }, { })
        assertThat(value).isEqualTo("old")
    }

    @Test
    fun watchRulesMatchTheSpec() {
        assertThat(WatchRules.isWatched(90, 100)).isTrue()
        assertThat(WatchRules.isWatched(89, 100)).isFalse()
        // Started (≥ 30 s) and not finished (RS-4)
        assertThat(WatchRules.qualifiesForContinue(20_000, 3_600_000)).isFalse()
        assertThat(WatchRules.qualifiesForContinue(60_000, 3_600_000)).isTrue()
        assertThat(WatchRules.qualifiesForContinue(150_000, 160_000)).isFalse()
    }
}
