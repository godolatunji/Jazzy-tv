package com.nexient.jazzy.core.database.app

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AppDatabase_Impl : AppDatabase() {
  private val _profileDao: Lazy<ProfileDao> = lazy {
    ProfileDao_Impl(this)
  }

  private val _settingsDao: Lazy<SettingsDao> = lazy {
    SettingsDao_Impl(this)
  }

  private val _titleSnapshotDao: Lazy<TitleSnapshotDao> = lazy {
    TitleSnapshotDao_Impl(this)
  }

  private val _libraryDao: Lazy<LibraryDao> = lazy {
    LibraryDao_Impl(this)
  }

  private val _progressDao: Lazy<ProgressDao> = lazy {
    ProgressDao_Impl(this)
  }

  private val _playSessionDao: Lazy<PlaySessionDao> = lazy {
    PlaySessionDao_Impl(this)
  }

  private val _searchHistoryDao: Lazy<SearchHistoryDao> = lazy {
    SearchHistoryDao_Impl(this)
  }

  private val _recommendationStateDao: Lazy<RecommendationStateDao> = lazy {
    RecommendationStateDao_Impl(this)
  }

  private val _providerDao: Lazy<ProviderDao> = lazy {
    ProviderDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(1, "f3e3819ce5aa6bf89f626d45a9f88ba6", "d09d34b07748f19f31650a3f1f185a00") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `profile` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `avatarId` INTEGER NOT NULL, `isKids` INTEGER NOT NULL, `maturity` TEXT NOT NULL, `language` TEXT NOT NULL, `subtitleLanguage` TEXT, `audioLanguage` TEXT, `pinHash` TEXT, `pinSalt` TEXT, `autoplayNext` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `app_setting` (`key` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`key`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `profile_setting` (`profileId` INTEGER NOT NULL, `key` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`profileId`, `key`), FOREIGN KEY(`profileId`) REFERENCES `profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `title_snapshot` (`tmdbId` INTEGER NOT NULL, `mediaType` TEXT NOT NULL, `title` TEXT NOT NULL, `originalTitle` TEXT NOT NULL, `year` INTEGER, `posterPath` TEXT, `backdropPath` TEXT, `genreIds` TEXT NOT NULL, `imdbId` TEXT, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`tmdbId`, `mediaType`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `my_list_item` (`profileId` INTEGER NOT NULL, `tmdbId` INTEGER NOT NULL, `mediaType` TEXT NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`profileId`, `tmdbId`, `mediaType`), FOREIGN KEY(`profileId`) REFERENCES `profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `liked_title` (`profileId` INTEGER NOT NULL, `tmdbId` INTEGER NOT NULL, `mediaType` TEXT NOT NULL, `likedAt` INTEGER NOT NULL, PRIMARY KEY(`profileId`, `tmdbId`, `mediaType`), FOREIGN KEY(`profileId`) REFERENCES `profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `watch_progress` (`profileId` INTEGER NOT NULL, `tmdbId` INTEGER NOT NULL, `mediaType` TEXT NOT NULL, `season` INTEGER NOT NULL, `episode` INTEGER NOT NULL, `positionMs` INTEGER NOT NULL, `durationMs` INTEGER NOT NULL, `completed` INTEGER NOT NULL, `lastInfoHash` TEXT, `lastFileIndex` INTEGER, `lastSourceName` TEXT, `lastQuality` TEXT, `episodeTitle` TEXT, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`profileId`, `tmdbId`, `mediaType`, `season`, `episode`), FOREIGN KEY(`profileId`) REFERENCES `profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_watch_progress_profileId_updatedAt` ON `watch_progress` (`profileId`, `updatedAt`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `play_session` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `profileId` INTEGER NOT NULL, `tmdbId` INTEGER NOT NULL, `mediaType` TEXT NOT NULL, `season` INTEGER NOT NULL, `episode` INTEGER NOT NULL, `episodeTitle` TEXT, `infoHash` TEXT, `fileIndex` INTEGER, `sourceName` TEXT, `quality` TEXT, `providerId` TEXT, `startedAt` INTEGER NOT NULL, `endedAt` INTEGER, `startPositionMs` INTEGER NOT NULL, `stopPositionMs` INTEGER NOT NULL, `durationMs` INTEGER NOT NULL, `watchedMs` INTEGER NOT NULL, `endReason` TEXT, FOREIGN KEY(`profileId`) REFERENCES `profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_play_session_profileId_startedAt` ON `play_session` (`profileId`, `startedAt`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_play_session_profileId_tmdbId` ON `play_session` (`profileId`, `tmdbId`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `search_history` (`profileId` INTEGER NOT NULL, `query` TEXT NOT NULL, `searchedAt` INTEGER NOT NULL, PRIMARY KEY(`profileId`, `query`), FOREIGN KEY(`profileId`) REFERENCES `profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `seed_genre` (`profileId` INTEGER NOT NULL, `unifiedGenreId` TEXT NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`profileId`, `unifiedGenreId`), FOREIGN KEY(`profileId`) REFERENCES `profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `genre_affinity` (`profileId` INTEGER NOT NULL, `unifiedGenreId` TEXT NOT NULL, `score` REAL NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`profileId`, `unifiedGenreId`), FOREIGN KEY(`profileId`) REFERENCES `profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `provider_setting` (`providerId` TEXT NOT NULL, `enabled` INTEGER, `baseUrlOverride` TEXT, `username` TEXT, `passwordEncrypted` TEXT, `cookiesEncrypted` TEXT, `lastOkAt` INTEGER, `avgLatencyMs` INTEGER, `errorCount` INTEGER NOT NULL, `lastError` TEXT, PRIMARY KEY(`providerId`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `source_flag` (`infoHash` TEXT NOT NULL, `badUntil` INTEGER NOT NULL, `reason` TEXT NOT NULL, PRIMARY KEY(`infoHash`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `originals_network` (`networkId` INTEGER NOT NULL, `name` TEXT NOT NULL, `mediaType` TEXT NOT NULL, `enabled` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, PRIMARY KEY(`networkId`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `public_tracker` (`url` TEXT NOT NULL, `enabled` INTEGER NOT NULL, PRIMARY KEY(`url`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'f3e3819ce5aa6bf89f626d45a9f88ba6')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `profile`")
        connection.execSQL("DROP TABLE IF EXISTS `app_setting`")
        connection.execSQL("DROP TABLE IF EXISTS `profile_setting`")
        connection.execSQL("DROP TABLE IF EXISTS `title_snapshot`")
        connection.execSQL("DROP TABLE IF EXISTS `my_list_item`")
        connection.execSQL("DROP TABLE IF EXISTS `liked_title`")
        connection.execSQL("DROP TABLE IF EXISTS `watch_progress`")
        connection.execSQL("DROP TABLE IF EXISTS `play_session`")
        connection.execSQL("DROP TABLE IF EXISTS `search_history`")
        connection.execSQL("DROP TABLE IF EXISTS `seed_genre`")
        connection.execSQL("DROP TABLE IF EXISTS `genre_affinity`")
        connection.execSQL("DROP TABLE IF EXISTS `provider_setting`")
        connection.execSQL("DROP TABLE IF EXISTS `source_flag`")
        connection.execSQL("DROP TABLE IF EXISTS `originals_network`")
        connection.execSQL("DROP TABLE IF EXISTS `public_tracker`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        connection.execSQL("PRAGMA foreign_keys = ON")
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
        val _columnsProfile: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsProfile.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProfile.put("name", TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProfile.put("avatarId", TableInfo.Column("avatarId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProfile.put("isKids", TableInfo.Column("isKids", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProfile.put("maturity", TableInfo.Column("maturity", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProfile.put("language", TableInfo.Column("language", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProfile.put("subtitleLanguage", TableInfo.Column("subtitleLanguage", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProfile.put("audioLanguage", TableInfo.Column("audioLanguage", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProfile.put("pinHash", TableInfo.Column("pinHash", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProfile.put("pinSalt", TableInfo.Column("pinSalt", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProfile.put("autoplayNext", TableInfo.Column("autoplayNext", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProfile.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysProfile: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesProfile: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoProfile: TableInfo = TableInfo("profile", _columnsProfile, _foreignKeysProfile, _indicesProfile)
        val _existingProfile: TableInfo = read(connection, "profile")
        if (!_infoProfile.equals(_existingProfile)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |profile(com.nexient.jazzy.core.database.app.ProfileEntity).
              | Expected:
              |""".trimMargin() + _infoProfile + """
              |
              | Found:
              |""".trimMargin() + _existingProfile)
        }
        val _columnsAppSetting: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsAppSetting.put("key", TableInfo.Column("key", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAppSetting.put("value", TableInfo.Column("value", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysAppSetting: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesAppSetting: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoAppSetting: TableInfo = TableInfo("app_setting", _columnsAppSetting, _foreignKeysAppSetting, _indicesAppSetting)
        val _existingAppSetting: TableInfo = read(connection, "app_setting")
        if (!_infoAppSetting.equals(_existingAppSetting)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |app_setting(com.nexient.jazzy.core.database.app.AppSettingEntity).
              | Expected:
              |""".trimMargin() + _infoAppSetting + """
              |
              | Found:
              |""".trimMargin() + _existingAppSetting)
        }
        val _columnsProfileSetting: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsProfileSetting.put("profileId", TableInfo.Column("profileId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProfileSetting.put("key", TableInfo.Column("key", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProfileSetting.put("value", TableInfo.Column("value", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysProfileSetting: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysProfileSetting.add(TableInfo.ForeignKey("profile", "CASCADE", "NO ACTION", listOf("profileId"), listOf("id")))
        val _indicesProfileSetting: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoProfileSetting: TableInfo = TableInfo("profile_setting", _columnsProfileSetting, _foreignKeysProfileSetting, _indicesProfileSetting)
        val _existingProfileSetting: TableInfo = read(connection, "profile_setting")
        if (!_infoProfileSetting.equals(_existingProfileSetting)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |profile_setting(com.nexient.jazzy.core.database.app.ProfileSettingEntity).
              | Expected:
              |""".trimMargin() + _infoProfileSetting + """
              |
              | Found:
              |""".trimMargin() + _existingProfileSetting)
        }
        val _columnsTitleSnapshot: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsTitleSnapshot.put("tmdbId", TableInfo.Column("tmdbId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTitleSnapshot.put("mediaType", TableInfo.Column("mediaType", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTitleSnapshot.put("title", TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTitleSnapshot.put("originalTitle", TableInfo.Column("originalTitle", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTitleSnapshot.put("year", TableInfo.Column("year", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTitleSnapshot.put("posterPath", TableInfo.Column("posterPath", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTitleSnapshot.put("backdropPath", TableInfo.Column("backdropPath", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTitleSnapshot.put("genreIds", TableInfo.Column("genreIds", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTitleSnapshot.put("imdbId", TableInfo.Column("imdbId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTitleSnapshot.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysTitleSnapshot: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesTitleSnapshot: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoTitleSnapshot: TableInfo = TableInfo("title_snapshot", _columnsTitleSnapshot, _foreignKeysTitleSnapshot, _indicesTitleSnapshot)
        val _existingTitleSnapshot: TableInfo = read(connection, "title_snapshot")
        if (!_infoTitleSnapshot.equals(_existingTitleSnapshot)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |title_snapshot(com.nexient.jazzy.core.database.app.TitleSnapshotEntity).
              | Expected:
              |""".trimMargin() + _infoTitleSnapshot + """
              |
              | Found:
              |""".trimMargin() + _existingTitleSnapshot)
        }
        val _columnsMyListItem: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsMyListItem.put("profileId", TableInfo.Column("profileId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMyListItem.put("tmdbId", TableInfo.Column("tmdbId", "INTEGER", true, 2, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMyListItem.put("mediaType", TableInfo.Column("mediaType", "TEXT", true, 3, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMyListItem.put("addedAt", TableInfo.Column("addedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysMyListItem: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysMyListItem.add(TableInfo.ForeignKey("profile", "CASCADE", "NO ACTION", listOf("profileId"), listOf("id")))
        val _indicesMyListItem: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoMyListItem: TableInfo = TableInfo("my_list_item", _columnsMyListItem, _foreignKeysMyListItem, _indicesMyListItem)
        val _existingMyListItem: TableInfo = read(connection, "my_list_item")
        if (!_infoMyListItem.equals(_existingMyListItem)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |my_list_item(com.nexient.jazzy.core.database.app.MyListItemEntity).
              | Expected:
              |""".trimMargin() + _infoMyListItem + """
              |
              | Found:
              |""".trimMargin() + _existingMyListItem)
        }
        val _columnsLikedTitle: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsLikedTitle.put("profileId", TableInfo.Column("profileId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsLikedTitle.put("tmdbId", TableInfo.Column("tmdbId", "INTEGER", true, 2, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsLikedTitle.put("mediaType", TableInfo.Column("mediaType", "TEXT", true, 3, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsLikedTitle.put("likedAt", TableInfo.Column("likedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysLikedTitle: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysLikedTitle.add(TableInfo.ForeignKey("profile", "CASCADE", "NO ACTION", listOf("profileId"), listOf("id")))
        val _indicesLikedTitle: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoLikedTitle: TableInfo = TableInfo("liked_title", _columnsLikedTitle, _foreignKeysLikedTitle, _indicesLikedTitle)
        val _existingLikedTitle: TableInfo = read(connection, "liked_title")
        if (!_infoLikedTitle.equals(_existingLikedTitle)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |liked_title(com.nexient.jazzy.core.database.app.LikedTitleEntity).
              | Expected:
              |""".trimMargin() + _infoLikedTitle + """
              |
              | Found:
              |""".trimMargin() + _existingLikedTitle)
        }
        val _columnsWatchProgress: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsWatchProgress.put("profileId", TableInfo.Column("profileId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWatchProgress.put("tmdbId", TableInfo.Column("tmdbId", "INTEGER", true, 2, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWatchProgress.put("mediaType", TableInfo.Column("mediaType", "TEXT", true, 3, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWatchProgress.put("season", TableInfo.Column("season", "INTEGER", true, 4, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWatchProgress.put("episode", TableInfo.Column("episode", "INTEGER", true, 5, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWatchProgress.put("positionMs", TableInfo.Column("positionMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWatchProgress.put("durationMs", TableInfo.Column("durationMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWatchProgress.put("completed", TableInfo.Column("completed", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWatchProgress.put("lastInfoHash", TableInfo.Column("lastInfoHash", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWatchProgress.put("lastFileIndex", TableInfo.Column("lastFileIndex", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWatchProgress.put("lastSourceName", TableInfo.Column("lastSourceName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWatchProgress.put("lastQuality", TableInfo.Column("lastQuality", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWatchProgress.put("episodeTitle", TableInfo.Column("episodeTitle", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWatchProgress.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysWatchProgress: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysWatchProgress.add(TableInfo.ForeignKey("profile", "CASCADE", "NO ACTION", listOf("profileId"), listOf("id")))
        val _indicesWatchProgress: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesWatchProgress.add(TableInfo.Index("index_watch_progress_profileId_updatedAt", false, listOf("profileId", "updatedAt"), listOf("ASC", "ASC")))
        val _infoWatchProgress: TableInfo = TableInfo("watch_progress", _columnsWatchProgress, _foreignKeysWatchProgress, _indicesWatchProgress)
        val _existingWatchProgress: TableInfo = read(connection, "watch_progress")
        if (!_infoWatchProgress.equals(_existingWatchProgress)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |watch_progress(com.nexient.jazzy.core.database.app.WatchProgressEntity).
              | Expected:
              |""".trimMargin() + _infoWatchProgress + """
              |
              | Found:
              |""".trimMargin() + _existingWatchProgress)
        }
        val _columnsPlaySession: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsPlaySession.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPlaySession.put("profileId", TableInfo.Column("profileId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPlaySession.put("tmdbId", TableInfo.Column("tmdbId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPlaySession.put("mediaType", TableInfo.Column("mediaType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPlaySession.put("season", TableInfo.Column("season", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPlaySession.put("episode", TableInfo.Column("episode", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPlaySession.put("episodeTitle", TableInfo.Column("episodeTitle", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPlaySession.put("infoHash", TableInfo.Column("infoHash", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPlaySession.put("fileIndex", TableInfo.Column("fileIndex", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPlaySession.put("sourceName", TableInfo.Column("sourceName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPlaySession.put("quality", TableInfo.Column("quality", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPlaySession.put("providerId", TableInfo.Column("providerId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPlaySession.put("startedAt", TableInfo.Column("startedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPlaySession.put("endedAt", TableInfo.Column("endedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPlaySession.put("startPositionMs", TableInfo.Column("startPositionMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPlaySession.put("stopPositionMs", TableInfo.Column("stopPositionMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPlaySession.put("durationMs", TableInfo.Column("durationMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPlaySession.put("watchedMs", TableInfo.Column("watchedMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPlaySession.put("endReason", TableInfo.Column("endReason", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysPlaySession: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysPlaySession.add(TableInfo.ForeignKey("profile", "CASCADE", "NO ACTION", listOf("profileId"), listOf("id")))
        val _indicesPlaySession: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesPlaySession.add(TableInfo.Index("index_play_session_profileId_startedAt", false, listOf("profileId", "startedAt"), listOf("ASC", "ASC")))
        _indicesPlaySession.add(TableInfo.Index("index_play_session_profileId_tmdbId", false, listOf("profileId", "tmdbId"), listOf("ASC", "ASC")))
        val _infoPlaySession: TableInfo = TableInfo("play_session", _columnsPlaySession, _foreignKeysPlaySession, _indicesPlaySession)
        val _existingPlaySession: TableInfo = read(connection, "play_session")
        if (!_infoPlaySession.equals(_existingPlaySession)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |play_session(com.nexient.jazzy.core.database.app.PlaySessionEntity).
              | Expected:
              |""".trimMargin() + _infoPlaySession + """
              |
              | Found:
              |""".trimMargin() + _existingPlaySession)
        }
        val _columnsSearchHistory: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsSearchHistory.put("profileId", TableInfo.Column("profileId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSearchHistory.put("query", TableInfo.Column("query", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSearchHistory.put("searchedAt", TableInfo.Column("searchedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysSearchHistory: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysSearchHistory.add(TableInfo.ForeignKey("profile", "CASCADE", "NO ACTION", listOf("profileId"), listOf("id")))
        val _indicesSearchHistory: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoSearchHistory: TableInfo = TableInfo("search_history", _columnsSearchHistory, _foreignKeysSearchHistory, _indicesSearchHistory)
        val _existingSearchHistory: TableInfo = read(connection, "search_history")
        if (!_infoSearchHistory.equals(_existingSearchHistory)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |search_history(com.nexient.jazzy.core.database.app.SearchHistoryEntity).
              | Expected:
              |""".trimMargin() + _infoSearchHistory + """
              |
              | Found:
              |""".trimMargin() + _existingSearchHistory)
        }
        val _columnsSeedGenre: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsSeedGenre.put("profileId", TableInfo.Column("profileId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSeedGenre.put("unifiedGenreId", TableInfo.Column("unifiedGenreId", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSeedGenre.put("addedAt", TableInfo.Column("addedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysSeedGenre: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysSeedGenre.add(TableInfo.ForeignKey("profile", "CASCADE", "NO ACTION", listOf("profileId"), listOf("id")))
        val _indicesSeedGenre: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoSeedGenre: TableInfo = TableInfo("seed_genre", _columnsSeedGenre, _foreignKeysSeedGenre, _indicesSeedGenre)
        val _existingSeedGenre: TableInfo = read(connection, "seed_genre")
        if (!_infoSeedGenre.equals(_existingSeedGenre)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |seed_genre(com.nexient.jazzy.core.database.app.SeedGenreEntity).
              | Expected:
              |""".trimMargin() + _infoSeedGenre + """
              |
              | Found:
              |""".trimMargin() + _existingSeedGenre)
        }
        val _columnsGenreAffinity: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsGenreAffinity.put("profileId", TableInfo.Column("profileId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsGenreAffinity.put("unifiedGenreId", TableInfo.Column("unifiedGenreId", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsGenreAffinity.put("score", TableInfo.Column("score", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsGenreAffinity.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysGenreAffinity: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysGenreAffinity.add(TableInfo.ForeignKey("profile", "CASCADE", "NO ACTION", listOf("profileId"), listOf("id")))
        val _indicesGenreAffinity: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoGenreAffinity: TableInfo = TableInfo("genre_affinity", _columnsGenreAffinity, _foreignKeysGenreAffinity, _indicesGenreAffinity)
        val _existingGenreAffinity: TableInfo = read(connection, "genre_affinity")
        if (!_infoGenreAffinity.equals(_existingGenreAffinity)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |genre_affinity(com.nexient.jazzy.core.database.app.GenreAffinityEntity).
              | Expected:
              |""".trimMargin() + _infoGenreAffinity + """
              |
              | Found:
              |""".trimMargin() + _existingGenreAffinity)
        }
        val _columnsProviderSetting: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsProviderSetting.put("providerId", TableInfo.Column("providerId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProviderSetting.put("enabled", TableInfo.Column("enabled", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProviderSetting.put("baseUrlOverride", TableInfo.Column("baseUrlOverride", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProviderSetting.put("username", TableInfo.Column("username", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProviderSetting.put("passwordEncrypted", TableInfo.Column("passwordEncrypted", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProviderSetting.put("cookiesEncrypted", TableInfo.Column("cookiesEncrypted", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProviderSetting.put("lastOkAt", TableInfo.Column("lastOkAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProviderSetting.put("avgLatencyMs", TableInfo.Column("avgLatencyMs", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProviderSetting.put("errorCount", TableInfo.Column("errorCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsProviderSetting.put("lastError", TableInfo.Column("lastError", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysProviderSetting: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesProviderSetting: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoProviderSetting: TableInfo = TableInfo("provider_setting", _columnsProviderSetting, _foreignKeysProviderSetting, _indicesProviderSetting)
        val _existingProviderSetting: TableInfo = read(connection, "provider_setting")
        if (!_infoProviderSetting.equals(_existingProviderSetting)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |provider_setting(com.nexient.jazzy.core.database.app.ProviderSettingEntity).
              | Expected:
              |""".trimMargin() + _infoProviderSetting + """
              |
              | Found:
              |""".trimMargin() + _existingProviderSetting)
        }
        val _columnsSourceFlag: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsSourceFlag.put("infoHash", TableInfo.Column("infoHash", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSourceFlag.put("badUntil", TableInfo.Column("badUntil", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSourceFlag.put("reason", TableInfo.Column("reason", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysSourceFlag: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesSourceFlag: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoSourceFlag: TableInfo = TableInfo("source_flag", _columnsSourceFlag, _foreignKeysSourceFlag, _indicesSourceFlag)
        val _existingSourceFlag: TableInfo = read(connection, "source_flag")
        if (!_infoSourceFlag.equals(_existingSourceFlag)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |source_flag(com.nexient.jazzy.core.database.app.SourceFlagEntity).
              | Expected:
              |""".trimMargin() + _infoSourceFlag + """
              |
              | Found:
              |""".trimMargin() + _existingSourceFlag)
        }
        val _columnsOriginalsNetwork: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsOriginalsNetwork.put("networkId", TableInfo.Column("networkId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsOriginalsNetwork.put("name", TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsOriginalsNetwork.put("mediaType", TableInfo.Column("mediaType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsOriginalsNetwork.put("enabled", TableInfo.Column("enabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsOriginalsNetwork.put("sortOrder", TableInfo.Column("sortOrder", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysOriginalsNetwork: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesOriginalsNetwork: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoOriginalsNetwork: TableInfo = TableInfo("originals_network", _columnsOriginalsNetwork, _foreignKeysOriginalsNetwork, _indicesOriginalsNetwork)
        val _existingOriginalsNetwork: TableInfo = read(connection, "originals_network")
        if (!_infoOriginalsNetwork.equals(_existingOriginalsNetwork)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |originals_network(com.nexient.jazzy.core.database.app.OriginalsNetworkEntity).
              | Expected:
              |""".trimMargin() + _infoOriginalsNetwork + """
              |
              | Found:
              |""".trimMargin() + _existingOriginalsNetwork)
        }
        val _columnsPublicTracker: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsPublicTracker.put("url", TableInfo.Column("url", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPublicTracker.put("enabled", TableInfo.Column("enabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysPublicTracker: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesPublicTracker: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoPublicTracker: TableInfo = TableInfo("public_tracker", _columnsPublicTracker, _foreignKeysPublicTracker, _indicesPublicTracker)
        val _existingPublicTracker: TableInfo = read(connection, "public_tracker")
        if (!_infoPublicTracker.equals(_existingPublicTracker)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |public_tracker(com.nexient.jazzy.core.database.app.PublicTrackerEntity).
              | Expected:
              |""".trimMargin() + _infoPublicTracker + """
              |
              | Found:
              |""".trimMargin() + _existingPublicTracker)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "profile", "app_setting", "profile_setting", "title_snapshot", "my_list_item", "liked_title", "watch_progress", "play_session", "search_history", "seed_genre", "genre_affinity", "provider_setting", "source_flag", "originals_network", "public_tracker")
  }

  public override fun clearAllTables() {
    super.performClear(true, "profile", "app_setting", "profile_setting", "title_snapshot", "my_list_item", "liked_title", "watch_progress", "play_session", "search_history", "seed_genre", "genre_affinity", "provider_setting", "source_flag", "originals_network", "public_tracker")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(ProfileDao::class, ProfileDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(SettingsDao::class, SettingsDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(TitleSnapshotDao::class, TitleSnapshotDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(LibraryDao::class, LibraryDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(ProgressDao::class, ProgressDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(PlaySessionDao::class, PlaySessionDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(SearchHistoryDao::class, SearchHistoryDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(RecommendationStateDao::class, RecommendationStateDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(ProviderDao::class, ProviderDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>): List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun profileDao(): ProfileDao = _profileDao.value

  public override fun settingsDao(): SettingsDao = _settingsDao.value

  public override fun titleSnapshotDao(): TitleSnapshotDao = _titleSnapshotDao.value

  public override fun libraryDao(): LibraryDao = _libraryDao.value

  public override fun progressDao(): ProgressDao = _progressDao.value

  public override fun playSessionDao(): PlaySessionDao = _playSessionDao.value

  public override fun searchHistoryDao(): SearchHistoryDao = _searchHistoryDao.value

  public override fun recommendationStateDao(): RecommendationStateDao = _recommendationStateDao.value

  public override fun providerDao(): ProviderDao = _providerDao.value
}
