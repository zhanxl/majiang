package com.majiang.repository.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "discard_records",
    foreignKeys = [
        ForeignKey(
            entity = GameRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("gameId")]
)
data class DiscardRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameId: String,
    val playerIndex: Int,
    val tileName: String,
    val turnNumber: Int,
    val timestamp: Long
)
