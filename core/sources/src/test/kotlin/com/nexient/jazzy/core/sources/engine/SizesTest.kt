package com.nexient.jazzy.core.sources.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SizesTest {
    @Test fun `parses si binary and french units`() {
        assertEquals(1_400_000_000L, Sizes.parse("1.4 GB"))
        assertEquals(700L * 1024 * 1024, Sizes.parse("700 MiB"))
        assertEquals(1_200_000_000L, Sizes.parse("1,2 Go"))
        assertEquals(1_234_000_000L, Sizes.parse("1,234 MB"))
        assertEquals(512L, Sizes.parse("512 B"))
        assertNull(Sizes.parse("unknown"))
        assertNull(Sizes.parse(null))
    }

    @Test fun `humanizes like the original helpers`() {
        assertEquals("999 B", Sizes.humanizeSi(999))
        assertEquals("276.4 MB", Sizes.humanizeSi(276_445_467))
        assertEquals("263.6 MiB", Sizes.humanizeBinary(276_445_467))
    }

    @Test fun `magnet helpers`() {
        val hash = "DD8255ECDC7CA55FB0BBF81323D87062DB1F6D1C"
        val magnet = Magnets.build(hash, "a b", trackers = listOf("udp://t:1/announce"))
        assertEquals("magnet:?xt=urn:btih:$hash&dn=a%20b&tr=udp%3A%2F%2Ft%3A1%2Fannounce", magnet)
        assertEquals(hash.lowercase(), Magnets.infoHash(magnet))
        assertTrue(Magnets.isMagnet(magnet))
        assertFalse(Magnets.isMagnet("https://example.org"))
        assertEquals(1234L, Magnets.exactLength("$magnet&xl=1234"))
    }

    @Test fun `info hashes are normalised to lowercase hex`() {
        // Base32 form of dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c.
        assertEquals("dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c", Magnets.normaliseHash("3WBFL3G4PSSV7MF37AJSHWDQMLNR63I4"))
        assertNull(Magnets.normaliseHash("ABC123"))
        assertNull(Magnets.infoHash("magnet:?xt=urn:btih:not-a-hash"))
    }
}
