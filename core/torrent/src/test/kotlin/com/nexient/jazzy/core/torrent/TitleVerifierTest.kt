package com.nexient.jazzy.core.torrent

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TitleVerifierTest {
    @Test fun `matching release names`() {
        assertThat(TitleVerifier.matches("Big Buck Bunny", 2008, "Big Buck Bunny", "Big Buck Bunny/Big Buck Bunny.mp4")).isTrue()
        assertThat(TitleVerifier.matches("The Matrix", 1999, "The.Matrix.1999.1080p.BluRay.x264", "The.Matrix.1999.1080p.BluRay.x264.mkv")).isTrue()
        assertThat(TitleVerifier.matches("Amélie", 2001, "Amelie.2001.720p", "Amelie.2001.720p.mkv")).isTrue()
        assertThat(TitleVerifier.matches("Ocean's Eleven", 2001, "Oceans.Eleven.2001.1080p", "x.mkv")).isTrue()
        assertThat(TitleVerifier.matches("Fast & Furious", 2009, "Fast.and.Furious.2009.1080p", "x.mkv")).isTrue()
        assertThat(TitleVerifier.matches("Rocky II", 1979, "Rocky.2.1979.1080p", "x.mkv")).isTrue()
    }

    @Test fun `title found only in the file path`() {
        assertThat(TitleVerifier.matches("Sintel", 2010, "1080p-collection", "collection/Sintel.2010.1080p.mkv")).isTrue()
    }

    @Test fun `compact spelling`() {
        assertThat(TitleVerifier.matches("Spider-Man", 2002, "SpiderMan.2002.1080p", "x.mkv")).isTrue()
        // Too short for compact matching: "Up" must not match "Pickup".
        assertThat(TitleVerifier.matches("Up", 2009, "Pickup.2009.1080p", "x.mkv")).isFalse()
    }

    @Test fun `bait-and-switch is rejected`() {
        assertThat(TitleVerifier.matches("Dune", 2021, "Big Buck Bunny", "Big Buck Bunny/Big Buck Bunny.mp4")).isFalse()
        assertThat(TitleVerifier.matches("Inception", 2010, "Free.Movies.Download.Pack", "setup.mkv")).isFalse()
    }

    @Test fun `wrong year for movies is rejected but missing year is fine`() {
        assertThat(TitleVerifier.matches("Dune", 2021, "Dune.1984.1080p", "Dune.1984.mkv")).isFalse()
        assertThat(TitleVerifier.matches("Dune", 2021, "Dune.2021.2160p", "Dune.mkv")).isTrue()
        assertThat(TitleVerifier.matches("Dune", 2021, "Dune.2022.1080p", "Dune.mkv")).isTrue() // ±1 (release vs festival year)
        assertThat(TitleVerifier.matches("Dune", 2021, "Dune.1080p", "Dune.mkv")).isTrue()
    }

    @Test fun `episodes ignore the year`() {
        assertThat(TitleVerifier.matches("The Office", 2005, "The.Office.US.S09.2013.1080p", "The.Office.S09E01.mkv", isEpisode = true)).isTrue()
    }

    @Test fun `stop words are optional`() {
        assertThat(TitleVerifier.matches("The Lord of the Rings", null, "Lord.Rings.Fellowship", "x.mkv")).isTrue()
    }

    @Test fun `unverifiable titles pass`() {
        assertThat(TitleVerifier.matches("…", null, "Anything", "x.mkv")).isTrue()
    }
}
