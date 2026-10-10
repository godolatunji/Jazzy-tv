package com.nexient.jazzy.core.database.app

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile ORDER BY createdAt")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profile WHERE id = :id")
    fun observe(id: Long): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = :id")
    suspend fun get(id: Long): ProfileEntity?

    @Query("SELECT COUNT(*) FROM profile")
    suspend fun count(): Int

    @Insert
    suspend fun insert(profile: ProfileEntity): Long

    @Upsert
    suspend fun upsert(profile: ProfileEntity)

    @Query("DELETE FROM profile WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface SettingsDao {
    @Query("SELECT value FROM app_setting WHERE `key` = :key")
    fun observeApp(key: String): Flow<String?>

    @Query("SELECT * FROM app_setting")
    fun observeAllApp(): Flow<List<AppSettingEntity>>

    @Query("SELECT value FROM app_setting WHERE `key` = :key")
    suspend fun getApp(key: String): String?

    @Upsert
    suspend fun putApp(setting: AppSettingEntity)

    @Query("DELETE FROM app_setting WHERE `key` = :key")
    suspend fun removeApp(key: String)

    @Query("SELECT * FROM profile_setting WHERE profileId = :profileId")
    fun observeProfile(profileId: Long): Flow<List<ProfileSettingEntity>>

    @Upsert
    suspend fun putProfile(setting: ProfileSettingEntity)
}

@Dao
interface TitleSnapshotDao {
    @Upsert
    suspend fun upsert(snapshot: TitleSnapshotEntity)

    @Query("SELECT * FROM title_snapshot WHERE tmdbId = :tmdbId AND mediaType = :mediaType")
    suspend fun get(tmdbId: Int, mediaType: String): TitleSnapshotEntity?

    /** Snapshots no longer referenced by any list, progress or history row. */
    @Query(
        """DELETE FROM title_snapshot WHERE NOT EXISTS (SELECT 1 FROM my_list_item m WHERE m.tmdbId = title_snapshot.tmdbId AND m.mediaType = title_snapshot.mediaType)
        AND NOT EXISTS (SELECT 1 FROM liked_title l WHERE l.tmdbId = title_snapshot.tmdbId AND l.mediaType = title_snapshot.mediaType)
        AND NOT EXISTS (SELECT 1 FROM watch_progress w WHERE w.tmdbId = title_snapshot.tmdbId AND w.mediaType = title_snapshot.mediaType)
        AND NOT EXISTS (SELECT 1 FROM play_session p WHERE p.tmdbId = title_snapshot.tmdbId AND p.mediaType = title_snapshot.mediaType)""",
    )
    suspend fun deleteUnreferenced(): Int
}

/** Row joined with its title snapshot. */
data class SnapshotWithTime(
    val tmdbId: Int,
    val mediaType: String,
    val title: String,
    val originalTitle: String,
    val year: Int?,
    val posterPath: String?,
    val backdropPath: String?,
    val genreIds: List<Int>,
    val imdbId: String?,
    val addedAt: Long,
)

@Dao
interface LibraryDao {
    // My List
    @Upsert
    suspend fun addToMyList(item: MyListItemEntity)

    @Query("DELETE FROM my_list_item WHERE profileId = :profileId AND tmdbId = :tmdbId AND mediaType = :mediaType")
    suspend fun removeFromMyList(profileId: Long, tmdbId: Int, mediaType: String)

    @Query("SELECT EXISTS(SELECT 1 FROM my_list_item WHERE profileId = :profileId AND tmdbId = :tmdbId AND mediaType = :mediaType)")
    fun observeInMyList(profileId: Long, tmdbId: Int, mediaType: String): Flow<Boolean>

    @Query(
        """SELECT s.tmdbId, s.mediaType, s.title, s.originalTitle, s.year, s.posterPath, s.backdropPath, s.genreIds, s.imdbId, m.addedAt
        FROM my_list_item m JOIN title_snapshot s ON s.tmdbId = m.tmdbId AND s.mediaType = m.mediaType
        WHERE m.profileId = :profileId ORDER BY m.addedAt DESC""",
    )
    fun observeMyList(profileId: Long): Flow<List<SnapshotWithTime>>

    @Query("SELECT tmdbId || ':' || mediaType FROM my_list_item WHERE profileId = :profileId")
    fun observeMyListKeys(profileId: Long): Flow<List<String>>

    // Liked
    @Upsert
    suspend fun like(item: LikedTitleEntity)

