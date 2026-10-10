package com.nexient.jazzy.core.common

import java.util.Locale

/** "1:58:00" / "42:13" style durations. */
fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%d:%02d", m, s)
}

/** "1h 52m" / "45m" for runtimes. */
fun formatRuntime(minutes: Int?): String? {
    if (minutes == null || minutes <= 0) return null
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "${m}m"
        m == 0 -> "${h}h"
        else -> "${h}h ${m}m"
    }
}

/** "2.1 GB" / "700 MB". */
fun formatBytes(bytes: Long?): String {
    if (bytes == null || bytes <= 0) return "–"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return if (unit >= 3) String.format(Locale.US, "%.1f %s", value, units[unit]) else String.format(Locale.US, "%.0f %s", value, units[unit])
}

/** "4.2 MB/s". */
fun formatRate(bytesPerSecond: Long): String = formatBytes(bytesPerSecond.coerceAtLeast(1)) + "/s"

/** "1,240". */
fun formatCount(count: Int): String = String.format(Locale.US, "%,d", count)
