package com.nexient.jazzy.core.sources

import com.nexient.jazzy.core.model.ProviderInfo
import com.nexient.jazzy.core.model.ProviderStatus
import com.nexient.jazzy.core.model.SourceQuery
import com.nexient.jazzy.core.model.SourceSearchResult
import com.nexient.jazzy.core.model.TorrentSource
import kotlinx.coroutines.flow.Flow

/**
 * Public entry point of `:core:sources` (spec §5.8). Searches every enabled torrent site in
 * parallel and emits the merged, matched, de-duplicated and seeder-sorted list progressively.
 */
interface SourceSearcher {
    /** All built-in providers, enabled or not. */
    val providers: List<ProviderInfo>

    /**
     * Emits once immediately (every enabled site `Searching`), then again after each site
     * finishes. The last emission has `isComplete = true`. Never throws for a single-site failure.
     */
    fun search(query: SourceQuery): Flow<SourceSearchResult>

    /** Returns a magnet for [source], scraping the site's detail page when needed (SRC-7). */
    suspend fun resolveMagnet(source: TorrentSource): String

    /** Runs a small known search against one provider and reports how it went (SRC-5 "Test"). */
    suspend fun testProvider(providerId: String): ProviderStatus
}

/** Provider configuration and health, implemented by `:core:data` on top of jazzy.db. */
interface ProviderSettingsStore {
    /** Settings keyed by provider ID. Providers missing from the map use their defaults. */
    val settings: Flow<Map<String, com.nexient.jazzy.core.model.ProviderSettings>>

    suspend fun recordResult(providerId: String, ok: Boolean, latencyMs: Long, error: String?)
}
