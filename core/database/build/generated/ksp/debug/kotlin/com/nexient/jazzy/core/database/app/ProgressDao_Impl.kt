package com.nexient.jazzy.core.database.app

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.EntityUpsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
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
public class ProgressDao_Impl(
  __db: RoomDatabase,
) : ProgressDao {
  private val __db: RoomDatabase

  private val __upsertAdapterOfWatchProgressEntity: EntityUpsertAdapter<WatchProgressEntity>
  init {
    this.__db = __db
    this.__upsertAdapterOfWatchProgressEntity = EntityUpsertAdapter<WatchProgressEntity>(object : EntityInsertAdapter<WatchProgressEntity>() {
      protected override fun createQuery(): String = "INSERT INTO `watch_progress` (`profileId`,`tmdbId`,`mediaType`,`season`,`episode`,`positionMs`,`durationMs`,`completed`,`lastInfoHash`,`lastFileIndex`,`lastSourceName`,`lastQuality`,`episodeTitle`,`updatedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: WatchProgressEntity) {
        statement.bindLong(1, entity.profileId)
        statement.bindLong(2, entity.tmdbId.toLong())
        statement.bindText(3, entity.mediaType)
        statement.bindLong(4, entity.season.toLong())
        statement.bindLong(5, entity.episode.toLong())
        statement.bindLong(6, entity.positionMs)
        statement.bindLong(7, entity.durationMs)
        val _tmp: Int = if (entity.completed) 1 else 0
        statement.bindLong(8, _tmp.toLong())
        val _tmpLastInfoHash: String? = entity.lastInfoHash
        if (_tmpLastInfoHash == null) {
          statement.bindNull(9)
        } else {
          statement.bindText(9, _tmpLastInfoHash)
        }
        val _tmpLastFileIndex: Int? = entity.lastFileIndex
        if (_tmpLastFileIndex == null) {
          statement.bindNull(10)
        } else {
          statement.bindLong(10, _tmpLastFileIndex.toLong())
        }
        val _tmpLastSourceName: String? = entity.lastSourceName
        if (_tmpLastSourceName == null) {
          statement.bindNull(11)
        } else {
          statement.bindText(11, _tmpLastSourceName)
        }
        val _tmpLastQuality: String? = entity.lastQuality
        if (_tmpLastQuality == null) {
          statement.bindNull(12)
        } else {
          statement.bindText(12, _tmpLastQuality)
        }
        val _tmpEpisodeTitle: String? = entity.episodeTitle
        if (_tmpEpisodeTitle == null) {
          statement.bindNull(13)
        } else {
          statement.bindText(13, _tmpEpisodeTitle)
        }
        statement.bindLong(14, entity.updatedAt)
      }
    }, object : EntityDeleteOrUpdateAdapter<WatchProgressEntity>() {
      protected override fun createQuery(): String = "UPDATE `watch_progress` SET `profileId` = ?,`tmdbId` = ?,`mediaType` = ?,`season` = ?,`episode` = ?,`positionMs` = ?,`durationMs` = ?,`completed` = ?,`lastInfoHash` = ?,`lastFileIndex` = ?,`lastSourceName` = ?,`lastQuality` = ?,`episodeTitle` = ?,`updatedAt` = ? WHERE `profileId` = ? AND `tmdbId` = ? AND `mediaType` = ? AND `season` = ? AND `episode` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: WatchProgressEntity) {
        statement.bindLong(1, entity.profileId)
        statement.bindLong(2, entity.tmdbId.toLong())
        statement.bindText(3, entity.mediaType)
        statement.bindLong(4, entity.season.toLong())
        statement.bindLong(5, entity.episode.toLong())
        statement.bindLong(6, entity.positionMs)
        statement.bindLong(7, entity.durationMs)
        val _tmp: Int = if (entity.completed) 1 else 0
        statement.bindLong(8, _tmp.toLong())
        val _tmpLastInfoHash: String? = entity.lastInfoHash
        if (_tmpLastInfoHash == null) {
          statement.bindNull(9)
        } else {
          statement.bindText(9, _tmpLastInfoHash)
        }
        val _tmpLastFileIndex: Int? = entity.lastFileIndex
        if (_tmpLastFileIndex == null) {
          statement.bindNull(10)
        } else {
          statement.bindLong(10, _tmpLastFileIndex.toLong())
        }
        val _tmpLastSourceName: String? = entity.lastSourceName
        if (_tmpLastSourceName == null) {
          statement.bindNull(11)
        } else {
          statement.bindText(11, _tmpLastSourceName)
        }
        val _tmpLastQuality: String? = entity.lastQuality
        if (_tmpLastQuality == null) {
          statement.bindNull(12)
        } else {
          statement.bindText(12, _tmpLastQuality)
        }
        val _tmpEpisodeTitle: String? = entity.episodeTitle
        if (_tmpEpisodeTitle == null) {
          statement.bindNull(13)
        } else {
          statement.bindText(13, _tmpEpisodeTitle)
        }
        statement.bindLong(14, entity.updatedAt)
        statement.bindLong(15, entity.profileId)
        statement.bindLong(16, entity.tmdbId.toLong())
        statement.bindText(17, entity.mediaType)
        statement.bindLong(18, entity.season.toLong())
        statement.bindLong(19, entity.episode.toLong())
      }
    })
  }

  public override suspend fun upsert(progress: WatchProgressEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __upsertAdapterOfWatchProgressEntity.upsert(_connection, progress)
  }

  public override suspend fun `get`(
    profileId: Long,
    tmdbId: Int,
    mediaType: String,
    season: Int,
    episode: Int,
  ): WatchProgressEntity? {
    val _sql: String = "SELECT * FROM watch_progress WHERE profileId = ? AND tmdbId = ? AND mediaType = ? AND season = ? AND episode = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, profileId)
        _argIndex = 2
        _stmt.bindLong(_argIndex, tmdbId.toLong())
        _argIndex = 3
        _stmt.bindText(_argIndex, mediaType)
        _argIndex = 4
        _stmt.bindLong(_argIndex, season.toLong())
        _argIndex = 5
        _stmt.bindLong(_argIndex, episode.toLong())
        val _columnIndexOfProfileId: Int = getColumnIndexOrThrow(_stmt, "profileId")
        val _columnIndexOfTmdbId: Int = getColumnIndexOrThrow(_stmt, "tmdbId")
        val _columnIndexOfMediaType: Int = getColumnIndexOrThrow(_stmt, "mediaType")
        val _columnIndexOfSeason: Int = getColumnIndexOrThrow(_stmt, "season")
        val _columnIndexOfEpisode: Int = getColumnIndexOrThrow(_stmt, "episode")
        val _columnIndexOfPositionMs: Int = getColumnIndexOrThrow(_stmt, "positionMs")
        val _columnIndexOfDurationMs: Int = getColumnIndexOrThrow(_stmt, "durationMs")
        val _columnIndexOfCompleted: Int = getColumnIndexOrThrow(_stmt, "completed")
        val _columnIndexOfLastInfoHash: Int = getColumnIndexOrThrow(_stmt, "lastInfoHash")
        val _columnIndexOfLastFileIndex: Int = getColumnIndexOrThrow(_stmt, "lastFileIndex")
        val _columnIndexOfLastSourceName: Int = getColumnIndexOrThrow(_stmt, "lastSourceName")
        val _columnIndexOfLastQuality: Int = getColumnIndexOrThrow(_stmt, "lastQuality")
        val _columnIndexOfEpisodeTitle: Int = getColumnIndexOrThrow(_stmt, "episodeTitle")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: WatchProgressEntity?
        if (_stmt.step()) {
          val _tmpProfileId: Long
          _tmpProfileId = _stmt.getLong(_columnIndexOfProfileId)
          val _tmpTmdbId: Int
          _tmpTmdbId = _stmt.getLong(_columnIndexOfTmdbId).toInt()
          val _tmpMediaType: String
          _tmpMediaType = _stmt.getText(_columnIndexOfMediaType)
          val _tmpSeason: Int
          _tmpSeason = _stmt.getLong(_columnIndexOfSeason).toInt()
          val _tmpEpisode: Int
          _tmpEpisode = _stmt.getLong(_columnIndexOfEpisode).toInt()
          val _tmpPositionMs: Long
          _tmpPositionMs = _stmt.getLong(_columnIndexOfPositionMs)
          val _tmpDurationMs: Long
          _tmpDurationMs = _stmt.getLong(_columnIndexOfDurationMs)
          val _tmpCompleted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfCompleted).toInt()
          _tmpCompleted = _tmp != 0
          val _tmpLastInfoHash: String?
          if (_stmt.isNull(_columnIndexOfLastInfoHash)) {
            _tmpLastInfoHash = null
          } else {
            _tmpLastInfoHash = _stmt.getText(_columnIndexOfLastInfoHash)
          }
          val _tmpLastFileIndex: Int?
          if (_stmt.isNull(_columnIndexOfLastFileIndex)) {
            _tmpLastFileIndex = null
          } else {
            _tmpLastFileIndex = _stmt.getLong(_columnIndexOfLastFileIndex).toInt()
          }
          val _tmpLastSourceName: String?
          if (_stmt.isNull(_columnIndexOfLastSourceName)) {
            _tmpLastSourceName = null
          } else {
            _tmpLastSourceName = _stmt.getText(_columnIndexOfLastSourceName)
          }
          val _tmpLastQuality: String?
          if (_stmt.isNull(_columnIndexOfLastQuality)) {
            _tmpLastQuality = null
          } else {
            _tmpLastQuality = _stmt.getText(_columnIndexOfLastQuality)
          }
          val _tmpEpisodeTitle: String?
          if (_stmt.isNull(_columnIndexOfEpisodeTitle)) {
            _tmpEpisodeTitle = null
          } else {
            _tmpEpisodeTitle = _stmt.getText(_columnIndexOfEpisodeTitle)
          }
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _result = WatchProgressEntity(_tmpProfileId,_tmpTmdbId,_tmpMediaType,_tmpSeason,_tmpEpisode,_tmpPositionMs,_tmpDurationMs,_tmpCompleted,_tmpLastInfoHash,_tmpLastFileIndex,_tmpLastSourceName,_tmpLastQuality,_tmpEpisodeTitle,_tmpUpdatedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observe(
    profileId: Long,
    tmdbId: Int,
    mediaType: String,
    season: Int,
    episode: Int,
  ): Flow<WatchProgressEntity?> {
    val _sql: String = "SELECT * FROM watch_progress WHERE profileId = ? AND tmdbId = ? AND mediaType = ? AND season = ? AND episode = ?"
    return createFlow(__db, false, arrayOf("watch_progress")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, profileId)
        _argIndex = 2
        _stmt.bindLong(_argIndex, tmdbId.toLong())
        _argIndex = 3
        _stmt.bindText(_argIndex, mediaType)
        _argIndex = 4
        _stmt.bindLong(_argIndex, season.toLong())
        _argIndex = 5
        _stmt.bindLong(_argIndex, episode.toLong())
        val _columnIndexOfProfileId: Int = getColumnIndexOrThrow(_stmt, "profileId")
        val _columnIndexOfTmdbId: Int = getColumnIndexOrThrow(_stmt, "tmdbId")
        val _columnIndexOfMediaType: Int = getColumnIndexOrThrow(_stmt, "mediaType")
        val _columnIndexOfSeason: Int = getColumnIndexOrThrow(_stmt, "season")
        val _columnIndexOfEpisode: Int = getColumnIndexOrThrow(_stmt, "episode")
        val _columnIndexOfPositionMs: Int = getColumnIndexOrThrow(_stmt, "positionMs")
        val _columnIndexOfDurationMs: Int = getColumnIndexOrThrow(_stmt, "durationMs")
        val _columnIndexOfCompleted: Int = getColumnIndexOrThrow(_stmt, "completed")
        val _columnIndexOfLastInfoHash: Int = getColumnIndexOrThrow(_stmt, "lastInfoHash")
        val _columnIndexOfLastFileIndex: Int = getColumnIndexOrThrow(_stmt, "lastFileIndex")
        val _columnIndexOfLastSourceName: Int = getColumnIndexOrThrow(_stmt, "lastSourceName")
        val _columnIndexOfLastQuality: Int = getColumnIndexOrThrow(_stmt, "lastQuality")
        val _columnIndexOfEpisodeTitle: Int = getColumnIndexOrThrow(_stmt, "episodeTitle")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: WatchProgressEntity?
        if (_stmt.step()) {
          val _tmpProfileId: Long
          _tmpProfileId = _stmt.getLong(_columnIndexOfProfileId)
          val _tmpTmdbId: Int
          _tmpTmdbId = _stmt.getLong(_columnIndexOfTmdbId).toInt()
          val _tmpMediaType: String
          _tmpMediaType = _stmt.getText(_columnIndexOfMediaType)
          val _tmpSeason: Int
          _tmpSeason = _stmt.getLong(_columnIndexOfSeason).toInt()
          val _tmpEpisode: Int
          _tmpEpisode = _stmt.getLong(_columnIndexOfEpisode).toInt()
          val _tmpPositionMs: Long
          _tmpPositionMs = _stmt.getLong(_columnIndexOfPositionMs)
          val _tmpDurationMs: Long
          _tmpDurationMs = _stmt.getLong(_columnIndexOfDurationMs)
          val _tmpCompleted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfCompleted).toInt()
          _tmpCompleted = _tmp != 0
          val _tmpLastInfoHash: String?
          if (_stmt.isNull(_columnIndexOfLastInfoHash)) {
            _tmpLastInfoHash = null
          } else {
            _tmpLastInfoHash = _stmt.getText(_columnIndexOfLastInfoHash)
          }
          val _tmpLastFileIndex: Int?
          if (_stmt.isNull(_columnIndexOfLastFileIndex)) {
            _tmpLastFileIndex = null
          } else {
            _tmpLastFileIndex = _stmt.getLong(_columnIndexOfLastFileIndex).toInt()
          }
          val _tmpLastSourceName: String?
          if (_stmt.isNull(_columnIndexOfLastSourceName)) {
            _tmpLastSourceName = null
          } else {
            _tmpLastSourceName = _stmt.getText(_columnIndexOfLastSourceName)
          }
          val _tmpLastQuality: String?
          if (_stmt.isNull(_columnIndexOfLastQuality)) {
            _tmpLastQuality = null
          } else {
            _tmpLastQuality = _stmt.getText(_columnIndexOfLastQuality)
          }
          val _tmpEpisodeTitle: String?
          if (_stmt.isNull(_columnIndexOfEpisodeTitle)) {
            _tmpEpisodeTitle = null
          } else {
            _tmpEpisodeTitle = _stmt.getText(_columnIndexOfEpisodeTitle)
          }
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _result = WatchProgressEntity(_tmpProfileId,_tmpTmdbId,_tmpMediaType,_tmpSeason,_tmpEpisode,_tmpPositionMs,_tmpDurationMs,_tmpCompleted,_tmpLastInfoHash,_tmpLastFileIndex,_tmpLastSourceName,_tmpLastQuality,_tmpEpisodeTitle,_tmpUpdatedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeForTitle(
    profileId: Long,
    tmdbId: Int,
    mediaType: String,
  ): Flow<List<WatchProgressEntity>> {
    val _sql: String = "SELECT * FROM watch_progress WHERE profileId = ? AND tmdbId = ? AND mediaType = ?"
    return createFlow(__db, false, arrayOf("watch_progress")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, profileId)
        _argIndex = 2
        _stmt.bindLong(_argIndex, tmdbId.toLong())
        _argIndex = 3
        _stmt.bindText(_argIndex, mediaType)
        val _columnIndexOfProfileId: Int = getColumnIndexOrThrow(_stmt, "profileId")
        val _columnIndexOfTmdbId: Int = getColumnIndexOrThrow(_stmt, "tmdbId")
        val _columnIndexOfMediaType: Int = getColumnIndexOrThrow(_stmt, "mediaType")
        val _columnIndexOfSeason: Int = getColumnIndexOrThrow(_stmt, "season")
        val _columnIndexOfEpisode: Int = getColumnIndexOrThrow(_stmt, "episode")
        val _columnIndexOfPositionMs: Int = getColumnIndexOrThrow(_stmt, "positionMs")
        val _columnIndexOfDurationMs: Int = getColumnIndexOrThrow(_stmt, "durationMs")
        val _columnIndexOfCompleted: Int = getColumnIndexOrThrow(_stmt, "completed")
        val _columnIndexOfLastInfoHash: Int = getColumnIndexOrThrow(_stmt, "lastInfoHash")
        val _columnIndexOfLastFileIndex: Int = getColumnIndexOrThrow(_stmt, "lastFileIndex")
        val _columnIndexOfLastSourceName: Int = getColumnIndexOrThrow(_stmt, "lastSourceName")
        val _columnIndexOfLastQuality: Int = getColumnIndexOrThrow(_stmt, "lastQuality")
        val _columnIndexOfEpisodeTitle: Int = getColumnIndexOrThrow(_stmt, "episodeTitle")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<WatchProgressEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: WatchProgressEntity
          val _tmpProfileId: Long
          _tmpProfileId = _stmt.getLong(_columnIndexOfProfileId)
          val _tmpTmdbId: Int
          _tmpTmdbId = _stmt.getLong(_columnIndexOfTmdbId).toInt()
          val _tmpMediaType: String
          _tmpMediaType = _stmt.getText(_columnIndexOfMediaType)
          val _tmpSeason: Int
          _tmpSeason = _stmt.getLong(_columnIndexOfSeason).toInt()
          val _tmpEpisode: Int
          _tmpEpisode = _stmt.getLong(_columnIndexOfEpisode).toInt()
          val _tmpPositionMs: Long
          _tmpPositionMs = _stmt.getLong(_columnIndexOfPositionMs)
          val _tmpDurationMs: Long
          _tmpDurationMs = _stmt.getLong(_columnIndexOfDurationMs)
          val _tmpCompleted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfCompleted).toInt()
          _tmpCompleted = _tmp != 0
          val _tmpLastInfoHash: String?
          if (_stmt.isNull(_columnIndexOfLastInfoHash)) {
            _tmpLastInfoHash = null
          } else {
            _tmpLastInfoHash = _stmt.getText(_columnIndexOfLastInfoHash)
          }
          val _tmpLastFileIndex: Int?
          if (_stmt.isNull(_columnIndexOfLastFileIndex)) {
            _tmpLastFileIndex = null
          } else {
            _tmpLastFileIndex = _stmt.getLong(_columnIndexOfLastFileIndex).toInt()
          }
          val _tmpLastSourceName: String?
          if (_stmt.isNull(_columnIndexOfLastSourceName)) {
            _tmpLastSourceName = null
          } else {
            _tmpLastSourceName = _stmt.getText(_columnIndexOfLastSourceName)
          }
          val _tmpLastQuality: String?
          if (_stmt.isNull(_columnIndexOfLastQuality)) {
            _tmpLastQuality = null
          } else {
            _tmpLastQuality = _stmt.getText(_columnIndexOfLastQuality)
          }
          val _tmpEpisodeTitle: String?
          if (_stmt.isNull(_columnIndexOfEpisodeTitle)) {
            _tmpEpisodeTitle = null
          } else {
            _tmpEpisodeTitle = _stmt.getText(_columnIndexOfEpisodeTitle)
          }
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item = WatchProgressEntity(_tmpProfileId,_tmpTmdbId,_tmpMediaType,_tmpSeason,_tmpEpisode,_tmpPositionMs,_tmpDurationMs,_tmpCompleted,_tmpLastInfoHash,_tmpLastFileIndex,_tmpLastSourceName,_tmpLastQuality,_tmpEpisodeTitle,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun latestForTitle(
    profileId: Long,
    tmdbId: Int,
    mediaType: String,
  ): WatchProgressEntity? {
    val _sql: String = "SELECT * FROM watch_progress WHERE profileId = ? AND tmdbId = ? AND mediaType = ? ORDER BY updatedAt DESC LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, profileId)
        _argIndex = 2
        _stmt.bindLong(_argIndex, tmdbId.toLong())
        _argIndex = 3
        _stmt.bindText(_argIndex, mediaType)
        val _columnIndexOfProfileId: Int = getColumnIndexOrThrow(_stmt, "profileId")
        val _columnIndexOfTmdbId: Int = getColumnIndexOrThrow(_stmt, "tmdbId")
        val _columnIndexOfMediaType: Int = getColumnIndexOrThrow(_stmt, "mediaType")
        val _columnIndexOfSeason: Int = getColumnIndexOrThrow(_stmt, "season")
        val _columnIndexOfEpisode: Int = getColumnIndexOrThrow(_stmt, "episode")
        val _columnIndexOfPositionMs: Int = getColumnIndexOrThrow(_stmt, "positionMs")
        val _columnIndexOfDurationMs: Int = getColumnIndexOrThrow(_stmt, "durationMs")
        val _columnIndexOfCompleted: Int = getColumnIndexOrThrow(_stmt, "completed")
        val _columnIndexOfLastInfoHash: Int = getColumnIndexOrThrow(_stmt, "lastInfoHash")
        val _columnIndexOfLastFileIndex: Int = getColumnIndexOrThrow(_stmt, "lastFileIndex")
        val _columnIndexOfLastSourceName: Int = getColumnIndexOrThrow(_stmt, "lastSourceName")
        val _columnIndexOfLastQuality: Int = getColumnIndexOrThrow(_stmt, "lastQuality")
        val _columnIndexOfEpisodeTitle: Int = getColumnIndexOrThrow(_stmt, "episodeTitle")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: WatchProgressEntity?
        if (_stmt.step()) {
          val _tmpProfileId: Long
          _tmpProfileId = _stmt.getLong(_columnIndexOfProfileId)
          val _tmpTmdbId: Int
          _tmpTmdbId = _stmt.getLong(_columnIndexOfTmdbId).toInt()
          val _tmpMediaType: String
          _tmpMediaType = _stmt.getText(_columnIndexOfMediaType)
          val _tmpSeason: Int
          _tmpSeason = _stmt.getLong(_columnIndexOfSeason).toInt()
          val _tmpEpisode: Int
          _tmpEpisode = _stmt.getLong(_columnIndexOfEpisode).toInt()
          val _tmpPositionMs: Long
          _tmpPositionMs = _stmt.getLong(_columnIndexOfPositionMs)
          val _tmpDurationMs: Long
          _tmpDurationMs = _stmt.getLong(_columnIndexOfDurationMs)
          val _tmpCompleted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfCompleted).toInt()
          _tmpCompleted = _tmp != 0
          val _tmpLastInfoHash: String?
          if (_stmt.isNull(_columnIndexOfLastInfoHash)) {
            _tmpLastInfoHash = null
          } else {
            _tmpLastInfoHash = _stmt.getText(_columnIndexOfLastInfoHash)
          }
          val _tmpLastFileIndex: Int?
          if (_stmt.isNull(_columnIndexOfLastFileIndex)) {
            _tmpLastFileIndex = null
          } else {
            _tmpLastFileIndex = _stmt.getLong(_columnIndexOfLastFileIndex).toInt()
          }
          val _tmpLastSourceName: String?
          if (_stmt.isNull(_columnIndexOfLastSourceName)) {
            _tmpLastSourceName = null
          } else {
            _tmpLastSourceName = _stmt.getText(_columnIndexOfLastSourceName)
          }
          val _tmpLastQuality: String?
          if (_stmt.isNull(_columnIndexOfLastQuality)) {
            _tmpLastQuality = null
          } else {
            _tmpLastQuality = _stmt.getText(_columnIndexOfLastQuality)
          }
          val _tmpEpisodeTitle: String?
          if (_stmt.isNull(_columnIndexOfEpisodeTitle)) {
            _tmpEpisodeTitle = null
          } else {
            _tmpEpisodeTitle = _stmt.getText(_columnIndexOfEpisodeTitle)
          }
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _result = WatchProgressEntity(_tmpProfileId,_tmpTmdbId,_tmpMediaType,_tmpSeason,_tmpEpisode,_tmpPositionMs,_tmpDurationMs,_tmpCompleted,_tmpLastInfoHash,_tmpLastFileIndex,_tmpLastSourceName,_tmpLastQuality,_tmpEpisodeTitle,_tmpUpdatedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeLatestPerTitle(profileId: Long, limit: Int): Flow<List<WatchProgressEntity>> {
    val _sql: String = """
        |SELECT w.* FROM watch_progress w
        |        WHERE w.profileId = ? AND w.updatedAt = (
        |            SELECT MAX(w2.updatedAt) FROM watch_progress w2
        |            WHERE w2.profileId = w.profileId AND w2.tmdbId = w.tmdbId AND w2.mediaType = w.mediaType)
        |        ORDER BY w.updatedAt DESC LIMIT ?
        """.trimMargin()
    return createFlow(__db, false, arrayOf("watch_progress")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, profileId)
        _argIndex = 2
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfProfileId: Int = getColumnIndexOrThrow(_stmt, "profileId")
        val _columnIndexOfTmdbId: Int = getColumnIndexOrThrow(_stmt, "tmdbId")
        val _columnIndexOfMediaType: Int = getColumnIndexOrThrow(_stmt, "mediaType")
        val _columnIndexOfSeason: Int = getColumnIndexOrThrow(_stmt, "season")
        val _columnIndexOfEpisode: Int = getColumnIndexOrThrow(_stmt, "episode")
        val _columnIndexOfPositionMs: Int = getColumnIndexOrThrow(_stmt, "positionMs")
        val _columnIndexOfDurationMs: Int = getColumnIndexOrThrow(_stmt, "durationMs")
        val _columnIndexOfCompleted: Int = getColumnIndexOrThrow(_stmt, "completed")
        val _columnIndexOfLastInfoHash: Int = getColumnIndexOrThrow(_stmt, "lastInfoHash")
        val _columnIndexOfLastFileIndex: Int = getColumnIndexOrThrow(_stmt, "lastFileIndex")
        val _columnIndexOfLastSourceName: Int = getColumnIndexOrThrow(_stmt, "lastSourceName")
        val _columnIndexOfLastQuality: Int = getColumnIndexOrThrow(_stmt, "lastQuality")
        val _columnIndexOfEpisodeTitle: Int = getColumnIndexOrThrow(_stmt, "episodeTitle")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<WatchProgressEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: WatchProgressEntity
          val _tmpProfileId: Long
          _tmpProfileId = _stmt.getLong(_columnIndexOfProfileId)
          val _tmpTmdbId: Int
          _tmpTmdbId = _stmt.getLong(_columnIndexOfTmdbId).toInt()
          val _tmpMediaType: String
          _tmpMediaType = _stmt.getText(_columnIndexOfMediaType)
          val _tmpSeason: Int
          _tmpSeason = _stmt.getLong(_columnIndexOfSeason).toInt()
          val _tmpEpisode: Int
          _tmpEpisode = _stmt.getLong(_columnIndexOfEpisode).toInt()
          val _tmpPositionMs: Long
          _tmpPositionMs = _stmt.getLong(_columnIndexOfPositionMs)
          val _tmpDurationMs: Long
          _tmpDurationMs = _stmt.getLong(_columnIndexOfDurationMs)
          val _tmpCompleted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfCompleted).toInt()
          _tmpCompleted = _tmp != 0
          val _tmpLastInfoHash: String?
          if (_stmt.isNull(_columnIndexOfLastInfoHash)) {
            _tmpLastInfoHash = null
          } else {
            _tmpLastInfoHash = _stmt.getText(_columnIndexOfLastInfoHash)
          }
          val _tmpLastFileIndex: Int?
          if (_stmt.isNull(_columnIndexOfLastFileIndex)) {
            _tmpLastFileIndex = null
          } else {
            _tmpLastFileIndex = _stmt.getLong(_columnIndexOfLastFileIndex).toInt()
          }
          val _tmpLastSourceName: String?
          if (_stmt.isNull(_columnIndexOfLastSourceName)) {
            _tmpLastSourceName = null
          } else {
            _tmpLastSourceName = _stmt.getText(_columnIndexOfLastSourceName)
          }
          val _tmpLastQuality: String?
          if (_stmt.isNull(_columnIndexOfLastQuality)) {
            _tmpLastQuality = null
          } else {
            _tmpLastQuality = _stmt.getText(_columnIndexOfLastQuality)
          }
          val _tmpEpisodeTitle: String?
          if (_stmt.isNull(_columnIndexOfEpisodeTitle)) {
            _tmpEpisodeTitle = null
          } else {
            _tmpEpisodeTitle = _stmt.getText(_columnIndexOfEpisodeTitle)
          }
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item = WatchProgressEntity(_tmpProfileId,_tmpTmdbId,_tmpMediaType,_tmpSeason,_tmpEpisode,_tmpPositionMs,_tmpDurationMs,_tmpCompleted,_tmpLastInfoHash,_tmpLastFileIndex,_tmpLastSourceName,_tmpLastQuality,_tmpEpisodeTitle,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun allFor(profileId: Long): List<WatchProgressEntity> {
    val _sql: String = "SELECT * FROM watch_progress WHERE profileId = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, profileId)
        val _columnIndexOfProfileId: Int = getColumnIndexOrThrow(_stmt, "profileId")
        val _columnIndexOfTmdbId: Int = getColumnIndexOrThrow(_stmt, "tmdbId")
        val _columnIndexOfMediaType: Int = getColumnIndexOrThrow(_stmt, "mediaType")
        val _columnIndexOfSeason: Int = getColumnIndexOrThrow(_stmt, "season")
        val _columnIndexOfEpisode: Int = getColumnIndexOrThrow(_stmt, "episode")
        val _columnIndexOfPositionMs: Int = getColumnIndexOrThrow(_stmt, "positionMs")
        val _columnIndexOfDurationMs: Int = getColumnIndexOrThrow(_stmt, "durationMs")
        val _columnIndexOfCompleted: Int = getColumnIndexOrThrow(_stmt, "completed")
        val _columnIndexOfLastInfoHash: Int = getColumnIndexOrThrow(_stmt, "lastInfoHash")
        val _columnIndexOfLastFileIndex: Int = getColumnIndexOrThrow(_stmt, "lastFileIndex")
        val _columnIndexOfLastSourceName: Int = getColumnIndexOrThrow(_stmt, "lastSourceName")
        val _columnIndexOfLastQuality: Int = getColumnIndexOrThrow(_stmt, "lastQuality")
        val _columnIndexOfEpisodeTitle: Int = getColumnIndexOrThrow(_stmt, "episodeTitle")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<WatchProgressEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: WatchProgressEntity
          val _tmpProfileId: Long
          _tmpProfileId = _stmt.getLong(_columnIndexOfProfileId)
          val _tmpTmdbId: Int
          _tmpTmdbId = _stmt.getLong(_columnIndexOfTmdbId).toInt()
          val _tmpMediaType: String
          _tmpMediaType = _stmt.getText(_columnIndexOfMediaType)
          val _tmpSeason: Int
          _tmpSeason = _stmt.getLong(_columnIndexOfSeason).toInt()
          val _tmpEpisode: Int
          _tmpEpisode = _stmt.getLong(_columnIndexOfEpisode).toInt()
          val _tmpPositionMs: Long
          _tmpPositionMs = _stmt.getLong(_columnIndexOfPositionMs)
          val _tmpDurationMs: Long
          _tmpDurationMs = _stmt.getLong(_columnIndexOfDurationMs)
          val _tmpCompleted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfCompleted).toInt()
          _tmpCompleted = _tmp != 0
          val _tmpLastInfoHash: String?
          if (_stmt.isNull(_columnIndexOfLastInfoHash)) {
            _tmpLastInfoHash = null
          } else {
            _tmpLastInfoHash = _stmt.getText(_columnIndexOfLastInfoHash)
          }
          val _tmpLastFileIndex: Int?
          if (_stmt.isNull(_columnIndexOfLastFileIndex)) {
            _tmpLastFileIndex = null
          } else {
            _tmpLastFileIndex = _stmt.getLong(_columnIndexOfLastFileIndex).toInt()
          }
          val _tmpLastSourceName: String?
          if (_stmt.isNull(_columnIndexOfLastSourceName)) {
            _tmpLastSourceName = null
          } else {
            _tmpLastSourceName = _stmt.getText(_columnIndexOfLastSourceName)
          }
          val _tmpLastQuality: String?
          if (_stmt.isNull(_columnIndexOfLastQuality)) {
            _tmpLastQuality = null
          } else {
            _tmpLastQuality = _stmt.getText(_columnIndexOfLastQuality)
          }
          val _tmpEpisodeTitle: String?
          if (_stmt.isNull(_columnIndexOfEpisodeTitle)) {
            _tmpEpisodeTitle = null
          } else {
            _tmpEpisodeTitle = _stmt.getText(_columnIndexOfEpisodeTitle)
          }
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item = WatchProgressEntity(_tmpProfileId,_tmpTmdbId,_tmpMediaType,_tmpSeason,_tmpEpisode,_tmpPositionMs,_tmpDurationMs,_tmpCompleted,_tmpLastInfoHash,_tmpLastFileIndex,_tmpLastSourceName,_tmpLastQuality,_tmpEpisodeTitle,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteForTitle(
    profileId: Long,
    tmdbId: Int,
    mediaType: String,
  ) {
    val _sql: String = "DELETE FROM watch_progress WHERE profileId = ? AND tmdbId = ? AND mediaType = ?"
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

  public override suspend fun hideFromContinue(
    profileId: Long,
    tmdbId: Int,
    mediaType: String,
  ) {
    val _sql: String = "UPDATE watch_progress SET updatedAt = 0 WHERE profileId = ? AND tmdbId = ? AND mediaType = ?"
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
