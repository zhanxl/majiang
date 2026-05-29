package com.majiang.repository.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.majiang.repository.entity.GameRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: GameRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<GameRecordEntity>)

    @Delete
    suspend fun delete(record: GameRecordEntity)

    @Query("DELETE FROM game_records WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM game_records WHERE id = :id")
    suspend fun getById(id: String): GameRecordEntity?

    @Query("SELECT * FROM game_records ORDER BY startTime DESC")
    fun getAll(): Flow<List<GameRecordEntity>>

    @Query("SELECT * FROM game_records ORDER BY startTime DESC LIMIT :limit OFFSET :offset")
    suspend fun getPaged(limit: Int, offset: Int): List<GameRecordEntity>

    @Query("SELECT * FROM game_records WHERE winnerIndex = :playerIndex ORDER BY startTime DESC")
    fun getByWinner(playerIndex: Int): Flow<List<GameRecordEntity>>

    @Query("SELECT * FROM game_records WHERE ruleName = :ruleName ORDER BY startTime DESC")
    fun getByRule(ruleName: String): Flow<List<GameRecordEntity>>

    @Query("SELECT COUNT(*) FROM game_records")
    suspend fun getCount(): Int

    @Query("SELECT * FROM game_records WHERE startTime BETWEEN :startTime AND :endTime ORDER BY startTime DESC")
    fun getByTimeRange(startTime: Long, endTime: Long): Flow<List<GameRecordEntity>>
}
