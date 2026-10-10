package com.nexient.jazzy.feature.discover

import com.nexient.jazzy.core.data.catalog.RailDefinition
import com.nexient.jazzy.core.data.catalog.RailSource
import com.nexient.jazzy.core.model.MediaType
import java.time.LocalDate

/** Default minimum vote count (spec §5.5), so rating sorts aren't topped by titles with 3 votes. */
const val DEFAULT_MIN_VOTES = 100

/** TMDB caps discover at 500 pages; 50 × 20 = 1,000 results is plenty for a grid. */
const val DISCOVER_MAX_PAGES = 50

enum class DiscoverSort(val label: String) {
    POPULARITY("Popularity"),
    RATING("Rating"),
    RELEASE_DATE("Release date"),
    REVENUE("Revenue"),
    TITLE("Title");

    /** Revenue only exists for movies on TMDB. */
    fun supports(type: MediaType): Boolean = this != REVENUE || type == MediaType.MOVIE

    fun apiValue(type: MediaType): String = when (this) {
        POPULARITY -> "popularity.desc"
        RATING -> "vote_average.desc"
        RELEASE_DATE -> if (type == MediaType.MOVIE) "primary_release_date.desc" else "first_air_date.desc"
        REVENUE -> "revenue.desc"
        TITLE -> if (type == MediaType.MOVIE) "original_title.asc" else "original_name.asc"
    }
}

/** A runtime preset in minutes; null bounds are open. */
data class RuntimeRange(val label: String, val min: Int?, val max: Int?)

fun runtimePresets(type: MediaType): List<RuntimeRange> = when (type) {
    MediaType.MOVIE -> listOf(
        RuntimeRange("Under 90 min", null, 89),
        RuntimeRange("90 min – 2 h", 90, 120),
        RuntimeRange("2 – 2½ h", 120, 150),
        RuntimeRange("Over 2½ h", 150, null),
    )
    MediaType.TV -> listOf(
        RuntimeRange("Under 30 min", null, 29),
        RuntimeRange("30 – 60 min", 30, 60),
        RuntimeRange("Over 1 h", 60, null),
    )
}

/** Original-language shortlist (ISO 639-1 → label). */
val DiscoverLanguages = listOf(
    "en" to "English",
    "es" to "Spanish",
    "fr" to "French",
    "de" to "German",
    "it" to "Italian",
    "ja" to "Japanese",
    "ko" to "Korean",
    "zh" to "Chinese",
    "hi" to "Hindi",
    "pt" to "Portuguese",
    "tr" to "Turkish",
)

val RatingOptions = listOf(5, 6, 7, 8)
val VoteOptions = listOf(0, 50, DEFAULT_MIN_VOTES, 500, 1_000, 5_000)

/** The Discover filter set (spec §5.5). Years are inclusive; ratings are TMDB's 0–10 scale. */
data class DiscoverFilters(
    val mediaType: MediaType = MediaType.MOVIE,
    val genreIds: Set<Int> = emptySet(),
    val yearFrom: Int? = null,
    val yearTo: Int? = null,
    val minRating: Int? = null,
    val minVotes: Int = DEFAULT_MIN_VOTES,
    val language: String? = null,
    val runtimeMin: Int? = null,
    val runtimeMax: Int? = null,
    val sort: DiscoverSort = DiscoverSort.POPULARITY,
) {
    /** How many filters differ from the defaults (media type and sort aren't counted). */
    val activeCount: Int
        get() = listOf(
            genreIds.isNotEmpty(),
            yearFrom != null || yearTo != null,
            minRating != null,
            minVotes != DEFAULT_MIN_VOTES,
            language != null,
            runtimeMin != null || runtimeMax != null,
        ).count { it }

    fun hasRuntime(range: RuntimeRange) = runtimeMin == range.min && runtimeMax == range.max && (range.min != null || range.max != null)

    /** Switching type keeps compatible choices: an unsupported sort falls back, runtime presets reset. */
    fun withMediaType(type: MediaType): DiscoverFilters = if (type == mediaType) this else copy(
        mediaType = type,
        sort = if (sort.supports(type)) sort else DiscoverSort.POPULARITY,
        runtimeMin = null,
        runtimeMax = null,
    )

    /** Keeps [from] ≤ [to] by moving the other bound. */
    fun withYears(from: Int?, to: Int?): DiscoverFilters = when {
        from != null && to != null && from > to -> copy(yearFrom = to, yearTo = from)
        else -> copy(yearFrom = from, yearTo = to)
    }

    /**
     * TMDB `/discover/{type}` query parameters. Genres are comma-joined (AND). When sorting by
     * release date with no upper year, unreleased titles are cut off at [today].
     */
    fun toParams(today: LocalDate): Map<String, String> {
        val movie = mediaType == MediaType.MOVIE
        val dateKey = if (movie) "primary_release_date" else "first_air_date"
        val params = linkedMapOf("sort_by" to sort.apiValue(mediaType))
        if (genreIds.isNotEmpty()) params["with_genres"] = genreIds.sorted().joinToString(",")
        yearFrom?.let { params["$dateKey.gte"] = "$it-01-01" }
        when {
            yearTo != null -> params["$dateKey.lte"] = "$yearTo-12-31"
            sort == DiscoverSort.RELEASE_DATE -> params["$dateKey.lte"] = today.toString()
        }
        minRating?.let { params["vote_average.gte"] = it.toString() }
        if (minVotes > 0) params["vote_count.gte"] = minVotes.toString()
        language?.let { params["with_original_language"] = it }
        runtimeMin?.let { params["with_runtime.gte"] = it.toString() }
        runtimeMax?.let { params["with_runtime.lte"] = it.toString() }
        return params
    }

    fun toSource(today: LocalDate): RailSource.Discover = RailSource.Discover(mediaType, toParams(today))

    fun toRail(today: LocalDate): RailDefinition =
        RailDefinition("discover", "Discover", listOf(toSource(today)), maxPages = DISCOVER_MAX_PAGES)
}
