package com.nexient.jazzy.core.torrent

import java.text.Normalizer

/**
 * TE-2 bait-and-switch guard: the normalised tokens of the expected title must appear in the
 * torrent name or the chosen file path. Pure, so it is unit-tested without native code.
 */
object TitleVerifier {
    private const val MIN_COMPACT_LENGTH = 6
    private val STOP_WORDS =setOf("the", "a", "an", "of", "and", "to", "in", "on", "at", "for", "with")
    private val ROMAN = mapOf(
        "ii" to "2", "iii" to "3", "iv" to "4", "v" to "5", "vi" to "6",
        "vii" to "7", "viii" to "8", "ix" to "9", "x" to "10",
    )
    private val YEAR = Regex("""(?<!\d)(19\d{2}|20\d{2})(?!\d)""")
    private val DIACRITICS = Regex("""\p{Mn}+""")
    private val APOSTROPHES = Regex("""['’`´]""")
    private val NON_ALNUM = Regex("""[^\p{L}\p{N}]+""")

    /**
     * @param expectedYear checked for movies only (season == null): when the haystack carries
     *   years and none is within ±1 of [expectedYear], it's another film (remake, sequel mirror).
     */
    fun matches(
        expectedTitle: String,
        expectedYear: Int?,
        torrentName: String,
        filePath: String,
        isEpisode: Boolean = false,
    ): Boolean {
        val titleTokens = significantTokens(expectedTitle)
        // Titles that normalise to nothing (e.g. only non-Latin script vs a Latin release name)
        // can't be verified; don't block playback on them.
        if (titleTokens.isEmpty()) return true

        val haystack = "$torrentName $filePath"
        val hayTokens = tokens(haystack).map(::canonical).toSet()
        val tokenMatch = titleTokens.all { it in hayTokens }
        // "Spider-Man" vs "SpiderMan": compare without separators, but only for titles long enough
        // not to match by accident ("Up" would match "Pickup").
        val compactTitle = titleTokens.joinToString("")
        val compactMatch = compactTitle.length >= MIN_COMPACT_LENGTH &&
            compactTitle in tokens(haystack).joinToString("") { canonical(it) }
        if (!tokenMatch && !compactMatch) return false

        if (!isEpisode && expectedYear != null) {
            val years = YEAR.findAll(haystack).map { it.value.toInt() }.toList()
            if (years.isNotEmpty() && years.none { kotlin.math.abs(it - expectedYear) <= 1 }) return false
        }
        return true
    }

    /** Lower-cased, accent-free alphanumeric tokens. */
    fun tokens(text: String): List<String> {
        val folded = Normalizer.normalize(text, Normalizer.Form.NFD).replace(DIACRITICS, "")
            .lowercase()
            .replace("&", " and ")
            .replace(APOSTROPHES, "")
        return folded.split(NON_ALNUM).filter { it.isNotEmpty() }
    }

    fun significantTokens(title: String): List<String> {
        val all = tokens(title).map(::canonical)
        val significant = all.filterNot { it in STOP_WORDS }
        return significant.ifEmpty { all }
    }

    private fun canonical(token: String): String = ROMAN[token] ?: token
}
