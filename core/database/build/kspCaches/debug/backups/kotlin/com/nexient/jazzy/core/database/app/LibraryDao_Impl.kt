package com.nexient.jazzy.core.database.app

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.EntityUpsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.nexient.jazzy.core.database.Converters
import javax.`annotation`.processing.Generated
import kotlin.Boolean
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
public class LibraryDao_Impl(
  __db: RoomDatabase,
) : LibraryDao {
  private val __db: RoomDatabase

  private val __upsertAdapterOfMyListItemEntity: EntityUpsertAdapter<MyListItemEntity>

  private val __upsertAdapterOfLikedTitleEntity: EntityUpsertAdapter<LikedTitleEntity>

  private val __converters: Converters = Converters()
  init {
    this.__db = __db
    this.__upsertAdapterOfMyListItemEntity = EntityUpsertAdapter<MyListItemEntity>(object : EntityInsertAdapter<MyListItemEntity>() {
      protected override fun createQuery(): String = "INSERT INTO `my_list_item` (`profileId`,`tmdbId`,`mediaType`,`addedAt`) VALUES (?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: MyListItemEntity) {
        statement.bindLong(1, entity.profileId)
        statement.bindLong(2, entity.tmdbId.toLong())
        statement.bindText(3, entity.mediaType)
        statement.bindLong(4, entity.addedAt)
      }
    }, object : EntityDeleteOrUpdateAdapter<MyListItemEntity>() {
      protected override fun createQuery(): String = "UPDATE `my_list_item` SET `profileId` = ?,`tmdbId` = ?,`mediaType` = ?,`addedAt` = ? WHERE `profileId` = ? AND `tmdbId` = ? AND `mediaType` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: MyListItemEntity) {
        statement.bindLong(1, entity.profileId)
        statement.bindLong(2, entity.tmdbId.toLong())
        statement.bindText(3, entity.mediaType)
        statement.bindLong(4, entity.addedAt)
        statement.bindLong(5, entity.profileId)
        statement.bindLong(6, entity.tmdbId.toLong())
        statement.bindText(7, entity.mediaType)
      }
    })
    this.__upsertAdapterOfLikedTitleEntity = EntityUpsertAdapter<LikedTitleEntity>(object : EntityInsertAdapter<LikedTitleEntity>() {
      protected override fun createQuery(): String = "INSERT INTO `liked_title` (`profileId`,`tmdbId`,`mediaType`,`likedAt`) VALUES (?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: LikedTitleEntity) {
        statement.bindLong(1, entity.profileId)
        statement.bindLong(2, entity.tmdbId.toLong())
        statement.bindText(3, entity.mediaType)
        statement.bindLong(4, entity.likedAt)
      }
    }, object : EntityDeleteOrUpdateAdapter<LikedTitleEntity>() {
      protected override fun createQuery(): String = "UPDATE `liked_title` SET `profileId` = ?,`tmdbId` = ?,`mediaType` = ?,`likedAt` = ? WHERE `profileId` = ? AND `tmdbId` = ? AND `mediaType` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: LikedTitleEntity) {
        statement.bindLong(1, entity.profileId)
        statement.bindLong(2, entity.tmdbId.toLong())
        statement.bindText(3, entity.mediaType)
        statement.bindLong(4, entity.likedAt)
        statement.bindLong(5, entity.profileId)
        statement.bindLong(6, entity.tmdbId.toLong())
        statement.bindText(7, entity.mediaType)
      }
    })
  }

  public override suspend fun addToMyList(item: MyListItemEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __upsertAdapterOfMyListItemEntity.upsert(_connection, item)
  }

  public override suspend fun like(item: LikedTitleEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __upsertAdapterOfLikedTitleEntity.upsert(_connection, item)
  }

  public override fun observeInMyList(
    profileId: Long,
    tmdbId: Int,
    mediaType: String,
  ): Flow<Boolean> {
    val _sql: String = "SELECT EXISTS(SELECT 1 FROM my_list_item WHERE profileId = ? AND tmdbId = ? AND mediaType = ?)"
    return createFlow(__db, false, arrayOf("my_list_item")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, profileId)
        _argIndex = 2
        _stmt.bindLong(_argIndex, tmdbId.toLong())
        _argIndex = 3
        _stmt.bindText(_argIndex, mediaType)
        val _result: Boolean
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp != 0
        } else {
          _result = false
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeMyList(profileId: Long): Flow<List<SnapshotWithTime>> {
    val _sql: String = """
        |SELECT s.tmdbId, s.mediaType, s.title, s.originalTitle, s.year, s.posterPath, s.backdropPath, s.genreIds, s.imdbId, m.addedAt
        |        FROM my_list_item m JOIN title_snapshot s ON s.tmdbId = m.tmdbId AND s.mediaType = m.mediaType
        |        WHERE m.profileId = ? ORDER BY m.addedAt DESC
        """.trimMargin()
    return createFlow(__db, false, arrayOf("my_list_item", "title_snapshot")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, profileId)
        val _columnIndexOfTmdbId: Int = 0
        val _columnIndexOfMediaType: Int = 1
        val _columnIndexOfTitle: Int = 2
        val _columnIndexOfOriginalTitle: Int = 3
        val _columnIndexOfYear: Int = 4
        val _columnIndexOfPosterPath: Int = 5
        val _columnIndexOfBackdropPath: Int = 6
        val _columnIndexOfGenreIds: Int = 7
        val _columnIndexOfImdbId: Int = 8
        val _columnIndexOfAddedAt: Int = 9
        val _result: MutableList<SnapshotWithTime> = mutableListOf()
        while (_stmt.step()) {
          val _item: SnapshotWithTime
          val _tmpTmdbId: Int
          _tmpTmdbId = _stmt.getLong(_columnIndexOfTmdbId).toInt()
          val _tmpMediaType: String
          _tmpMediaType = _stmt.getText(_columnIndexOfMediaType)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpOriginalTitle: String
          _tmpOriginalTitle = _stmt.getText(_columnIndexOfOriginalTitle)
          val _tmpYear: Int?
          if (_stmt.isNull(_columnIndexOfYear)) {
            _tmpYear = null
          } else {
            _tmpYear = _stmt.getLong(_columnIndexOfYear).toInt()
          }
          val _tmpPosterPath: String?
          if (_stmt.isNull(_columnIndexOfPosterPath)) {
            _tmpPosterPath = null
          } else {
            _tmpPosterPath = _stmt.getText(_columnIndexOfPosterPath)
          }
          val _tmpBackdropPath: String?
          if (_stmt.isNull(_columnIndexOfBackdropPath)) {
            _tmpBackdropPath = null
          } else {
            _tmpBackdropPath = _stmt.getText(_columnIndexOfBackdropPath)
          }
          val _tmpGenreIds: List<Int>
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfGenreIds)
          _tmpGenreIds = __converters.stringToIntList(_tmp)
          val _tmpImdbId: String?
          if (_stmt.isNull(_columnIndexOfImdbId)) {
            _tmpImdbId = null
          } else {
            _tmpImdbId = _stmt.getText(_columnIndexOfImdbId)
          }
          val _tmpAddedAt: Long
          _tmpAddedAt = _stmt.getLong(_columnIndexOfAddedAt)
          _item = SnapshotWithTime(_tmpTmdbId,_tmpMediaType,_tmpTitle,_tmpOriginalTitle,_tmpYear,_tmpPosterPath,_tmpBackdropPath,_tmpGenreIds,_tmpImdbId,_tmpAddedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeMyListKeys(profileId: Long): Flow<List<String>> {
    val _sql: String = "SELECT tmdbId || ':' || mediaType FROM my_list_item WHERE profileId = ?"
    return createFlow(__db, false, arrayOf("my_list_item")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, profileId)
        val _result: MutableList<String> = mutableListOf()
        while (_stmt.step()) {
          val _item: String
          val _tmp: String
          _tmp = _stmt.getText(0)
          _item = _tmp
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeLiked(
    profileId: Long,
    tmdbId: Int,
    mediaType: String,
  ): Flow<Boolean> {
    val _sql: String = "SELECT EXISTS(SELECT 1 FROM liked_title WHERE profileId = ? AND tmdbId = ? AND mediaType = ?)"
    return createFlow(__db, false, arrayOf("liked_title")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, profileId)
        _argIndex = 2
        _stmt.bindLong(_argIndex, tmdbId.toLong())
        _argIndex = 3
        _stmt.bindText(_argIndex, mediaType)
        val _result: Boolean
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp != 0
        } else {
          _result = false
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeLikedTitles(profileId: Long): Flow<List<SnapshotWithTime>> {
    val _sql: String = """
        |SELECT s.tmdbId, s.mediaType, s.title, s.originalTitle, s.year, s.posterPath, s.backdropPath, s.genreIds, s.imdbId, l.likedAt AS addedAt
        |        FROM liked_title l JOIN title_snapshot s ON s.tmdbId = l.tmdbId AND s.mediaType = l.mediaType
        |        WHERE l.profileId = ? ORDER BY l.likedAt DESC
        """.trimMargin()
    return createFlow(__db, false, arrayOf("liked_title", "title_snapshot")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, profileId)
        val _columnIndexOfTmdbId: Int = 0
        val _columnIndexOfMediaType: Int = 1
        val _columnIndexOfTitle: Int = 2
        val _columnIndexOfOriginalTitle: Int = 3
        val _columnIndexOfYear: Int = 4
        val _columnIndexOfPosterPath: Int = 5
        val _columnIndexOfBackdropPath: Int = 6
        val _columnIndexOfGenreIds: Int = 7
        val _columnIndexOfImdbId: Int = 8
        val _columnIndexOfAddedAt: Int = 9
        val _result: MutableList<SnapshotWithTime> = mutableListOf()
        while (_stmt.step()) {
          val _item: SnapshotWithTime
          val _tmpTmdbId: Int
          _tmpTmdbId = _stmt.getLong(_columnIndexOfTmdbId).toInt()
          val _tmpMediaType: String
          _tmpMediaType = _stmt.getText(_columnIndexOfMediaType)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpOriginalTitle: String
          _tmpOriginalTitle = _stmt.getText(_columnIndexOfOriginalTitle)
          val _tmpYear: Int?
          if (_stmt.isNull(_columnIndexOfYear)) {
            _tmpYear = null
          } else {
            _tmpYear = _stmt.getLong(_columnIndexOfYear).toInt()
          }
          val _tmpPosterPath: String?
          if (_stmt.isNull(_columnIndexOfPosterPath)) {
            _tmpPosterPath = null
          } else {
            _tmpPosterPath = _stmt.getText(_columnIndexOfPosterPath)
          }
          val _tmpBackdropPath: String?
          if (_stmt.isNull(_columnIndexOfBackdropPath)) {
            _tmpBackdropPath = null
          } else {
            _tmpBackdropPath = _stmt.getText(_columnIndexOfBackdropPath)
          }
          val _tmpGenreIds: List<Int>
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfGenreIds)
          _tmpGenreIds = __converters.stringToIntList(_tmp)
          val _tmpImdbId: String?
          if (_stmt.isNull(_columnIndexOfImdbId)) {
            _tmpImdbId = null
          } else {
            _tmpImdbId = _stmt.getText(_columnIndexOfImdbId)
          }
          val _tmpAddedAt: Long
          _tmpAddedAt = _stmt.getLong(_columnIndexOfAddedAt)
          _item = SnapshotWithTime(_tmpTmdbId,_tmpMediaType,_tmpTitle,_tmpOriginalTitle,_tmpYear,_tmpPosterPath,_tmpBackdropPath,_tmpGenreIds,_tmpImdbId,_tmpAddedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeLikedKeys(profileId: Long): Flow<List<String>> {
    val _sql: String = "SELECT tmdbId || ':' || mediaType FROM liked_title WHERE profileId = ?"
    return createFlow(__db, false, arrayOf("liked_title")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, profileId)
        val _result: MutableList<String> = mutableListOf()
        while (_stmt.step()) {
          val _item: String
          val _tmp: String
          _tmp = _stmt.getText(0)
          _item = _tmp
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun likedFor(profileId: Long): List<LikedTitleEntity> {
    val _sql: String = "SELECT * FROM liked_title WHERE profileId = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, profileId)
        val _columnIndexOfProfileId: Int = getColumnIndexOrThrow(_stmt, "profileId")
        val _columnIndexOfTmdbId: Int = getColumnIndexOrThrow(_stmt, "tmdbId")
        val _columnIndexOfMediaType: Int = getColumnIndexOrThrow(_stmt, "mediaType")
        val _columnIndexOfLikedAt: Int = getColumnIndexOrThrow(_stmt, "likedAt")
        val _result: MutableList<LikedTitleEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: LikedTitleEntity
          val _tmpProfileId: Long
          _tmpProfileId = _stmt.getLong(_columnIndexOfProfileId)
          val _tmpTmdbId: Int
          _tmpTmdbId = _stmt.getLong(_columnIndexOfTmdbId).toInt()
          val _tmpMediaType: String
          _tmpMediaType = _stmt.getText(_columnIndexOfMediaType)
          val _tmpLikedAt: Long
          _tmpLikedAt = _stmt.getLong(_columnIndexOfLikedAt)
          _item = LikedTitleEntity(_tmpProfileId,_tmpTmdbId,_tmpMediaType,_tmpLikedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun myListFor(profileId: Long): List<MyListItemEntity> {
    val _sql: String = "SELECT * FROM my_list_item WHERE profileId = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, profileId)
        val _columnIndexOfProfileId: Int = getColumnIndexOrThrow(_stmt, "profileId")
        val _columnIndexOfTmdbId: Int = getColumnIndexOrThrow(_stmt, "tmdbId")
        val _columnIndexOfMediaType: Int = getColumnIndexOrThrow(_stmt, "mediaType")
        val _columnIndexOfAddedAt: Int = getColumnIndexOrThrow(_stmt, "addedAt")
        val _result: MutableList<MyListItemEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: MyListItemEntity
          val _tmpProfileId: Long
          _tmpProfileId = _stmt.getLong(_columnIndexOfProfileId)
          val _tmpTmdbId: Int
          _tmpTmdbId = _stmt.getLong(_columnIndexOfTmdbId).toInt()
          val _tmpMediaType: String
          _tmpMediaType = _stmt.getText(_columnIndexOfMediaType)
          val _tmpAddedAt: Long
          _tmpAddedAt = _stmt.getLong(_columnIndexOfAddedAt)
          _item = MyListItemEntity(_tmpProfileId,_tmpTmdbId,_tmpMediaType,_tmpAddedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun removeFromMyList(
    profileId: Long,
    tmdbId: Int,
    mediaType: String,
  ) {
    val _sql: String = "DELETE FROM my_list_item WHERE profileId = ? AND tmdbId = ? AND mediaType = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, profileId)
        _argIndex = 2
        _stmt.bindLong(_argIndex, tmdbId.toLong())
        _argIndex = 3
        _stmt.bindText(_argIndex, mediaType)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun unlike(
    profileId: Long,
    tmdbId: Int,
    mediaType: String,
  ) {
    val _sql: String = "DELETE FROM liked_title WHERE profileId = ? AND tmdbId = ? AND mediaType = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, profileId)
        _argIndex = 2
        _stmt.bindLong(_argIndex, tmdbId.toLong())
        _argIndex = 3
        _stmt.bindText(_argIndex, mediaType)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
