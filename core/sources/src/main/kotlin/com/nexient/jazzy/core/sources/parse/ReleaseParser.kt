package com.nexient.jazzy.core.sources.parse

import com.nexient.jazzy.core.model.HdrFormat
import com.nexient.jazzy.core.model.ReleaseInfo
import com.nexient.jazzy.core.model.ReleaseSource
import com.nexient.jazzy.core.model.Resolution
import com.nexient.jazzy.core.model.VideoCodec

/**
 * Parses scene/P2P release names (`Movie.Title.2021.1080p.WEB-DL.DDP5.1.H.264-GRP`, YTS-style
 * `Movie Title (2021) [1080p] [BluRay] [YTS]`, ...) into [ReleaseInfo] (spec §5.8 "Result parsing").
 *
 * How it works:
 * 1. Leading site/group tags (`[YTS.MX]`, `www.site.org - `) are stripped.
 * 2. The title ends at the release year or at the first "strong" marker (SxxEyy, season,
 *    complete, resolution); without one, at the first quality tag.
 * 3. Quality tags (source, codec, HDR, audio, channels) are only looked for after the title, so
 *    a film called "Cam" or "TS" isn't flagged as a camrip.
 */
object ReleaseParser {

    /** Parsed name plus what matching needs beyond [ReleaseInfo]. */
    data class ParsedRelease(
        val info: ReleaseInfo,
        /** The title part of the name, separators replaced by spaces (e.g. "Spider Man No Way Home"). */
        val title: String,
        /** Seasons covered: `1..1` for S01E02 or S01, `1..3` for S01-S03. Null when the name has none. */
        val seasons: IntRange?,
        /** Episodes covered: `2..2` for S01E02, `1..3` for S01E01-E03. Null for packs. */
        val episodes: IntRange?,
    )

    fun parse(name: String): ReleaseInfo = parseDetailed(name).info

    fun parseDetailed(name: String): ParsedRelease {
        val text = stripPrefix(name.trim())
        val episodeInfo = findEpisodeInfo(text)
        val firstStrong = listOfNotNull(
            episodeInfo?.takeIf { it.strong }?.index,
            firstIndex(text, RESOLUTION_MARKERS),
            firstIndex(text, COMPLETE),
        ).minOrNull()
        val firstWeak = firstIndex(text, WEAK_MARKERS)

        // The release year is the last year before the first strong marker ("Wonder.Woman.1984.2020",
        // "1917.2019"); a year at the very start is part of the title ("2012.2009.1080p").
        val years = YEAR.findAll(text).filter { it.range.first > 0 }.toList()
        val yearBefore = years.lastOrNull { it.range.first < (firstStrong ?: text.length) }
        val yearMatch = yearBefore ?: years.firstOrNull()
        val boundary = when {
            yearBefore != null -> yearBefore.range.first
            // "Sintel FRENCH WEBRIP 1080p 2010": the tags right before the resolution aren't title.
            firstStrong != null -> trailingWeakStart(text.substring(0, firstStrong)) ?: firstStrong
            else -> firstWeak ?: text.length
        }

        val tail = text.substring(boundary)
        val source = SOURCES.firstOrNull { (_, regex) -> regex.containsMatchIn(tail) }?.first
        val resolution = resolution(tail) ?: when {
            source == ReleaseSource.DVD -> Resolution.SD
            XVID.containsMatchIn(tail) -> Resolution.SD
            else -> null
        }
        val complete = COMPLETE.find(text)
        // A weak "Episode 4" before the release year is part of a title (Star.Wars.Episode.4...1977).
        val episodeMarker = episodeInfo?.takeUnless { !it.strong && yearBefore != null && it.index < boundary }
        val seasons = episodeMarker?.seasons
        val episodes = episodeMarker?.episodes

        val info = ReleaseInfo(
            resolution = resolution,
            source = source,
            videoCodec = CODECS.firstOrNull { (_, regex) -> regex.containsMatchIn(tail) }?.first,
            hdr = HDR.filter { (_, regex) -> regex.containsMatchIn(tail) }.map { it.first }.sortedBy { it.ordinal },
            audio = audio(tail),
            audioChannels = channels(tail),
            year = yearMatch?.value?.toInt(),
            season = seasons?.first,
            episode = episodes?.first,
            isSeasonPack = seasons != null && episodes == null,
            isCompleteSeries = complete != null && (seasons == null || COMPLETE_SERIES.containsMatchIn(text)) && episodes == null,
        )
        val title = text.substring(0, boundary)
            .replace(SEPARATORS, " ")
            .trim { it.isWhitespace() || it in "-–([{:" }
        return ParsedRelease(info, title, seasons, episodes)
    }

