package com.nexient.jazzy.core.torrent

import java.util.Locale

object DefaultTrackers {
    /** Public trackers appended to magnets when `:core:data` doesn't bind a [TrackerListStore]. */
    val LIST: List<String> = listOf(
        "udp://tracker.opentrackr.org:1337/announce",
        "udp://open.demonii.com:1337/announce",
        "udp://open.stealth.si:80/announce",
        "udp://tracker.torrent.eu.org:451/announce",
        "udp://exodus.desync.com:6969/announce",
        "udp://explodie.org:6969/announce",
        "udp://tracker.openbittorrent.com:6969/announce",
        "udp://tracker.tiny-vps.com:6969/announce",
        "udp://open.tracker.cl:1337/announce",
        "http://tracker.opentrackr.org:1337/announce",
    )
}

object Magnets {
    private val BTIH = Regex("""(?i)xt=urn:btih:([0-9a-z]+)""")
    private const val BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    /** Lower-case hex v1 info-hash of a magnet (hex or base32 form), or null. */
    fun infoHash(magnet: String): String? {
        val raw = BTIH.find(magnet)?.groupValues?.get(1) ?: return null
        return when (raw.length) {
            40 -> raw.lowercase(Locale.ROOT).takeIf { s -> s.all { it in '0'..'9' || it in 'a'..'f' } }
            32 -> base32ToHex(raw.uppercase(Locale.ROOT))
            else -> null
        }
    }

    fun normalizeInfoHash(hash: String): String = hash.trim().lowercase(Locale.ROOT)

    /** [existing] followed by every tracker of [extra] not already present, order kept. */
    fun mergeTrackers(existing: List<String>, extra: List<String>): List<String> {
        val seen = existing.map { it.trim().trimEnd('/') }.toMutableSet()
        return existing + extra.filter { it.isNotBlank() && seen.add(it.trim().trimEnd('/')) }
    }

    private fun base32ToHex(s: String): String? {
        val bytes = ByteArray(20)
        var buffer = 0
        var bits = 0
        var out = 0
        for (c in s) {
            val v = BASE32.indexOf(c)
            if (v < 0) return null
            buffer = (buffer shl 5) or v
            bits += 5
            if (bits >= 8) {
                bits -= 8
                if (out < bytes.size) bytes[out++] = (buffer shr bits and 0xff).toByte()
            }
        }
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
