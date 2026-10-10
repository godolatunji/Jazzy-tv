package com.nexient.jazzy.core.database.cache

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.EntityUpsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.getTotalChangedRows
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
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
public class SourceCacheDao_Impl(
  __db: RoomDatabase,
) : SourceCacheDao {
  private val __db: RoomDatabase

  private val __upsertAdapterOfSourceResultEntity: EntityUpsertAdapter<SourceResultEntity>
  init {
    this.__db = __db
    this.__upsertAdapterOfSourceResultEntity = EntityUpsertAdapter<SourceResultEntity>(object : EntityInsertAdapter<SourceResultEntity>() {
      protected override fun createQuery(): String = "INSERT INTO `source_result` (`queryKey`,`json`,`fetchedAt`) VALUES (?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: SourceResultEntity) {
        statement.bindText(1, entity.queryKey)
        statement.bindText(2, entity.json)
        statement.bindLong(3, entity.fetchedAt)
      }
    }, object : EntityDeleteOrUpdateAdapter<SourceResultEntity>() {
      protected override fun createQuery(): String = "UPDATE `source_result` SET `queryKey` = ?,`json` = ?,`fetchedAt` = ? WHERE `queryKey` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: SourceResultEntity) {
        statement.bindText(1, entity.queryKey)
        statement.bindText(2, entity.json)
        statement.bindLong(3, entity.fetchedAt)
        statement.bindText(4, entity.queryKey)
      }
    })
  }

  public override suspend fun put(entity: SourceResultEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __upsertAdapterOfSourceResultEntity.upsert(_connection, entity)
  }

  public override suspend fun `get`(queryKey: String): SourceResultEntity? {
    val _sql: String = "SELECT * FROM source_result WHERE queryKey = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, queryKey)
        val _columnIndexOfQueryKey: Int = getColumnIndexOrThrow(_stmt, "queryKey")
        val _columnIndexOfJson: Int = getColumnIndexOrThrow(_stmt, "json")
        val _columnIndexOfFetchedAt: Int = getColumnIndexOrThrow(_stmt, "fetchedAt")
        val _result: SourceResultEntity?
        if (_stmt.step()) {
          val _tmpQueryKey: String
          _tmpQueryKey = _stmt.getText(_columnIndexOfQueryKey)
          val _tmpJson: String
          _tmpJson = _stmt.getText(_columnIndexOfJson)
          val _tmpFetchedAt: Long
          _tmpFetchedAt = _stmt.getLong(_columnIndexOfFetchedAt)
          _result = SourceResultEntity(_tmpQueryKey,_tmpJson,_tmpFetchedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun evict(before: Long): Int {
    val _sql: String = "DELETE FROM source_result WHERE fetchedAt < ?"
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
