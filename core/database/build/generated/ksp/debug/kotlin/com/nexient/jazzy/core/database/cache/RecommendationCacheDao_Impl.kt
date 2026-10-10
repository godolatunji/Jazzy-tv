package com.nexient.jazzy.core.database.cache

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
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
public class RecommendationCacheDao_Impl(
  __db: RoomDatabase,
) : RecommendationCacheDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfRecommendationCacheEntity:
      EntityInsertAdapter<RecommendationCacheEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfRecommendationCacheEntity = object : EntityInsertAdapter<RecommendationCacheEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `recommendation_cache` (`profileId`,`railKey`,`title`,`json`,`generatedAt`) VALUES (?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: RecommendationCacheEntity) {
        statement.bindLong(1, entity.profileId)
        statement.bindText(2, entity.railKey)
        statement.bindText(3, entity.title)
        statement.bindText(4, entity.json)
        statement.bindLong(5, entity.generatedAt)
      }
    }
  }

  public override suspend fun insert(rows: List<RecommendationCacheEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfRecommendationCacheEntity.insert(_connection, rows)
  }

  public override suspend fun replace(profileId: Long, rows: List<RecommendationCacheEntity>): Unit = performInTransactionSuspending(__db) {
    super@RecommendationCacheDao_Impl.replace(profileId, rows)
  }

  public override fun observe(profileId: Long): Flow<List<RecommendationCacheEntity>> {
    val _sql: String = "SELECT * FROM recommendation_cache WHERE profileId = ?"
    return createFlow(__db, false, arrayOf("recommendation_cache")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, profileId)
        val _columnIndexOfProfileId: Int = getColumnIndexOrThrow(_stmt, "profileId")
        val _columnIndexOfRailKey: Int = getColumnIndexOrThrow(_stmt, "railKey")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfJson: Int = getColumnIndexOrThrow(_stmt, "json")
        val _columnIndexOfGeneratedAt: Int = getColumnIndexOrThrow(_stmt, "generatedAt")
        val _result: MutableList<RecommendationCacheEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: RecommendationCacheEntity
          val _tmpProfileId: Long
          _tmpProfileId = _stmt.getLong(_columnIndexOfProfileId)
          val _tmpRailKey: String
          _tmpRailKey = _stmt.getText(_columnIndexOfRailKey)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpJson: String
          _tmpJson = _stmt.getText(_columnIndexOfJson)
          val _tmpGeneratedAt: Long
          _tmpGeneratedAt = _stmt.getLong(_columnIndexOfGeneratedAt)
          _item = RecommendationCacheEntity(_tmpProfileId,_tmpRailKey,_tmpTitle,_tmpJson,_tmpGeneratedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun lastGenerated(profileId: Long): Long? {
    val _sql: String = "SELECT MAX(generatedAt) FROM recommendation_cache WHERE profileId = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, profileId)
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

  public override suspend fun clear(profileId: Long) {
    val _sql: String = "DELETE FROM recommendation_cache WHERE profileId = ?"
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
