package com.nexient.jazzy.core.sources.parse

import java.text.Normalizer

/**
 * Text folding shared by [QueryBuilder] (search strings) and [ResultMatcher] (title comparison):
 * accents are stripped, `&` becomes `and`, apostrophes are dropped (`Ocean's` -> `Oceans`) and any
 * other punctuation, dot or underscore becomes a space.
 */
object TitleNormalizer {
    private val LEADING_ARTICLES = setOf("the", "a", "an")
    private val combiningMarks = Regex("""\p{M}+""")
    private val apostrophes = Regex("""['’‘`´]""")
    private val nonAlphanumeric = Regex("""[^\p{L}\p{N}]+""")
    private val specialLetters = mapOf(
        'ß' to "ss", 'æ' to "ae", 'Æ' to "AE", 'œ' to "oe", 'Œ' to "OE", 'ø' to "o", 'Ø' to "O",
        'ł' to "l", 'Ł' to "L", 'đ' to "d", 'Đ' to "D", 'ð' to "d", 'þ' to "th",
    )

    /** Accent-free text with `&` -> `and` and apostrophes removed; case and other punctuation kept. */
    fun fold(text: String): String {
        val replaced = buildString(text.length) {
            for (c in text) append(specialLetters[c] ?: c)
        }
        return Normalizer.normalize(replaced, Normalizer.Form.NFD)
            .replace(combiningMarks, "")
            .replace("&", " and ")
            .replace(apostrophes, "")
    }

    /** A clean free-text search string: folded, punctuation replaced by spaces, case kept. */
    fun searchText(text: String): String = fold(text).replace(nonAlphanumeric, " ").trim()

    /**
     * Lowercase comparison tokens. A leading article is dropped and so is every `and`, so
     * "The Fast & the Furious" and "Fast.and.the.Furious" compare equal.
     */
    fun tokens(text: String): List<String> {
        val words = fold(text).lowercase().split(nonAlphanumeric).filter { it.isNotEmpty() }
        val withoutArticle = if (words.size > 1 && words.first() in LEADING_ARTICLES) words.drop(1) else words
        return withoutArticle.filter { it != "and" }
    }

    /** [tokens] joined by single spaces; equal keys mean "same title". */
    fun matchKey(text: String): String = tokens(text).joinToString(" ")

    /** True when [text] has at least one Latin letter or digit once folded (i.e. sites can search it). */
    fun isLatinSearchable(text: String): Boolean = fold(text).any { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' }
}
