package com.majiang.repository.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_stats")
data class PlayerStatsEntity(
    @PrimaryKey
    val playerName: String,
    val totalGames: Int,
    val wins: Int,
    val losses: Int,
    val draws: Int,
    val totalScore: Int,
    val averageScore: Double,
    val lastPlayedTime: Long
)
