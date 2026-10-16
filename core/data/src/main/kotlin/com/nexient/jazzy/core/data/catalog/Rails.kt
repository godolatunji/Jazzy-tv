package com.nexient.jazzy.core.data.catalog

import com.nexient.jazzy.core.model.MediaType

/** Where a rail's titles come from. */
sealed interface RailSource {
    val cacheKey: String

    data class Trending(val mediaType: MediaType?, val window: String = "week") : RailSource {
        override val cacheKey get() = "trending:${mediaType?.apiValue ?: "all"}:$window"
    }

    /** `movie/popular`, `tv/on_the_air`, … */
    data class TmdbList(val mediaType: MediaType, val list: String) : RailSource {
        override val cacheKey get() = "list:${mediaType.apiValue}:$list"
    }

    data class Discover(val mediaType: MediaType, val filters: Map<String, String>) : RailSource {
        override val cacheKey get() = "discover:${mediaType.apiValue}:" + filters.toSortedMap().entries.joinToString("&") { "${it.key}=${it.value}" }
    }
}

enum class RailStyle { POSTER, BACKDROP_PROGRESS, TOP_TEN }

/**
 * A Home rail definition (spec §5.2). A rail with [fullPage] has its own page that pages through
 * the same query without the [maxPages] cap; see [fullPageRail].
 */
data class RailDefinition(
    val key: String,
    val title: String,
    val sources: List<RailSource>,
    val style: RailStyle = RailStyle.POSTER,
    val maxPages: Int = 5,
    val fullPage: Boolean = false,
)

/** TMDB serves at most 500 pages of any list or discover query. */
const val TMDB_MAX_PAGES = 500

/** Originals rails stop at 40 titles on Home; the rest are on the network's own page. */
private const val ORIGINALS_RAIL_PAGES = 2

/** Home tabs: All, Movies only, TV only (HM-4). */
enum class HomeTab(val mediaType: MediaType?) { ALL(null), MOVIES(MediaType.MOVIE), TV(MediaType.TV) }

/** Default Originals networks (spec §5.2 note); seeded into `originals_network`. */
data class OriginalsNetwork(val id: Int, val name: String)

val DefaultOriginals = listOf(
    OriginalsNetwork(213, "Netflix"),
    OriginalsNetwork(49, "HBO"),
    OriginalsNetwork(2552, "Apple TV+"),
    OriginalsNetwork(1024, "Prime Video"),
    OriginalsNetwork(2739, "Disney+"),
)

/** The TMDB-backed Home rails for a tab, in spec HM-3 order. Local and recommendation rails are added by Home. */
fun tmdbRails(tab: HomeTab, networks: List<OriginalsNetwork> = DefaultOriginals): List<RailDefinition> {
    val types = tab.mediaType?.let { listOf(it) } ?: listOf(MediaType.MOVIE, MediaType.TV)
    fun lists(movieList: String, tvList: String) = types.map {
        RailSource.TmdbList(it, if (it == MediaType.MOVIE) movieList else tvList)
    }
    val rails = mutableListOf(
        RailDefinition("trending", "Trending Now", listOf(RailSource.Trending(tab.mediaType, "week"))),
        RailDefinition("top10", "Top 10 Today", listOf(RailSource.Trending(tab.mediaType, "day")), RailStyle.TOP_TEN, maxPages = 1),
        RailDefinition("popular", "Popular", lists("popular", "popular")),
    )
    // Originals: one rail per top-3 network, the rest mixed (plan decision D7).
    if (tab != HomeTab.MOVIES) {
        networks.take(3).forEach { n ->
            rails += RailDefinition(
                "originals_${n.id}", "${n.name} Originals",
                listOf(RailSource.Discover(MediaType.TV, mapOf("with_networks" to n.id.toString(), "sort_by" to "popularity.desc"))),
                maxPages = ORIGINALS_RAIL_PAGES,
                fullPage = true,
            )
        }
        if (networks.size > 3) {
            rails += RailDefinition(
                "originals_more", "More Originals",
                listOf(RailSource.Discover(MediaType.TV, mapOf("with_networks" to networks.drop(3).joinToString("|") { it.id.toString() }, "sort_by" to "popularity.desc"))),
                maxPages = ORIGINALS_RAIL_PAGES,
                fullPage = true,
            )
        }
    }
    rails += RailDefinition("top_rated", "Top Rated", lists("top_rated", "top_rated"))
    rails += RailDefinition("upcoming", if (tab == HomeTab.TV) "New Episodes" else "Upcoming", lists("upcoming", "on_the_air"))
    rails += RailDefinition("now_playing", if (tab == HomeTab.TV) "Airing Today" else "Now Playing", lists("now_playing", "airing_today"))
    genreRails(tab).forEach { rails += it }
    return rails
}

/** The full-page version of the rail with [key] (an Originals network): the same query, every page. */
fun fullPageRail(key: String, networks: List<OriginalsNetwork> = DefaultOriginals): RailDefinition? =
    tmdbRails(HomeTab.ALL, networks).firstOrNull { it.key == key && it.fullPage }?.copy(maxPages = TMDB_MAX_PAGES)

private fun genreRails(tab: HomeTab): List<RailDefinition> {
    val genres = listOf(
        Triple("Action & Adventure", 28, 10759),
        Triple("Comedy", 35, 35),
        Triple("Sci-Fi & Fantasy", 878, 10765),
        Triple("Crime", 80, 80),
        Triple("Documentaries", 99, 99),
        Triple("Animation", 16, 16),
    )
    val types = tab.mediaType?.let { listOf(it) } ?: listOf(MediaType.MOVIE, MediaType.TV)
    return genres.map { (name, movieId, tvId) ->
        RailDefinition(
            "genre_${name.lowercase().filter(Char::isLetter)}", name,
            types.map {
                RailSource.Discover(it, mapOf("with_genres" to (if (it == MediaType.MOVIE) movieId else tvId).toString(), "sort_by" to "popularity.desc", "vote_count.gte" to "100"))
            },
        )
    }
}
