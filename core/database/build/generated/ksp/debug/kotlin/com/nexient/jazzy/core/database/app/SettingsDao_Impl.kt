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
public class SettingsDao_Impl(
  __db: RoomDatabase,
) : SettingsDao {
  private val __db: RoomDatabase

  private val __upsertAdapterOfAppSettingEntity: EntityUpsertAdapter<AppSettingEntity>

  private val __upsertAdapterOfProfileSettingEntity: EntityUpsertAdapter<ProfileSettingEntity>
  init {
    this.__db = __db
    this.__upsertAdapterOfAppSettingEntity = EntityUpsertAdapter<AppSettingEntity>(object : EntityInsertAdapter<AppSettingEntity>() {
      protected override fun createQuery(): String = "INSERT INTO `app_setting` (`key`,`value`) VALUES (?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: AppSettingEntity) {
        statement.bindText(1, entity.key)
        statement.bindText(2, entity.value)
      }
    }, object : EntityDeleteOrUpdateAdapter<AppSettingEntity>() {
      protected override fun createQuery(): String = "UPDATE `app_setting` SET `key` = ?,`value` = ? WHERE `key` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: AppSettingEntity) {
        statement.bindText(1, entity.key)
        statement.bindText(2, entity.value)
        statement.bindText(3, entity.key)
      }
    })
    this.__upsertAdapterOfProfileSettingEntity = EntityUpsertAdapter<ProfileSettingEntity>(object : EntityInsertAdapter<ProfileSettingEntity>() {
      protected override fun createQuery(): String = "INSERT INTO `profile_setting` (`profileId`,`key`,`value`) VALUES (?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ProfileSettingEntity) {
        statement.bindLong(1, entity.profileId)
        statement.bindText(2, entity.key)
        statement.bindText(3, entity.value)
      }
    }, object : EntityDeleteOrUpdateAdapter<ProfileSettingEntity>() {
      protected override fun createQuery(): String = "UPDATE `profile_setting` SET `profileId` = ?,`key` = ?,`value` = ? WHERE `profileId` = ? AND `key` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: ProfileSettingEntity) {
        statement.bindLong(1, entity.profileId)
        statement.bindText(2, entity.key)
        statement.bindText(3, entity.value)
        statement.bindLong(4, entity.profileId)
        statement.bindText(5, entity.key)
      }
    })
  }

  public override suspend fun putApp(setting: AppSettingEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __upsertAdapterOfAppSettingEntity.upsert(_connection, setting)
  }

  public override suspend fun putProfile(setting: ProfileSettingEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __upsertAdapterOfProfileSettingEntity.upsert(_connection, setting)
  }

  public override fun observeApp(key: String): Flow<String?> {
    val _sql: String = "SELECT value FROM app_setting WHERE `key` = ?"
    return createFlow(__db, false, arrayOf("app_setting")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, key)
        val _result: String?
        if (_stmt.step()) {
          if (_stmt.isNull(0)) {
            _result = null
          } else {
            _result = _stmt.getText(0)
          }
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeAllApp(): Flow<List<AppSettingEntity>> {
    val _sql: String = "SELECT * FROM app_setting"
    return createFlow(__db, false, arrayOf("app_setting")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfKey: Int = getColumnIndexOrThrow(_stmt, "key")
        val _columnIndexOfValue: Int = getColumnIndexOrThrow(_stmt, "value")
        val _result: MutableList<AppSettingEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: AppSettingEntity
          val _tmpKey: String
          _tmpKey = _stmt.getText(_columnIndexOfKey)
          val _tmpValue: String
          _tmpValue = _stmt.getText(_columnIndexOfValue)
          _item = AppSettingEntity(_tmpKey,_tmpValue)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getApp(key: String): String? {
    val _sql: String = "SELECT value FROM app_setting WHERE `key` = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, key)
        val _result: String?
        if (_stmt.step()) {
          if (_stmt.isNull(0)) {
            _result = null
          } else {
            _result = _stmt.getText(0)
          }
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeProfile(profileId: Long): Flow<List<ProfileSettingEntity>> {
    val _sql: String = "SELECT * FROM profile_setting WHERE profileId = ?"
    return createFlow(__db, false, arrayOf("profile_setting")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, profileId)
        val _columnIndexOfProfileId: Int = getColumnIndexOrThrow(_stmt, "profileId")
        val _columnIndexOfKey: Int = getColumnIndexOrThrow(_stmt, "key")
        val _columnIndexOfValue: Int = getColumnIndexOrThrow(_stmt, "value")
        val _result: MutableList<ProfileSettingEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ProfileSettingEntity
          val _tmpProfileId: Long
          _tmpProfileId = _stmt.getLong(_columnIndexOfProfileId)
          val _tmpKey: String
          _tmpKey = _stmt.getText(_columnIndexOfKey)
          val _tmpValue: String
          _tmpValue = _stmt.getText(_columnIndexOfValue)
          _item = ProfileSettingEntity(_tmpProfileId,_tmpKey,_tmpValue)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun removeApp(key: String) {
    val _sql: String = "DELETE FROM app_setting WHERE `key` = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, key)
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
