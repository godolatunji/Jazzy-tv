package com.nexient.jazzy.core.sources

import com.nexient.jazzy.core.sources.engine.EngineHttp
import com.nexient.jazzy.core.sources.engine.ProviderRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Hits the real sites, so it is skipped unless run with
 * `./gradlew :core:sources:testDebugUnitTest --tests "*LiveProvidersSmokeTest" -PliveSmoke=true`
 * (optionally `-PliveQuery=...`). It never fails: it prints one line per public provider, to show
 * which domains currently work. Hosts are printed, never full URLs.
 */
class LiveProvidersSmokeTest {
    @Test fun `report which public providers answer`() = runBlocking {
        assumeTrue("Live smoke test is opt-in (-PliveSmoke=true)", System.getProperty("liveSmoke") == "true")
        val query = System.getProperty("liveQuery")?.ifBlank { null } ?: DefaultSourceSearcher.TEST_QUERY
        val providers = ProviderRegistry.allProviders(EngineHttp.defaultClient()).filter { it.info.isPublic }
        providers.forEach { provider ->
            provider.enable()
            val line = try {
                val results = withTimeoutOrNull(DefaultSourceSearcher.PROVIDER_TIMEOUT_MS * 2) { provider.search(query) }
                when {
                    results == null -> "TIMEOUT"
                    results.isEmpty() -> "OK, 0 results"
                    else -> "OK, ${results.size} results, e.g. \"${results.first().title}\""
                }
            } catch (e: Exception) {
                "FAIL ${e::class.simpleName}: ${DefaultSourceSearcher.failureOf(e).message}"
            }
            val host = provider.baseUrl.removePrefix("https://").removePrefix("http://")
            println("LIVE ${provider.id.padEnd(16)} ${host.padEnd(28)} $line")
        }
    }
}
