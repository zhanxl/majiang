package com.majiang.model

import java.util.UUID

data class GameRecord(
    val id: String = UUID.randomUUID().toString(),
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val playerNames: List<String>,
    val winnerIndex: Int? = null,
    val scores: List<Int> = emptyList(),
    val ruleName: String = "",
    val roundWind: Wind = Wind.EAST,
    val discardHistory: List<DiscardRecord> = emptyList()
)

data class DiscardRecord(
    val playerIndex: Int,
    val tile: Tile,
    val turnNumber: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class PlayerStats(
    val playerName: String,
    val totalGames: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val totalScore: Int = 0,
    val averageScore: Double = 0.0
) {
    val winRate: Double
        get() = if (totalGames > 0) wins.toDouble() / totalGames else 0.0

    fun withNewGame(isWin: Boolean, score: Int, isDraw: Boolean = false): PlayerStats {
        return copy(
            totalGames = totalGames + 1,
            wins = wins + if (isWin) 1 else 0,
            losses = losses + if (!isWin && !isDraw) 1 else 0,
            draws = draws + if (isDraw) 1 else 0,
            totalScore = totalScore + score,
            averageScore = (totalScore + score).toDouble() / (totalGames + 1)
        )
    }
}
