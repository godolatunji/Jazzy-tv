package com.nexient.jazzy.core.model

import kotlinx.serialization.Serializable

/** Identifies a movie or show on TMDB. */
@Serializable
data class TitleRef(val tmdbId: Int, val mediaType: MediaType)

/** A title as it appears on cards and rails. */
@Serializable
data class TitleSummary(
    val tmdbId: Int,
    val mediaType: MediaType,
    val title: String,
    val originalTitle: String = title,
    val overview: String = "",
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val releaseDate: String? = null,
    val voteAverage: Double = 0.0,
    val voteCount: Int = 0,
    val popularity: Double = 0.0,
    val genreIds: List<Int> = emptyList(),
    val originalLanguage: String? = null,
    val adult: Boolean = false,
) {
    val ref: TitleRef get() = TitleRef(tmdbId, mediaType)
    val year: Int? get() = releaseDate?.take(4)?.toIntOrNull()
}

@Serializable
data class Genre(val id: Int, val name: String)

@Serializable
data class CastMember(val id: Int, val name: String, val character: String?, val profilePath: String?)

@Serializable
data class SeasonSummary(
    val seasonNumber: Int,
    val name: String,
    val episodeCount: Int,
    val posterPath: String?,
    val airDate: String?,
)

@Serializable
data class Episode(
    val showId: Int,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val name: String,
    val overview: String,
    val stillPath: String?,
    val runtimeMinutes: Int?,
    val airDate: String?,
)

@Serializable
data class Season(
    val showId: Int,
    val seasonNumber: Int,
    val name: String,
    val overview: String,
    val episodes: List<Episode>,
)

@Serializable
data class TitleDetails(
    val summary: TitleSummary,
    val tagline: String? = null,
    val runtimeMinutes: Int? = null,
    val numberOfSeasons: Int? = null,
    val genres: List<Genre> = emptyList(),
    val cast: List<CastMember> = emptyList(),
    val directors: List<String> = emptyList(),
    val creators: List<String> = emptyList(),
    /** Certification for the profile's region, e.g. "PG-13" or "TV-MA". */
    val certification: String? = null,
    val imdbId: String? = null,
    val tvdbId: Int? = null,
    val logoPath: String? = null,
    val trailerYoutubeKey: String? = null,
    val seasons: List<SeasonSummary> = emptyList(),
    val recommendations: List<TitleSummary> = emptyList(),
    val status: String? = null,
    val inProduction: Boolean = false,
) {
    val tmdbId: Int get() = summary.tmdbId
    val mediaType: MediaType get() = summary.mediaType
    val title: String get() = summary.title
    val year: Int? get() = summary.year
    /** True when the release date is in the future (spec DT-6). */
    fun isUnreleased(todayIso: String): Boolean = summary.releaseDate?.let { it > todayIso } ?: false
}

@Serializable
data class PersonSummary(val id: Int, val name: String, val profilePath: String?, val knownFor: String?)

/** One item of a /search/multi response. */
@Serializable
sealed interface SearchResult {
    @Serializable
    data class Title(val title: TitleSummary) : SearchResult

    @Serializable
    data class Person(val person: PersonSummary) : SearchResult
}
