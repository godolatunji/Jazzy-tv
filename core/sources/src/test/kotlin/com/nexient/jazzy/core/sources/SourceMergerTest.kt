package com.nexient.jazzy.core.sources

import com.google.common.truth.Truth.assertThat
import com.nexient.jazzy.core.model.ReleaseInfo
import com.nexient.jazzy.core.model.Resolution
import com.nexient.jazzy.core.model.TorrentSource
import org.junit.Test

class SourceMergerTest {
    private fun row(
        provider: String,
        name: String,
        seeders: Int,
        hash: String? = null,
        size: Long? = null,
        resolution: Resolution? = null,
        leechers: Int = 0,
        desc: String = "https://$provider.invalid/${name.hashCode()}",
    ) = TorrentSource(
        key = hash ?: "$provider:$desc",
        name = name,
        infoHash = hash,
        magnet = hash?.let { "magnet:?xt=urn:btih:$it" },
        sizeBytes = size,
        seeders = seeders,
        leechers = leechers,
        providerIds = listOf(provider),
        providerNames = listOf(provider.uppercase()),
        release = ReleaseInfo(resolution = resolution),
        resolveHint = mapOf("$provider:desc" to desc),
    )

    private val hashA = "a".repeat(40)
    private val hashB = "b".repeat(40)

    @Test fun `same hash merges with max seeders and every provider`() {
        val merged = SourceMerger.mergeAndSort(
            listOf(
                row("thepiratebay", "Sintel.2010.1080p", 40, hashA, 1_200_000_000, leechers = 9),
                row("yts", "Sintel (2010) [1080p] [YTS]", 120, hashA, null, leechers = 3),
                row("magnetdl", "Sintel 2010 720p", 10, hashB),
            ),
        )
        assertThat(merged).hasSize(2)
        val first = merged.first()
        assertThat(first.key).isEqualTo(hashA)
        assertThat(first.seeders).isEqualTo(120)
        assertThat(first.leechers).isEqualTo(9)
        assertThat(first.name).isEqualTo("Sintel (2010) [1080p] [YTS]") // best-seeded listing's name
        assertThat(first.sizeBytes).isEqualTo(1_200_000_000)
        assertThat(first.providerIds).containsExactly("thepiratebay", "yts").inOrder()
        assertThat(first.providerNames).containsExactly("THEPIRATEBAY", "YTS").inOrder()
        assertThat(first.resolveHint.keys).containsExactly("thepiratebay:desc", "yts:desc")
    }

    @Test fun `rows without hash merge by normalised name and size within one percent`() {
        val merged = SourceMerger.merge(
            listOf(
                row("kickasstorrents", "Sintel.2010.1080p.BluRay", 30, size = 1_000_000_000),
                row("limetorrents", "Sintel 2010 1080p BluRay", 50, size = 1_009_000_000),
                row("torrentproject", "Sintel 2010 1080p BluRay", 70, size = 1_100_000_000),
            ),
        )
        assertThat(merged).hasSize(2)
        val group = merged.first { it.providerIds.size == 2 }
        assertThat(group.providerIds).containsExactly("kickasstorrents", "limetorrents")
        assertThat(group.seeders).isEqualTo(50)
        assertThat(group.key).isEqualTo("limetorrents:https://limetorrents.invalid/${"Sintel 2010 1080p BluRay".hashCode()}")
        assertThat(group.infoHash).isNull()
    }

    @Test fun `rows without hash or size are only merged with the same key`() {
        val merged = SourceMerger.merge(
            listOf(
                row("kickasstorrents", "Sintel 2010 1080p", 30, desc = "https://kat.invalid/1"),
                row("kickasstorrents", "Sintel 2010 1080p", 30, desc = "https://kat.invalid/1"),
                row("limetorrents", "Sintel 2010 1080p", 50),
            ),
        )
        assertThat(merged).hasSize(2)
    }

    @Test fun `a row without hash joins a hashed group with the same name and size`() {
        val merged = SourceMerger.merge(
            listOf(
                row("thepiratebay", "Sintel.2010.1080p.BluRay", 40, hashA, 1_000_000_000),
                row("kickasstorrents", "Sintel 2010 1080p BluRay", 90, size = 1_005_000_000),
            ),
        )
        val single = merged.single()
        assertThat(single.infoHash).isEqualTo(hashA)
        assertThat(single.key).isEqualTo(hashA)
        assertThat(single.magnet).isNotNull()
        assertThat(single.seeders).isEqualTo(90)
        assertThat(single.providerIds).containsExactly("thepiratebay", "kickasstorrents")
    }

    @Test fun `zero seeder rows are kept`() {
        val merged = SourceMerger.mergeAndSort(listOf(row("a", "Dead 2010", 0, hashA), row("b", "Alive 2010", 3, hashB)))
        assertThat(merged.map { it.seeders }).containsExactly(3, 0).inOrder()
    }

    @Test fun `sort is seeders, then resolution, then smaller size`() {
        val sorted = SourceMerger.mergeAndSort(
            listOf(
                row("a", "x720", 10, "1".repeat(40), 500, Resolution.HD_720),
                row("a", "x1080big", 10, "2".repeat(40), 2_000, Resolution.FHD_1080),
                row("a", "x1080small", 10, "3".repeat(40), 1_000, Resolution.FHD_1080),
                row("a", "x1080nosize", 10, "4".repeat(40), null, Resolution.FHD_1080),
                row("a", "top", 99, "5".repeat(40), 9_000, Resolution.SD),
                row("a", "unknown", 10, "6".repeat(40), 1, null),
            ),
        )
        assertThat(sorted.map { it.name }).containsExactly("top", "x1080small", "x1080big", "x1080nosize", "x720", "unknown").inOrder()
    }

    @Test fun `size tolerance helpers`() {
        assertThat(SourceMerger.sizesClose(1000, 1010)).isTrue()
        assertThat(SourceMerger.sizesClose(1000, 1011)).isFalse()
        assertThat(SourceMerger.sizesClose(null, 1000)).isFalse()
        assertThat(SourceMerger.nameKey("Sintel.2010.[1080p]-GRP")).isEqualTo("sintel 2010 1080p grp")
    }
}
