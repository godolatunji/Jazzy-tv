package com.nexient.jazzy.feature.library

import androidx.annotation.StringRes
import com.nexient.jazzy.core.common.formatDuration
import com.nexient.jazzy.core.common.formatRuntime
import com.nexient.jazzy.core.data.library.HistoryDay
import com.nexient.jazzy.core.designsystem.text.UiText
import com.nexient.jazzy.core.model.HistoryEntry
import com.nexient.jazzy.core.model.LibraryItem
import com.nexient.jazzy.core.model.LibrarySort
import com.nexient.jazzy.core.model.MediaType
import com.nexient.jazzy.core.model.TitleSnapshot
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/** Library tabs, in the order of `LibraryRoute(initialTab)`. */
enum class LibraryTab(@StringRes val label: Int) {
    MY_LIST(R.string.library_tab_my_list),
    LIKED(R.string.library_tab_liked),
    HISTORY(R.string.library_tab_history),
}

/** The two poster-grid lists (ML-3, LK-3). */
enum class CollectionKind { MY_LIST, LIKED }

enum class TypeFilter(@StringRes val label: Int, val mediaType: MediaType?) {
    ALL(R.string.library_filter_all, null),
    MOVIES(R.string.library_filter_movies, MediaType.MOVIE),
    TV(R.string.library_filter_tv, MediaType.TV),
}

@StringRes
fun LibrarySort.label(kind: CollectionKind): Int = when (this) {
    LibrarySort.DATE_ADDED -> if (kind == CollectionKind.LIKED) R.string.library_sort_date_liked else R.string.library_sort_date_added
    LibrarySort.TITLE -> R.string.library_sort_title
    LibrarySort.RELEASE_YEAR -> R.string.library_sort_release_year
}

/** Filters by type and sorts: newest first, A–Z, or newest release first (ML-3, LK-3). */
fun List<LibraryItem>.arrange(filter: TypeFilter, sort: LibrarySort): List<LibraryItem> {
    val filtered = if (filter.mediaType == null) this else filter { it.snapshot.mediaType == filter.mediaType }
    return when (sort) {
        LibrarySort.DATE_ADDED -> filtered.sortedByDescending { it.addedAt }
        LibrarySort.TITLE -> filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.snapshot.title })
        LibrarySort.RELEASE_YEAR -> filtered.sortedWith(
            compareBy<LibraryItem, Int?>(nullsLast(reverseOrder())) { it.snapshot.year }.thenByDescending { it.addedAt },
        )
    }
}

/** One History row: a (possibly folded) entry plus every session ID it stands for (HS-5). */
data class HistoryItem(
    val entry: HistoryEntry,
    val sessionIds: List<Long>,
    /** Every folded session, newest first. */
    val sessions: List<com.nexient.jazzy.core.model.PlaySession> = listOf(entry.latest),
) {
    val key: Long get() = entry.latest.id
    val snapshot: TitleSnapshot get() = entry.snapshot
    val folded: Boolean get() = entry.sessionCount > 1
}

data class HistorySection(val date: LocalDate, val label: UiText, val items: List<HistoryItem>)

/** "Today", "Yesterday", a weekday within the last week, then a date (HS-3). */
fun dayLabel(date: LocalDate, today: LocalDate, locale: Locale = Locale.getDefault()): UiText = when {
    date == today -> UiText.Res(R.string.library_history_today)
    date == today.minusDays(1) -> UiText.Res(R.string.library_history_yesterday)
    date.isAfter(today.minusDays(7)) && !date.isAfter(today) -> UiText.Raw(date.dayOfWeek.getDisplayName(TextStyle.FULL, locale))
    date.year == today.year -> UiText.Raw(date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale)))
    else -> UiText.Raw(date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", locale)))
}

/** Builds day sections, keeping only entries whose title (or episode title) matches [query]. */
fun List<HistoryDay>.toSections(query: String, today: LocalDate): List<HistorySection> {
    val q = query.trim()
    return mapNotNull { day ->
        val items = day.entries
            .filter { q.isEmpty() || it.matches(q) }
            .map { HistoryItem(it, day.sessionIds[it.latest.id] ?: listOf(it.latest.id), day.sessions[it.latest.id] ?: listOf(it.latest)) }
            .sortedByDescending { it.entry.latest.startedAt }
        if (items.isEmpty()) null else HistorySection(day.date, dayLabel(day.date, today), items)
    }
}

private fun HistoryEntry.matches(q: String): Boolean =
    snapshot.title.contains(q, ignoreCase = true) ||
        snapshot.originalTitle.contains(q, ignoreCase = true) ||
        latest.episodeTitle?.contains(q, ignoreCase = true) == true

/** "S1:E3 · Episode title" for episodes (HS-3). */
fun HistoryEntry.episodeLine(): String? {
    val label = latest.playable.label ?: return null
    return listOfNotNull(label, latest.episodeTitle?.takeIf { it.isNotBlank() }).joinToString(" · ")
}

/** "Stopped at 42:13 of 1:58:00" (HS-3). */
fun HistoryEntry.stoppedLine(): UiText {
    val stop = formatDuration(latest.stopPositionMs)
    return if (latest.durationMs > 0) {
        UiText.Res(R.string.library_history_stopped_at_of, stop, formatDuration(latest.durationMs))
    } else {
        UiText.Res(R.string.library_history_stopped_at, stop)
    }
}

fun HistoryEntry.progress(): Float =
    if (latest.durationMs <= 0) 0f else (latest.stopPositionMs.toFloat() / latest.durationMs).coerceIn(0f, 1f)

/** "45m" / "1h 52m" / "40s" of watching. */
fun formatWatched(ms: Long): UiText {
    val minutes = (ms / 60_000).toInt()
    return if (minutes < 1) {
        UiText.Res(R.string.library_history_duration_seconds, (ms / 1000).coerceAtLeast(1).toInt())
    } else {
        formatRuntime(minutes)?.let(UiText::Raw) ?: UiText.Res(R.string.library_history_duration_minutes, minutes)
    }
}

/** "Watched 45m · 1080p · 21:40" — time watched, source quality and when the session started. */
fun HistoryEntry.metaLine(locale: Locale = Locale.getDefault(), zone: ZoneId = ZoneId.systemDefault()): UiText {
    val watched = formatWatched(totalWatchedMs)
    val clock = formatClock(latest.startedAt, locale, zone)
    val quality = latest.quality?.takeIf { it.isNotBlank() }
    return if (quality == null) {
        UiText.Res(R.string.library_history_meta, watched, clock)
    } else {
        UiText.Res(R.string.library_history_meta_quality, watched, quality, clock)
    }
}

fun formatClock(epochMs: Long, locale: Locale = Locale.getDefault(), zone: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(epochMs).atZone(zone).toLocalTime()
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
