package com.nexient.jazzy.core.network

import com.nexient.jazzy.core.model.CastMember
import com.nexient.jazzy.core.model.Episode
import com.nexient.jazzy.core.model.Genre
import com.nexient.jazzy.core.model.MediaType
import com.nexient.jazzy.core.model.PersonSummary
import com.nexient.jazzy.core.model.SearchResult
import com.nexient.jazzy.core.model.Season
import com.nexient.jazzy.core.model.SeasonSummary
import com.nexient.jazzy.core.model.TitleDetails
import com.nexient.jazzy.core.model.TitleSummary

/** Maps a list item; [fallbackType] is used when the endpoint doesn't say (e.g. `movie/popular`). */
fun MediaItemDto.toSummary(fallbackType: MediaType? = null): TitleSummary? {
    val type = MediaType.fromApi(mediaType) ?: fallbackType ?: return null
    val displayTitle = (if (type == MediaType.MOVIE) title else name) ?: title ?: name ?: return null
    return TitleSummary(
        tmdbId = id,
        mediaType = type,
        title = displayTitle,
        originalTitle = (if (type == MediaType.MOVIE) originalTitle else originalName) ?: displayTitle,
        overview = overview.orEmpty(),
        posterPath = posterPath,
        backdropPath = backdropPath,
        releaseDate = (if (type == MediaType.MOVIE) releaseDate else firstAirDate)?.takeIf { it.isNotBlank() },
        voteAverage = voteAverage ?: 0.0,
        voteCount = voteCount ?: 0,
        popularity = popularity ?: 0.0,
        genreIds = genreIds.orEmpty(),
        originalLanguage = originalLanguage,
        adult = adult ?: false,
    )
}

fun MediaItemDto.toSearchResult(): SearchResult? = when (mediaType) {
    "person" -> SearchResult.Person(PersonSummary(id, name ?: return null, profilePath, knownForDepartment))
    else -> toSummary()?.let { SearchResult.Title(it) }
}

fun DetailsDto.toDetails(type: MediaType, region: String, language: String): TitleDetails {
    val summary = TitleSummary(
        tmdbId = id,
        mediaType = type,
        title = (if (type == MediaType.MOVIE) title else name) ?: title ?: name ?: "",
        originalTitle = (if (type == MediaType.MOVIE) originalTitle else originalName) ?: title ?: name ?: "",
        overview = overview.orEmpty(),
        posterPath = posterPath,
        backdropPath = backdropPath,
        releaseDate = (if (type == MediaType.MOVIE) releaseDate else firstAirDate)?.takeIf { it.isNotBlank() },
        voteAverage = voteAverage ?: 0.0,
        voteCount = voteCount ?: 0,
        popularity = popularity ?: 0.0,
        genreIds = genres.map { it.id },
        originalLanguage = originalLanguage,
        adult = adult ?: false,
    )
    val lang = language.substringBefore('-')
    val logo = images?.logos.orEmpty()
        .filter { it.filePath != null && !it.filePath.endsWith(".svg", ignoreCase = true) && (it.language == lang || it.language == "en" || it.language == null) }
        .sortedWith(compareBy<ImageDto> { if (it.language == lang) 0 else if (it.language == "en") 1 else 2 }.thenByDescending { it.voteAverage ?: 0.0 })
        .firstOrNull()?.filePath
    val trailer = videos?.results.orEmpty()
        .filter { it.site == "YouTube" && it.key != null }
        .sortedWith(compareBy<VideoDto> { if (it.type == "Trailer") 0 else if (it.type == "Teaser") 1 else 2 }.thenBy { if (it.official == true) 0 else 1 })
        .firstOrNull()?.key
    return TitleDetails(
        summary = summary,
        tagline = tagline?.takeIf { it.isNotBlank() },
        runtimeMinutes = runtime ?: episodeRunTime.firstOrNull(),
        numberOfSeasons = numberOfSeasons,
        genres = genres.map { Genre(it.id, it.name) },
        cast = credits?.cast.orEmpty().sortedBy { it.order ?: Int.MAX_VALUE }.take(10)
            .map { CastMember(it.id, it.name, it.character, it.profilePath) },
        directors = credits?.crew.orEmpty().filter { it.job == "Director" }.map { it.name }.distinct(),
        creators = createdBy.map { it.name },
        certification = if (type == MediaType.MOVIE) movieCertification(region) else tvCertification(region),
        imdbId = externalIds?.imdbId?.takeIf { it.isNotBlank() },
        tvdbId = externalIds?.tvdbId,
        logoPath = logo,
        trailerYoutubeKey = trailer,
        seasons = seasons.sortedWith(compareBy { if (it.seasonNumber == 0) Int.MAX_VALUE else it.seasonNumber })
            .map { SeasonSummary(it.seasonNumber, it.name ?: "Season ${it.seasonNumber}", it.episodeCount ?: 0, it.posterPath, it.airDate) },
        recommendations = (recommendations?.results.orEmpty().ifEmpty { similar?.results.orEmpty() })
            .mapNotNull { it.toSummary(type) },
        status = status,
        inProduction = inProduction ?: false,
    )
}

fun DetailsDto.movieCertification(region: String): String? = releaseDates.certificationFor(region)

fun ReleaseDatesDto?.certificationFor(region: String): String? {
    val results = this?.results.orEmpty()
    fun forCountry(c: String) = results.firstOrNull { it.country == c }?.releaseDates
        ?.sortedBy { it.type ?: 99 }?.mapNotNull { it.certification?.takeIf(String::isNotBlank) }?.firstOrNull()
    return forCountry(region) ?: forCountry("US")
}

fun DetailsDto.tvCertification(region: String): String? = contentRatings.ratingFor(region)

fun ContentRatingsDto?.ratingFor(region: String): String? {
    val results = this?.results.orEmpty()
    return (results.firstOrNull { it.country == region } ?: results.firstOrNull { it.country == "US" })
        ?.rating?.takeIf { it.isNotBlank() }
}

fun SeasonDto.toSeason(showId: Int) = Season(
    showId = showId,
    seasonNumber = seasonNumber,
    name = name ?: "Season $seasonNumber",
    overview = overview.orEmpty(),
    episodes = episodes.map {
        Episode(showId, it.seasonNumber, it.episodeNumber, it.name ?: "Episode ${it.episodeNumber}", it.overview.orEmpty(), it.stillPath, it.runtime, it.airDate)
    },
)
