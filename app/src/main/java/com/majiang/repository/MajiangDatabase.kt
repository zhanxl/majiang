package com.majiang.repository

import androidx.room.Database
import androidx.room.RoomDatabase
import com.majiang.repository.dao.DiscardRecordDao
import com.majiang.repository.dao.GameRecordDao
import com.majiang.repository.dao.PlayerStatsDao
import com.majiang.repository.entity.DiscardRecordEntity
import com.majiang.repository.entity.GameRecordEntity
import com.majiang.repository.entity.PlayerStatsEntity

@Database(
    entities = [
        GameRecordEntity::class,
        PlayerStatsEntity::class,
        DiscardRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MajiangDatabase : RoomDatabase() {
    abstract fun gameRecordDao(): GameRecordDao
    abstract fun playerStatsDao(): PlayerStatsDao
    abstract fun discardRecordDao(): DiscardRecordDao
}
