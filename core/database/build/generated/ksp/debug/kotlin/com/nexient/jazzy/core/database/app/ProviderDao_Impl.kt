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
public class ProviderDao_Impl(
  __db: RoomDatabase,
) : ProviderDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfOriginalsNetworkEntity: EntityInsertAdapter<OriginalsNetworkEntity>

  private val __insertAdapterOfPublicTrackerEntity: EntityInsertAdapter<PublicTrackerEntity>

  private val __upsertAdapterOfProviderSettingEntity: EntityUpsertAdapter<ProviderSettingEntity>

  private val __upsertAdapterOfSourceFlagEntity: EntityUpsertAdapter<SourceFlagEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfOriginalsNetworkEntity = object : EntityInsertAdapter<OriginalsNetworkEntity>() {
      protected override fun createQuery(): String = "INSERT OR IGNORE INTO `originals_network` (`networkId`,`name`,`mediaType`,`enabled`,`sortOrder`) VALUES (?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: OriginalsNetworkEntity) {
        statement.bindLong(1, entity.networkId.toLong())
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.mediaType)
        val _tmp: Int = if (entity.enabled) 1 else 0
        statement.bindLong(4, _tmp.toLong())
        statement.bindLong(5, entity.sortOrder.toLong())
      }
    }
    this.__insertAdapterOfPublicTrackerEntity = object : EntityInsertAdapter<PublicTrackerEntity>() {
      protected override fun createQuery(): String = "INSERT OR IGNORE INTO `public_tracker` (`url`,`enabled`) VALUES (?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: PublicTrackerEntity) {
        statement.bindText(1, entity.url)
        val _tmp: Int = if (entity.enabled) 1 else 0
        statement.bindLong(2, _tmp.toLong())
      }
    }
    this.__upsertAdapterOfProviderSettingEntity = EntityUpsertAdapter<ProviderSettingEntity>(object : EntityInsertAdapter<ProviderSettingEntity>() {
      protected override fun createQuery(): String = "INSERT INTO `provider_setting` (`providerId`,`enabled`,`baseUrlOverride`,`username`,`passwordEncrypted`,`cookiesEncrypted`,`lastOkAt`,`avgLatencyMs`,`errorCount`,`lastError`) VALUES (?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ProviderSettingEntity) {
        statement.bindText(1, entity.providerId)
        val _tmpEnabled: Boolean? = entity.enabled
        val _tmp: Int? = _tmpEnabled?.let { if (it) 1 else 0 }
        if (_tmp == null) {
          statement.bindNull(2)
        } else {
          statement.bindLong(2, _tmp.toLong())
        }
        val _tmpBaseUrlOverride: String? = entity.baseUrlOverride
        if (_tmpBaseUrlOverride == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmpBaseUrlOverride)
        }
        val _tmpUsername: String? = entity.username
        if (_tmpUsername == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpUsername)
        }
        val _tmpPasswordEncrypted: String? = entity.passwordEncrypted
        if (_tmpPasswordEncrypted == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmpPasswordEncrypted)
        }
        val _tmpCookiesEncrypted: String? = entity.cookiesEncrypted
        if (_tmpCookiesEncrypted == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpCookiesEncrypted)
        }
        val _tmpLastOkAt: Long? = entity.lastOkAt
        if (_tmpLastOkAt == null) {
          statement.bindNull(7)
        } else {
          statement.bindLong(7, _tmpLastOkAt)
        }
        val _tmpAvgLatencyMs: Long? = entity.avgLatencyMs
        if (_tmpAvgLatencyMs == null) {
          statement.bindNull(8)
        } else {
          statement.bindLong(8, _tmpAvgLatencyMs)
        }
        statement.bindLong(9, entity.errorCount.toLong())
        val _tmpLastError: String? = entity.lastError
        if (_tmpLastError == null) {
          statement.bindNull(10)
        } else {
          statement.bindText(10, _tmpLastError)
        }
      }
    }, object : EntityDeleteOrUpdateAdapter<ProviderSettingEntity>() {
      protected override fun createQuery(): String = "UPDATE `provider_setting` SET `providerId` = ?,`enabled` = ?,`baseUrlOverride` = ?,`username` = ?,`passwordEncrypted` = ?,`cookiesEncrypted` = ?,`lastOkAt` = ?,`avgLatencyMs` = ?,`errorCount` = ?,`lastError` = ? WHERE `providerId` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: ProviderSettingEntity) {
        statement.bindText(1, entity.providerId)
        val _tmpEnabled: Boolean? = entity.enabled
        val _tmp: Int? = _tmpEnabled?.let { if (it) 1 else 0 }
        if (_tmp == null) {
          statement.bindNull(2)
        } else {
          statement.bindLong(2, _tmp.toLong())
        }
        val _tmpBaseUrlOverride: String? = entity.baseUrlOverride
        if (_tmpBaseUrlOverride == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmpBaseUrlOverride)
        }
        val _tmpUsername: String? = entity.username
        if (_tmpUsername == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpUsername)
        }
        val _tmpPasswordEncrypted: String? = entity.passwordEncrypted
        if (_tmpPasswordEncrypted == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmpPasswordEncrypted)
        }
        val _tmpCookiesEncrypted: String? = entity.cookiesEncrypted
        if (_tmpCookiesEncrypted == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpCookiesEncrypted)
        }
        val _tmpLastOkAt: Long? = entity.lastOkAt
        if (_tmpLastOkAt == null) {
          statement.bindNull(7)
        } else {
          statement.bindLong(7, _tmpLastOkAt)
        }
        val _tmpAvgLatencyMs: Long? = entity.avgLatencyMs
        if (_tmpAvgLatencyMs == null) {
          statement.bindNull(8)
        } else {
          statement.bindLong(8, _tmpAvgLatencyMs)
        }
        statement.bindLong(9, entity.errorCount.toLong())
        val _tmpLastError: String? = entity.lastError
        if (_tmpLastError == null) {
          statement.bindNull(10)
        } else {
          statement.bindText(10, _tmpLastError)
        }
        statement.bindText(11, entity.providerId)
      }
    })
    this.__upsertAdapterOfSourceFlagEntity = EntityUpsertAdapter<SourceFlagEntity>(object : EntityInsertAdapter<SourceFlagEntity>() {
      protected override fun createQuery(): String = "INSERT INTO `source_flag` (`infoHash`,`badUntil`,`reason`) VALUES (?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: SourceFlagEntity) {
        statement.bindText(1, entity.infoHash)
        statement.bindLong(2, entity.badUntil)
        statement.bindText(3, entity.reason)
      }
    }, object : EntityDeleteOrUpdateAdapter<SourceFlagEntity>() {
      protected override fun createQuery(): String = "UPDATE `source_flag` SET `infoHash` = ?,`badUntil` = ?,`reason` = ? WHERE `infoHash` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: SourceFlagEntity) {
        statement.bindText(1, entity.infoHash)
        statement.bindLong(2, entity.badUntil)
        statement.bindText(3, entity.reason)
        statement.bindText(4, entity.infoHash)
      }
    })
  }

  public override suspend fun insertOriginals(rows: List<OriginalsNetworkEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfOriginalsNetworkEntity.insert(_connection, rows)
  }

  public override suspend fun insertTrackers(rows: List<PublicTrackerEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfPublicTrackerEntity.insert(_connection, rows)
  }

  public override suspend fun upsert(entity: ProviderSettingEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __upsertAdapterOfProviderSettingEntity.upsert(_connection, entity)
  }

  public override suspend fun flag(entity: SourceFlagEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __upsertAdapterOfSourceFlagEntity.upsert(_connection, entity)
  }

  public override fun observeAll(): Flow<List<ProviderSettingEntity>> {
    val _sql: String = "SELECT * FROM provider_setting"
    return createFlow(__db, false, arrayOf("provider_setting")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfProviderId: Int = getColumnIndexOrThrow(_stmt, "providerId")
        val _columnIndexOfEnabled: Int = getColumnIndexOrThrow(_stmt, "enabled")
        val _columnIndexOfBaseUrlOverride: Int = getColumnIndexOrThrow(_stmt, "baseUrlOverride")
        val _columnIndexOfUsername: Int = getColumnIndexOrThrow(_stmt, "username")
        val _columnIndexOfPasswordEncrypted: Int = getColumnIndexOrThrow(_stmt, "passwordEncrypted")
        val _columnIndexOfCookiesEncrypted: Int = getColumnIndexOrThrow(_stmt, "cookiesEncrypted")
        val _columnIndexOfLastOkAt: Int = getColumnIndexOrThrow(_stmt, "lastOkAt")
        val _columnIndexOfAvgLatencyMs: Int = getColumnIndexOrThrow(_stmt, "avgLatencyMs")
        val _columnIndexOfErrorCount: Int = getColumnIndexOrThrow(_stmt, "errorCount")
        val _columnIndexOfLastError: Int = getColumnIndexOrThrow(_stmt, "lastError")
        val _result: MutableList<ProviderSettingEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ProviderSettingEntity
          val _tmpProviderId: String
          _tmpProviderId = _stmt.getText(_columnIndexOfProviderId)
          val _tmpEnabled: Boolean?
          val _tmp: Int?
          if (_stmt.isNull(_columnIndexOfEnabled)) {
            _tmp = null
          } else {
            _tmp = _stmt.getLong(_columnIndexOfEnabled).toInt()
          }
          _tmpEnabled = _tmp?.let { it != 0 }
          val _tmpBaseUrlOverride: String?
          if (_stmt.isNull(_columnIndexOfBaseUrlOverride)) {
            _tmpBaseUrlOverride = null
          } else {
            _tmpBaseUrlOverride = _stmt.getText(_columnIndexOfBaseUrlOverride)
          }
          val _tmpUsername: String?
          if (_stmt.isNull(_columnIndexOfUsername)) {
            _tmpUsername = null
          } else {
            _tmpUsername = _stmt.getText(_columnIndexOfUsername)
          }
          val _tmpPasswordEncrypted: String?
          if (_stmt.isNull(_columnIndexOfPasswordEncrypted)) {
            _tmpPasswordEncrypted = null
          } else {
            _tmpPasswordEncrypted = _stmt.getText(_columnIndexOfPasswordEncrypted)
          }
          val _tmpCookiesEncrypted: String?
          if (_stmt.isNull(_columnIndexOfCookiesEncrypted)) {
            _tmpCookiesEncrypted = null
          } else {
            _tmpCookiesEncrypted = _stmt.getText(_columnIndexOfCookiesEncrypted)
          }
          val _tmpLastOkAt: Long?
          if (_stmt.isNull(_columnIndexOfLastOkAt)) {
            _tmpLastOkAt = null
          } else {
            _tmpLastOkAt = _stmt.getLong(_columnIndexOfLastOkAt)
          }
          val _tmpAvgLatencyMs: Long?
          if (_stmt.isNull(_columnIndexOfAvgLatencyMs)) {
            _tmpAvgLatencyMs = null
          } else {
            _tmpAvgLatencyMs = _stmt.getLong(_columnIndexOfAvgLatencyMs)
          }
          val _tmpErrorCount: Int
          _tmpErrorCount = _stmt.getLong(_columnIndexOfErrorCount).toInt()
          val _tmpLastError: String?
          if (_stmt.isNull(_columnIndexOfLastError)) {
            _tmpLastError = null
          } else {
            _tmpLastError = _stmt.getText(_columnIndexOfLastError)
          }
          _item = ProviderSettingEntity(_tmpProviderId,_tmpEnabled,_tmpBaseUrlOverride,_tmpUsername,_tmpPasswordEncrypted,_tmpCookiesEncrypted,_tmpLastOkAt,_tmpAvgLatencyMs,_tmpErrorCount,_tmpLastError)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun `get`(providerId: String): ProviderSettingEntity? {
    val _sql: String = "SELECT * FROM provider_setting WHERE providerId = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, providerId)
        val _columnIndexOfProviderId: Int = getColumnIndexOrThrow(_stmt, "providerId")
        val _columnIndexOfEnabled: Int = getColumnIndexOrThrow(_stmt, "enabled")
        val _columnIndexOfBaseUrlOverride: Int = getColumnIndexOrThrow(_stmt, "baseUrlOverride")
        val _columnIndexOfUsername: Int = getColumnIndexOrThrow(_stmt, "username")
        val _columnIndexOfPasswordEncrypted: Int = getColumnIndexOrThrow(_stmt, "passwordEncrypted")
        val _columnIndexOfCookiesEncrypted: Int = getColumnIndexOrThrow(_stmt, "cookiesEncrypted")
        val _columnIndexOfLastOkAt: Int = getColumnIndexOrThrow(_stmt, "lastOkAt")
        val _columnIndexOfAvgLatencyMs: Int = getColumnIndexOrThrow(_stmt, "avgLatencyMs")
        val _columnIndexOfErrorCount: Int = getColumnIndexOrThrow(_stmt, "errorCount")
        val _columnIndexOfLastError: Int = getColumnIndexOrThrow(_stmt, "lastError")
        val _result: ProviderSettingEntity?
        if (_stmt.step()) {
          val _tmpProviderId: String
          _tmpProviderId = _stmt.getText(_columnIndexOfProviderId)
          val _tmpEnabled: Boolean?
          val _tmp: Int?
          if (_stmt.isNull(_columnIndexOfEnabled)) {
            _tmp = null
          } else {
            _tmp = _stmt.getLong(_columnIndexOfEnabled).toInt()
          }
          _tmpEnabled = _tmp?.let { it != 0 }
          val _tmpBaseUrlOverride: String?
          if (_stmt.isNull(_columnIndexOfBaseUrlOverride)) {
            _tmpBaseUrlOverride = null
          } else {
            _tmpBaseUrlOverride = _stmt.getText(_columnIndexOfBaseUrlOverride)
          }
          val _tmpUsername: String?
          if (_stmt.isNull(_columnIndexOfUsername)) {
            _tmpUsername = null
          } else {
            _tmpUsername = _stmt.getText(_columnIndexOfUsername)
          }
          val _tmpPasswordEncrypted: String?
          if (_stmt.isNull(_columnIndexOfPasswordEncrypted)) {
            _tmpPasswordEncrypted = null
          } else {
            _tmpPasswordEncrypted = _stmt.getText(_columnIndexOfPasswordEncrypted)
          }
          val _tmpCookiesEncrypted: String?
          if (_stmt.isNull(_columnIndexOfCookiesEncrypted)) {
            _tmpCookiesEncrypted = null
          } else {
            _tmpCookiesEncrypted = _stmt.getText(_columnIndexOfCookiesEncrypted)
          }
          val _tmpLastOkAt: Long?
          if (_stmt.isNull(_columnIndexOfLastOkAt)) {
            _tmpLastOkAt = null
          } else {
            _tmpLastOkAt = _stmt.getLong(_columnIndexOfLastOkAt)
          }
          val _tmpAvgLatencyMs: Long?
          if (_stmt.isNull(_columnIndexOfAvgLatencyMs)) {
            _tmpAvgLatencyMs = null
          } else {
            _tmpAvgLatencyMs = _stmt.getLong(_columnIndexOfAvgLatencyMs)
          }
          val _tmpErrorCount: Int
          _tmpErrorCount = _stmt.getLong(_columnIndexOfErrorCount).toInt()
          val _tmpLastError: String?
          if (_stmt.isNull(_columnIndexOfLastError)) {
            _tmpLastError = null
          } else {
            _tmpLastError = _stmt.getText(_columnIndexOfLastError)
          }
          _result = ProviderSettingEntity(_tmpProviderId,_tmpEnabled,_tmpBaseUrlOverride,_tmpUsername,_tmpPasswordEncrypted,_tmpCookiesEncrypted,_tmpLastOkAt,_tmpAvgLatencyMs,_tmpErrorCount,_tmpLastError)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeActiveFlags(now: Long): Flow<List<SourceFlagEntity>> {
    val _sql: String = "SELECT * FROM source_flag WHERE badUntil > ?"
    return createFlow(__db, false, arrayOf("source_flag")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, now)
        val _columnIndexOfInfoHash: Int = getColumnIndexOrThrow(_stmt, "infoHash")
        val _columnIndexOfBadUntil: Int = getColumnIndexOrThrow(_stmt, "badUntil")
        val _columnIndexOfReason: Int = getColumnIndexOrThrow(_stmt, "reason")
        val _result: MutableList<SourceFlagEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: SourceFlagEntity
          val _tmpInfoHash: String
          _tmpInfoHash = _stmt.getText(_columnIndexOfInfoHash)
          val _tmpBadUntil: Long
          _tmpBadUntil = _stmt.getLong(_columnIndexOfBadUntil)
          val _tmpReason: String
          _tmpReason = _stmt.getText(_columnIndexOfReason)
          _item = SourceFlagEntity(_tmpInfoHash,_tmpBadUntil,_tmpReason)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeOriginals(): Flow<List<OriginalsNetworkEntity>> {
    val _sql: String = "SELECT * FROM originals_network ORDER BY sortOrder"
    return createFlow(__db, false, arrayOf("originals_network")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfNetworkId: Int = getColumnIndexOrThrow(_stmt, "networkId")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfMediaType: Int = getColumnIndexOrThrow(_stmt, "mediaType")
        val _columnIndexOfEnabled: Int = getColumnIndexOrThrow(_stmt, "enabled")
        val _columnIndexOfSortOrder: Int = getColumnIndexOrThrow(_stmt, "sortOrder")
        val _result: MutableList<OriginalsNetworkEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: OriginalsNetworkEntity
          val _tmpNetworkId: Int
          _tmpNetworkId = _stmt.getLong(_columnIndexOfNetworkId).toInt()
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpMediaType: String
          _tmpMediaType = _stmt.getText(_columnIndexOfMediaType)
          val _tmpEnabled: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfEnabled).toInt()
          _tmpEnabled = _tmp != 0
          val _tmpSortOrder: Int
          _tmpSortOrder = _stmt.getLong(_columnIndexOfSortOrder).toInt()
          _item = OriginalsNetworkEntity(_tmpNetworkId,_tmpName,_tmpMediaType,_tmpEnabled,_tmpSortOrder)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun enabledTrackers(): List<PublicTrackerEntity> {
    val _sql: String = "SELECT * FROM public_tracker WHERE enabled = 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfUrl: Int = getColumnIndexOrThrow(_stmt, "url")
        val _columnIndexOfEnabled: Int = getColumnIndexOrThrow(_stmt, "enabled")
        val _result: MutableList<PublicTrackerEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: PublicTrackerEntity
          val _tmpUrl: String
          _tmpUrl = _stmt.getText(_columnIndexOfUrl)
          val _tmpEnabled: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfEnabled).toInt()
          _tmpEnabled = _tmp != 0
          _item = PublicTrackerEntity(_tmpUrl,_tmpEnabled)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
