package com.nexient.jazzy.core.database.cache

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.EntityUpsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.getTotalChangedRows
import androidx.room.util.performInTransactionSuspending
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class TmdbCacheDao_Impl(
  __db: RoomDatabase,
) : TmdbCacheDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfGenreEntity: EntityInsertAdapter<GenreEntity>

  private val __upsertAdapterOfTmdbConfigEntity: EntityUpsertAdapter<TmdbConfigEntity>

  private val __upsertAdapterOfTmdbListPageEntity: EntityUpsertAdapter<TmdbListPageEntity>

  private val __upsertAdapterOfTmdbTitleEntity: EntityUpsertAdapter<TmdbTitleEntity>

  private val __upsertAdapterOfTmdbSeasonEntity: EntityUpsertAdapter<TmdbSeasonEntity>

  private val __upsertAdapterOfTmdbSearchEntity: EntityUpsertAdapter<TmdbSearchEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfGenreEntity = object : EntityInsertAdapter<GenreEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `genre` (`id`,`mediaType`,`language`,`name`,`fetchedAt`) VALUES (?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: GenreEntity) {
        statement.bindLong(1, entity.id.toLong())
        statement.bindText(2, entity.mediaType)
        statement.bindText(3, entity.language)
        statement.bindText(4, entity.name)
        statement.bindLong(5, entity.fetchedAt)
      }
    }
    this.__upsertAdapterOfTmdbConfigEntity = EntityUpsertAdapter<TmdbConfigEntity>(object : EntityInsertAdapter<TmdbConfigEntity>() {
      protected override fun createQuery(): String = "INSERT INTO `tmdb_config` (`key`,`json`,`fetchedAt`) VALUES (?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: TmdbConfigEntity) {
        statement.bindText(1, entity.key)
        statement.bindText(2, entity.json)
        statement.bindLong(3, entity.fetchedAt)
      }
    }, object : EntityDeleteOrUpdateAdapter<TmdbConfigEntity>() {
      protected override fun createQuery(): String = "UPDATE `tmdb_config` SET `key` = ?,`json` = ?,`fetchedAt` = ? WHERE `key` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: TmdbConfigEntity) {
        statement.bindText(1, entity.key)
        statement.bindText(2, entity.json)
        statement.bindLong(3, entity.fetchedAt)
        statement.bindText(4, entity.key)
      }
    })
    this.__upsertAdapterOfTmdbListPageEntity = EntityUpsertAdapter<TmdbListPageEntity>(object : EntityInsertAdapter<TmdbListPageEntity>() {
      protected override fun createQuery(): String = "INSERT INTO `tmdb_list_page` (`listKey`,`page`,`language`,`json`,`totalPages`,`fetchedAt`,`lastAccessedAt`) VALUES (?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: TmdbListPageEntity) {
        statement.bindText(1, entity.listKey)
        statement.bindLong(2, entity.page.toLong())
        statement.bindText(3, entity.language)
        statement.bindText(4, entity.json)
        statement.bindLong(5, entity.totalPages.toLong())
        statement.bindLong(6, entity.fetchedAt)
        statement.bindLong(7, entity.lastAccessedAt)
      }
    }, object : EntityDeleteOrUpdateAdapter<TmdbListPageEntity>() {
      protected override fun createQuery(): String = "UPDATE `tmdb_list_page` SET `listKey` = ?,`page` = ?,`language` = ?,`json` = ?,`totalPages` = ?,`fetchedAt` = ?,`lastAccessedAt` = ? WHERE `listKey` = ? AND `page` = ? AND `language` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: TmdbListPageEntity) {
        statement.bindText(1, entity.listKey)
        statement.bindLong(2, entity.page.toLong())
        statement.bindText(3, entity.language)
        statement.bindText(4, entity.json)
        statement.bindLong(5, entity.totalPages.toLong())
        statement.bindLong(6, entity.fetchedAt)
        statement.bindLong(7, entity.lastAccessedAt)
        statement.bindText(8, entity.listKey)
        statement.bindLong(9, entity.page.toLong())
        statement.bindText(10, entity.language)
      }
    })
    this.__upsertAdapterOfTmdbTitleEntity = EntityUpsertAdapter<TmdbTitleEntity>(object : EntityInsertAdapter<TmdbTitleEntity>() {
      protected override fun createQuery(): String = "INSERT INTO `tmdb_title` (`tmdbId`,`mediaType`,`language`,`json`,`fetchedAt`,`lastAccessedAt`) VALUES (?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: TmdbTitleEntity) {
        statement.bindLong(1, entity.tmdbId.toLong())
        statement.bindText(2, entity.mediaType)
        statement.bindText(3, entity.language)
        statement.bindText(4, entity.json)
        statement.bindLong(5, entity.fetchedAt)
        statement.bindLong(6, entity.lastAccessedAt)
      }
    }, object : EntityDeleteOrUpdateAdapter<TmdbTitleEntity>() {
      protected override fun createQuery(): String = "UPDATE `tmdb_title` SET `tmdbId` = ?,`mediaType` = ?,`language` = ?,`json` = ?,`fetchedAt` = ?,`lastAccessedAt` = ? WHERE `tmdbId` = ? AND `mediaType` = ? AND `language` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: TmdbTitleEntity) {
        statement.bindLong(1, entity.tmdbId.toLong())
        statement.bindText(2, entity.mediaType)
        statement.bindText(3, entity.language)
        statement.bindText(4, entity.json)
        statement.bindLong(5, entity.fetchedAt)
        statement.bindLong(6, entity.lastAccessedAt)
        statement.bindLong(7, entity.tmdbId.toLong())
        statement.bindText(8, entity.mediaType)
        statement.bindText(9, entity.language)
      }
    })
    this.__upsertAdapterOfTmdbSeasonEntity = EntityUpsertAdapter<TmdbSeasonEntity>(object : EntityInsertAdapter<TmdbSeasonEntity>() {
      protected override fun createQuery(): String = "INSERT INTO `tmdb_season` (`tmdbId`,`season`,`language`,`json`,`fetchedAt`,`lastAccessedAt`) VALUES (?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: TmdbSeasonEntity) {
        statement.bindLong(1, entity.tmdbId.toLong())
        statement.bindLong(2, entity.season.toLong())
        statement.bindText(3, entity.language)
        statement.bindText(4, entity.json)
        statement.bindLong(5, entity.fetchedAt)
        statement.bindLong(6, entity.lastAccessedAt)
      }
    }, object : EntityDeleteOrUpdateAdapter<TmdbSeasonEntity>() {
      protected override fun createQuery(): String = "UPDATE `tmdb_season` SET `tmdbId` = ?,`season` = ?,`language` = ?,`json` = ?,`fetchedAt` = ?,`lastAccessedAt` = ? WHERE `tmdbId` = ? AND `season` = ? AND `language` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: TmdbSeasonEntity) {
        statement.bindLong(1, entity.tmdbId.toLong())
        statement.bindLong(2, entity.season.toLong())
        statement.bindText(3, entity.language)
        statement.bindText(4, entity.json)
        statement.bindLong(5, entity.fetchedAt)
        statement.bindLong(6, entity.lastAccessedAt)
        statement.bindLong(7, entity.tmdbId.toLong())
        statement.bindLong(8, entity.season.toLong())
        statement.bindText(9, entity.language)
      }
    })
    this.__upsertAdapterOfTmdbSearchEntity = EntityUpsertAdapter<TmdbSearchEntity>(object : EntityInsertAdapter<TmdbSearchEntity>() {
      protected override fun createQuery(): String = "INSERT INTO `tmdb_search` (`queryKey`,`page`,`json`,`totalPages`,`fetchedAt`) VALUES (?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: TmdbSearchEntity) {
        statement.bindText(1, entity.queryKey)
        statement.bindLong(2, entity.page.toLong())
        statement.bindText(3, entity.json)
        statement.bindLong(4, entity.totalPages.toLong())
        statement.bindLong(5, entity.fetchedAt)
      }
    }, object : EntityDeleteOrUpdateAdapter<TmdbSearchEntity>() {
      protected override fun createQuery(): String = "UPDATE `tmdb_search` SET `queryKey` = ?,`page` = ?,`json` = ?,`totalPages` = ?,`fetchedAt` = ? WHERE `queryKey` = ? AND `page` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: TmdbSearchEntity) {
        statement.bindText(1, entity.queryKey)
        statement.bindLong(2, entity.page.toLong())
        statement.bindText(3, entity.json)
        statement.bindLong(4, entity.totalPages.toLong())
        statement.bindLong(5, entity.fetchedAt)
        statement.bindText(6, entity.queryKey)
        statement.bindLong(7, entity.page.toLong())
      }
    })
  }

  public override suspend fun insertGenres(rows: List<GenreEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfGenreEntity.insert(_connection, rows)
  }

  public override suspend fun replaceGenres(
    mediaType: String,
    language: String,
    rows: List<GenreEntity>,
  ): Unit = performInTransactionSuspending(__db) {
    super@TmdbCacheDao_Impl.replaceGenres(mediaType, language, rows)
  }

  public override suspend fun putConfig(entity: TmdbConfigEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __upsertAdapterOfTmdbConfigEntity.upsert(_connection, entity)
  }

  public override suspend fun putListPage(entity: TmdbListPageEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __upsertAdapterOfTmdbListPageEntity.upsert(_connection, entity)
  }

  public override suspend fun putTitle(entity: TmdbTitleEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __upsertAdapterOfTmdbTitleEntity.upsert(_connection, entity)
  }

  public override suspend fun putSeason(entity: TmdbSeasonEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __upsertAdapterOfTmdbSeasonEntity.upsert(_connection, entity)
  }

  public override suspend fun putSearch(entity: TmdbSearchEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __upsertAdapterOfTmdbSearchEntity.upsert(_connection, entity)
  }

  public override suspend fun config(key: String): TmdbConfigEntity? {
    val _sql: String = "SELECT * FROM tmdb_config WHERE `key` = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, key)
        val _columnIndexOfKey: Int = getColumnIndexOrThrow(_stmt, "key")
        val _columnIndexOfJson: Int = getColumnIndexOrThrow(_stmt, "json")
        val _columnIndexOfFetchedAt: Int = getColumnIndexOrThrow(_stmt, "fetchedAt")
        val _result: TmdbConfigEntity?
        if (_stmt.step()) {
          val _tmpKey: String
          _tmpKey = _stmt.getText(_columnIndexOfKey)
          val _tmpJson: String
          _tmpJson = _stmt.getText(_columnIndexOfJson)
          val _tmpFetchedAt: Long
          _tmpFetchedAt = _stmt.getLong(_columnIndexOfFetchedAt)
          _result = TmdbConfigEntity(_tmpKey,_tmpJson,_tmpFetchedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun genres(mediaType: String, language: String): List<GenreEntity> {
    val _sql: String = "SELECT * FROM genre WHERE mediaType = ? AND language = ? ORDER BY name"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, mediaType)
        _argIndex = 2
        _stmt.bindText(_argIndex, language)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfMediaType: Int = getColumnIndexOrThrow(_stmt, "mediaType")
        val _columnIndexOfLanguage: Int = getColumnIndexOrThrow(_stmt, "language")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfFetchedAt: Int = getColumnIndexOrThrow(_stmt, "fetchedAt")
        val _result: MutableList<GenreEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: GenreEntity
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpMediaType: String
          _tmpMediaType = _stmt.getText(_columnIndexOfMediaType)
          val _tmpLanguage: String
          _tmpLanguage = _stmt.getText(_columnIndexOfLanguage)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpFetchedAt: Long
          _tmpFetchedAt = _stmt.getLong(_columnIndexOfFetchedAt)
          _item = GenreEntity(_tmpId,_tmpMediaType,_tmpLanguage,_tmpName,_tmpFetchedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeGenres(language: String): Flow<List<GenreEntity>> {
    val _sql: String = "SELECT * FROM genre WHERE language = ?"
    return createFlow(__db, false, arrayOf("genre")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, language)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfMediaType: Int = getColumnIndexOrThrow(_stmt, "mediaType")
        val _columnIndexOfLanguage: Int = getColumnIndexOrThrow(_stmt, "language")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfFetchedAt: Int = getColumnIndexOrThrow(_stmt, "fetchedAt")
        val _result: MutableList<GenreEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: GenreEntity
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpMediaType: String
          _tmpMediaType = _stmt.getText(_columnIndexOfMediaType)
          val _tmpLanguage: String
          _tmpLanguage = _stmt.getText(_columnIndexOfLanguage)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpFetchedAt: Long
          _tmpFetchedAt = _stmt.getLong(_columnIndexOfFetchedAt)
          _item = GenreEntity(_tmpId,_tmpMediaType,_tmpLanguage,_tmpName,_tmpFetchedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun listPage(
    listKey: String,
    page: Int,
    language: String,
  ): TmdbListPageEntity? {
    val _sql: String = "SELECT * FROM tmdb_list_page WHERE listKey = ? AND page = ? AND language = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, listKey)
        _argIndex = 2
        _stmt.bindLong(_argIndex, page.toLong())
        _argIndex = 3
        _stmt.bindText(_argIndex, language)
        val _columnIndexOfListKey: Int = getColumnIndexOrThrow(_stmt, "listKey")
        val _columnIndexOfPage: Int = getColumnIndexOrThrow(_stmt, "page")
        val _columnIndexOfLanguage: Int = getColumnIndexOrThrow(_stmt, "language")
        val _columnIndexOfJson: Int = getColumnIndexOrThrow(_stmt, "json")
        val _columnIndexOfTotalPages: Int = getColumnIndexOrThrow(_stmt, "totalPages")
        val _columnIndexOfFetchedAt: Int = getColumnIndexOrThrow(_stmt, "fetchedAt")
        val _columnIndexOfLastAccessedAt: Int = getColumnIndexOrThrow(_stmt, "lastAccessedAt")
        val _result: TmdbListPageEntity?
        if (_stmt.step()) {
          val _tmpListKey: String
          _tmpListKey = _stmt.getText(_columnIndexOfListKey)
          val _tmpPage: Int
          _tmpPage = _stmt.getLong(_columnIndexOfPage).toInt()
          val _tmpLanguage: String
          _tmpLanguage = _stmt.getText(_columnIndexOfLanguage)
          val _tmpJson: String
          _tmpJson = _stmt.getText(_columnIndexOfJson)
          val _tmpTotalPages: Int
          _tmpTotalPages = _stmt.getLong(_columnIndexOfTotalPages).toInt()
          val _tmpFetchedAt: Long
          _tmpFetchedAt = _stmt.getLong(_columnIndexOfFetchedAt)
          val _tmpLastAccessedAt: Long
          _tmpLastAccessedAt = _stmt.getLong(_columnIndexOfLastAccessedAt)
          _result = TmdbListPageEntity(_tmpListKey,_tmpPage,_tmpLanguage,_tmpJson,_tmpTotalPages,_tmpFetchedAt,_tmpLastAccessedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun title(
    tmdbId: Int,
    mediaType: String,
    language: String,
  ): TmdbTitleEntity? {
    val _sql: String = "SELECT * FROM tmdb_title WHERE tmdbId = ? AND mediaType = ? AND language = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, tmdbId.toLong())
        _argIndex = 2
        _stmt.bindText(_argIndex, mediaType)
        _argIndex = 3
        _stmt.bindText(_argIndex, language)
        val _columnIndexOfTmdbId: Int = getColumnIndexOrThrow(_stmt, "tmdbId")
        val _columnIndexOfMediaType: Int = getColumnIndexOrThrow(_stmt, "mediaType")
        val _columnIndexOfLanguage: Int = getColumnIndexOrThrow(_stmt, "language")
        val _columnIndexOfJson: Int = getColumnIndexOrThrow(_stmt, "json")
        val _columnIndexOfFetchedAt: Int = getColumnIndexOrThrow(_stmt, "fetchedAt")
        val _columnIndexOfLastAccessedAt: Int = getColumnIndexOrThrow(_stmt, "lastAccessedAt")
        val _result: TmdbTitleEntity?
        if (_stmt.step()) {
          val _tmpTmdbId: Int
          _tmpTmdbId = _stmt.getLong(_columnIndexOfTmdbId).toInt()
          val _tmpMediaType: String
          _tmpMediaType = _stmt.getText(_columnIndexOfMediaType)
          val _tmpLanguage: String
          _tmpLanguage = _stmt.getText(_columnIndexOfLanguage)
          val _tmpJson: String
          _tmpJson = _stmt.getText(_columnIndexOfJson)
          val _tmpFetchedAt: Long
          _tmpFetchedAt = _stmt.getLong(_columnIndexOfFetchedAt)
          val _tmpLastAccessedAt: Long
          _tmpLastAccessedAt = _stmt.getLong(_columnIndexOfLastAccessedAt)
          _result = TmdbTitleEntity(_tmpTmdbId,_tmpMediaType,_tmpLanguage,_tmpJson,_tmpFetchedAt,_tmpLastAccessedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun season(
    tmdbId: Int,
    season: Int,
    language: String,
  ): TmdbSeasonEntity? {
    val _sql: String = "SELECT * FROM tmdb_season WHERE tmdbId = ? AND season = ? AND language = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, tmdbId.toLong())
        _argIndex = 2
        _stmt.bindLong(_argIndex, season.toLong())
        _argIndex = 3
        _stmt.bindText(_argIndex, language)
        val _columnIndexOfTmdbId: Int = getColumnIndexOrThrow(_stmt, "tmdbId")
        val _columnIndexOfSeason: Int = getColumnIndexOrThrow(_stmt, "season")
        val _columnIndexOfLanguage: Int = getColumnIndexOrThrow(_stmt, "language")
        val _columnIndexOfJson: Int = getColumnIndexOrThrow(_stmt, "json")
        val _columnIndexOfFetchedAt: Int = getColumnIndexOrThrow(_stmt, "fetchedAt")
        val _columnIndexOfLastAccessedAt: Int = getColumnIndexOrThrow(_stmt, "lastAccessedAt")
        val _result: TmdbSeasonEntity?
        if (_stmt.step()) {
          val _tmpTmdbId: Int
          _tmpTmdbId = _stmt.getLong(_columnIndexOfTmdbId).toInt()
          val _tmpSeason: Int
          _tmpSeason = _stmt.getLong(_columnIndexOfSeason).toInt()
          val _tmpLanguage: String
          _tmpLanguage = _stmt.getText(_columnIndexOfLanguage)
          val _tmpJson: String
          _tmpJson = _stmt.getText(_columnIndexOfJson)
          val _tmpFetchedAt: Long
          _tmpFetchedAt = _stmt.getLong(_columnIndexOfFetchedAt)
          val _tmpLastAccessedAt: Long
          _tmpLastAccessedAt = _stmt.getLong(_columnIndexOfLastAccessedAt)
          _result = TmdbSeasonEntity(_tmpTmdbId,_tmpSeason,_tmpLanguage,_tmpJson,_tmpFetchedAt,_tmpLastAccessedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun search(queryKey: String, page: Int): TmdbSearchEntity? {
    val _sql: String = "SELECT * FROM tmdb_search WHERE queryKey = ? AND page = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, queryKey)
        _argIndex = 2
        _stmt.bindLong(_argIndex, page.toLong())
        val _columnIndexOfQueryKey: Int = getColumnIndexOrThrow(_stmt, "queryKey")
        val _columnIndexOfPage: Int = getColumnIndexOrThrow(_stmt, "page")
        val _columnIndexOfJson: Int = getColumnIndexOrThrow(_stmt, "json")
        val _columnIndexOfTotalPages: Int = getColumnIndexOrThrow(_stmt, "totalPages")
        val _columnIndexOfFetchedAt: Int = getColumnIndexOrThrow(_stmt, "fetchedAt")
        val _result: TmdbSearchEntity?
        if (_stmt.step()) {
          val _tmpQueryKey: String
          _tmpQueryKey = _stmt.getText(_columnIndexOfQueryKey)
          val _tmpPage: Int
          _tmpPage = _stmt.getLong(_columnIndexOfPage).toInt()
          val _tmpJson: String
          _tmpJson = _stmt.getText(_columnIndexOfJson)
          val _tmpTotalPages: Int
          _tmpTotalPages = _stmt.getLong(_columnIndexOfTotalPages).toInt()
          val _tmpFetchedAt: Long
          _tmpFetchedAt = _stmt.getLong(_columnIndexOfFetchedAt)
          _result = TmdbSearchEntity(_tmpQueryKey,_tmpPage,_tmpJson,_tmpTotalPages,_tmpFetchedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun approximateBytes(): Long {
    val _sql: String = """
        |SELECT COALESCE(SUM(LENGTH(json)), 0) FROM (
        |            SELECT json FROM tmdb_list_page UNION ALL SELECT json FROM tmdb_title UNION ALL
        |            SELECT json FROM tmdb_season UNION ALL SELECT json FROM tmdb_search UNION ALL SELECT json FROM source_result)
        """.trimMargin()
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Long
        if (_stmt.step()) {
          val _tmp: Long
          _tmp = _stmt.getLong(0)
          _result = _tmp
        } else {
          _result = 0L
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun oldestAccess(): Long? {
    val _sql: String = "SELECT MIN(lastAccessedAt) FROM (SELECT lastAccessedAt FROM tmdb_list_page UNION ALL SELECT lastAccessedAt FROM tmdb_title UNION ALL SELECT lastAccessedAt FROM tmdb_season)"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Long?
        if (_stmt.step()) {
          val _tmp: Long?
          if (_stmt.isNull(0)) {
            _tmp = null
          } else {
            _tmp = _stmt.getLong(0)
          }
          _result = _tmp
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteGenres(mediaType: String, language: String) {
    val _sql: String = "DELETE FROM genre WHERE mediaType = ? AND language = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, mediaType)
        _argIndex = 2
        _stmt.bindText(_argIndex, language)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun touchListPage(
    listKey: String,
    page: Int,
    language: String,
    now: Long,
  ) {
    val _sql: String = "UPDATE tmdb_list_page SET lastAccessedAt = ? WHERE listKey = ? AND page = ? AND language = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, now)
        _argIndex = 2
        _stmt.bindText(_argIndex, listKey)
        _argIndex = 3
        _stmt.bindLong(_argIndex, page.toLong())
        _argIndex = 4
        _stmt.bindText(_argIndex, language)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun touchTitle(
    tmdbId: Int,
    mediaType: String,
    language: String,
    now: Long,
  ) {
    val _sql: String = "UPDATE tmdb_title SET lastAccessedAt = ? WHERE tmdbId = ? AND mediaType = ? AND language = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, now)
        _argIndex = 2
        _stmt.bindLong(_argIndex, tmdbId.toLong())
        _argIndex = 3
        _stmt.bindText(_argIndex, mediaType)
        _argIndex = 4
        _stmt.bindText(_argIndex, language)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun evictListPages(before: Long): Int {
    val _sql: String = "DELETE FROM tmdb_list_page WHERE lastAccessedAt < ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, before)
        _stmt.step()
        getTotalChangedRows(_connection)
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun evictTitles(before: Long): Int {
    val _sql: String = "DELETE FROM tmdb_title WHERE lastAccessedAt < ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, before)
        _stmt.step()
        getTotalChangedRows(_connection)
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun evictSeasons(before: Long): Int {
    val _sql: String = "DELETE FROM tmdb_season WHERE lastAccessedAt < ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, before)
        _stmt.step()
        getTotalChangedRows(_connection)
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun evictSearches(before: Long): Int {
    val _sql: String = "DELETE FROM tmdb_search WHERE fetchedAt < ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, before)
        _stmt.step()
        getTotalChangedRows(_connection)
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
