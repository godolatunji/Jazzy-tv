package com.nexient.jazzy.core.recommendations

import com.nexient.jazzy.core.model.MediaType

/**
 * One genre space across movies and TV (spec §5.7 "Movie ↔ TV genre mapping").
 * [movieIds]/[tvIds] are the TMDB IDs that map *into* this genre; [movieQueryIds]/[tvQueryIds]
 * are what to send to Discover for each media type (movie-only genres borrow their nearest TV genre).
 */
enum class UnifiedGenre(
    val id: String,
    val label: String,
    val movieIds: Set<Int>,
    val tvIds: Set<Int>,
    val movieQueryIds: Set<Int> = movieIds,
    val tvQueryIds: Set<Int> = tvIds,
) {
    ACTION_ADVENTURE("action_adventure", "Action & Adventure", setOf(28, 12), setOf(10759)),
    SCIFI_FANTASY("scifi_fantasy", "Sci-Fi & Fantasy", setOf(878, 14), setOf(10765)),
    WAR_POLITICS("war_politics", "War & Politics", setOf(10752), setOf(10768)),
    KIDS_FAMILY("kids_family", "Kids & Family", setOf(10751), setOf(10751, 10762)),
    ANIMATION("animation", "Animation", setOf(16), setOf(16)),
    COMEDY("comedy", "Comedy", setOf(35), setOf(35)),
    CRIME("crime", "Crime", setOf(80), setOf(80)),
    DOCUMENTARY("documentary", "Documentary", setOf(99), setOf(99)),
    DRAMA("drama", "Drama", setOf(18), setOf(18)),
    MYSTERY("mystery", "Mystery", setOf(9648), setOf(9648)),
    WESTERN("western", "Western", setOf(37), setOf(37)),
    HISTORY("history", "History", setOf(36), emptySet(), tvQueryIds = setOf(99, 10768)),
    HORROR("horror", "Horror", setOf(27), emptySet(), tvQueryIds = setOf(9648)),
    MUSIC("music", "Music", setOf(10402), emptySet(), tvQueryIds = setOf(99)),
    ROMANCE("romance", "Romance", setOf(10749), emptySet(), tvQueryIds = setOf(18)),
    THRILLER("thriller", "Thriller", setOf(53), emptySet(), tvQueryIds = setOf(9648, 80)),
    TV_MOVIE("tv_movie", "TV Movie", setOf(10770), emptySet(), tvQueryIds = setOf(18)),
    NEWS("news", "News", emptySet(), setOf(10763), movieQueryIds = setOf(99)),
    REALITY("reality", "Reality", emptySet(), setOf(10764), movieQueryIds = setOf(99)),
    SOAP("soap", "Soap", emptySet(), setOf(10766), movieQueryIds = setOf(18)),
    TALK("talk", "Talk", emptySet(), setOf(10767), movieQueryIds = setOf(35));

    fun queryIds(mediaType: MediaType): Set<Int> = when (mediaType) {
        MediaType.MOVIE -> movieQueryIds
        MediaType.TV -> tvQueryIds
    }

    companion object {
        private val byMovieId = entries.flatMap { g -> g.movieIds.map { it to g } }.toMap()
        private val byTvId = entries.flatMap { g -> g.tvIds.map { it to g } }.toMap()
        private val byId = entries.associateBy { it.id }

        fun fromId(id: String): UnifiedGenre? = byId[id]

        /** Unified genres for a title's TMDB genre IDs (duplicates collapsed). */
        fun of(mediaType: MediaType, tmdbGenreIds: Collection<Int>): Set<UnifiedGenre> = tmdbGenreIds.mapNotNull {
            when (mediaType) {
                MediaType.MOVIE -> byMovieId[it] ?: byTvId[it]
                MediaType.TV -> byTvId[it] ?: byMovieId[it]
            }
        }.toSet()

        /** Genres offered in the profile "pick genres you like" step (PR-3). */
        val pickable: List<UnifiedGenre> = listOf(
            ACTION_ADVENTURE, COMEDY, DRAMA, SCIFI_FANTASY, THRILLER, HORROR, ROMANCE, CRIME,
            MYSTERY, ANIMATION, DOCUMENTARY, KIDS_FAMILY, HISTORY, WAR_POLITICS, WESTERN, MUSIC, REALITY,
        )
    }
}
