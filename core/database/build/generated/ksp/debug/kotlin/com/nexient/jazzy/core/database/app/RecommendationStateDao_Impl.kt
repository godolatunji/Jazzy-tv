package com.nexient.jazzy.core.database.app

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performInTransactionSuspending
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Double
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
public class RecommendationStateDao_Impl(
  __db: RoomDatabase,
) : RecommendationStateDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfSeedGenreEntity: EntityInsertAdapter<SeedGenreEntity>

  private val __insertAdapterOfGenreAffinityEntity: EntityInsertAdapter<GenreAffinityEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfSeedGenreEntity = object : EntityInsertAdapter<SeedGenreEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `seed_genre` (`profileId`,`unifiedGenreId`,`addedAt`) VALUES (?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: SeedGenreEntity) {
        statement.bindLong(1, entity.profileId)
        statement.bindText(2, entity.unifiedGenreId)
        statement.bindLong(3, entity.addedAt)
      }
    }
    this.__insertAdapterOfGenreAffinityEntity = object : EntityInsertAdapter<GenreAffinityEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `genre_affinity` (`profileId`,`unifiedGenreId`,`score`,`updatedAt`) VALUES (?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: GenreAffinityEntity) {
        statement.bindLong(1, entity.profileId)
        statement.bindText(2, entity.unifiedGenreId)
        statement.bindDouble(3, entity.score)
        statement.bindLong(4, entity.updatedAt)
      }
    }
  }

  public override suspend fun insertSeeds(seeds: List<SeedGenreEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfSeedGenreEntity.insert(_connection, seeds)
  }

  public override suspend fun insertAffinity(rows: List<GenreAffinityEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfGenreAffinityEntity.insert(_connection, rows)
  }

  public override suspend fun replaceSeeds(profileId: Long, seeds: List<SeedGenreEntity>): Unit = performInTransactionSuspending(__db) {
    super@RecommendationStateDao_Impl.replaceSeeds(profileId, seeds)
  }

  public override suspend fun replaceAffinity(profileId: Long, rows: List<GenreAffinityEntity>): Unit = performInTransactionSuspending(__db) {
    super@RecommendationStateDao_Impl.replaceAffinity(profileId, rows)
  }

  public override suspend fun seeds(profileId: Long): List<SeedGenreEntity> {
    val _sql: String = "SELECT * FROM seed_genre WHERE profileId = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, profileId)
        val _columnIndexOfProfileId: Int = getColumnIndexOrThrow(_stmt, "profileId")
        val _columnIndexOfUnifiedGenreId: Int = getColumnIndexOrThrow(_stmt, "unifiedGenreId")
        val _columnIndexOfAddedAt: Int = getColumnIndexOrThrow(_stmt, "addedAt")
        val _result: MutableList<SeedGenreEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: SeedGenreEntity
          val _tmpProfileId: Long
          _tmpProfileId = _stmt.getLong(_columnIndexOfProfileId)
          val _tmpUnifiedGenreId: String
          _tmpUnifiedGenreId = _stmt.getText(_columnIndexOfUnifiedGenreId)
          val _tmpAddedAt: Long
          _tmpAddedAt = _stmt.getLong(_columnIndexOfAddedAt)
          _item = SeedGenreEntity(_tmpProfileId,_tmpUnifiedGenreId,_tmpAddedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeSeeds(profileId: Long): Flow<List<SeedGenreEntity>> {
    val _sql: String = "SELECT * FROM seed_genre WHERE profileId = ?"
    return createFlow(__db, false, arrayOf("seed_genre")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, profileId)
        val _columnIndexOfProfileId: Int = getColumnIndexOrThrow(_stmt, "profileId")
        val _columnIndexOfUnifiedGenreId: Int = getColumnIndexOrThrow(_stmt, "unifiedGenreId")
        val _columnIndexOfAddedAt: Int = getColumnIndexOrThrow(_stmt, "addedAt")
        val _result: MutableList<SeedGenreEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: SeedGenreEntity
          val _tmpProfileId: Long
          _tmpProfileId = _stmt.getLong(_columnIndexOfProfileId)
          val _tmpUnifiedGenreId: String
          _tmpUnifiedGenreId = _stmt.getText(_columnIndexOfUnifiedGenreId)
          val _tmpAddedAt: Long
          _tmpAddedAt = _stmt.getLong(_columnIndexOfAddedAt)
          _item = SeedGenreEntity(_tmpProfileId,_tmpUnifiedGenreId,_tmpAddedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun affinity(profileId: Long): List<GenreAffinityEntity> {
    val _sql: String = "SELECT * FROM genre_affinity WHERE profileId = ? ORDER BY score DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, profileId)
        val _columnIndexOfProfileId: Int = getColumnIndexOrThrow(_stmt, "profileId")
        val _columnIndexOfUnifiedGenreId: Int = getColumnIndexOrThrow(_stmt, "unifiedGenreId")
        val _columnIndexOfScore: Int = getColumnIndexOrThrow(_stmt, "score")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<GenreAffinityEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: GenreAffinityEntity
          val _tmpProfileId: Long
          _tmpProfileId = _stmt.getLong(_columnIndexOfProfileId)
          val _tmpUnifiedGenreId: String
          _tmpUnifiedGenreId = _stmt.getText(_columnIndexOfUnifiedGenreId)
          val _tmpScore: Double
          _tmpScore = _stmt.getDouble(_columnIndexOfScore)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item = GenreAffinityEntity(_tmpProfileId,_tmpUnifiedGenreId,_tmpScore,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clearSeeds(profileId: Long) {
    val _sql: String = "DELETE FROM seed_genre WHERE profileId = ?"
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

  public override suspend fun clearAffinity(profileId: Long) {
    val _sql: String = "DELETE FROM genre_affinity WHERE profileId = ?"
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
