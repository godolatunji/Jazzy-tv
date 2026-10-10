package com.nexient.jazzy.feature.discover

import com.google.common.truth.Truth.assertThat
import com.nexient.jazzy.core.model.MediaType
import org.junit.Test
import java.time.LocalDate

class DiscoverFiltersTest {
    private val today = LocalDate.of(2026, 10, 10)

    @Test
    fun `defaults sort by popularity with the minimum vote count`() {
        assertThat(DiscoverFilters().toParams(today)).containsExactly("sort_by", "popularity.desc", "vote_count.gte", "100")
    }

    @Test
    fun `movie filters map to TMDB discover params`() {
        val params = DiscoverFilters(
            genreIds = setOf(878, 28),
            yearFrom = 1990,
            yearTo = 1999,
            minRating = 7,
            minVotes = 500,
            language = "en",
            runtimeMin = 90,
            runtimeMax = 120,
            sort = DiscoverSort.REVENUE,
        ).toParams(today)
        assertThat(params).containsExactly(
            "sort_by", "revenue.desc",
            "with_genres", "28,878",
            "primary_release_date.gte", "1990-01-01",
            "primary_release_date.lte", "1999-12-31",
            "vote_average.gte", "7",
            "vote_count.gte", "500",
            "with_original_language", "en",
            "with_runtime.gte", "90",
            "with_runtime.lte", "120",
        )
    }

    @Test
    fun `tv uses first air date and original name`() {
        val f = DiscoverFilters(mediaType = MediaType.TV, yearFrom = 2010, sort = DiscoverSort.TITLE, minVotes = 0)
        assertThat(f.toParams(today)).containsExactly("sort_by", "original_name.asc", "first_air_date.gte", "2010-01-01")
        assertThat(DiscoverFilters(mediaType = MediaType.TV, sort = DiscoverSort.RELEASE_DATE).toParams(today))
            .containsEntry("first_air_date.lte", "2026-10-10")
    }

    @Test
    fun `switching to tv drops the movie-only revenue sort and runtime`() {
        val f = DiscoverFilters(sort = DiscoverSort.REVENUE, runtimeMin = 90, runtimeMax = 120).withMediaType(MediaType.TV)
        assertThat(f.sort).isEqualTo(DiscoverSort.POPULARITY)
        assertThat(f.runtimeMin).isNull()
        assertThat(f.toSource(today).mediaType).isEqualTo(MediaType.TV)
    }

    @Test
    fun `year bounds are kept in order`() {
        val f = DiscoverFilters().withYears(2020, 2001)
        assertThat(f.yearFrom).isEqualTo(2001)
        assertThat(f.yearTo).isEqualTo(2020)
    }
}
