package com.nexient.jazzy.core.recommendations

import com.nexient.jazzy.core.model.MediaType
import com.nexient.jazzy.core.model.TitleSummary

/** A Discover query to run for a recommendation rail. */
data class DiscoverQuery(val mediaType: MediaType, val filters: Map<String, String>, val pages: Int)

/** Rail definitions and re-ranking from spec §5.7 "Building the rails". */
object RecommendationPlanner {
    const val RECOMMENDED_RAIL = "recommended_for_you"
    const val RECOMMENDED_LIMIT = 40
    const val GENRE_RAIL_LIMIT = 20
    const val MAX_OVERLAP_WITH_RECOMMENDED = 10

    /** Movie/TV split from history, with at least 30% of each when both were watched. */
    fun mediaSplit(movieShare: Double?): Map<MediaType, Double> {
        if (movieShare == null) return mapOf(MediaType.MOVIE to 0.5, MediaType.TV to 0.5)
        if (movieShare >= 1.0) return mapOf(MediaType.MOVIE to 1.0)
        if (movieShare <= 0.0) return mapOf(MediaType.TV to 1.0)
        val m = movieShare.coerceIn(0.3, 0.7)
        return mapOf(MediaType.MOVIE to m, MediaType.TV to 1 - m)
    }

    fun recommendedQueries(top: List<UnifiedGenre>, split: Map<MediaType, Double>): List<DiscoverQuery> =
        split.keys.mapNotNull { type ->
            val ids = top.take(3).flatMap { it.queryIds(type) }.distinct()
            if (ids.isEmpty()) return@mapNotNull null
            DiscoverQuery(
                mediaType = type,
                filters = mapOf(
                    "with_genres" to ids.joinToString("|"),
                    "sort_by" to "popularity.desc",
                    "vote_count.gte" to "200",
                    "vote_average.gte" to "6.5",
                ),
                pages = 3,
            )
        }

    fun genreRailQueries(genre: UnifiedGenre, split: Map<MediaType, Double>): List<DiscoverQuery> =
        split.keys.mapNotNull { type ->
            val ids = genre.queryIds(type)
            if (ids.isEmpty()) return@mapNotNull null
            DiscoverQuery(
                mediaType = type,
                filters = mapOf(
                    "with_genres" to ids.joinToString("|"),
                    "sort_by" to "vote_average.desc",
                    "vote_count.gte" to "500",
                ),
                pages = 1,
            )
        }

    /** score = Σ affinity over the item's genres × (vote_average / 10). */
    fun score(item: TitleSummary, affinity: Map<UnifiedGenre, Double>): Double {
        val genres = UnifiedGenre.of(item.mediaType, item.genreIds)
        return genres.sumOf { affinity[it] ?: 0.0 } * (item.voteAverage / 10.0)
    }

    /**
     * Merges candidates, removes exclusions and duplicates, interleaves media types by [split]
     * and keeps the best [limit] by score.
     */
    fun rank(
        candidates: List<TitleSummary>,
        affinity: Map<UnifiedGenre, Double>,
        excluded: Set<Pair<Int, MediaType>>,
        split: Map<MediaType, Double>,
        limit: Int,
    ): List<TitleSummary> {
        val unique = candidates.distinctBy { it.tmdbId to it.mediaType }
            .filter { (it.tmdbId to it.mediaType) !in excluded }
        val byType = unique.groupBy { it.mediaType }
            .mapValues { (_, list) -> list.sortedByDescending { score(it, affinity) } }
        val quotas = split.mapValues { (_, share) -> (limit * share).toInt().coerceAtLeast(1) }
        val picked = split.keys.flatMap { type -> byType[type].orEmpty().take(quotas[type] ?: 0) }
        return picked.sortedByDescending { score(it, affinity) }.take(limit)
    }
}