    @Query("DELETE FROM liked_title WHERE profileId = :profileId AND tmdbId = :tmdbId AND mediaType = :mediaType")
    suspend fun unlike(profileId: Long, tmdbId: Int, mediaType: String)

    @Query("SELECT EXISTS(SELECT 1 FROM liked_title WHERE profileId = :profileId AND tmdbId = :tmdbId AND mediaType = :mediaType)")
    fun observeLiked(profileId: Long, tmdbId: Int, mediaType: String): Flow<Boolean>

    @Query(
        """SELECT s.tmdbId, s.mediaType, s.title, s.originalTitle, s.year, s.posterPath, s.backdropPath, s.genreIds, s.imdbId, l.likedAt AS addedAt
        FROM liked_title l JOIN title_snapshot s ON s.tmdbId = l.tmdbId AND s.mediaType = l.mediaType
        WHERE l.profileId = :profileId ORDER BY l.likedAt DESC""",
    )
    fun observeLikedTitles(profileId: Long): Flow<List<SnapshotWithTime>>

    @Query("SELECT tmdbId || ':' || mediaType FROM liked_title WHERE profileId = :profileId")
    fun observeLikedKeys(profileId: Long): Flow<List<String>>

    @Query("SELECT * FROM liked_title WHERE profileId = :profileId")
    suspend fun likedFor(profileId: Long): List<LikedTitleEntity>

    @Query("SELECT * FROM my_list_item WHERE profileId = :profileId")
    suspend fun myListFor(profileId: Long): List<MyListItemEntity>
}

@Dao
interface ProgressDao {
    @Upsert
    suspend fun upsert(progress: WatchProgressEntity)

    @Query(
        "SELECT * FROM watch_progress WHERE profileId = :profileId AND tmdbId = :tmdbId AND mediaType = :mediaType AND season = :season AND episode = :episode",
    )
    suspend fun get(profileId: Long, tmdbId: Int, mediaType: String, season: Int, episode: Int): WatchProgressEntity?

    @Query(
        "SELECT * FROM watch_progress WHERE profileId = :profileId AND tmdbId = :tmdbId AND mediaType = :mediaType AND season = :season AND episode = :episode",
    )
    fun observe(profileId: Long, tmdbId: Int, mediaType: String, season: Int, episode: Int): Flow<WatchProgressEntity?>

    /** All progress rows for one title (every episode of a show). */
    @Query("SELECT * FROM watch_progress WHERE profileId = :profileId AND tmdbId = :tmdbId AND mediaType = :mediaType")
    fun observeForTitle(profileId: Long, tmdbId: Int, mediaType: String): Flow<List<WatchProgressEntity>>

    @Query("SELECT * FROM watch_progress WHERE profileId = :profileId AND tmdbId = :tmdbId AND mediaType = :mediaType ORDER BY updatedAt DESC LIMIT 1")
    suspend fun latestForTitle(profileId: Long, tmdbId: Int, mediaType: String): WatchProgressEntity?

    /** The most recent progress row per title, newest first (Continue Watching candidates). */
    @Query(
        """SELECT w.* FROM watch_progress w
        WHERE w.profileId = :profileId AND w.updatedAt = (
            SELECT MAX(w2.updatedAt) FROM watch_progress w2
            WHERE w2.profileId = w.profileId AND w2.tmdbId = w.tmdbId AND w2.mediaType = w.mediaType)
        ORDER BY w.updatedAt DESC LIMIT :limit""",
    )
    fun observeLatestPerTitle(profileId: Long, limit: Int): Flow<List<WatchProgressEntity>>

    @Query("SELECT * FROM watch_progress WHERE profileId = :profileId")
    suspend fun allFor(profileId: Long): List<WatchProgressEntity>

    @Query("DELETE FROM watch_progress WHERE profileId = :profileId AND tmdbId = :tmdbId AND mediaType = :mediaType")
    suspend fun deleteForTitle(profileId: Long, tmdbId: Int, mediaType: String)

    @Query("UPDATE watch_progress SET updatedAt = 0 WHERE profileId = :profileId AND tmdbId = :tmdbId AND mediaType = :mediaType")
    suspend fun hideFromContinue(profileId: Long, tmdbId: Int, mediaType: String)
}

@Dao
interface PlaySessionDao {
    @Insert
    suspend fun insert(session: PlaySessionEntity): Long

