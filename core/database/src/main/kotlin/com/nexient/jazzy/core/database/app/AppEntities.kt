package com.nexient.jazzy.core.database.app

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val avatarId: Int,
    val isKids: Boolean,
    val maturity: String,
    val language: String,
    val subtitleLanguage: String?,
    val audioLanguage: String?,
    val pinHash: String?,
    val pinSalt: String?,
    val autoplayNext: Boolean,
    val createdAt: Long,
)

@Entity(tableName = "app_setting")
data class AppSettingEntity(@PrimaryKey val key: String, val value: String)

@Entity(
    tableName = "profile_setting",
    primaryKeys = ["profileId", "key"],
    foreignKeys = [ForeignKey(ProfileEntity::class, ["id"], ["profileId"], onDelete = ForeignKey.CASCADE)],
)
data class ProfileSettingEntity(val profileId: Long, val key: String, val value: String)

/** Minimal copy of every title referenced by a list, history or resume point (spec §7.1). */
@Entity(tableName = "title_snapshot", primaryKeys = ["tmdbId", "mediaType"])
data class TitleSnapshotEntity(
    val tmdbId: Int,
    val mediaType: String,
    val title: String,
    val originalTitle: String,
    val year: Int?,
    val posterPath: String?,
    val backdropPath: String?,
    val genreIds: List<Int>,
    val imdbId: String?,
    val updatedAt: Long,
)

@Entity(
    tableName = "my_list_item",
    primaryKeys = ["profileId", "tmdbId", "mediaType"],
    foreignKeys = [ForeignKey(ProfileEntity::class, ["id"], ["profileId"], onDelete = ForeignKey.CASCADE)],
)
data class MyListItemEntity(val profileId: Long, val tmdbId: Int, val mediaType: String, val addedAt: Long)

@Entity(
    tableName = "liked_title",
    primaryKeys = ["profileId", "tmdbId", "mediaType"],
    foreignKeys = [ForeignKey(ProfileEntity::class, ["id"], ["profileId"], onDelete = ForeignKey.CASCADE)],
)
data class LikedTitleEntity(val profileId: Long, val tmdbId: Int, val mediaType: String, val likedAt: Long)

@Entity(
    tableName = "watch_progress",
    primaryKeys = ["profileId", "tmdbId", "mediaType", "season", "episode"],
    foreignKeys = [ForeignKey(ProfileEntity::class, ["id"], ["profileId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("profileId", "updatedAt")],
)
data class WatchProgressEntity(
    val profileId: Long,
    val tmdbId: Int,
    val mediaType: String,
    /** -1 for movies. */
    val season: Int,
    val episode: Int,
    val positionMs: Long,
    val durationMs: Long,
    val completed: Boolean,
    val lastInfoHash: String?,
    val lastFileIndex: Int?,
    val lastSourceName: String?,
    val lastQuality: String?,
    val episodeTitle: String?,
    val updatedAt: Long,
)

@Entity(
    tableName = "play_session",
    foreignKeys = [ForeignKey(ProfileEntity::class, ["id"], ["profileId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("profileId", "startedAt"), Index("profileId", "tmdbId")],
)
data class PlaySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val tmdbId: Int,
    val mediaType: String,
    val season: Int,
    val episode: Int,
    val episodeTitle: String?,
    val infoHash: String?,
    val fileIndex: Int?,
    val sourceName: String?,
    val quality: String?,
    val providerId: String?,
    val startedAt: Long,
    val endedAt: Long?,
    val startPositionMs: Long,
    val stopPositionMs: Long,
    val durationMs: Long,
    val watchedMs: Long,
    val endReason: String?,
)

@Entity(
    tableName = "search_history",
    primaryKeys = ["profileId", "query"],
    foreignKeys = [ForeignKey(ProfileEntity::class, ["id"], ["profileId"], onDelete = ForeignKey.CASCADE)],
)
data class SearchHistoryEntity(val profileId: Long, val query: String, val searchedAt: Long)

@Entity(
    tableName = "seed_genre",
    primaryKeys = ["profileId", "unifiedGenreId"],
    foreignKeys = [ForeignKey(ProfileEntity::class, ["id"], ["profileId"], onDelete = ForeignKey.CASCADE)],
)
data class SeedGenreEntity(val profileId: Long, val unifiedGenreId: String, val addedAt: Long)

@Entity(
    tableName = "genre_affinity",
    primaryKeys = ["profileId", "unifiedGenreId"],
    foreignKeys = [ForeignKey(ProfileEntity::class, ["id"], ["profileId"], onDelete = ForeignKey.CASCADE)],
)
data class GenreAffinityEntity(val profileId: Long, val unifiedGenreId: String, val score: Double, val updatedAt: Long)

@Entity(tableName = "provider_setting")
data class ProviderSettingEntity(
    @PrimaryKey val providerId: String,
    /** Null means "use the provider's default". */
    val enabled: Boolean?,
    val baseUrlOverride: String?,
    val username: String?,
    val passwordEncrypted: String?,
    val cookiesEncrypted: String?,
    val lastOkAt: Long?,
    val avgLatencyMs: Long?,
    val errorCount: Int,
    val lastError: String?,
)

@Entity(tableName = "source_flag")
data class SourceFlagEntity(@PrimaryKey val infoHash: String, val badUntil: Long, val reason: String)

@Entity(tableName = "originals_network")
data class OriginalsNetworkEntity(
    @PrimaryKey val networkId: Int,
    val name: String,
    val mediaType: String,
    val enabled: Boolean,
    val sortOrder: Int,
)

@Entity(tableName = "public_tracker")
data class PublicTrackerEntity(@PrimaryKey val url: String, val enabled: Boolean)
