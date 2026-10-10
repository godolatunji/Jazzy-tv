package com.nexient.jazzy.core.recommendations

import com.google.common.truth.Truth.assertThat
import com.nexient.jazzy.core.model.MediaType
import com.nexient.jazzy.core.model.TitleSummary
import org.junit.Test

class AffinityCalculatorTest {
    @Test
    fun movieAndTvGenresLandInTheSameUnifiedGenre() {
        assertThat(UnifiedGenre.of(MediaType.MOVIE, listOf(878))).containsExactly(UnifiedGenre.SCIFI_FANTASY)
        assertThat(UnifiedGenre.of(MediaType.TV, listOf(10765))).containsExactly(UnifiedGenre.SCIFI_FANTASY)
        assertThat(UnifiedGenre.of(MediaType.MOVIE, listOf(28, 12))).containsExactly(UnifiedGenre.ACTION_ADVENTURE)
    }

    @Test
    fun engagementBuckets() {
        assertThat(AffinityCalculator.engagement(0.95f)).isEqualTo(1.0)
        assertThat(AffinityCalculator.engagement(0.6f)).isEqualTo(0.7)
        assertThat(AffinityCalculator.engagement(0.2f)).isEqualTo(0.4)
        assertThat(AffinityCalculator.engagement(0.05f)).isEqualTo(0.1)
    }

    @Test
    fun recentFullWatchesOutweighOldAbandonedOnes() {
        val signals = listOf(
            TitleSignal(MediaType.MOVIE, listOf(878), completion = 1f, daysSinceLastPlayed = 1.0),
            TitleSignal(MediaType.TV, listOf(10765), completion = 0.95f, daysSinceLastPlayed = 3.0),
            TitleSignal(MediaType.MOVIE, listOf(35), completion = 0.05f, daysSinceLastPlayed = 60.0),
        )
        val result = AffinityCalculator.compute(signals, emptySet(), sessionCount = 3)
        assertThat(result.first().first).isEqualTo(UnifiedGenre.SCIFI_FANTASY)
        assertThat(result.first().second).isEqualTo(1.0)
        assertThat(result.last().first).isEqualTo(UnifiedGenre.COMEDY)
        assertThat(result.last().second).isLessThan(0.05)
    }

    @Test
    fun likesAreTheStrongestSignal() {
        val signals = listOf(
            TitleSignal(MediaType.MOVIE, listOf(18), completion = 1f, daysSinceLastPlayed = 0.0),
            TitleSignal(MediaType.MOVIE, listOf(27), completion = null, daysSinceLastPlayed = null, liked = true, daysSinceLiked = 0.0),
        )
        val result = AffinityCalculator.compute(signals, emptySet(), sessionCount = 1)
        assertThat(result.first().first).isEqualTo(UnifiedGenre.HORROR)
    }

    @Test
    fun seedsCountUntilTenSessionsThenFade() {
        val early = AffinityCalculator.compute(emptyList(), setOf(UnifiedGenre.WESTERN), sessionCount = 0)
        assertThat(early.single().first).isEqualTo(UnifiedGenre.WESTERN)
        val late = AffinityCalculator.compute(emptyList(), setOf(UnifiedGenre.WESTERN), sessionCount = 10)
        assertThat(late).isEmpty()
    }

    @Test
    fun noSignalMeansNoRecommendations() {
        assertThat(AffinityCalculator.compute(emptyList(), emptySet(), 0)).isEmpty()
    }

    @Test
    fun rankingExcludesWatchedAndRespectsSplit() {
        val affinity = mapOf(UnifiedGenre.SCIFI_FANTASY to 1.0, UnifiedGenre.DRAMA to 0.2)
        fun t(id: Int, type: MediaType, genres: List<Int>, vote: Double) =
            TitleSummary(id, type, "t$id", genreIds = genres, voteAverage = vote)
        val candidates = listOf(
            t(1, MediaType.MOVIE, listOf(878), 8.0),
            t(2, MediaType.MOVIE, listOf(18), 9.0),
            t(3, MediaType.TV, listOf(10765), 7.0),
            t(4, MediaType.MOVIE, listOf(878), 7.5),
        )
        val ranked = RecommendationPlanner.rank(
            candidates, affinity, excluded = setOf(4 to MediaType.MOVIE),
            split = mapOf(MediaType.MOVIE to 0.5, MediaType.TV to 0.5), limit = 4,
        )
        assertThat(ranked.map { it.tmdbId }).containsExactly(1, 3, 2).inOrder()
    }

    @Test
    fun recommendedQueryUsesTopThreeGenresPerMediaType() {
        val queries = RecommendationPlanner.recommendedQueries(
            listOf(UnifiedGenre.SCIFI_FANTASY, UnifiedGenre.HORROR, UnifiedGenre.COMEDY, UnifiedGenre.DRAMA),
            RecommendationPlanner.mediaSplit(0.5),
        )
        val tv = queries.first { it.mediaType == MediaType.TV }
        assertThat(tv.filters["with_genres"]).isEqualTo("10765|9648|35")
        val movie = queries.first { it.mediaType == MediaType.MOVIE }
        assertThat(movie.filters["with_genres"]).isEqualTo("878|14|27|35")
    }
}
