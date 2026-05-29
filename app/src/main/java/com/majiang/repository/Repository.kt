package com.majiang.repository

import com.majiang.model.GameRecord
import com.majiang.model.PlayerStats
import com.majiang.model.Wind
import com.majiang.repository.dao.DiscardRecordDao
import com.majiang.repository.dao.GameRecordDao
import com.majiang.repository.dao.PlayerStatsDao
import com.majiang.repository.entity.DiscardRecordEntity
import com.majiang.repository.entity.GameRecordEntity
import com.majiang.repository.entity.PlayerStatsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameRepository @Inject constructor(
    private val gameRecordDao: GameRecordDao,
    private val playerStatsDao: PlayerStatsDao,
    private val discardRecordDao: DiscardRecordDao
) {

    fun getAllGames(): Flow<List<GameRecord>> =
        gameRecordDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getGameById(id: String): GameRecord? =
        gameRecordDao.getById(id)?.toDomain()

    suspend fun saveGame(record: GameRecord) {
        gameRecordDao.insert(record.toEntity())
        record.discardHistory.forEach { discard ->
            discardRecordDao.insert(
                DiscardRecordEntity(
                    gameId = record.id,
                    playerIndex = discard.playerIndex,
                    tileName = discard.tile.name,
                    turnNumber = discard.turnNumber,
                    timestamp = discard.timestamp
                )
            )
        }
    }

    suspend fun deleteGame(id: String) {
        gameRecordDao.deleteById(id)
        discardRecordDao.deleteByGameId(id)
    }

    fun getGamesByTimeRange(startTime: Long, endTime: Long): Flow<List<GameRecord>> =
        gameRecordDao.getByTimeRange(startTime, endTime).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getGameCount(): Int = gameRecordDao.getCount()
}

@Singleton
class StatsRepository @Inject constructor(
    private val playerStatsDao: PlayerStatsDao
) {

    fun getAllStats(): Flow<List<PlayerStats>> =
        playerStatsDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }

    fun getAllStatsSortedByWins(): Flow<List<PlayerStats>> =
        playerStatsDao.getAllSortedByWins().map { entities ->
            entities.map { it.toDomain() }
        }

    fun getAllStatsSortedByScore(): Flow<List<PlayerStats>> =
        playerStatsDao.getAllSortedByScore().map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getStatsByName(name: String): PlayerStats? =
        playerStatsDao.getByName(name)?.toDomain()

    suspend fun saveStats(stats: PlayerStats) {
        playerStatsDao.insert(stats.toEntity())
    }

    suspend fun updatePlayerStats(playerName: String, isWin: Boolean, score: Int, isDraw: Boolean = false) {
        val existing = playerStatsDao.getByName(playerName)
        val stats = existing?.toDomain()?.withNewGame(isWin, score, isDraw)
            ?: PlayerStats(playerName = playerName).withNewGame(isWin, score, isDraw)
        playerStatsDao.insert(stats.toEntity())
    }
}

private fun GameRecordEntity.toDomain() = GameRecord(
    id = id,
    startTime = startTime,
    endTime = endTime,
    playerNames = listOf(player1Name, player2Name, player3Name, player4Name),
    winnerIndex = winnerIndex,
    scores = listOf(player1Score, player2Score, player3Score, player4Score),
    ruleName = ruleName,
    roundWind = try { Wind.valueOf(roundWind) } catch (_: Exception) { Wind.EAST }
)

private fun GameRecord.toEntity() = GameRecordEntity(
    id = id,
    startTime = startTime,
    endTime = endTime,
    player1Name = playerNames.getOrElse(0) { "" },
    player2Name = playerNames.getOrElse(1) { "" },
    player3Name = playerNames.getOrElse(2) { "" },
    player4Name = playerNames.getOrElse(3) { "" },
    winnerIndex = winnerIndex,
    player1Score = scores.getOrElse(0) { 0 },
    player2Score = scores.getOrElse(1) { 0 },
    player3Score = scores.getOrElse(2) { 0 },
    player4Score = scores.getOrElse(3) { 0 },
    ruleName = ruleName,
    roundWind = roundWind.name
)

private fun PlayerStatsEntity.toDomain() = PlayerStats(
    playerName = playerName,
    totalGames = totalGames,
    wins = wins,
    losses = losses,
    draws = draws,
    totalScore = totalScore,
    averageScore = averageScore
)

private fun PlayerStats.toEntity() = PlayerStatsEntity(
    playerName = playerName,
    totalGames = totalGames,
    wins = wins,
    losses = losses,
    draws = draws,
    totalScore = totalScore,
    averageScore = averageScore,
    lastPlayedTime = System.currentTimeMillis()
)