    @Query(
        """UPDATE play_session SET endedAt = :endedAt, stopPositionMs = :stopPositionMs, durationMs = :durationMs,
        watchedMs = :watchedMs, endReason = :endReason WHERE id = :id""",
    )
    suspend fun finish(id: Long, endedAt: Long, stopPositionMs: Long, durationMs: Long, watchedMs: Long, endReason: String?)

    @Query("UPDATE play_session SET stopPositionMs = :stopPositionMs, durationMs = :durationMs, watchedMs = :watchedMs, endedAt = :now WHERE id = :id")
    suspend fun checkpoint(id: Long, stopPositionMs: Long, durationMs: Long, watchedMs: Long, now: Long)

    @Query("SELECT * FROM play_session WHERE profileId = :profileId AND watchedMs >= :minWatchedMs ORDER BY startedAt DESC")
    fun observeVisible(profileId: Long, minWatchedMs: Long): Flow<List<PlaySessionEntity>>

    @Query("SELECT * FROM play_session WHERE profileId = :profileId ORDER BY startedAt DESC")
    suspend fun allFor(profileId: Long): List<PlaySessionEntity>

    @Query("DELETE FROM play_session WHERE id IN (:ids)")
    suspend fun delete(ids: List<Long>)

    @Query("DELETE FROM play_session WHERE profileId = :profileId AND tmdbId = :tmdbId AND mediaType = :mediaType")
    suspend fun deleteForTitle(profileId: Long, tmdbId: Int, mediaType: String)

    @Query("DELETE FROM play_session WHERE profileId = :profileId")
    suspend fun deleteAll(profileId: Long)
}

@Dao
interface SearchHistoryDao {
    @Upsert
    suspend fun upsert(entry: SearchHistoryEntity)

    @Query("SELECT * FROM search_history WHERE profileId = :profileId ORDER BY searchedAt DESC LIMIT :limit")
    fun observeRecent(profileId: Long, limit: Int): Flow<List<SearchHistoryEntity>>

    @Query("DELETE FROM search_history WHERE profileId = :profileId")
    suspend fun clear(profileId: Long)

    @Query("DELETE FROM search_history WHERE profileId = :profileId AND query = :query")
    suspend fun delete(profileId: Long, query: String)
}

@Dao
interface RecommendationStateDao {
    @Query("SELECT * FROM seed_genre WHERE profileId = :profileId")
    suspend fun seeds(profileId: Long): List<SeedGenreEntity>

    @Query("SELECT * FROM seed_genre WHERE profileId = :profileId")
    fun observeSeeds(profileId: Long): Flow<List<SeedGenreEntity>>

    @Query("DELETE FROM seed_genre WHERE profileId = :profileId")
    suspend fun clearSeeds(profileId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeeds(seeds: List<SeedGenreEntity>)

    @Transaction
    suspend fun replaceSeeds(profileId: Long, seeds: List<SeedGenreEntity>) {
        clearSeeds(profileId)
        insertSeeds(seeds)
    }

    @Query("SELECT * FROM genre_affinity WHERE profileId = :profileId ORDER BY score DESC")
    suspend fun affinity(profileId: Long): List<GenreAffinityEntity>

    @Query("DELETE FROM genre_affinity WHERE profileId = :profileId")
    suspend fun clearAffinity(profileId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAffinity(rows: List<GenreAffinityEntity>)

    @Transaction
    suspend fun replaceAffinity(profileId: Long, rows: List<GenreAffinityEntity>) {
        clearAffinity(profileId)
        insertAffinity(rows)
    }
}

@Dao
interface ProviderDao {
    @Query("SELECT * FROM provider_setting")
    fun observeAll(): Flow<List<ProviderSettingEntity>>

    @Query("SELECT * FROM provider_setting WHERE providerId = :providerId")
    suspend fun get(providerId: String): ProviderSettingEntity?

    @Upsert
    suspend fun upsert(entity: ProviderSettingEntity)

    @Query("SELECT * FROM source_flag WHERE badUntil > :now")
    fun observeActiveFlags(now: Long): Flow<List<SourceFlagEntity>>

    @Upsert
    suspend fun flag(entity: SourceFlagEntity)

    @Query("SELECT * FROM originals_network ORDER BY sortOrder")
    fun observeOriginals(): Flow<List<OriginalsNetworkEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOriginals(rows: List<OriginalsNetworkEntity>)

    @Query("SELECT * FROM public_tracker WHERE enabled = 1")
    suspend fun enabledTrackers(): List<PublicTrackerEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTrackers(rows: List<PublicTrackerEntity>)
}
