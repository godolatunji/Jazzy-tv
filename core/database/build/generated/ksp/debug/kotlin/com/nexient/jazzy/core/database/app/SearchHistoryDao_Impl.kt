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
public class SearchHistoryDao_Impl(
  __db: RoomDatabase,
) : SearchHistoryDao {
  private val __db: RoomDatabase

  private val __upsertAdapterOfSearchHistoryEntity: EntityUpsertAdapter<SearchHistoryEntity>
  init {
    this.__db = __db
    this.__upsertAdapterOfSearchHistoryEntity = EntityUpsertAdapter<SearchHistoryEntity>(object : EntityInsertAdapter<SearchHistoryEntity>() {
      protected override fun createQuery(): String = "INSERT INTO `search_history` (`profileId`,`query`,`searchedAt`) VALUES (?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: SearchHistoryEntity) {
        statement.bindLong(1, entity.profileId)
        statement.bindText(2, entity.query)
        statement.bindLong(3, entity.searchedAt)
      }
    }, object : EntityDeleteOrUpdateAdapter<SearchHistoryEntity>() {
      protected override fun createQuery(): String = "UPDATE `search_history` SET `profileId` = ?,`query` = ?,`searchedAt` = ? WHERE `profileId` = ? AND `query` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: SearchHistoryEntity) {
        statement.bindLong(1, entity.profileId)
        statement.bindText(2, entity.query)
        statement.bindLong(3, entity.searchedAt)
        statement.bindLong(4, entity.profileId)
        statement.bindText(5, entity.query)
      }
    })
  }

  public override suspend fun upsert(entry: SearchHistoryEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __upsertAdapterOfSearchHistoryEntity.upsert(_connection, entry)
  }

  public override fun observeRecent(profileId: Long, limit: Int): Flow<List<SearchHistoryEntity>> {
    val _sql: String = "SELECT * FROM search_history WHERE profileId = ? ORDER BY searchedAt DESC LIMIT ?"
    return createFlow(__db, false, arrayOf("search_history")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, profileId)
        _argIndex = 2
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfProfileId: Int = getColumnIndexOrThrow(_stmt, "profileId")
        val _columnIndexOfQuery: Int = getColumnIndexOrThrow(_stmt, "query")
        val _columnIndexOfSearchedAt: Int = getColumnIndexOrThrow(_stmt, "searchedAt")
        val _result: MutableList<SearchHistoryEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: SearchHistoryEntity
          val _tmpProfileId: Long
          _tmpProfileId = _stmt.getLong(_columnIndexOfProfileId)
          val _tmpQuery: String
          _tmpQuery = _stmt.getText(_columnIndexOfQuery)
          val _tmpSearchedAt: Long
          _tmpSearchedAt = _stmt.getLong(_columnIndexOfSearchedAt)
          _item = SearchHistoryEntity(_tmpProfileId,_tmpQuery,_tmpSearchedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clear(profileId: Long) {
    val _sql: String = "DELETE FROM search_history WHERE profileId = ?"
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

  public override suspend fun delete(profileId: Long, query: String) {
    val _sql: String = "DELETE FROM search_history WHERE profileId = ? AND query = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, profileId)
        _argIndex = 2
        _stmt.bindText(_argIndex, query)
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
