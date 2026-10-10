package com.nexient.jazzy.core.sources.parse

import com.google.common.truth.Truth.assertThat
import com.nexient.jazzy.core.model.MediaType
import com.nexient.jazzy.core.model.SourceQuery
import org.junit.Test

class ResultMatcherTest {
    private fun movie(title: String, year: Int?, original: String? = null, imdb: String? = null) =
        SourceQuery(tmdbId = 1, mediaType = MediaType.MOVIE, title = title, originalTitle = original, year = year, imdbId = imdb)

    private fun episode(show: String, season: Int, episode: Int?, year: Int? = 2019) =
        SourceQuery(tmdbId = 2, mediaType = MediaType.TV, title = show, originalTitle = null, year = year, imdbId = null, season = season, episode = episode)

    private fun SourceQuery.accepts(name: String, imdb: String? = null) = ResultMatcher.matches(this, name, imdb)

    // region Titles

    @Test fun `punctuation and separators are tolerated`() {
        val q = movie("Spider-Man: No Way Home", 2021)
        assertThat(q.accepts("Spider.Man.No.Way.Home.2021.1080p.WEBRip.x264")).isTrue()
        assertThat(q.accepts("Spider-Man No Way Home (2021) [2160p] [WEB] [YTS]")).isTrue()
        assertThat(q.accepts("SpiderMan.No.Way.Home.2021.720p")).isTrue() // same letters, different split
        assertThat(movie("Spider-Man", 2002).accepts("SpiderMan.2002.1080p.BluRay")).isTrue()
    }

    @Test fun `ampersand, accents, apostrophes and leading articles are normalised`() {
        assertThat(movie("Fast & Furious", 2009).accepts("Fast.and.Furious.2009.1080p.BluRay.x264")).isTrue()
        assertThat(movie("Fast & Furious", 2009).accepts("Fast.Furious.2009.720p")).isTrue()
        assertThat(movie("Amélie", 2001).accepts("Amelie.2001.1080p.BluRay.x264")).isTrue()
        assertThat(movie("Ocean's Eleven", 2001).accepts("Oceans.Eleven.2001.720p.BluRay")).isTrue()
        assertThat(movie("The Quiet Harbor", 2019).accepts("Quiet.Harbor.2019.1080p.WEB-DL")).isTrue()
        assertThat(movie("Quiet Harbor", 2019).accepts("The.Quiet.Harbor.2019.1080p.WEB-DL")).isTrue()
    }

    @Test fun `different titles are dropped`() {
        val q = movie("Alien", 1979)
        assertThat(q.accepts("Aliens.1986.1080p.BluRay")).isFalse()
        assertThat(q.accepts("Alien.Romulus.2024.1080p.WEB-DL")).isFalse()
        assertThat(q.accepts("Alien.1979.Directors.Cut.1080p.BluRay")).isTrue()
        assertThat(movie("Up", 2009).accepts("Up.in.the.Air.2009.1080p.BluRay")).isFalse()
        assertThat(movie("The Quiet Harbor", 2019).accepts("The.Loud.Harbor.2019.1080p")).isFalse()
    }

    @Test fun `edition and country suffixes are allowed`() {
        assertThat(movie("Midnight Atlas", 2016).accepts("Midnight.Atlas.Extended.Cut.2016.1080p.BluRay")).isTrue()
        assertThat(episode("Harbor Lights", 4, 7).accepts("Harbor.Lights.US.S04E07.720p.HDTV.x264")).isTrue()
    }

    @Test fun `original title matches too`() {
        val q = movie("Amélie", 2001, original = "Le Fabuleux Destin d'Amélie Poulain")
        assertThat(q.accepts("Le.Fabuleux.Destin.d.Amelie.Poulain.2001.1080p.BluRay")).isTrue() // "d amelie" vs "damelie"
        assertThat(q.accepts("Le.Fabuleux.Destin.dAmelie.Poulain.2001.1080p.BluRay")).isTrue()
        assertThat(q.accepts("Le Fabuleux Destin d'Amélie Poulain AKA Amelie 2001 720p")).isTrue()
    }

    @Test fun `a number at the end of the title is not mistaken for the year`() {
        assertThat(movie("Blade Runner 2049", 2017).accepts("Blade.Runner.2049.1080p.BluRay.x264")).isTrue()
        assertThat(movie("Blade Runner 2049", 2017).accepts("Blade.Runner.2049.2017.1080p.BluRay.x264")).isTrue()
        assertThat(movie("Wonder Woman 1984", 2020).accepts("Wonder.Woman.1984.2020.2160p.WEB-DL")).isTrue()
        assertThat(movie("1917", 2019).accepts("1917.2019.1080p.BluRay.x264")).isTrue()
    }

