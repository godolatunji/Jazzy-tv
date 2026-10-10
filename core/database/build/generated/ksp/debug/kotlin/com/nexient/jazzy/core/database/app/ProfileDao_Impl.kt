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
public class ProfileDao_Impl(
  __db: RoomDatabase,
) : ProfileDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfProfileEntity: EntityInsertAdapter<ProfileEntity>

  private val __upsertAdapterOfProfileEntity: EntityUpsertAdapter<ProfileEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfProfileEntity = object : EntityInsertAdapter<ProfileEntity>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `profile` (`id`,`name`,`avatarId`,`isKids`,`maturity`,`language`,`subtitleLanguage`,`audioLanguage`,`pinHash`,`pinSalt`,`autoplayNext`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ProfileEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindLong(3, entity.avatarId.toLong())
        val _tmp: Int = if (entity.isKids) 1 else 0
        statement.bindLong(4, _tmp.toLong())
        statement.bindText(5, entity.maturity)
        statement.bindText(6, entity.language)
        val _tmpSubtitleLanguage: String? = entity.subtitleLanguage
        if (_tmpSubtitleLanguage == null) {
          statement.bindNull(7)
        } else {
          statement.bindText(7, _tmpSubtitleLanguage)
        }
        val _tmpAudioLanguage: String? = entity.audioLanguage
        if (_tmpAudioLanguage == null) {
          statement.bindNull(8)
        } else {
          statement.bindText(8, _tmpAudioLanguage)
        }
        val _tmpPinHash: String? = entity.pinHash
        if (_tmpPinHash == null) {
          statement.bindNull(9)
        } else {
          statement.bindText(9, _tmpPinHash)
        }
        val _tmpPinSalt: String? = entity.pinSalt
        if (_tmpPinSalt == null) {
          statement.bindNull(10)
        } else {
          statement.bindText(10, _tmpPinSalt)
        }
        val _tmp_1: Int = if (entity.autoplayNext) 1 else 0
        statement.bindLong(11, _tmp_1.toLong())
        statement.bindLong(12, entity.createdAt)
      }
    }
    this.__upsertAdapterOfProfileEntity = EntityUpsertAdapter<ProfileEntity>(object : EntityInsertAdapter<ProfileEntity>() {
      protected override fun createQuery(): String = "INSERT INTO `profile` (`id`,`name`,`avatarId`,`isKids`,`maturity`,`language`,`subtitleLanguage`,`audioLanguage`,`pinHash`,`pinSalt`,`autoplayNext`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ProfileEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindLong(3, entity.avatarId.toLong())
        val _tmp: Int = if (entity.isKids) 1 else 0
        statement.bindLong(4, _tmp.toLong())
        statement.bindText(5, entity.maturity)
        statement.bindText(6, entity.language)
        val _tmpSubtitleLanguage: String? = entity.subtitleLanguage
        if (_tmpSubtitleLanguage == null) {
          statement.bindNull(7)
        } else {
          statement.bindText(7, _tmpSubtitleLanguage)
        }
        val _tmpAudioLanguage: String? = entity.audioLanguage
        if (_tmpAudioLanguage == null) {
          statement.bindNull(8)
        } else {
          statement.bindText(8, _tmpAudioLanguage)
        }
        val _tmpPinHash: String? = entity.pinHash
        if (_tmpPinHash == null) {
          statement.bindNull(9)
        } else {
          statement.bindText(9, _tmpPinHash)
        }
        val _tmpPinSalt: String? = entity.pinSalt
        if (_tmpPinSalt == null) {
          statement.bindNull(10)
        } else {
          statement.bindText(10, _tmpPinSalt)
        }
        val _tmp_1: Int = if (entity.autoplayNext) 1 else 0
        statement.bindLong(11, _tmp_1.toLong())
        statement.bindLong(12, entity.createdAt)
      }
    }, object : EntityDeleteOrUpdateAdapter<ProfileEntity>() {
      protected override fun createQuery(): String = "UPDATE `profile` SET `id` = ?,`name` = ?,`avatarId` = ?,`isKids` = ?,`maturity` = ?,`language` = ?,`subtitleLanguage` = ?,`audioLanguage` = ?,`pinHash` = ?,`pinSalt` = ?,`autoplayNext` = ?,`createdAt` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: ProfileEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindLong(3, entity.avatarId.toLong())
        val _tmp: Int = if (entity.isKids) 1 else 0
        statement.bindLong(4, _tmp.toLong())
        statement.bindText(5, entity.maturity)
        statement.bindText(6, entity.language)
        val _tmpSubtitleLanguage: String? = entity.subtitleLanguage
        if (_tmpSubtitleLanguage == null) {
          statement.bindNull(7)
        } else {
          statement.bindText(7, _tmpSubtitleLanguage)
        }
        val _tmpAudioLanguage: String? = entity.audioLanguage
        if (_tmpAudioLanguage == null) {
          statement.bindNull(8)
        } else {
          statement.bindText(8, _tmpAudioLanguage)
        }
        val _tmpPinHash: String? = entity.pinHash
        if (_tmpPinHash == null) {
          statement.bindNull(9)
        } else {
          statement.bindText(9, _tmpPinHash)
        }
        val _tmpPinSalt: String? = entity.pinSalt
        if (_tmpPinSalt == null) {
          statement.bindNull(10)
        } else {
          statement.bindText(10, _tmpPinSalt)
        }
        val _tmp_1: Int = if (entity.autoplayNext) 1 else 0
        statement.bindLong(11, _tmp_1.toLong())
        statement.bindLong(12, entity.createdAt)
        statement.bindLong(13, entity.id)
      }
    })
  }

  public override suspend fun insert(profile: ProfileEntity): Long = performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfProfileEntity.insertAndReturnId(_connection, profile)
    _result
  }

  public override suspend fun upsert(profile: ProfileEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __upsertAdapterOfProfileEntity.upsert(_connection, profile)
  }

  public override fun observeAll(): Flow<List<ProfileEntity>> {
    val _sql: String = "SELECT * FROM profile ORDER BY createdAt"
    return createFlow(__db, false, arrayOf("profile")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfAvatarId: Int = getColumnIndexOrThrow(_stmt, "avatarId")
        val _columnIndexOfIsKids: Int = getColumnIndexOrThrow(_stmt, "isKids")
        val _columnIndexOfMaturity: Int = getColumnIndexOrThrow(_stmt, "maturity")
        val _columnIndexOfLanguage: Int = getColumnIndexOrThrow(_stmt, "language")
        val _columnIndexOfSubtitleLanguage: Int = getColumnIndexOrThrow(_stmt, "subtitleLanguage")
        val _columnIndexOfAudioLanguage: Int = getColumnIndexOrThrow(_stmt, "audioLanguage")
        val _columnIndexOfPinHash: Int = getColumnIndexOrThrow(_stmt, "pinHash")
        val _columnIndexOfPinSalt: Int = getColumnIndexOrThrow(_stmt, "pinSalt")
        val _columnIndexOfAutoplayNext: Int = getColumnIndexOrThrow(_stmt, "autoplayNext")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _result: MutableList<ProfileEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ProfileEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpAvatarId: Int
          _tmpAvatarId = _stmt.getLong(_columnIndexOfAvatarId).toInt()
          val _tmpIsKids: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsKids).toInt()
          _tmpIsKids = _tmp != 0
          val _tmpMaturity: String
          _tmpMaturity = _stmt.getText(_columnIndexOfMaturity)
          val _tmpLanguage: String
          _tmpLanguage = _stmt.getText(_columnIndexOfLanguage)
          val _tmpSubtitleLanguage: String?
          if (_stmt.isNull(_columnIndexOfSubtitleLanguage)) {
            _tmpSubtitleLanguage = null
          } else {
            _tmpSubtitleLanguage = _stmt.getText(_columnIndexOfSubtitleLanguage)
          }
          val _tmpAudioLanguage: String?
          if (_stmt.isNull(_columnIndexOfAudioLanguage)) {
            _tmpAudioLanguage = null
          } else {
            _tmpAudioLanguage = _stmt.getText(_columnIndexOfAudioLanguage)
          }
          val _tmpPinHash: String?
          if (_stmt.isNull(_columnIndexOfPinHash)) {
            _tmpPinHash = null
          } else {
            _tmpPinHash = _stmt.getText(_columnIndexOfPinHash)
          }
          val _tmpPinSalt: String?
          if (_stmt.isNull(_columnIndexOfPinSalt)) {
            _tmpPinSalt = null
          } else {
            _tmpPinSalt = _stmt.getText(_columnIndexOfPinSalt)
          }
          val _tmpAutoplayNext: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfAutoplayNext).toInt()
          _tmpAutoplayNext = _tmp_1 != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _item = ProfileEntity(_tmpId,_tmpName,_tmpAvatarId,_tmpIsKids,_tmpMaturity,_tmpLanguage,_tmpSubtitleLanguage,_tmpAudioLanguage,_tmpPinHash,_tmpPinSalt,_tmpAutoplayNext,_tmpCreatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observe(id: Long): Flow<ProfileEntity?> {
    val _sql: String = "SELECT * FROM profile WHERE id = ?"
    return createFlow(__db, false, arrayOf("profile")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfAvatarId: Int = getColumnIndexOrThrow(_stmt, "avatarId")
        val _columnIndexOfIsKids: Int = getColumnIndexOrThrow(_stmt, "isKids")
        val _columnIndexOfMaturity: Int = getColumnIndexOrThrow(_stmt, "maturity")
        val _columnIndexOfLanguage: Int = getColumnIndexOrThrow(_stmt, "language")
        val _columnIndexOfSubtitleLanguage: Int = getColumnIndexOrThrow(_stmt, "subtitleLanguage")
        val _columnIndexOfAudioLanguage: Int = getColumnIndexOrThrow(_stmt, "audioLanguage")
        val _columnIndexOfPinHash: Int = getColumnIndexOrThrow(_stmt, "pinHash")
        val _columnIndexOfPinSalt: Int = getColumnIndexOrThrow(_stmt, "pinSalt")
        val _columnIndexOfAutoplayNext: Int = getColumnIndexOrThrow(_stmt, "autoplayNext")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _result: ProfileEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpAvatarId: Int
          _tmpAvatarId = _stmt.getLong(_columnIndexOfAvatarId).toInt()
          val _tmpIsKids: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsKids).toInt()
          _tmpIsKids = _tmp != 0
          val _tmpMaturity: String
          _tmpMaturity = _stmt.getText(_columnIndexOfMaturity)
          val _tmpLanguage: String
          _tmpLanguage = _stmt.getText(_columnIndexOfLanguage)
          val _tmpSubtitleLanguage: String?
          if (_stmt.isNull(_columnIndexOfSubtitleLanguage)) {
            _tmpSubtitleLanguage = null
          } else {
            _tmpSubtitleLanguage = _stmt.getText(_columnIndexOfSubtitleLanguage)
          }
          val _tmpAudioLanguage: String?
          if (_stmt.isNull(_columnIndexOfAudioLanguage)) {
            _tmpAudioLanguage = null
          } else {
            _tmpAudioLanguage = _stmt.getText(_columnIndexOfAudioLanguage)
          }
          val _tmpPinHash: String?
          if (_stmt.isNull(_columnIndexOfPinHash)) {
            _tmpPinHash = null
          } else {
            _tmpPinHash = _stmt.getText(_columnIndexOfPinHash)
          }
          val _tmpPinSalt: String?
          if (_stmt.isNull(_columnIndexOfPinSalt)) {
            _tmpPinSalt = null
          } else {
            _tmpPinSalt = _stmt.getText(_columnIndexOfPinSalt)
          }
          val _tmpAutoplayNext: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfAutoplayNext).toInt()
          _tmpAutoplayNext = _tmp_1 != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _result = ProfileEntity(_tmpId,_tmpName,_tmpAvatarId,_tmpIsKids,_tmpMaturity,_tmpLanguage,_tmpSubtitleLanguage,_tmpAudioLanguage,_tmpPinHash,_tmpPinSalt,_tmpAutoplayNext,_tmpCreatedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun `get`(id: Long): ProfileEntity? {
    val _sql: String = "SELECT * FROM profile WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfAvatarId: Int = getColumnIndexOrThrow(_stmt, "avatarId")
        val _columnIndexOfIsKids: Int = getColumnIndexOrThrow(_stmt, "isKids")
        val _columnIndexOfMaturity: Int = getColumnIndexOrThrow(_stmt, "maturity")
        val _columnIndexOfLanguage: Int = getColumnIndexOrThrow(_stmt, "language")
        val _columnIndexOfSubtitleLanguage: Int = getColumnIndexOrThrow(_stmt, "subtitleLanguage")
        val _columnIndexOfAudioLanguage: Int = getColumnIndexOrThrow(_stmt, "audioLanguage")
        val _columnIndexOfPinHash: Int = getColumnIndexOrThrow(_stmt, "pinHash")
        val _columnIndexOfPinSalt: Int = getColumnIndexOrThrow(_stmt, "pinSalt")
        val _columnIndexOfAutoplayNext: Int = getColumnIndexOrThrow(_stmt, "autoplayNext")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _result: ProfileEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpAvatarId: Int
          _tmpAvatarId = _stmt.getLong(_columnIndexOfAvatarId).toInt()
          val _tmpIsKids: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsKids).toInt()
          _tmpIsKids = _tmp != 0
          val _tmpMaturity: String
          _tmpMaturity = _stmt.getText(_columnIndexOfMaturity)
          val _tmpLanguage: String
          _tmpLanguage = _stmt.getText(_columnIndexOfLanguage)
          val _tmpSubtitleLanguage: String?
          if (_stmt.isNull(_columnIndexOfSubtitleLanguage)) {
            _tmpSubtitleLanguage = null
          } else {
            _tmpSubtitleLanguage = _stmt.getText(_columnIndexOfSubtitleLanguage)
          }
          val _tmpAudioLanguage: String?
          if (_stmt.isNull(_columnIndexOfAudioLanguage)) {
            _tmpAudioLanguage = null
          } else {
            _tmpAudioLanguage = _stmt.getText(_columnIndexOfAudioLanguage)
          }
          val _tmpPinHash: String?
          if (_stmt.isNull(_columnIndexOfPinHash)) {
            _tmpPinHash = null
          } else {
            _tmpPinHash = _stmt.getText(_columnIndexOfPinHash)
          }
          val _tmpPinSalt: String?
          if (_stmt.isNull(_columnIndexOfPinSalt)) {
            _tmpPinSalt = null
          } else {
            _tmpPinSalt = _stmt.getText(_columnIndexOfPinSalt)
          }
          val _tmpAutoplayNext: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfAutoplayNext).toInt()
          _tmpAutoplayNext = _tmp_1 != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _result = ProfileEntity(_tmpId,_tmpName,_tmpAvatarId,_tmpIsKids,_tmpMaturity,_tmpLanguage,_tmpSubtitleLanguage,_tmpAudioLanguage,_tmpPinHash,_tmpPinSalt,_tmpAutoplayNext,_tmpCreatedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun count(): Int {
    val _sql: String = "SELECT COUNT(*) FROM profile"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun delete(id: Long) {
    val _sql: String = "DELETE FROM profile WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
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
