package com.nexient.jazzy.feature.library

import com.google.common.truth.Truth.assertThat
import com.nexient.jazzy.core.data.library.HistoryDay
import com.nexient.jazzy.core.model.HistoryEntry
import com.nexient.jazzy.core.model.LibraryItem
import com.nexient.jazzy.core.model.LibrarySort
import com.nexient.jazzy.core.model.MediaType
import com.nexient.jazzy.core.model.PlayableRef
import com.nexient.jazzy.core.model.PlaySession
import com.nexient.jazzy.core.model.TitleSnapshot
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class LibraryModelsTest {
    private val today = LocalDate.of(2026, 10, 10) // a Saturday

    private fun snapshot(id: Int, title: String, type: MediaType = MediaType.MOVIE, year: Int? = 2000) =
        TitleSnapshot(id, type, title, title, year, null, null, emptyList(), null)

    private fun item(id: Int, title: String, addedAt: Long, type: MediaType = MediaType.MOVIE, year: Int? = 2000) =
        LibraryItem(snapshot(id, title, type, year), addedAt)

    private fun session(id: Long, tmdbId: Int, season: Int = -1, episode: Int = -1, episodeTitle: String? = null) = PlaySession(
        id = id, profileId = 1, playable = PlayableRef(tmdbId, if (season >= 0) MediaType.TV else MediaType.MOVIE, season, episode),
        episodeTitle = episodeTitle, infoHash = null, fileIndex = null, sourceName = null, quality = "1080p", providerId = null,
        startedAt = id * 1000, endedAt = null, startPositionMs = 0, stopPositionMs = 2_533_000, durationMs = 7_080_000,
        watchedMs = 1_500_000, endReason = null,
    )

    @Test
    fun `collections sort newest first, by title, or by release year and filter by type`() {
        val items = listOf(
            item(1, "beta", 10, year = 1999),
            item(2, "Alpha", 30, MediaType.TV, year = null),
            item(3, "gamma", 20, year = 2020),
        )
        assertThat(items.arrange(TypeFilter.ALL, LibrarySort.DATE_ADDED).map { it.snapshot.tmdbId }).containsExactly(2, 3, 1).inOrder()
        assertThat(items.arrange(TypeFilter.ALL, LibrarySort.TITLE).map { it.snapshot.tmdbId }).containsExactly(2, 1, 3).inOrder()
        assertThat(items.arrange(TypeFilter.ALL, LibrarySort.RELEASE_YEAR).map { it.snapshot.tmdbId }).containsExactly(3, 1, 2).inOrder()
        assertThat(items.arrange(TypeFilter.TV, LibrarySort.DATE_ADDED).map { it.snapshot.tmdbId }).containsExactly(2)
    }

    @Test
    fun `day labels follow today, yesterday, weekday, then date`() {
        assertThat(dayLabel(today, today, Locale.UK)).isEqualTo("Today")
        assertThat(dayLabel(today.minusDays(1), today, Locale.UK)).isEqualTo("Yesterday")
        assertThat(dayLabel(today.minusDays(3), today, Locale.UK)).isEqualTo("Wednesday")
        assertThat(dayLabel(LocalDate.of(2026, 3, 2), today, Locale.UK)).isEqualTo("Monday 2 March")
        assertThat(dayLabel(LocalDate.of(2024, 3, 2), today, Locale.UK)).isEqualTo("2 March 2024")
    }

    @Test
    fun `sections keep folded session ids and filter by title`() {
        val movie = HistoryEntry(snapshot(7, "Heat"), session(5, 7), sessionCount = 2, totalWatchedMs = 3_000_000)
        val show = HistoryEntry(snapshot(9, "Dark", MediaType.TV), session(6, 9, 1, 3, "Past and Present"), 1, 1_500_000)
        val days = listOf(HistoryDay(today, listOf(movie, show), mapOf(5L to listOf(5L, 4L), 6L to listOf(6L))))

        val all = days.toSections("", today)
        assertThat(all).hasSize(1)
        assertThat(all[0].label).isEqualTo("Today")
        assertThat(all[0].items.map { it.key }).containsExactly(6L, 5L).inOrder()
        assertThat(all[0].items.first { it.key == 5L }.sessionIds).containsExactly(5L, 4L)

        assertThat(days.toSections("present", today)[0].items.map { it.key }).containsExactly(6L)
        assertThat(days.toSections("nothing", today)).isEmpty()
    }

    @Test
    fun `entry lines`() {
        val show = HistoryEntry(snapshot(9, "Dark", MediaType.TV), session(6, 9, 1, 3, "Past and Present"), 1, 1_500_000)
        assertThat(show.episodeLine()).isEqualTo("S1:E3 · Past and Present")
        assertThat(show.stoppedLine()).isEqualTo("Stopped at 42:13 of 1:58:00")
        assertThat(formatWatched(1_500_000)).isEqualTo("25m")
        assertThat(formatWatched(40_000)).isEqualTo("40s")
    }
}
