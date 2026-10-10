package com.nexient.jazzy.core.sources

import com.nexient.jazzy.core.model.TorrentSource
import kotlin.math.abs
import kotlin.math.max

/**
 * Cross-site de-duplication and ordering (SRC-8, SL-2).
 *
 * - Rows with the same info-hash become one row: highest seeder (and leecher) count, every
 *   provider ID/name, the first known magnet, size and resolve hints.
 * - Rows without a hash join a group with the same row key, or with the same normalised name and
 *   a size within ±1% (this can attach them to a hashed group, which then gives them a magnet).
 * - 0-seeder rows are kept; the UI hides them by default (SL-1).
 * - Order: seeders desc, then resolution rank desc, then smaller size (unknown size last).
 */
internal object SourceMerger {
    private val nonAlphanumeric = Regex("""[^\p{L}\p{N}]+""")

    val ORDER: Comparator<TorrentSource> = compareByDescending<TorrentSource> { it.seeders }
        .thenByDescending { it.release.resolution?.rank ?: 0 }
        .thenBy { it.sizeBytes ?: Long.MAX_VALUE }

    fun mergeAndSort(rows: List<TorrentSource>): List<TorrentSource> = merge(rows).sortedWith(ORDER)

    fun merge(rows: List<TorrentSource>): List<TorrentSource> {
        val groups = mutableListOf<MutableList<TorrentSource>>()
        val byHash = HashMap<String, MutableList<TorrentSource>>()
        for (row in rows) {
            val hash = row.infoHash ?: continue
            byHash.getOrPut(hash) { mutableListOf<TorrentSource>().also(groups::add) } += row
        }
        for (row in rows) {
            if (row.infoHash != null) continue
            val nameKey = nameKey(row.name)
            val group = groups.firstOrNull { group ->
                group.any { other -> other.key == row.key || (nameKey(other.name) == nameKey && sizesClose(other.sizeBytes, row.sizeBytes)) }
            }
            if (group != null) group += row else groups += mutableListOf(row)
        }
        return groups.map(::combine)
    }

    internal fun nameKey(name: String): String = name.lowercase().replace(nonAlphanumeric, " ").trim()

    internal fun sizesClose(a: Long?, b: Long?): Boolean {
        if (a == null || b == null || a <= 0 || b <= 0) return false
        return abs(a - b) <= max(a, b) * 0.01
    }

    private fun combine(group: List<TorrentSource>): TorrentSource {
        if (group.size == 1) return group.single()
        // The best-seeded listing is the representative; ties keep the earliest (provider order).
        val primary = group.maxBy { it.seeders }
        val ordered = listOf(primary) + (group - primary)
        val hash = ordered.firstNotNullOfOrNull { it.infoHash }
        return primary.copy(
            key = hash ?: primary.key,
            infoHash = hash,
            magnet = ordered.firstNotNullOfOrNull { it.magnet },
            sizeBytes = ordered.firstNotNullOfOrNull { it.sizeBytes },
            seeders = group.maxOf { it.seeders },
            leechers = group.maxOf { it.leechers },
            providerIds = group.flatMap { it.providerIds }.distinct(),
            providerNames = group.flatMap { it.providerNames }.distinct(),
            resolveHint = group.fold(emptyMap()) { acc, row -> row.resolveHint + acc },
            uploadedAt = ordered.firstNotNullOfOrNull { it.uploadedAt },
        )
    }
}
