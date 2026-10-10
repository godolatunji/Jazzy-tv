package com.nexient.jazzy.feature.profiles

import java.util.Locale

/** TMDB `language` tags offered for a profile's content language (spec §6.2). */
internal val ContentLanguages = listOf(
    "en-US", "en-GB", "fr-FR", "es-ES", "es-MX", "de-DE", "it-IT", "pt-BR", "pt-PT", "nl-NL",
    "sv-SE", "da-DK", "nb-NO", "fi-FI", "pl-PL", "tr-TR", "ru-RU", "uk-UA", "ar-SA", "hi-IN",
    "ja-JP", "ko-KR", "zh-CN", "zh-TW", "id-ID", "th-TH",
)

/** ISO 639-1 codes for the preferred subtitle language; null means subtitles off. */
internal val SubtitleLanguages = listOf<String?>(
    null, "en", "fr", "es", "de", "it", "pt", "nl", "sv", "da", "no", "fi", "pl", "tr", "ru", "uk",
    "ar", "hi", "ja", "ko", "zh", "id", "th", "yo", "ha", "ig", "sw",
)

/** "English (United States)" for "en-US", "English" for "en"; [off] when there's no language. */
internal fun languageLabel(tag: String?, off: String): String {
    if (tag.isNullOrBlank()) return off
    val locale = Locale.forLanguageTag(tag)
    return locale.getDisplayName(Locale.ENGLISH).ifBlank { tag }
}

/** The list to show, with [current] added at the top when it isn't one of the defaults. */
internal fun <T> List<T>.withCurrent(current: T): List<T> = if (current in this) this else listOf(current) + this
