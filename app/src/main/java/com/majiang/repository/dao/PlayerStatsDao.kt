package com.majiang.repository.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.majiang.repository.entity.PlayerStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerStatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stats: PlayerStatsEntity)

    @Query("SELECT * FROM player_stats WHERE playerName = :name")
    suspend fun getByName(name: String): PlayerStatsEntity?

    @Query("SELECT * FROM player_stats ORDER BY wins DESC")
    fun getAllSortedByWins(): Flow<List<PlayerStatsEntity>>

    @Query("SELECT * FROM player_stats ORDER BY averageScore DESC")
    fun getAllSortedByScore(): Flow<List<PlayerStatsEntity>>

    @Query("SELECT * FROM player_stats")
    fun getAll(): Flow<List<PlayerStatsEntity>>

    @Query("UPDATE player_stats SET totalGames = :totalGames, wins = :wins, losses = :losses, draws = :draws, totalScore = :totalScore, averageScore = :averageScore, lastPlayedTime = :lastPlayedTime WHERE playerName = :playerName")
    suspend fun updateStats(
        playerName: String,
        totalGames: Int,
        wins: Int,
        losses: Int,
        draws: Int,
        totalScore: Int,
        averageScore: Double,
        lastPlayedTime: Long
    )

    @Query("DELETE FROM player_stats WHERE playerName = :name")
    suspend fun deleteByName(name: String)
}
