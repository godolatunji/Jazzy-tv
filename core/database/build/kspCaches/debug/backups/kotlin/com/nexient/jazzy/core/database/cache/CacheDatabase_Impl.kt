package com.nexient.jazzy.core.database.cache

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
public class CacheDatabase_Impl : CacheDatabase() {
  private val _tmdbCacheDao: Lazy<TmdbCacheDao> = lazy {
    TmdbCacheDao_Impl(this)
  }

  private val _sourceCacheDao: Lazy<SourceCacheDao> = lazy {
    SourceCacheDao_Impl(this)
  }

  private val _recommendationCacheDao: Lazy<RecommendationCacheDao> = lazy {
    RecommendationCacheDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(1, "efac3a0dc7da352743008d2aa446d8b7", "07a274a8b46931005d66d3c5c92aa6bc") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `tmdb_config` (`key` TEXT NOT NULL, `json` TEXT NOT NULL, `fetchedAt` INTEGER NOT NULL, PRIMARY KEY(`key`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `genre` (`id` INTEGER NOT NULL, `mediaType` TEXT NOT NULL, `language` TEXT NOT NULL, `name` TEXT NOT NULL, `fetchedAt` INTEGER NOT NULL, PRIMARY KEY(`id`, `mediaType`, `language`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `tmdb_list_page` (`listKey` TEXT NOT NULL, `page` INTEGER NOT NULL, `language` TEXT NOT NULL, `json` TEXT NOT NULL, `totalPages` INTEGER NOT NULL, `fetchedAt` INTEGER NOT NULL, `lastAccessedAt` INTEGER NOT NULL, PRIMARY KEY(`listKey`, `page`, `language`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `tmdb_title` (`tmdbId` INTEGER NOT NULL, `mediaType` TEXT NOT NULL, `language` TEXT NOT NULL, `json` TEXT NOT NULL, `fetchedAt` INTEGER NOT NULL, `lastAccessedAt` INTEGER NOT NULL, PRIMARY KEY(`tmdbId`, `mediaType`, `language`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `tmdb_season` (`tmdbId` INTEGER NOT NULL, `season` INTEGER NOT NULL, `language` TEXT NOT NULL, `json` TEXT NOT NULL, `fetchedAt` INTEGER NOT NULL, `lastAccessedAt` INTEGER NOT NULL, PRIMARY KEY(`tmdbId`, `season`, `language`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `tmdb_search` (`queryKey` TEXT NOT NULL, `page` INTEGER NOT NULL, `json` TEXT NOT NULL, `totalPages` INTEGER NOT NULL, `fetchedAt` INTEGER NOT NULL, PRIMARY KEY(`queryKey`, `page`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `source_result` (`queryKey` TEXT NOT NULL, `json` TEXT NOT NULL, `fetchedAt` INTEGER NOT NULL, PRIMARY KEY(`queryKey`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `recommendation_cache` (`profileId` INTEGER NOT NULL, `railKey` TEXT NOT NULL, `title` TEXT NOT NULL, `json` TEXT NOT NULL, `generatedAt` INTEGER NOT NULL, PRIMARY KEY(`profileId`, `railKey`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'efac3a0dc7da352743008d2aa446d8b7')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `tmdb_config`")
        connection.execSQL("DROP TABLE IF EXISTS `genre`")
        connection.execSQL("DROP TABLE IF EXISTS `tmdb_list_page`")
        connection.execSQL("DROP TABLE IF EXISTS `tmdb_title`")
        connection.execSQL("DROP TABLE IF EXISTS `tmdb_season`")
        connection.execSQL("DROP TABLE IF EXISTS `tmdb_search`")
        connection.execSQL("DROP TABLE IF EXISTS `source_result`")
        connection.execSQL("DROP TABLE IF EXISTS `recommendation_cache`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
        val _columnsTmdbConfig: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsTmdbConfig.put("key", TableInfo.Column("key", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTmdbConfig.put("json", TableInfo.Column("json", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTmdbConfig.put("fetchedAt", TableInfo.Column("fetchedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysTmdbConfig: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesTmdbConfig: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoTmdbConfig: TableInfo = TableInfo("tmdb_config", _columnsTmdbConfig, _foreignKeysTmdbConfig, _indicesTmdbConfig)
        val _existingTmdbConfig: TableInfo = read(connection, "tmdb_config")
        if (!_infoTmdbConfig.equals(_existingTmdbConfig)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |tmdb_config(com.nexient.jazzy.core.database.cache.TmdbConfigEntity).
              | Expected:
              |""".trimMargin() + _infoTmdbConfig + """
              |
              | Found:
              |""".trimMargin() + _existingTmdbConfig)
        }
        val _columnsGenre: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsGenre.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsGenre.put("mediaType", TableInfo.Column("mediaType", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsGenre.put("language", TableInfo.Column("language", "TEXT", true, 3, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsGenre.put("name", TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsGenre.put("fetchedAt", TableInfo.Column("fetchedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysGenre: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesGenre: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoGenre: TableInfo = TableInfo("genre", _columnsGenre, _foreignKeysGenre, _indicesGenre)
        val _existingGenre: TableInfo = read(connection, "genre")
        if (!_infoGenre.equals(_existingGenre)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |genre(com.nexient.jazzy.core.database.cache.GenreEntity).
              | Expected:
              |""".trimMargin() + _infoGenre + """
              |
              | Found:
              |""".trimMargin() + _existingGenre)
        }
        val _columnsTmdbListPage: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsTmdbListPage.put("listKey", TableInfo.Column("listKey", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTmdbListPage.put("page", TableInfo.Column("page", "INTEGER", true, 2, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTmdbListPage.put("language", TableInfo.Column("language", "TEXT", true, 3, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTmdbListPage.put("json", TableInfo.Column("json", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTmdbListPage.put("totalPages", TableInfo.Column("totalPages", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTmdbListPage.put("fetchedAt", TableInfo.Column("fetchedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTmdbListPage.put("lastAccessedAt", TableInfo.Column("lastAccessedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysTmdbListPage: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesTmdbListPage: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoTmdbListPage: TableInfo = TableInfo("tmdb_list_page", _columnsTmdbListPage, _foreignKeysTmdbListPage, _indicesTmdbListPage)
        val _existingTmdbListPage: TableInfo = read(connection, "tmdb_list_page")
        if (!_infoTmdbListPage.equals(_existingTmdbListPage)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |tmdb_list_page(com.nexient.jazzy.core.database.cache.TmdbListPageEntity).
              | Expected:
              |""".trimMargin() + _infoTmdbListPage + """
              |
              | Found:
              |""".trimMargin() + _existingTmdbListPage)
        }
        val _columnsTmdbTitle: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsTmdbTitle.put("tmdbId", TableInfo.Column("tmdbId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTmdbTitle.put("mediaType", TableInfo.Column("mediaType", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTmdbTitle.put("language", TableInfo.Column("language", "TEXT", true, 3, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTmdbTitle.put("json", TableInfo.Column("json", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTmdbTitle.put("fetchedAt", TableInfo.Column("fetchedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTmdbTitle.put("lastAccessedAt", TableInfo.Column("lastAccessedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysTmdbTitle: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesTmdbTitle: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoTmdbTitle: TableInfo = TableInfo("tmdb_title", _columnsTmdbTitle, _foreignKeysTmdbTitle, _indicesTmdbTitle)
        val _existingTmdbTitle: TableInfo = read(connection, "tmdb_title")
        if (!_infoTmdbTitle.equals(_existingTmdbTitle)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |tmdb_title(com.nexient.jazzy.core.database.cache.TmdbTitleEntity).
              | Expected:
              |""".trimMargin() + _infoTmdbTitle + """
              |
              | Found:
              |""".trimMargin() + _existingTmdbTitle)
        }
        val _columnsTmdbSeason: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsTmdbSeason.put("tmdbId", TableInfo.Column("tmdbId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTmdbSeason.put("season", TableInfo.Column("season", "INTEGER", true, 2, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTmdbSeason.put("language", TableInfo.Column("language", "TEXT", true, 3, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTmdbSeason.put("json", TableInfo.Column("json", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTmdbSeason.put("fetchedAt", TableInfo.Column("fetchedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTmdbSeason.put("lastAccessedAt", TableInfo.Column("lastAccessedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysTmdbSeason: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesTmdbSeason: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoTmdbSeason: TableInfo = TableInfo("tmdb_season", _columnsTmdbSeason, _foreignKeysTmdbSeason, _indicesTmdbSeason)
        val _existingTmdbSeason: TableInfo = read(connection, "tmdb_season")
        if (!_infoTmdbSeason.equals(_existingTmdbSeason)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |tmdb_season(com.nexient.jazzy.core.database.cache.TmdbSeasonEntity).
              | Expected:
              |""".trimMargin() + _infoTmdbSeason + """
              |
              | Found:
              |""".trimMargin() + _existingTmdbSeason)
        }
        val _columnsTmdbSearch: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsTmdbSearch.put("queryKey", TableInfo.Column("queryKey", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTmdbSearch.put("page", TableInfo.Column("page", "INTEGER", true, 2, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTmdbSearch.put("json", TableInfo.Column("json", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTmdbSearch.put("totalPages", TableInfo.Column("totalPages", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTmdbSearch.put("fetchedAt", TableInfo.Column("fetchedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysTmdbSearch: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesTmdbSearch: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoTmdbSearch: TableInfo = TableInfo("tmdb_search", _columnsTmdbSearch, _foreignKeysTmdbSearch, _indicesTmdbSearch)
        val _existingTmdbSearch: TableInfo = read(connection, "tmdb_search")
        if (!_infoTmdbSearch.equals(_existingTmdbSearch)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |tmdb_search(com.nexient.jazzy.core.database.cache.TmdbSearchEntity).
              | Expected:
              |""".trimMargin() + _infoTmdbSearch + """
              |
              | Found:
              |""".trimMargin() + _existingTmdbSearch)
        }
        val _columnsSourceResult: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsSourceResult.put("queryKey", TableInfo.Column("queryKey", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSourceResult.put("json", TableInfo.Column("json", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSourceResult.put("fetchedAt", TableInfo.Column("fetchedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysSourceResult: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesSourceResult: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoSourceResult: TableInfo = TableInfo("source_result", _columnsSourceResult, _foreignKeysSourceResult, _indicesSourceResult)
        val _existingSourceResult: TableInfo = read(connection, "source_result")
        if (!_infoSourceResult.equals(_existingSourceResult)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |source_result(com.nexient.jazzy.core.database.cache.SourceResultEntity).
              | Expected:
              |""".trimMargin() + _infoSourceResult + """
              |
              | Found:
              |""".trimMargin() + _existingSourceResult)
        }
        val _columnsRecommendationCache: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsRecommendationCache.put("profileId", TableInfo.Column("profileId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRecommendationCache.put("railKey", TableInfo.Column("railKey", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRecommendationCache.put("title", TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRecommendationCache.put("json", TableInfo.Column("json", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRecommendationCache.put("generatedAt", TableInfo.Column("generatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysRecommendationCache: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesRecommendationCache: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoRecommendationCache: TableInfo = TableInfo("recommendation_cache", _columnsRecommendationCache, _foreignKeysRecommendationCache, _indicesRecommendationCache)
        val _existingRecommendationCache: TableInfo = read(connection, "recommendation_cache")
        if (!_infoRecommendationCache.equals(_existingRecommendationCache)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |recommendation_cache(com.nexient.jazzy.core.database.cache.RecommendationCacheEntity).
              | Expected:
              |""".trimMargin() + _infoRecommendationCache + """
              |
              | Found:
              |""".trimMargin() + _existingRecommendationCache)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "tmdb_config", "genre", "tmdb_list_page", "tmdb_title", "tmdb_season", "tmdb_search", "source_result", "recommendation_cache")
  }

  public override fun clearAllTables() {
    super.performClear(false, "tmdb_config", "genre", "tmdb_list_page", "tmdb_title", "tmdb_season", "tmdb_search", "source_result", "recommendation_cache")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(TmdbCacheDao::class, TmdbCacheDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(SourceCacheDao::class, SourceCacheDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(RecommendationCacheDao::class, RecommendationCacheDao_Impl.getRequiredConverters())
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

  public override fun tmdbCacheDao(): TmdbCacheDao = _tmdbCacheDao.value

  public override fun sourceCacheDao(): SourceCacheDao = _sourceCacheDao.value

  public override fun recommendationCacheDao(): RecommendationCacheDao = _recommendationCacheDao.value
}
