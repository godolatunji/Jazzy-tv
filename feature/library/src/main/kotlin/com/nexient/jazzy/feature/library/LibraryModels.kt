package com.nexient.jazzy.feature.library

import com.nexient.jazzy.core.common.formatDuration
import com.nexient.jazzy.core.common.formatRuntime
import com.nexient.jazzy.core.data.library.HistoryDay
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
enum class LibraryTab(val label: String) { MY_LIST("My List"), LIKED("Liked"), HISTORY("History") }

/** The two poster-grid lists (ML-3, LK-3). */
enum class CollectionKind { MY_LIST, LIKED }

enum class TypeFilter(val label: String, val mediaType: MediaType?) {
    ALL("All", null),
    MOVIES("Movies", MediaType.MOVIE),
    TV("TV Shows", MediaType.TV),
}

fun LibrarySort.label(kind: CollectionKind): String = when (this) {
    LibrarySort.DATE_ADDED -> if (kind == CollectionKind.LIKED) "Date liked" else "Date added"
    LibrarySort.TITLE -> "Title"
    LibrarySort.RELEASE_YEAR -> "Release year"
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

data class HistorySection(val date: LocalDate, val label: String, val items: List<HistoryItem>)

/** "Today", "Yesterday", a weekday within the last week, then a date (HS-3). */
fun dayLabel(date: LocalDate, today: LocalDate, locale: Locale = Locale.getDefault()): String = when {
    date == today -> "Today"
    date == today.minusDays(1) -> "Yesterday"
    date.isAfter(today.minusDays(7)) && !date.isAfter(today) -> date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
    date.year == today.year -> date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale))
    else -> date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", locale))
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
fun HistoryEntry.stoppedLine(): String {
    val stop = formatDuration(latest.stopPositionMs)
    return if (latest.durationMs > 0) "Stopped at $stop of ${formatDuration(latest.durationMs)}" else "Stopped at $stop"
}

fun HistoryEntry.progress(): Float =
    if (latest.durationMs <= 0) 0f else (latest.stopPositionMs.toFloat() / latest.durationMs).coerceIn(0f, 1f)

/** "45m" / "1h 52m" / "40s" of watching. */
fun formatWatched(ms: Long): String {
    val minutes = (ms / 60_000).toInt()
    return if (minutes < 1) "${(ms / 1000).coerceAtLeast(1)}s" else formatRuntime(minutes) ?: "${minutes}m"
}

/** "Watched 45m · 1080p · 21:40" — time watched, source quality and when the session started. */
fun HistoryEntry.metaLine(locale: Locale = Locale.getDefault(), zone: ZoneId = ZoneId.systemDefault()): String =
    listOfNotNull(
        "Watched ${formatWatched(totalWatchedMs)}",
        latest.quality?.takeIf { it.isNotBlank() },
        formatClock(latest.startedAt, locale, zone),
    ).joinToString(" · ")

fun formatClock(epochMs: Long, locale: Locale = Locale.getDefault(), zone: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(epochMs).atZone(zone).toLocalTime()
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
