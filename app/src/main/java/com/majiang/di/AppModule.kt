package com.majiang.di

import android.content.Context
import androidx.room.Room
import com.majiang.repository.MajiangDatabase
import com.majiang.repository.dao.DiscardRecordDao
import com.majiang.repository.dao.GameRecordDao
import com.majiang.repository.dao.PlayerStatsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): MajiangDatabase {
        return Room.databaseBuilder(
            context,
            MajiangDatabase::class.java,
            "majiang_database"
        ).build()
    }

    @Provides
    fun provideGameRecordDao(database: MajiangDatabase): GameRecordDao =
        database.gameRecordDao()

    @Provides
    fun providePlayerStatsDao(database: MajiangDatabase): PlayerStatsDao =
        database.playerStatsDao()

    @Provides
    fun provideDiscardRecordDao(database: MajiangDatabase): DiscardRecordDao =
        database.discardRecordDao()
}
