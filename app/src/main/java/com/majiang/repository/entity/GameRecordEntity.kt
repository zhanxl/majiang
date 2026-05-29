package com.majiang.repository.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_records")
data class GameRecordEntity(
    @PrimaryKey
    val id: String,
    val startTime: Long,
    val endTime: Long?,
    val player1Name: String,
    val player2Name: String,
    val player3Name: String,
    val player4Name: String,
    val winnerIndex: Int?,
    val player1Score: Int,
    val player2Score: Int,
    val player3Score: Int,
    val player4Score: Int,
    val ruleName: String,
    val roundWind: String
)
