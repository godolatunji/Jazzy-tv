package com.nexient.jazzy.core.torrent

import android.system.Os
import java.io.File

data class CacheEntry(val infoHash: String, val sizeBytes: Long, val lastAccessMs: Long)

/** TE-9 LRU eviction. Pure, so it is unit-tested. */
object CacheEvictionPolicy {
    /**
     * The cache may use at most [capBytes], and at most half of the space it could grow into
     * (its current size plus the free space).
     */
    fun effectiveCap(capBytes: Long, cacheBytes: Long, freeBytes: Long): Long =
        minOf(capBytes, (cacheBytes + freeBytes) / 2).coerceAtLeast(0)

    /** Least-recently-used released torrents to delete so the cache fits; active ones are never chosen. */
    fun selectVictims(entries: List<CacheEntry>, active: Set<String>, capBytes: Long, freeBytes: Long): List<String> {
        var total = entries.sumOf { it.sizeBytes }
        val cap = effectiveCap(capBytes, total, freeBytes)
        if (total <= cap) return emptyList()
        val victims = mutableListOf<String>()
        for (entry in entries.filter { it.infoHash !in active }.sortedBy { it.lastAccessMs }) {
            if (total <= cap) break
            victims += entry.infoHash
            total -= entry.sizeBytes
        }
        return victims
    }
}

/** On-disk layout: `<root>/<infohash>/` per torrent, with a last-access marker for LRU. */
class TorrentCache(val root: File) {
    fun dirFor(infoHash: String): File = File(root, infoHash)

    fun resumeFile(infoHash: String): File = File(dirFor(infoHash), RESUME_FILE)

    fun touch(infoHash: String, nowMs: Long = System.currentTimeMillis()) {
        val dir = dirFor(infoHash)
        if (!dir.isDirectory && !dir.mkdirs()) return
        runCatching { File(dir, ACCESS_FILE).writeText(nowMs.toString()) }
    }

    fun entries(): List<CacheEntry> = root.listFiles().orEmpty()
        .filter { it.isDirectory && HASH.matches(it.name) }
        .map { dir ->
            val access = runCatching { File(dir, ACCESS_FILE).readText().trim().toLong() }.getOrNull() ?: dir.lastModified()
            CacheEntry(dir.name, diskUsage(dir), access)
        }

    fun sizeBytes(): Long = if (root.exists()) diskUsage(root) else 0L

    fun freeBytes(): Long = (if (root.exists()) root else root.parentFile ?: root).usableSpace

    fun delete(infoHash: String) {
        dirFor(infoHash).deleteRecursively()
    }

    companion object {
        const val RESUME_FILE = ".jazzy-resume"
        private const val ACCESS_FILE = ".jazzy-last-access"
        private val HASH = Regex("[0-9a-f]{40}")

        /**
         * Real bytes on disk. libtorrent creates sparse files, so `File.length()` reports the full
         * file size long before it's downloaded; `st_blocks` reports what is actually allocated.
         */
        fun diskUsage(file: File): Long {
            if (file.isDirectory) return file.listFiles().orEmpty().sumOf(::diskUsage)
            return runCatching { Os.lstat(file.absolutePath).st_blocks * 512 }.getOrElse { file.length() }
        }
    }
}