    // region Episode / season

    private class EpisodeInfo(val index: Int, val seasons: IntRange?, val episodes: IntRange?, val strong: Boolean)

    private fun findEpisodeInfo(text: String): EpisodeInfo? {
        SXXEYY.find(text)?.let { m ->
            val season = m.groupValues[1].toInt()
            val first = m.groupValues[2].toInt()
            val last = (m.groups[3]?.value ?: m.groups[4]?.value)?.toInt()?.takeIf { it >= first } ?: first
            return EpisodeInfo(m.range.first, season..season, first..last, strong = true)
        }
        CROSS_FORMAT.find(text)?.let { m ->
            val season = m.groupValues[1].toInt()
            val episode = m.groupValues[2].toInt()
            return EpisodeInfo(m.range.first, season..season, episode..episode, strong = true)
        }
        SEASON_RANGE.find(text)?.let { m ->
            val from = m.groupValues[1].toInt()
            val to = m.groupValues[2].toInt()
            return EpisodeInfo(m.range.first, if (to >= from) from..to else from..from, null, strong = true)
        }
        SEASON_WORD.find(text)?.let { m ->
            val from = m.groupValues[1].toInt()
            val to = m.groups[2]?.value?.toInt()?.takeIf { it >= from } ?: from
            return EpisodeInfo(m.range.first, from..to, null, strong = true)
        }
        SEASON_ONLY.find(text)?.let { m ->
            val season = m.groupValues[1].toInt()
            return EpisodeInfo(m.range.first, season..season, null, strong = true)
        }
        EPISODE_ONLY.find(text)?.takeIf { it.range.first > 0 }?.let { m ->
            val episode = m.groupValues[1].toInt()
            // "Episode 4" alone is also a movie-title word (Star Wars), so it isn't a strong marker.
            return EpisodeInfo(m.range.first, null, episode..episode, strong = false)
        }
        return null
    }

    // endregion

    // region Quality tags

    private fun resolution(tail: String): Resolution? {
        RESOLUTION.find(tail)?.let { m ->
            return when (m.groupValues[1].toInt()) {
                2160 -> Resolution.UHD_2160
                1080 -> Resolution.FHD_1080
                720 -> Resolution.HD_720
                else -> Resolution.SD
            }
        }
        if (UHD.containsMatchIn(tail)) return Resolution.UHD_2160
        DIMENSIONS.find(tail)?.let { m ->
            val height = m.groupValues[1].toInt()
            return when {
                height >= 1600 -> Resolution.UHD_2160
                height >= 1000 -> Resolution.FHD_1080
                height >= 700 -> Resolution.HD_720
                else -> Resolution.SD
            }
        }
        return null
    }

    private fun audio(tail: String): String? {
        val codec = AUDIO.firstOrNull { (_, regex) -> regex.containsMatchIn(tail) }?.first
        val atmos = ATMOS.containsMatchIn(tail)
        return when {
            codec != null && atmos -> "$codec Atmos"
            codec != null -> codec
            atmos -> "Atmos"
            else -> null
        }
    }

    private fun channels(tail: String): String? {
        CHANNELS.find(tail)?.let { return "${it.groupValues[1]}.${it.groupValues[2]}" }
        CHANNEL_COUNT.find(tail)?.let { m ->
            return when (m.groupValues[1].toInt()) {
                2 -> "2.0"
                6 -> "5.1"
                8 -> "7.1"
                else -> null
            }
        }
        return null
    }

    // endregion

    /** Start of the run of quality/edition tags that ends [segment], if any (and not at index 0). */
    private fun trailingWeakStart(segment: String): Int? = WEAK_RUN.find(segment)?.let { m ->
        m.range.first + m.value.indexOfFirst { it.isLetterOrDigit() }
    }?.takeIf { it > 0 }

    private fun firstIndex(text: String, regex: Regex): Int? =
        regex.findAll(text).firstOrNull { it.range.first > 0 }?.range?.first

