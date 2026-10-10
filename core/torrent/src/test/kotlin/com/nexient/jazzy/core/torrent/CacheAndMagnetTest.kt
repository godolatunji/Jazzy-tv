package com.nexient.jazzy.core.torrent

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CacheEvictionPolicyTest {
    private val gb = 1024L * 1024 * 1024

    @Test fun `nothing evicted under the cap`() {
        val entries = listOf(CacheEntry("a", 1 * gb, 1), CacheEntry("b", 1 * gb, 2))
        assertThat(CacheEvictionPolicy.selectVictims(entries, emptySet(), capBytes = 4 * gb, freeBytes = 100 * gb)).isEmpty()
    }

    @Test fun `evicts least recently used first until under the cap`() {
        val entries = listOf(
            CacheEntry("new", 2 * gb, 300),
            CacheEntry("old", 2 * gb, 100),
            CacheEntry("mid", 2 * gb, 200),
        )
        assertThat(CacheEvictionPolicy.selectVictims(entries, emptySet(), capBytes = 4 * gb, freeBytes = 100 * gb))
            .containsExactly("old")
        assertThat(CacheEvictionPolicy.selectVictims(entries, emptySet(), capBytes = 1 * gb, freeBytes = 100 * gb))
            .containsExactly("old", "mid", "new").inOrder()
    }

    @Test fun `active torrents are never evicted`() {
        val entries = listOf(CacheEntry("playing", 5 * gb, 1), CacheEntry("old", 1 * gb, 2))
        assertThat(CacheEvictionPolicy.selectVictims(entries, setOf("playing"), capBytes = 4 * gb, freeBytes = 100 * gb))
            .containsExactly("old")
    }

    @Test fun `cap is limited to half of the available space`() {
        // 3 GB cached + 3 GB free → the cache may use 3 GB at most, even with a 4 GB cap.
        assertThat(CacheEvictionPolicy.effectiveCap(capBytes = 4 * gb, cacheBytes = 3 * gb, freeBytes = 3 * gb)).isEqualTo(3 * gb)
        val entries = listOf(CacheEntry("a", 2 * gb, 1), CacheEntry("b", 2 * gb, 2))
        // 4 GB cached + 2 GB free → effective cap 3 GB → drop the oldest.
        assertThat(CacheEvictionPolicy.selectVictims(entries, emptySet(), capBytes = 10 * gb, freeBytes = 2 * gb))
            .containsExactly("a")
    }
}

class MagnetsTest {
    @Test fun `hex and base32 info hashes`() {
        val hex = "dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c"
        assertThat(Magnets.infoHash("magnet:?xt=urn:btih:DD8255ECDC7CA55FB0BBF81323D87062DB1F6D1C&dn=x")).isEqualTo(hex)
        // Base32 of the same 20 bytes.
        assertThat(Magnets.infoHash("magnet:?xt=urn:btih:3WBFL3G4PSSV7MF37AJSHWDQMLNR63I4")).isEqualTo(hex)
        assertThat(Magnets.infoHash("magnet:?dn=nohash")).isNull()
    }

    @Test fun `trackers are merged without duplicates`() {
        val merged = Magnets.mergeTrackers(
            listOf("udp://tracker.opentrackr.org:1337/announce", "udp://explodie.org:6969"),
            listOf("udp://explodie.org:6969/", "udp://open.stealth.si:80/announce", ""),
        )
        assertThat(merged).containsExactly(
            "udp://tracker.opentrackr.org:1337/announce",
            "udp://explodie.org:6969",
            "udp://open.stealth.si:80/announce",
        ).inOrder()
    }
}

class LoopbackRangeTest {
    @Test fun `range parsing`() {
        assertThat(LoopbackStreamServer.parseRange(null, 1000)).isNull()
        assertThat(LoopbackStreamServer.parseRange("bytes=0-99", 1000)).isEqualTo(0L to 99L)
        assertThat(LoopbackStreamServer.parseRange("bytes=500-", 1000)).isEqualTo(500L to 999L)
        assertThat(LoopbackStreamServer.parseRange("bytes=-100", 1000)).isEqualTo(900L to 999L)
        assertThat(LoopbackStreamServer.parseRange("bytes=900-5000", 1000)).isEqualTo(900L to 999L)
        assertThat(LoopbackStreamServer.parseRange("bytes=1000-", 1000)).isEqualTo(-1L to -1L)
        assertThat(LoopbackStreamServer.parseRange("items=0-1", 1000)).isEqualTo(-1L to -1L)
        assertThat(LoopbackStreamServer.parseRange("bytes=abc", 1000)).isEqualTo(-1L to -1L)
    }
}
