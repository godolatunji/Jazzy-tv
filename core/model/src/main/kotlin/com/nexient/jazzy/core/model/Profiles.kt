package com.nexient.jazzy.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: Long,
    val name: String,
    val avatarId: Int,
    val isKids: Boolean,
    val maturity: MaturityLevel,
    /** TMDB language tag, e.g. "en-US". */
    val language: String,
    val subtitleLanguage: String?,
    val audioLanguage: String?,
    val hasPin: Boolean,
    val autoplayNext: Boolean,
    val createdAt: Long,
)

/** Maturity ceilings, ordered from youngest to oldest. Each maps to US movie and TV certifications. */
@Serializable
enum class MaturityLevel(val label: String, val movieCerts: List<String>, val tvCerts: List<String>) {
    LITTLE_KIDS("Little kids", listOf("G"), listOf("TV-Y", "TV-G")),
    OLDER_KIDS("Older kids", listOf("G", "PG"), listOf("TV-Y", "TV-Y7", "TV-G", "TV-PG")),
    TEENS("Teens", listOf("G", "PG", "PG-13"), listOf("TV-Y", "TV-Y7", "TV-G", "TV-PG", "TV-14")),
    ALL("All maturity ratings", listOf("G", "PG", "PG-13", "R", "NC-17"), listOf("TV-Y", "TV-Y7", "TV-G", "TV-PG", "TV-14", "TV-MA"));

    /** The highest movie certification, for TMDB's `certification.lte`. */
    val movieCeiling: String get() = movieCerts.last()

    fun allows(mediaType: MediaType, certification: String?): Boolean {
        if (this == ALL) return true
        if (certification.isNullOrBlank()) return false
        return when (mediaType) {
            MediaType.MOVIE -> certification in movieCerts
            MediaType.TV -> certification in tvCerts
        }
    }
}

const val MAX_PROFILES = 5
const val AVATAR_COUNT = 24
