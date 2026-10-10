package com.nexient.jazzy.core.database.app

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.appendPlaceholders
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlin.text.StringBuilder
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class PlaySessionDao_Impl(
  __db: RoomDatabase,
) : PlaySessionDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfPlaySessionEntity: EntityInsertAdapter<PlaySessionEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfPlaySessionEntity = object : EntityInsertAdapter<PlaySessionEntity>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `play_session` (`id`,`profileId`,`tmdbId`,`mediaType`,`season`,`episode`,`episodeTitle`,`infoHash`,`fileIndex`,`sourceName`,`quality`,`providerId`,`startedAt`,`endedAt`,`startPositionMs`,`stopPositionMs`,`durationMs`,`watchedMs`,`endReason`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: PlaySessionEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.profileId)
        statement.bindLong(3, entity.tmdbId.toLong())
        statement.bindText(4, entity.mediaType)
        statement.bindLong(5, entity.season.toLong())
        statement.bindLong(6, entity.episode.toLong())
        val _tmpEpisodeTitle: String? = entity.episodeTitle
        if (_tmpEpisodeTitle == null) {
          statement.bindNull(7)
        } else {
          statement.bindText(7, _tmpEpisodeTitle)
        }
        val _tmpInfoHash: String? = entity.infoHash
        if (_tmpInfoHash == null) {
          statement.bindNull(8)
        } else {
          statement.bindText(8, _tmpInfoHash)
        }
        val _tmpFileIndex: Int? = entity.fileIndex
        if (_tmpFileIndex == null) {
          statement.bindNull(9)
        } else {
          statement.bindLong(9, _tmpFileIndex.toLong())
        }
        val _tmpSourceName: String? = entity.sourceName
        if (_tmpSourceName == null) {
          statement.bindNull(10)
        } else {
          statement.bindText(10, _tmpSourceName)
        }
        val _tmpQuality: String? = entity.quality
        if (_tmpQuality == null) {
          statement.bindNull(11)
        } else {
          statement.bindText(11, _tmpQuality)
        }
        val _tmpProviderId: String? = entity.providerId
        if (_tmpProviderId == null) {
          statement.bindNull(12)
        } else {
          statement.bindText(12, _tmpProviderId)
        }
        statement.bindLong(13, entity.startedAt)
        val _tmpEndedAt: Long? = entity.endedAt
        if (_tmpEndedAt == null) {
          statement.bindNull(14)
        } else {
          statement.bindLong(14, _tmpEndedAt)
        }
        statement.bindLong(15, entity.startPositionMs)
        statement.bindLong(16, entity.stopPositionMs)
        statement.bindLong(17, entity.durationMs)
        statement.bindLong(18, entity.watchedMs)
        val _tmpEndReason: String? = entity.endReason
        if (_tmpEndReason == null) {
          statement.bindNull(19)
        } else {
          statement.bindText(19, _tmpEndReason)
        }
      }
    }
  }

  public override suspend fun insert(session: PlaySessionEntity): Long = performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfPlaySessionEntity.insertAndReturnId(_connection, session)
    _result
  }

  public override fun observeVisible(profileId: Long, minWatchedMs: Long): Flow<List<PlaySessionEntity>> {
    val _sql: String = "SELECT * FROM play_session WHERE profileId = ? AND watchedMs >= ? ORDER BY startedAt DESC"
    return createFlow(__db, false, arrayOf("play_session")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, profileId)
        _argIndex = 2
        _stmt.bindLong(_argIndex, minWatchedMs)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfProfileId: Int = getColumnIndexOrThrow(_stmt, "profileId")
        val _columnIndexOfTmdbId: Int = getColumnIndexOrThrow(_stmt, "tmdbId")
        val _columnIndexOfMediaType: Int = getColumnIndexOrThrow(_stmt, "mediaType")
        val _columnIndexOfSeason: Int = getColumnIndexOrThrow(_stmt, "season")
        val _columnIndexOfEpisode: Int = getColumnIndexOrThrow(_stmt, "episode")
        val _columnIndexOfEpisodeTitle: Int = getColumnIndexOrThrow(_stmt, "episodeTitle")
        val _columnIndexOfInfoHash: Int = getColumnIndexOrThrow(_stmt, "infoHash")
        val _columnIndexOfFileIndex: Int = getColumnIndexOrThrow(_stmt, "fileIndex")
        val _columnIndexOfSourceName: Int = getColumnIndexOrThrow(_stmt, "sourceName")
        val _columnIndexOfQuality: Int = getColumnIndexOrThrow(_stmt, "quality")
        val _columnIndexOfProviderId: Int = getColumnIndexOrThrow(_stmt, "providerId")
        val _columnIndexOfStartedAt: Int = getColumnIndexOrThrow(_stmt, "startedAt")
        val _columnIndexOfEndedAt: Int = getColumnIndexOrThrow(_stmt, "endedAt")
        val _columnIndexOfStartPositionMs: Int = getColumnIndexOrThrow(_stmt, "startPositionMs")
        val _columnIndexOfStopPositionMs: Int = getColumnIndexOrThrow(_stmt, "stopPositionMs")
        val _columnIndexOfDurationMs: Int = getColumnIndexOrThrow(_stmt, "durationMs")
        val _columnIndexOfWatchedMs: Int = getColumnIndexOrThrow(_stmt, "watchedMs")
        val _columnIndexOfEndReason: Int = getColumnIndexOrThrow(_stmt, "endReason")
        val _result: MutableList<PlaySessionEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: PlaySessionEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
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
          val _tmpEpisodeTitle: String?
          if (_stmt.isNull(_columnIndexOfEpisodeTitle)) {
            _tmpEpisodeTitle = null
          } else {
            _tmpEpisodeTitle = _stmt.getText(_columnIndexOfEpisodeTitle)
          }
          val _tmpInfoHash: String?
          if (_stmt.isNull(_columnIndexOfInfoHash)) {
            _tmpInfoHash = null
          } else {
            _tmpInfoHash = _stmt.getText(_columnIndexOfInfoHash)
          }
          val _tmpFileIndex: Int?
          if (_stmt.isNull(_columnIndexOfFileIndex)) {
            _tmpFileIndex = null
          } else {
            _tmpFileIndex = _stmt.getLong(_columnIndexOfFileIndex).toInt()
          }
          val _tmpSourceName: String?
          if (_stmt.isNull(_columnIndexOfSourceName)) {
            _tmpSourceName = null
          } else {
            _tmpSourceName = _stmt.getText(_columnIndexOfSourceName)
          }
          val _tmpQuality: String?
          if (_stmt.isNull(_columnIndexOfQuality)) {
            _tmpQuality = null
          } else {
            _tmpQuality = _stmt.getText(_columnIndexOfQuality)
          }
          val _tmpProviderId: String?
          if (_stmt.isNull(_columnIndexOfProviderId)) {
            _tmpProviderId = null
          } else {
            _tmpProviderId = _stmt.getText(_columnIndexOfProviderId)
          }
          val _tmpStartedAt: Long
          _tmpStartedAt = _stmt.getLong(_columnIndexOfStartedAt)
          val _tmpEndedAt: Long?
          if (_stmt.isNull(_columnIndexOfEndedAt)) {
            _tmpEndedAt = null
          } else {
            _tmpEndedAt = _stmt.getLong(_columnIndexOfEndedAt)
          }
          val _tmpStartPositionMs: Long
          _tmpStartPositionMs = _stmt.getLong(_columnIndexOfStartPositionMs)
          val _tmpStopPositionMs: Long
          _tmpStopPositionMs = _stmt.getLong(_columnIndexOfStopPositionMs)
          val _tmpDurationMs: Long
          _tmpDurationMs = _stmt.getLong(_columnIndexOfDurationMs)
          val _tmpWatchedMs: Long
          _tmpWatchedMs = _stmt.getLong(_columnIndexOfWatchedMs)
          val _tmpEndReason: String?
          if (_stmt.isNull(_columnIndexOfEndReason)) {
            _tmpEndReason = null
          } else {
            _tmpEndReason = _stmt.getText(_columnIndexOfEndReason)
          }
          _item = PlaySessionEntity(_tmpId,_tmpProfileId,_tmpTmdbId,_tmpMediaType,_tmpSeason,_tmpEpisode,_tmpEpisodeTitle,_tmpInfoHash,_tmpFileIndex,_tmpSourceName,_tmpQuality,_tmpProviderId,_tmpStartedAt,_tmpEndedAt,_tmpStartPositionMs,_tmpStopPositionMs,_tmpDurationMs,_tmpWatchedMs,_tmpEndReason)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun allFor(profileId: Long): List<PlaySessionEntity> {
    val _sql: String = "SELECT * FROM play_session WHERE profileId = ? ORDER BY startedAt DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, profileId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfProfileId: Int = getColumnIndexOrThrow(_stmt, "profileId")
        val _columnIndexOfTmdbId: Int = getColumnIndexOrThrow(_stmt, "tmdbId")
        val _columnIndexOfMediaType: Int = getColumnIndexOrThrow(_stmt, "mediaType")
        val _columnIndexOfSeason: Int = getColumnIndexOrThrow(_stmt, "season")
        val _columnIndexOfEpisode: Int = getColumnIndexOrThrow(_stmt, "episode")
        val _columnIndexOfEpisodeTitle: Int = getColumnIndexOrThrow(_stmt, "episodeTitle")
        val _columnIndexOfInfoHash: Int = getColumnIndexOrThrow(_stmt, "infoHash")
        val _columnIndexOfFileIndex: Int = getColumnIndexOrThrow(_stmt, "fileIndex")
        val _columnIndexOfSourceName: Int = getColumnIndexOrThrow(_stmt, "sourceName")
        val _columnIndexOfQuality: Int = getColumnIndexOrThrow(_stmt, "quality")
        val _columnIndexOfProviderId: Int = getColumnIndexOrThrow(_stmt, "providerId")
        val _columnIndexOfStartedAt: Int = getColumnIndexOrThrow(_stmt, "startedAt")
        val _columnIndexOfEndedAt: Int = getColumnIndexOrThrow(_stmt, "endedAt")
        val _columnIndexOfStartPositionMs: Int = getColumnIndexOrThrow(_stmt, "startPositionMs")
        val _columnIndexOfStopPositionMs: Int = getColumnIndexOrThrow(_stmt, "stopPositionMs")
        val _columnIndexOfDurationMs: Int = getColumnIndexOrThrow(_stmt, "durationMs")
        val _columnIndexOfWatchedMs: Int = getColumnIndexOrThrow(_stmt, "watchedMs")
        val _columnIndexOfEndReason: Int = getColumnIndexOrThrow(_stmt, "endReason")
        val _result: MutableList<PlaySessionEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: PlaySessionEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
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
          val _tmpEpisodeTitle: String?
          if (_stmt.isNull(_columnIndexOfEpisodeTitle)) {
            _tmpEpisodeTitle = null
          } else {
            _tmpEpisodeTitle = _stmt.getText(_columnIndexOfEpisodeTitle)
          }
          val _tmpInfoHash: String?
          if (_stmt.isNull(_columnIndexOfInfoHash)) {
            _tmpInfoHash = null
          } else {
            _tmpInfoHash = _stmt.getText(_columnIndexOfInfoHash)
          }
          val _tmpFileIndex: Int?
          if (_stmt.isNull(_columnIndexOfFileIndex)) {
            _tmpFileIndex = null
          } else {
            _tmpFileIndex = _stmt.getLong(_columnIndexOfFileIndex).toInt()
          }
          val _tmpSourceName: String?
          if (_stmt.isNull(_columnIndexOfSourceName)) {
            _tmpSourceName = null
          } else {
            _tmpSourceName = _stmt.getText(_columnIndexOfSourceName)
          }
          val _tmpQuality: String?
          if (_stmt.isNull(_columnIndexOfQuality)) {
            _tmpQuality = null
          } else {
            _tmpQuality = _stmt.getText(_columnIndexOfQuality)
          }
          val _tmpProviderId: String?
          if (_stmt.isNull(_columnIndexOfProviderId)) {
            _tmpProviderId = null
          } else {
            _tmpProviderId = _stmt.getText(_columnIndexOfProviderId)
          }
          val _tmpStartedAt: Long
          _tmpStartedAt = _stmt.getLong(_columnIndexOfStartedAt)
          val _tmpEndedAt: Long?
          if (_stmt.isNull(_columnIndexOfEndedAt)) {
            _tmpEndedAt = null
          } else {
            _tmpEndedAt = _stmt.getLong(_columnIndexOfEndedAt)
          }
          val _tmpStartPositionMs: Long
          _tmpStartPositionMs = _stmt.getLong(_columnIndexOfStartPositionMs)
          val _tmpStopPositionMs: Long
          _tmpStopPositionMs = _stmt.getLong(_columnIndexOfStopPositionMs)
          val _tmpDurationMs: Long
          _tmpDurationMs = _stmt.getLong(_columnIndexOfDurationMs)
          val _tmpWatchedMs: Long
          _tmpWatchedMs = _stmt.getLong(_columnIndexOfWatchedMs)
          val _tmpEndReason: String?
          if (_stmt.isNull(_columnIndexOfEndReason)) {
            _tmpEndReason = null
          } else {
            _tmpEndReason = _stmt.getText(_columnIndexOfEndReason)
          }
          _item = PlaySessionEntity(_tmpId,_tmpProfileId,_tmpTmdbId,_tmpMediaType,_tmpSeason,_tmpEpisode,_tmpEpisodeTitle,_tmpInfoHash,_tmpFileIndex,_tmpSourceName,_tmpQuality,_tmpProviderId,_tmpStartedAt,_tmpEndedAt,_tmpStartPositionMs,_tmpStopPositionMs,_tmpDurationMs,_tmpWatchedMs,_tmpEndReason)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun finish(
    id: Long,
    endedAt: Long,
    stopPositionMs: Long,
    durationMs: Long,
    watchedMs: Long,
    endReason: String?,
  ) {
    val _sql: String = """
        |UPDATE play_session SET endedAt = ?, stopPositionMs = ?, durationMs = ?,
        |        watchedMs = ?, endReason = ? WHERE id = ?
        """.trimMargin()
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, endedAt)
        _argIndex = 2
        _stmt.bindLong(_argIndex, stopPositionMs)
        _argIndex = 3
        _stmt.bindLong(_argIndex, durationMs)
        _argIndex = 4
        _stmt.bindLong(_argIndex, watchedMs)
        _argIndex = 5
        if (endReason == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, endReason)
        }
        _argIndex = 6
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun checkpoint(
    id: Long,
    stopPositionMs: Long,
    durationMs: Long,
    watchedMs: Long,
    now: Long,
  ) {
    val _sql: String = "UPDATE play_session SET stopPositionMs = ?, durationMs = ?, watchedMs = ?, endedAt = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, stopPositionMs)
        _argIndex = 2
        _stmt.bindLong(_argIndex, durationMs)
        _argIndex = 3
        _stmt.bindLong(_argIndex, watchedMs)
        _argIndex = 4
        _stmt.bindLong(_argIndex, now)
        _argIndex = 5
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun delete(ids: List<Long>) {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("DELETE FROM play_session WHERE id IN (")
    val _inputSize: Int = ids.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        for (_item: Long in ids) {
          _stmt.bindLong(_argIndex, _item)
          _argIndex++
        }
        _stmt.step()
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
    val _sql: String = "DELETE FROM play_session WHERE profileId = ? AND tmdbId = ? AND mediaType = ?"
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

  public override suspend fun deleteAll(profileId: Long) {
    val _sql: String = "DELETE FROM play_session WHERE profileId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, profileId)
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
