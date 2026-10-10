package com.nexient.jazzy.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// TMDB v3 DTOs. Every field is nullable or defaulted: TMDB omits fields freely.

@Serializable
data class PageDto<T>(
    val page: Int = 1,
    val results: List<T> = emptyList(),
    @SerialName("total_pages") val totalPages: Int = 1,
    @SerialName("total_results") val totalResults: Int = 0,
)

/** A movie, show or person from list, discover, trending and search endpoints. */
@Serializable
data class MediaItemDto(
    val id: Int,
    @SerialName("media_type") val mediaType: String? = null,
    val title: String? = null,
    val name: String? = null,
    @SerialName("original_title") val originalTitle: String? = null,
    @SerialName("original_name") val originalName: String? = null,
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("profile_path") val profilePath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int? = null,
    val popularity: Double? = null,
    @SerialName("genre_ids") val genreIds: List<Int>? = null,
    @SerialName("original_language") val originalLanguage: String? = null,
    val adult: Boolean? = null,
    @SerialName("known_for_department") val knownForDepartment: String? = null,
)

@Serializable
data class GenreDto(val id: Int, val name: String = "")

@Serializable
data class GenreListDto(val genres: List<GenreDto> = emptyList())

@Serializable
data class ConfigurationDto(val images: ImagesConfigDto? = null)

@Serializable
data class ImagesConfigDto(
    @SerialName("secure_base_url") val secureBaseUrl: String? = null,
    @SerialName("poster_sizes") val posterSizes: List<String> = emptyList(),
    @SerialName("backdrop_sizes") val backdropSizes: List<String> = emptyList(),
    @SerialName("logo_sizes") val logoSizes: List<String> = emptyList(),
    @SerialName("still_sizes") val stillSizes: List<String> = emptyList(),
)

@Serializable
data class VideoDto(
    val key: String? = null,
    val site: String? = null,
    val type: String? = null,
    val official: Boolean? = null,
    val name: String? = null,
)

@Serializable
data class VideosDto(val results: List<VideoDto> = emptyList())

@Serializable
data class CastDto(
    val id: Int,
    val name: String = "",
    val character: String? = null,
    @SerialName("profile_path") val profilePath: String? = null,
    val order: Int? = null,
)

@Serializable
data class CrewDto(val id: Int, val name: String = "", val job: String? = null, val department: String? = null)

@Serializable
data class CreditsDto(val cast: List<CastDto> = emptyList(), val crew: List<CrewDto> = emptyList())

@Serializable
data class ImageDto(
    @SerialName("file_path") val filePath: String? = null,
    @SerialName("iso_639_1") val language: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("aspect_ratio") val aspectRatio: Double? = null,
)

@Serializable
data class ImagesDto(
    val logos: List<ImageDto> = emptyList(),
    val backdrops: List<ImageDto> = emptyList(),
    val posters: List<ImageDto> = emptyList(),
)

@Serializable
data class ReleaseDateDto(val certification: String? = null, val type: Int? = null)

@Serializable
data class ReleaseDatesCountryDto(
    @SerialName("iso_3166_1") val country: String,
    @SerialName("release_dates") val releaseDates: List<ReleaseDateDto> = emptyList(),
)

@Serializable
data class ReleaseDatesDto(val results: List<ReleaseDatesCountryDto> = emptyList())

@Serializable
data class ContentRatingDto(@SerialName("iso_3166_1") val country: String, val rating: String? = null)

@Serializable
data class ContentRatingsDto(val results: List<ContentRatingDto> = emptyList())

@Serializable
data class ExternalIdsDto(
    @SerialName("imdb_id") val imdbId: String? = null,
    @SerialName("tvdb_id") val tvdbId: Int? = null,
)

@Serializable
data class SeasonSummaryDto(
    @SerialName("season_number") val seasonNumber: Int,
    val name: String? = null,
    @SerialName("episode_count") val episodeCount: Int? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("air_date") val airDate: String? = null,
)

@Serializable
data class CreatorDto(val id: Int, val name: String = "")

/** Movie or TV details with `append_to_response` payloads. */
@Serializable
data class DetailsDto(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    @SerialName("original_title") val originalTitle: String? = null,
    @SerialName("original_name") val originalName: String? = null,
    val overview: String? = null,
    val tagline: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int? = null,
    val popularity: Double? = null,
    val genres: List<GenreDto> = emptyList(),
    @SerialName("original_language") val originalLanguage: String? = null,
    val adult: Boolean? = null,
    val runtime: Int? = null,
    @SerialName("episode_run_time") val episodeRunTime: List<Int> = emptyList(),
    @SerialName("number_of_seasons") val numberOfSeasons: Int? = null,
    val seasons: List<SeasonSummaryDto> = emptyList(),
    @SerialName("created_by") val createdBy: List<CreatorDto> = emptyList(),
    val status: String? = null,
    @SerialName("in_production") val inProduction: Boolean? = null,
    val videos: VideosDto? = null,
    val credits: CreditsDto? = null,
    val images: ImagesDto? = null,
    @SerialName("release_dates") val releaseDates: ReleaseDatesDto? = null,
    @SerialName("content_ratings") val contentRatings: ContentRatingsDto? = null,
    @SerialName("external_ids") val externalIds: ExternalIdsDto? = null,
    val recommendations: PageDto<MediaItemDto>? = null,
    val similar: PageDto<MediaItemDto>? = null,
)

@Serializable
data class EpisodeDto(
    @SerialName("episode_number") val episodeNumber: Int,
    @SerialName("season_number") val seasonNumber: Int,
    val name: String? = null,
    val overview: String? = null,
    @SerialName("still_path") val stillPath: String? = null,
    val runtime: Int? = null,
    @SerialName("air_date") val airDate: String? = null,
)

@Serializable
data class SeasonDto(
    @SerialName("season_number") val seasonNumber: Int,
    val name: String? = null,
    val overview: String? = null,
    val episodes: List<EpisodeDto> = emptyList(),
)

@Serializable
data class CombinedCreditsDto(val cast: List<MediaItemDto> = emptyList(), val crew: List<MediaItemDto> = emptyList())
