package com.nexient.jazzy.core.sources.parse

import com.google.common.truth.Truth.assertThat
import com.nexient.jazzy.core.model.MediaType
import com.nexient.jazzy.core.model.SourceQuery
import org.junit.Test

class QueryBuilderTest {
    private fun movie(title: String, year: Int?, original: String? = null) =
        SourceQuery(tmdbId = 1, mediaType = MediaType.MOVIE, title = title, originalTitle = original, year = year, imdbId = null)

    private fun episode(show: String, season: Int?, episode: Int?) =
        SourceQuery(tmdbId = 2, mediaType = MediaType.TV, title = show, originalTitle = show, year = 2019, imdbId = null, season = season, episode = episode)

    @Test fun `movie query is title plus year`() {
        assertThat(QueryBuilder.build(movie("The Quiet Harbor", 2019, "The Quiet Harbor"))).containsExactly("The Quiet Harbor 2019")
    }

    @Test fun `movie without year is the title alone`() {
        assertThat(QueryBuilder.build(movie("The Quiet Harbor", null))).containsExactly("The Quiet Harbor")
    }

    @Test fun `punctuation is stripped for searching`() {
        assertThat(QueryBuilder.build(movie("Spider-Man: No Way Home", 2021))).containsExactly("Spider Man No Way Home 2021")
    }

    @Test fun `ampersand becomes and, apostrophes and accents are dropped`() {
        assertThat(QueryBuilder.build(movie("Fast & Furious", 2009))).containsExactly("Fast and Furious 2009")
        assertThat(QueryBuilder.build(movie("Ocean's Eleven", 2001))).containsExactly("Oceans Eleven 2001")
        assertThat(QueryBuilder.build(movie("Amélie", 2001))).containsExactly("Amelie 2001")
    }

    @Test fun `different original title adds a second query`() {
        assertThat(QueryBuilder.build(movie("Amélie", 2001, "Le Fabuleux Destin d'Amélie Poulain")))
            .containsExactly("Amelie 2001", "Le Fabuleux Destin dAmelie Poulain 2001").inOrder()
    }

    @Test fun `original title equal after normalisation is not repeated`() {
        assertThat(QueryBuilder.build(movie("Spider-Man: Homecoming", 2017, "Spider-Man Homecoming"))).hasSize(1)
    }

    @Test fun `original title in a non-latin script is skipped`() {
        assertThat(QueryBuilder.build(movie("Lantern Keeper", 2015, "灯台守"))).containsExactly("Lantern Keeper 2015")
    }

    @Test fun `episode query then season pack then complete`() {
        assertThat(QueryBuilder.build(episode("Harbor Lights", 1, 2)))
            .containsExactly("Harbor Lights S01E02", "Harbor Lights S01", "Harbor Lights complete").inOrder()
    }

    @Test fun `double digit seasons and episodes are zero padded only to two`() {
        assertThat(QueryBuilder.build(episode("Law & Order: SVU", 22, 105)))
            .containsExactly("Law and Order SVU S22E105", "Law and Order SVU S22", "Law and Order SVU complete").inOrder()
    }

    @Test fun `season without episode skips the episode query`() {
        assertThat(QueryBuilder.build(episode("Harbor Lights", 3, null)))
            .containsExactly("Harbor Lights S03", "Harbor Lights complete").inOrder()
    }

    @Test fun `show without season is the show name`() {
        assertThat(QueryBuilder.build(episode("Harbor Lights", null, null))).containsExactly("Harbor Lights")
    }

    @Test fun `normaliser drops leading articles and and for matching`() {
        assertThat(TitleNormalizer.matchKey("The Fast & the Furious")).isEqualTo(TitleNormalizer.matchKey("Fast.and.the.Furious"))
        assertThat(TitleNormalizer.matchKey("A Quiet Place")).isEqualTo("quiet place")
        assertThat(TitleNormalizer.matchKey("Æon Flux")).isEqualTo("aeon flux")
        assertThat(TitleNormalizer.matchKey("The")).isEqualTo("the")
    }
}
