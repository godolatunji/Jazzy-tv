package com.nexient.jazzy.core.sources.engine

import java.net.URLEncoder
import java.util.Locale
import kotlin.math.pow

internal object Sizes {
    private val sizePattern = Regex("""([\d.,]+)\s*([kKMGTPE]?)(i?)([Bo])""")

    /** Parses display sizes like `1.4 GB`, `700 MiB` or the French `1,2 Go` into bytes. */
    fun parse(size: String?): Long? {
        if (size.isNullOrBlank()) return null
        val match = sizePattern.find(size) ?: return null
        val (rawNumber, prefix, binary) = match.destructured
        val number = normaliseDecimal(rawNumber) ?: return null
        val exponent = if (prefix.isEmpty()) 0 else "KMGTPE".indexOf(prefix.uppercase()) + 1
        val base = if (binary.isNotEmpty()) 1024.0 else 1000.0
        return (number * base.pow(exponent)).toLong()
    }

    /** "1,234.5" and "1.234,5" -> the last separator is the decimal one; "1,234" -> thousands; "1,2" -> decimal. */
    private fun normaliseDecimal(raw: String): Double? {
        val lastComma = raw.lastIndexOf(',')
        val lastDot = raw.lastIndexOf('.')
        val normalised = when {
            lastComma < 0 -> raw
            lastDot > lastComma -> raw.replace(",", "")
            lastDot >= 0 -> raw.replace(".", "").replace(',', '.')
            raw.split(',').drop(1).all { it.length == 3 } -> raw.replace(",", "")
            else -> raw.replace(',', '.')
        }
        return normalised.toDoubleOrNull()
    }

    /** Port of `humanizeSize` from the original helpers (SI units). */
    fun humanizeSi(bytes: Long): String = humanize(bytes, 1000.0, listOf("kB", "MB", "GB", "TB", "PB", "EB"))

    /** Port of TorrentLeech's `humanFileSize` (binary units). */
    fun humanizeBinary(bytes: Long): String =
        humanize(bytes, 1024.0, listOf("KiB", "MiB", "GiB", "TiB", "PiB", "EiB"))

    private fun humanize(bytes: Long, threshold: Double, units: List<String>): String {
        if (bytes < threshold) return "$bytes B"
        var value = bytes.toDouble()
        var unit = -1
        do {
            value /= threshold
            unit++
        } while (value >= threshold && unit < units.lastIndex)
        return String.format(Locale.US, "%.1f %s", value, units[unit])
    }
}

internal object Magnets {
    private val btih = Regex("""urn:btih:([a-zA-Z0-9]+)""")
    private val exactLength = Regex("""[?&]xl=(\d+)""")
    private val hex40 = Regex("""^[0-9a-fA-F]{40}$""")
    private val base32 = Regex("""^[A-Za-z2-7]{32}$""")

    /** Trackers appended by the original ThePirateBay provider. */
    val defaultTrackers = listOf(
        "udp://tracker.opentrackr.org:1337/announce",
        "udp://open.stealth.si:80/announce",
        "udp://tracker.torrent.eu.org:451/announce",
        "udp://exodus.desync.com:6969/announce",
        "udp://tracker.cyberia.is:6969/announce",
        "udp://tracker.internetwarriors.net:1337/announce",
        "udp://tracker.pirateparty.gr:6969/announce",
    )

    fun isMagnet(value: String?): Boolean = value != null && value.startsWith("magnet:?", ignoreCase = true)

    /** Lowercase hex info-hash of [magnet], or null when it has no valid `btih`. */
    fun infoHash(magnet: String): String? = btih.find(magnet)?.groupValues?.get(1)?.let(::normaliseHash)

    /** `xl` (exact length) of [magnet] when present. */
    fun exactLength(magnet: String): Long? = exactLength.find(magnet)?.groupValues?.get(1)?.toLongOrNull()

    /** Hex hashes are lowercased; 32-char base32 hashes are converted to hex. Anything else is null. */
    fun normaliseHash(hash: String): String? {
        val trimmed = hash.trim()
        return when {
            hex40.matches(trimmed) -> trimmed.lowercase()
            base32.matches(trimmed) -> base32ToHex(trimmed.uppercase())
            else -> null
        }
    }

    private fun base32ToHex(value: String): String {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val out = StringBuilder(40)
        var buffer = 0L
        var bits = 0
        for (c in value) {
            buffer = (buffer shl 5) or alphabet.indexOf(c).toLong()
            bits += 5
            while (bits >= 4) {
                bits -= 4
                out.append("0123456789abcdef"[((buffer shr bits) and 0xF).toInt()])
            }
        }
        return out.toString()
    }

    fun build(infoHash: String, name: String, trackers: List<String> = defaultTrackers): String =
        buildString {
            append("magnet:?xt=urn:btih:").append(infoHash)
            append("&dn=").append(Uris.encodeComponent(name))
            trackers.forEach { append("&tr=").append(Uris.encodeComponent(it)) }
        }
}

internal object Uris {
    /** Sites still publish some links as `http://`; Android blocks cleartext traffic, so upgrade them. */
    fun preferHttps(url: String): String =
        if (url.startsWith("http://", ignoreCase = true)) "https://" + url.substring(7) else url

    /** JavaScript `encodeURIComponent` semantics (space -> %20, keeps `!'()*~`). */
    fun encodeComponent(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8)
            .replace("+", "%20")
            .replace("%21", "!")
            .replace("%27", "'")
            .replace("%28", "(")
            .replace("%29", ")")
            .replace("%7E", "~")
}
