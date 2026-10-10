package com.nexient.jazzy.core.model

import kotlinx.serialization.Serializable

/** What to search torrent sites for. Built from TMDB data (spec §5.8). */
@Serializable
data class SourceQuery(
    val tmdbId: Int,
    val mediaType: MediaType,
    val title: String,
    val originalTitle: String?,
    val year: Int?,
    val imdbId: String?,
    val season: Int? = null,
    val episode: Int? = null,
) {
    /** Cache key: one result list per movie or per episode. */
    val cacheKey: String
        get() = buildString {
            append(mediaType.apiValue).append(':').append(tmdbId)
            if (season != null) append(":s").append(season)
            if (episode != null) append(":e").append(episode)
        }
}

@Serializable
enum class Resolution(val label: String, val rank: Int) {
    UHD_2160("2160p", 4), FHD_1080("1080p", 3), HD_720("720p", 2), SD("SD", 1);
}

@Serializable
enum class ReleaseSource(val label: String, val isLowQuality: Boolean = false) {
    BLURAY("BluRay"), REMUX("Remux"), WEB_DL("WEB-DL"), WEBRIP("WEBRip"), HDTV("HDTV"), DVD("DVD"),
    CAM("CAM", true), TELESYNC("TS", true), TELECINE("TC", true), HDCAM("HDCAM", true), SCREENER("SCR", true);
}

@Serializable
enum class VideoCodec(val label: String) { H264("x264"), H265("x265"), AV1("AV1"), MPEG4("XviD") }

@Serializable
enum class HdrFormat(val label: String) { HDR10("HDR10"), HDR10_PLUS("HDR10+"), DOLBY_VISION("DV") }

/** Everything parsed from a release name (spec §5.8 "Result parsing"). */
@Serializable
data class ReleaseInfo(
    val resolution: Resolution? = null,
    val source: ReleaseSource? = null,
    val videoCodec: VideoCodec? = null,
    val hdr: List<HdrFormat> = emptyList(),
    val audio: String? = null,
    val audioChannels: String? = null,
    val year: Int? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val isSeasonPack: Boolean = false,
    val isCompleteSeries: Boolean = false,
) {
    val qualityLabel: String
        get() = listOfNotNull(resolution?.label, source?.label).joinToString(" ").ifEmpty { "Unknown" }
}

/** One row in the Sources list. Rows from several sites with the same info-hash are merged (SRC-8). */
@Serializable
data class TorrentSource(
    /** Stable row key: info-hash when known, otherwise provider + site key. */
    val key: String,
    val name: String,
    val infoHash: String?,
    /** Null when the site only lists a detail page; resolved on Play (SRC-7). */
    val magnet: String?,
    val sizeBytes: Long?,
    val seeders: Int,
    val leechers: Int,
    /** Provider IDs that listed this torrent, e.g. ["thepiratebay", "yts"]. */
    val providerIds: List<String>,
    val providerNames: List<String>,
    val release: ReleaseInfo,
    /** Opaque data the owning provider needs to resolve a magnet later. */
    val resolveHint: Map<String, String> = emptyMap(),
    val uploadedAt: String? = null,
) {
    val health: SourceHealth
        get() = when {
            seeders >= 50 -> SourceHealth.GOOD
            seeders >= 10 -> SourceHealth.FAIR
            else -> SourceHealth.POOR
        }
}

enum class SourceHealth { GOOD, FAIR, POOR }

enum class SourceSort { SEEDERS, SIZE, QUALITY }

/** Per-site progress for one search (SL-8). */
@Serializable
sealed interface ProviderSearchState {
    @Serializable data object Searching : ProviderSearchState
    @Serializable data class Done(val count: Int) : ProviderSearchState
    @Serializable data object Unsupported : ProviderSearchState
    @Serializable data class Failed(val reason: FailureReason, val message: String? = null) : ProviderSearchState
}

@Serializable
enum class FailureReason { TIMEOUT, BLOCKED, UNREACHABLE, HTTP_ERROR, PARSE_ERROR, LOGIN_REQUIRED, OTHER }

@Serializable
data class ProviderStatus(val providerId: String, val providerName: String, val state: ProviderSearchState)

/** Progressive search output: emitted initially and after each site finishes. */
data class SourceSearchResult(
    val query: SourceQuery,
    val sources: List<TorrentSource>,
    val statuses: List<ProviderStatus>,
    val isComplete: Boolean,
    val fromCache: Boolean = false,
)

/** Static description of a built-in site provider. */
data class ProviderInfo(
    val id: String,
    val name: String,
    val isPublic: Boolean,
    val enabledByDefault: Boolean,
    val defaultBaseUrl: String,
    val supportsMovies: Boolean,
    val supportsTv: Boolean,
    val authKinds: Set<ProviderAuthKind> = emptySet(),
)

enum class ProviderAuthKind { CREDENTIALS, COOKIES }

/** User configuration for one provider (SRC-5), stored in jazzy.db. */
data class ProviderSettings(
    val providerId: String,
    val enabled: Boolean,
    val baseUrlOverride: String? = null,
    val username: String? = null,
    val password: String? = null,
    val cookies: String? = null,
)

data class ProviderHealth(
    val providerId: String,
    val lastOkAt: Long?,
    val avgLatencyMs: Long?,
    val errorCount: Int,
    val lastError: String?,
)
