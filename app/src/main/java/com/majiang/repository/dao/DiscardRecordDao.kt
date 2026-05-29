package com.majiang.repository.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.majiang.repository.entity.DiscardRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiscardRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: DiscardRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<DiscardRecordEntity>)

    @Query("SELECT * FROM discard_records WHERE gameId = :gameId ORDER BY turnNumber ASC")
    fun getByGameId(gameId: String): Flow<List<DiscardRecordEntity>>

    @Query("SELECT * FROM discard_records WHERE gameId = :gameId AND playerIndex = :playerIndex ORDER BY turnNumber ASC")
    fun getByGameIdAndPlayer(gameId: String, playerIndex: Int): Flow<List<DiscardRecordEntity>>

    @Query("DELETE FROM discard_records WHERE gameId = :gameId")
    suspend fun deleteByGameId(gameId: String)

    @Query("SELECT COUNT(*) FROM discard_records WHERE gameId = :gameId")
    suspend fun getCountByGameId(gameId: Int): Int
}
