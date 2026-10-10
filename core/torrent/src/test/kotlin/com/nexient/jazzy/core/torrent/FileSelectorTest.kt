package com.nexient.jazzy.core.torrent

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FileSelectorTest {
    private val mb = 1024L * 1024

    private fun f(index: Int, path: String, sizeMb: Long) = TorrentFileEntry(index, path, sizeMb * mb)

    @Test fun `movie picks the largest video file`() {
        val files = listOf(
            f(0, "Movie (2020)/Movie.2020.1080p.mkv", 2_000),
            f(1, "Movie (2020)/Movie.2020.1080p.srt", 1),
            f(2, "Movie (2020)/extras/Featurette.mp4", 400),
        )
        assertThat(FileSelector.select(files)?.index).isEqualTo(0)
    }

    @Test fun `samples and tiny files are skipped when alternatives exist`() {
        val files = listOf(
            f(0, "Movie/Sample/movie-sample.mkv", 150),
            f(1, "Movie/movie.mp4", 120),
            f(2, "Movie/trailer.mp4", 40),
        )
        assertThat(FileSelector.select(files)?.index).isEqualTo(1)
    }

    @Test fun `sample word inside another word is not a sample`() {
        assertThat(FileSelector.isSample(f(0, "Samples.Of.Life.2019.mkv", 900))).isFalse()
        assertThat(FileSelector.isSample(f(0, "movie.SAMPLE.mkv", 900))).isTrue()
    }

    @Test fun `falls back to small videos when nothing else exists`() {
        val files = listOf(f(0, "short/clip.webm", 20), f(1, "short/readme.txt", 0))
        assertThat(FileSelector.select(files)?.index).isEqualTo(0)
    }

    @Test fun `no video file returns null`() {
        val files = listOf(f(0, "album/01.flac", 30), f(1, "album/cover.jpg", 1), f(2, "setup.exe", 200))
        assertThat(FileSelector.select(files)).isNull()
    }

    @Test fun `all supported extensions are videos`() {
        listOf("a.mkv", "a.MP4", "a.avi", "a.m4v", "a.webm", "a.ts").forEach {
            assertThat(FileSelector.isVideo(it)).isTrue()
        }
        assertThat(FileSelector.isVideo("a.srt")).isFalse()
        assertThat(FileSelector.isVideo("mkv")).isFalse()
    }

    @Test fun `season pack picks the matching episode`() {
        val files = listOf(
            f(0, "Show.S02.1080p/Show.S02E01.1080p.mkv", 1_500),
            f(1, "Show.S02.1080p/Show.S02E02.1080p.mkv", 1_400),
            f(2, "Show.S02.1080p/Show.S02E03.1080p.mkv", 1_600),
        )
        assertThat(FileSelector.select(files, season = 2, episode = 2)?.index).isEqualTo(1)
    }

    @Test fun `episode not in the pack returns null`() {
        val files = listOf(
            f(0, "Show.S02/Show.S02E01.mkv", 1_500),
            f(1, "Show.S02/Show.S02E02.mkv", 1_400),
        )
        assertThat(FileSelector.select(files, season = 2, episode = 9)).isNull()
        assertThat(FileSelector.select(files, season = 3, episode = 1)).isNull()
    }

    @Test fun `single untagged file is accepted for an episode`() {
        val files = listOf(f(0, "Show.S01E05.720p/video.mkv", 700), f(1, "Show.S01E05.720p/info.nfo", 0))
        assertThat(FileSelector.select(files, season = 1, episode = 5)?.index).isEqualTo(0)
        val untagged = listOf(f(0, "episode.mkv", 700))
        assertThat(FileSelector.select(untagged, season = 1, episode = 5)?.index).isEqualTo(0)
    }

    @Test fun `single file tagged as another episode is rejected`() {
        val files = listOf(f(0, "Show.S01E04.mkv", 700))
        assertThat(FileSelector.select(files, season = 1, episode = 5)).isNull()
    }

    @Test fun `mime types`() {
        assertThat(FileSelector.mimeTypeFor("x/a.mkv")).isEqualTo("video/x-matroska")
        assertThat(FileSelector.mimeTypeFor("a.M4V")).isEqualTo("video/mp4")
        assertThat(FileSelector.mimeTypeFor("a.txt")).isNull()
    }
}

class EpisodeMatcherTest {
    @Test fun `common tag formats`() {
        assertThat(EpisodeMatcher.matches("Show.S01E02.720p.mkv", 1, 2)).isTrue()
        assertThat(EpisodeMatcher.matches("show s1e2.mkv", 1, 2)).isTrue()
        assertThat(EpisodeMatcher.matches("Show - 1x02 - Title.mkv", 1, 2)).isTrue()
        assertThat(EpisodeMatcher.matches("Show Season 1 Episode 2.mkv", 1, 2)).isTrue()
        assertThat(EpisodeMatcher.matches("Show.S01.E02.mkv", 1, 2)).isTrue()
        assertThat(EpisodeMatcher.matches("Show.S01E12.mkv", 1, 2)).isFalse()
        assertThat(EpisodeMatcher.matches("Show.S11E02.mkv", 1, 2)).isFalse()
    }

    @Test fun `multi-episode files`() {
        assertThat(EpisodeMatcher.matches("Show.S01E01E02.mkv", 1, 2)).isTrue()
        assertThat(EpisodeMatcher.matches("Show.S01E01-E03.mkv", 1, 2)).isTrue()
        assertThat(EpisodeMatcher.matches("Show.S01E01-03.mkv", 1, 3)).isTrue()
        assertThat(EpisodeMatcher.matches("Show.S01E01-03.mkv", 1, 4)).isFalse()
    }

    @Test fun `resolution is not an episode tag`() {
        assertThat(EpisodeMatcher.parse("Movie.1920x1080.x264.mkv")).isEmpty()
        assertThat(EpisodeMatcher.matches("Show.S01E01 720p.mkv", 1, 720)).isFalse()
    }

    @Test fun `file name wins over the directory`() {
        assertThat(EpisodeMatcher.matches("Show.S01E01-E10/Show.S01E04.mkv", 1, 2)).isFalse()
        assertThat(EpisodeMatcher.matches("Show.S01E02/video.mkv", 1, 2)).isTrue()
    }
}
