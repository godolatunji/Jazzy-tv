package com.nexient.jazzy.core.data

import com.google.common.truth.Truth.assertThat
import com.nexient.jazzy.core.data.catalog.HomeTab
import com.nexient.jazzy.core.data.catalog.TMDB_MAX_PAGES
import com.nexient.jazzy.core.data.catalog.fullPageRail
import com.nexient.jazzy.core.data.catalog.tmdbRails
import org.junit.Test

class FullPageRailTest {
    @Test
    fun originalsRailsHaveAFullPageAndOthersDont() {
        val withPage = tmdbRails(HomeTab.ALL).filter { it.fullPage }.map { it.key }
        assertThat(withPage).containsExactly("originals_213", "originals_49", "originals_2552", "originals_more")
    }

    @Test
    fun fullPageRailIsTheSameQueryWithoutThePageCap() {
        val rail = tmdbRails(HomeTab.ALL).first { it.key == "originals_213" }
        val page = fullPageRail("originals_213")!!
        assertThat(page.sources).isEqualTo(rail.sources)
        assertThat(rail.maxPages).isLessThan(TMDB_MAX_PAGES)
        assertThat(page.maxPages).isEqualTo(TMDB_MAX_PAGES)
    }

    @Test
    fun railsWithoutAPageAndUnknownKeysGiveNull() {
        assertThat(fullPageRail("trending")).isNull()
        assertThat(fullPageRail("originals_999")).isNull()
    }
}