    private fun stripPrefix(name: String): String {
        var text = name
        while (true) {
            val stripped = text.replaceFirst(LEADING_TAG, "").replaceFirst(LEADING_SITE, "")
            if (stripped == text || stripped.isBlank()) return text
            text = stripped
        }
    }

    // region Patterns
    private const val B = """(?<![\p{L}\p{N}])"""
    private const val E = """(?![\p{L}\p{N}])"""
    private val I = setOf(RegexOption.IGNORE_CASE)
    private fun rx(pattern: String) = Regex(pattern, I)

    private val SEPARATORS = Regex("""[._]+|\s+""")
    private val LEADING_TAG = Regex("""^\s*[\[【][^\]】]*[\]】]\s*[-–:]?\s*""")
    private val LEADING_SITE = rx("""^\s*(?:www\.)?[a-z0-9-]+\.(?:com|org|net|to|io|cc|me|info|xyz|se|li|tv|mx|lol|ws|ag|is)\s*[-–:]\s*""")

    private val YEAR = Regex("""(?<![\p{L}\p{N}])(19[2-9]\d|20\d{2})(?![\p{L}\p{N}])""")

    private val SXXEYY = rx("""${B}s(\d{1,2})[ ._-]?e(\d{1,3})(?:[ ._-]?e(\d{1,3})|-(\d{1,3}))?(?!\d)""")
    private val CROSS_FORMAT = rx("""${B}(\d{1,2})x(\d{2,3})$E""")
    private val SEASON_RANGE = rx("""${B}s(\d{1,2})\s*(?:-|to|~)\s*s?(\d{1,2})$E""")
    private val SEASON_WORD = rx(
        """${B}(?:seasons?|saisons?|series|stagione|temporada)[ ._-]*(\d{1,2})(?:\s*(?:-|to|~|&|and|à|a)\s*(?:(?:seasons?|saisons?|s)[ ._-]*)?(\d{1,2}))?$E""",
    )
    private val SEASON_ONLY = rx("""${B}s(\d{1,2})$E""")
    private val EPISODE_ONLY = rx("""${B}(?:episode|ep)[ ._-]*(\d{1,3})$E""")
    private val COMPLETE = rx("""$B(?:the[ ._-]+)?(?:complete|int[eé]grale)$E""")
    private val COMPLETE_SERIES = rx("""${B}complete[ ._-]+(?:series|collection|box[ ._-]?set|seasons)$E|${B}int[eé]grale$E""")

    private val RESOLUTION = rx("""$B(2160|1080|720|576|480|360|240)[pi]$E""")
    private val UHD = rx("""$B(?:4k|uhd)$E""")
    private val DIMENSIONS = rx("""$B\d{3,4}x(\d{3,4})$E""")
    private val RESOLUTION_MARKERS = rx("""$B(?:(?:2160|1080|720|576|480|360|240)[pi]|4k|uhd|\d{3,4}x\d{3,4})$E""")
    private val XVID = rx("""$B(?:xvid|divx)$E""")

    private val SOURCES: List<Pair<ReleaseSource, Regex>> = listOf(
        ReleaseSource.REMUX to rx("""$B(?:bd|uhd)?[ .-]?remux$E"""),
        ReleaseSource.HDCAM to rx("""${B}hd[ .-]?cam(?:[ .-]?rip)?$E"""),
        ReleaseSource.CAM to rx("""${B}cam(?:[ .-]?rip)?$E"""),
        ReleaseSource.TELESYNC to rx("""$B(?:(?:hd[ .-]?)?ts(?:[ .-]?rip)?|telesync|pdvd)$E"""),
        ReleaseSource.TELECINE to rx("""$B(?:(?:hd[ .-]?)?tc|telecine)$E"""),
        ReleaseSource.SCREENER to rx("""$B(?:(?:dvd|bd|web|hd)[ .-]?)?(?:scr|screener)$E"""),
        ReleaseSource.BLURAY to rx("""$B(?:blu[ .-]?ray|bd[ .-]?rip|br[ .-]?rip|bd(?:25|50|66|100))$E"""),
        ReleaseSource.WEBRIP to rx("""$B(?:web[ .-]?rip|web[ .-]?cap|hd[ .-]?rip)$E"""),
        ReleaseSource.WEB_DL to rx("""$B(?:web[ .-]?dl|web)$E"""),
        ReleaseSource.HDTV to rx("""$B(?:hdtv(?:rip)?|pdtv|sdtv|dsr(?:ip)?|tv[ .-]?rip)$E"""),
        ReleaseSource.DVD to rx("""$B(?:dvd[ .-]?rip|dvdr|dvd[ .-]?(?:5|9)|dvd|r5)$E"""),
    )