    @Test fun `longer or shorter titles pass when the year matches`() {
        assertThat(movie("Star Wars", 1977).accepts("Star.Wars.Episode.4.A.New.Hope.1977.1080p.BluRay")).isTrue()
        assertThat(movie("Mission: Impossible - Dead Reckoning Part One", 2023).accepts("Mission.Impossible.Dead.Reckoning.2023.1080p")).isTrue()
        assertThat(movie("Star Wars", 1977).accepts("Star.Wars.The.Last.Jedi.2017.1080p.BluRay")).isFalse()
    }

    @Test fun `acronym endings match`() =
        assertThat(episode("Law & Order: Special Victims Unit", 22, 5, year = 1999).accepts("Law.and.Order.SVU.S22E05.720p.HDTV.x264")).isTrue()

    @Test fun `matching imdb id skips the title check`() {
        val q = movie("Le Fabuleux Destin d'Amélie Poulain", 2001, imdb = "tt0211915")
        assertThat(q.accepts("Amelie (2001) [1080p] [BluRay] [YTS]", imdb = "tt0211915")).isTrue()
        assertThat(q.accepts("Amelie (2001) [1080p] [BluRay] [YTS]", imdb = "tt0000001")).isFalse()
        assertThat(q.accepts("Amelie (1990) [1080p] [YTS]", imdb = "tt0211915")).isFalse() // year still checked
    }

    // endregion

    // region Years

    @Test fun `movie year must be within one`() {
        val q = movie("The Quiet Harbor", 2019)
        assertThat(q.accepts("The.Quiet.Harbor.2019.1080p")).isTrue()
        assertThat(q.accepts("The.Quiet.Harbor.2018.1080p")).isTrue()
        assertThat(q.accepts("The.Quiet.Harbor.2020.1080p")).isTrue()
        assertThat(q.accepts("The.Quiet.Harbor.2021.1080p")).isFalse()
        assertThat(q.accepts("The.Quiet.Harbor.1962.1080p")).isFalse()
        assertThat(q.accepts("The.Quiet.Harbor.1080p.WEB-DL")).isTrue() // no year in the name
        assertThat(movie("The Quiet Harbor", null).accepts("The.Quiet.Harbor.1962.1080p")).isTrue()
    }

    @Test fun `show year only rejects an earlier show`() {
        val q = episode("Harbor Lights", 1, 1, year = 2005)
        assertThat(q.accepts("Harbor.Lights.2005.S01E01.1080p")).isTrue()
        assertThat(q.accepts("Harbor.Lights.2006.S01E01.1080p")).isTrue()
        assertThat(q.accepts("Harbor.Lights.1963.S01E01.1080p")).isFalse()
    }

    // endregion

    // region Episodes

    @Test fun `episode must match`() {
        val q = episode("Harbor Lights", 1, 2)
        assertThat(q.accepts("Harbor.Lights.S01E02.1080p.WEB.H264")).isTrue()
        assertThat(q.accepts("Harbor.Lights.1x02.HDTV.XviD")).isTrue()
        assertThat(q.accepts("Harbor.Lights.S01E03.1080p.WEB.H264")).isFalse()
        assertThat(q.accepts("Harbor.Lights.S02E02.1080p.WEB.H264")).isFalse()
        assertThat(q.accepts("Harbor.Lights.S01E01-E03.720p")).isTrue()
        assertThat(q.accepts("Harbor.Lights.S01E01E02.720p")).isTrue()
        assertThat(q.accepts("Other.Show.S01E02.720p")).isFalse()
    }

    @Test fun `season packs containing the episode are kept`() {
        val q = episode("Harbor Lights", 2, 5)
        assertThat(q.accepts("Harbor.Lights.S02.1080p.BluRay.x264")).isTrue()
        assertThat(q.accepts("Harbor Lights Season 2 Complete 720p")).isTrue()
        assertThat(q.accepts("Harbor.Lights.S01-S03.1080p.BluRay")).isTrue()
        assertThat(q.accepts("Harbor Lights Seasons 3-4 1080p")).isFalse()
        assertThat(q.accepts("Harbor.Lights.S01.1080p.BluRay")).isFalse()
        assertThat(q.accepts("Harbor Lights Complete Series 1080p")).isTrue()
    }

    @Test fun `rows without episode info are dropped for an episode query`() =
        assertThat(episode("Harbor Lights", 1, 2).accepts("Harbor.Lights.1080p.WEB-DL")).isFalse()

    @Test fun `movie queries drop tv releases`() {
        val q = movie("Harbor Lights", 2019)
        assertThat(q.accepts("Harbor.Lights.S01E02.1080p.WEB")).isFalse()
        assertThat(q.accepts("Harbor.Lights.S01.1080p.WEB")).isFalse()
        assertThat(q.accepts("Harbor.Lights.2019.1080p.WEB")).isTrue()
    }

    // endregion
}
