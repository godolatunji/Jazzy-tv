package com.nexient.jazzy.core.torrent

/** One file of a torrent, as seen by the selection logic (pad files already removed). */
data class TorrentFileEntry(val index: Int, val path: String, val size: Long)

/** TE-3: picks the file to stream. Pure, so it is unit-tested without native code. */
object FileSelector {
    val VIDEO_EXTENSIONS = setOf("mkv", "mp4", "avi", "m4v", "webm", "ts")
    const val SAMPLE_MAX_BYTES = 100L * 1024 * 1024

    private val SAMPLE_WORD = Regex("""(?i)(?:^|[^a-z])sample(?:[^a-z]|$)""")

    /**
     * Movies: the largest video file. Episodes: the largest video file whose path carries a
     * matching `SxxEyy` (season packs). Samples (named "sample", or < 100 MB) are skipped when
     * there are alternatives. Returns null when nothing playable matches.
     */
    fun select(files: List<TorrentFileEntry>, season: Int? = null, episode: Int? = null): TorrentFileEntry? {
        val videos = files.filter { isVideo(it.path) && it.size > 0 }
        if (videos.isEmpty()) return null
        val pool = videos.filterNot(::isSample).ifEmpty { videos }
        if (season == null || episode == null) return pool.maxBy { it.size }

        val matching = pool.filter { EpisodeMatcher.matches(it.path, season, episode) }
        if (matching.isNotEmpty()) return matching.maxBy { it.size }
        // A single-episode torrent whose file isn't tagged (the tag is in the torrent name, which
        // TitleVerifier checks): accept it unless the file is explicitly tagged as another episode.
        val single = pool.singleOrNull() ?: return null
        return single.takeIf { EpisodeMatcher.parse(it.path).isEmpty() }
    }

    fun isVideo(path: String): Boolean = extension(path) in VIDEO_EXTENSIONS

    fun isSample(file: TorrentFileEntry): Boolean =
        file.size < SAMPLE_MAX_BYTES || SAMPLE_WORD.containsMatchIn(file.path.substringAfterLast('/'))

    fun extension(path: String): String = path.substringAfterLast('/').substringAfterLast('.', "").lowercase()

    fun mimeTypeFor(path: String): String? = when (extension(path)) {
        "mkv" -> "video/x-matroska"
        "mp4", "m4v" -> "video/mp4"
        "webm" -> "video/webm"
        "avi" -> "video/x-msvideo"
        "ts" -> "video/mp2t"
        else -> null
    }
}

/** Parses `S01E02`, `S01E01E02`, `S01E01-E03`, `1x02` and `Season 1 Episode 2` tags. */
object EpisodeMatcher {
    data class Tag(val season: Int, val episodes: IntRange)

    private val SXXEYY = Regex("""(?i)(?<![a-z0-9])s(\d{1,2})[ ._-]?e(\d{1,3})(?:-?e(\d{1,3})|-(\d{1,3})(?!\d))?""")
    private val NXNN = Regex("""(?i)(?<![a-z0-9])(\d{1,2})x(\d{2,3})(?![0-9])""")
    private val WORDS = Regex("""(?i)season[ ._-]?(\d{1,2})[ ._-]*episode[ ._-]?(\d{1,3})""")

    fun parse(path: String): List<Tag> = buildList {
        SXXEYY.findAll(path).forEach { m ->
            val s = m.groupValues[1].toInt()
            val first = m.groupValues[2].toInt()
            val last = (m.groupValues[3].ifEmpty { m.groupValues[4] }).toIntOrNull()
            add(Tag(s, if (last != null && last >= first) first..last else first..first))
        }
        NXNN.findAll(path).forEach { m ->
            val e = m.groupValues[2].toInt()
            add(Tag(m.groupValues[1].toInt(), e..e))
        }
        WORDS.findAll(path).forEach { m ->
            val e = m.groupValues[2].toInt()
            add(Tag(m.groupValues[1].toInt(), e..e))
        }
    }

    fun matches(path: String, season: Int, episode: Int): Boolean {
        // Prefer the file name; fall back to directories (e.g. "Show.S01E02/video.mkv").
        val name = path.substringAfterLast('/')
        val nameTags = parse(name)
        val tags = nameTags.ifEmpty { parse(path) }
        return tags.any { it.season == season && episode in it.episodes }
    }
}