    private val CODECS: List<Pair<VideoCodec, Regex>> = listOf(
        VideoCodec.H265 to rx("""$B(?:[xh][ .]?265|hevc)$E"""),
        VideoCodec.AV1 to rx("""${B}av1$E"""),
        VideoCodec.H264 to rx("""$B(?:[xh][ .]?264|avc)$E"""),
        VideoCodec.MPEG4 to rx("""$B(?:xvid|divx)$E"""),
    )

    private val HDR: List<Pair<HdrFormat, Regex>> = listOf(
        HdrFormat.HDR10 to rx("""${B}hdr(?:10)?(?![\p{L}\p{N}+])"""),
        HdrFormat.HDR10_PLUS to rx("""${B}hdr10(?:\+|plus$E|p$E)"""),
        HdrFormat.DOLBY_VISION to rx("""$B(?:dv|dovi|dolby[ .-]?vision)$E"""),
    )

    private val AUDIO: List<Pair<String, Regex>> = listOf(
        "TrueHD" to rx("""${B}true[ .-]?hd(?![\p{L}])"""),
        "DTS-HD MA" to rx("""${B}dts[ .-]?hd[ .-]?ma(?![\p{L}])"""),
        "DTS:X" to rx("""${B}dts[ .:-]?x$E"""),
        "DTS-HD" to rx("""${B}dts[ .-]?hd(?![\p{L}])"""),
        "DTS" to rx("""${B}dts(?![\p{L}])"""),
        "DDP" to rx("""$B(?:ddp|dd\+|e[ .-]?ac[ .-]?3|dolby[ .]digital[ .]plus)(?![\p{L}])"""),
        "AC3" to rx("""$B(?:ac[ .-]?3(?![\p{L}\p{N}])|dd(?![\p{L}+]))"""),
        "AAC" to rx("""${B}aac(?![\p{L}])"""),
        "FLAC" to rx("""${B}flac(?![\p{L}])"""),
        "LPCM" to rx("""${B}l?pcm(?![\p{L}])"""),
        "Opus" to rx("""${B}opus(?![\p{L}])"""),
        "MP3" to rx("""${B}mp3$E"""),
    )
    private val ATMOS = rx("""${B}atmos$E""")
    private val CHANNELS = Regex("""(?<!\d)([1257])[ .]([01])(?!\d)""")
    private val CHANNEL_COUNT = rx("""$B([268])ch$E""")

    private val WEAK_ALTERNATIVES = listOf(
        "remux", "hd[ .-]?cam", "cam(?:[ .-]?rip)?", "hd[ .-]?ts", "ts", "telesync", "hd[ .-]?tc", "tc", "telecine",
        "(?:dvd|bd)[ .-]?scr", "scr", "screener", "blu[ .-]?ray", "bd[ .-]?rip", "br[ .-]?rip", "web[ .-]?dl", "web[ .-]?rip",
        "web", "hd[ .-]?rip", "hdtv", "pdtv", "dvd[ .-]?rip", "dvd", "[xh][ .]?26[45]", "hevc", "avc", "xvid", "divx", "av1",
        "extended", "unrated", "uncut", "remastered", "directors[ .]cut", "theatrical", "imax", "proper", "repack",
        "limited", "internal", "multi", "truefrench", "french", "vostfr", "subbed", "dubbed", "hdr(?:10)?", "dv",
        "(?:dts|aac|ac3|ddp|eac3)",
    ).joinToString("|")

    /** Tags that end the title when there's no year or strong marker. */
    private val WEAK_MARKERS = rx("$B(?:$WEAK_ALTERNATIVES)$E")

    /** A run of [WEAK_MARKERS] at the very end of a string. */
    private val WEAK_RUN = rx("""(?:[\s._\-\[\]()]*$B(?:$WEAK_ALTERNATIVES)$E)+[\s._\-\[\]()]*$""")
    // endregion
}
