package com.nexient.jazzy.core.database.app

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.EntityUpsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.getTotalChangedRows
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.nexient.jazzy.core.database.Converters
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class TitleSnapshotDao_Impl(
  __db: RoomDatabase,
) : TitleSnapshotDao {
  private val __db: RoomDatabase

  private val __upsertAdapterOfTitleSnapshotEntity: EntityUpsertAdapter<TitleSnapshotEntity>

  private val __converters: Converters = Converters()
  init {
    this.__db = __db
    this.__upsertAdapterOfTitleSnapshotEntity = EntityUpsertAdapter<TitleSnapshotEntity>(object : EntityInsertAdapter<TitleSnapshotEntity>() {
      protected override fun createQuery(): String = "INSERT INTO `title_snapshot` (`tmdbId`,`mediaType`,`title`,`originalTitle`,`year`,`posterPath`,`backdropPath`,`genreIds`,`imdbId`,`updatedAt`) VALUES (?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: TitleSnapshotEntity) {
        statement.bindLong(1, entity.tmdbId.toLong())
        statement.bindText(2, entity.mediaType)
        statement.bindText(3, entity.title)
        statement.bindText(4, entity.originalTitle)
        val _tmpYear: Int? = entity.year
        if (_tmpYear == null) {
          statement.bindNull(5)
        } else {
          statement.bindLong(5, _tmpYear.toLong())
        }
        val _tmpPosterPath: String? = entity.posterPath
        if (_tmpPosterPath == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpPosterPath)
        }
        val _tmpBackdropPath: String? = entity.backdropPath
        if (_tmpBackdropPath == null) {
          statement.bindNull(7)
        } else {
          statement.bindText(7, _tmpBackdropPath)
        }
        val _tmp: String = __converters.intListToString(entity.genreIds)
        statement.bindText(8, _tmp)
        val _tmpImdbId: String? = entity.imdbId
        if (_tmpImdbId == null) {
          statement.bindNull(9)
        } else {
          statement.bindText(9, _tmpImdbId)
        }
        statement.bindLong(10, entity.updatedAt)
      }
    }, object : EntityDeleteOrUpdateAdapter<TitleSnapshotEntity>() {
      protected override fun createQuery(): String = "UPDATE `title_snapshot` SET `tmdbId` = ?,`mediaType` = ?,`title` = ?,`originalTitle` = ?,`year` = ?,`posterPath` = ?,`backdropPath` = ?,`genreIds` = ?,`imdbId` = ?,`updatedAt` = ? WHERE `tmdbId` = ? AND `mediaType` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: TitleSnapshotEntity) {
        statement.bindLong(1, entity.tmdbId.toLong())
        statement.bindText(2, entity.mediaType)
        statement.bindText(3, entity.title)
        statement.bindText(4, entity.originalTitle)
        val _tmpYear: Int? = entity.year
        if (_tmpYear == null) {
          statement.bindNull(5)
        } else {
          statement.bindLong(5, _tmpYear.toLong())
        }
        val _tmpPosterPath: String? = entity.posterPath
        if (_tmpPosterPath == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpPosterPath)
        }
        val _tmpBackdropPath: String? = entity.backdropPath
        if (_tmpBackdropPath == null) {
          statement.bindNull(7)
        } else {
          statement.bindText(7, _tmpBackdropPath)
        }
        val _tmp: String = __converters.intListToString(entity.genreIds)
        statement.bindText(8, _tmp)
        val _tmpImdbId: String? = entity.imdbId
        if (_tmpImdbId == null) {
          statement.bindNull(9)
        } else {
          statement.bindText(9, _tmpImdbId)
        }
        statement.bindLong(10, entity.updatedAt)
        statement.bindLong(11, entity.tmdbId.toLong())
        statement.bindText(12, entity.mediaType)
      }
    })
  }

  public override suspend fun upsert(snapshot: TitleSnapshotEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __upsertAdapterOfTitleSnapshotEntity.upsert(_connection, snapshot)
  }

  public override suspend fun `get`(tmdbId: Int, mediaType: String): TitleSnapshotEntity? {
    val _sql: String = "SELECT * FROM title_snapshot WHERE tmdbId = ? AND mediaType = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, tmdbId.toLong())
        _argIndex = 2
        _stmt.bindText(_argIndex, mediaType)
        val _columnIndexOfTmdbId: Int = getColumnIndexOrThrow(_stmt, "tmdbId")
        val _columnIndexOfMediaType: Int = getColumnIndexOrThrow(_stmt, "mediaType")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfOriginalTitle: Int = getColumnIndexOrThrow(_stmt, "originalTitle")
        val _columnIndexOfYear: Int = getColumnIndexOrThrow(_stmt, "year")
        val _columnIndexOfPosterPath: Int = getColumnIndexOrThrow(_stmt, "posterPath")
        val _columnIndexOfBackdropPath: Int = getColumnIndexOrThrow(_stmt, "backdropPath")
        val _columnIndexOfGenreIds: Int = getColumnIndexOrThrow(_stmt, "genreIds")
        val _columnIndexOfImdbId: Int = getColumnIndexOrThrow(_stmt, "imdbId")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: TitleSnapshotEntity?
        if (_stmt.step()) {
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
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _result = TitleSnapshotEntity(_tmpTmdbId,_tmpMediaType,_tmpTitle,_tmpOriginalTitle,_tmpYear,_tmpPosterPath,_tmpBackdropPath,_tmpGenreIds,_tmpImdbId,_tmpUpdatedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteUnreferenced(): Int {
    val _sql: String = """
        |DELETE FROM title_snapshot WHERE NOT EXISTS (SELECT 1 FROM my_list_item m WHERE m.tmdbId = title_snapshot.tmdbId AND m.mediaType = title_snapshot.mediaType)
        |        AND NOT EXISTS (SELECT 1 FROM liked_title l WHERE l.tmdbId = title_snapshot.tmdbId AND l.mediaType = title_snapshot.mediaType)
        |        AND NOT EXISTS (SELECT 1 FROM watch_progress w WHERE w.tmdbId = title_snapshot.tmdbId AND w.mediaType = title_snapshot.mediaType)
        |        AND NOT EXISTS (SELECT 1 FROM play_session p WHERE p.tmdbId = title_snapshot.tmdbId AND p.mediaType = title_snapshot.mediaType)
        """.trimMargin()
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
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
