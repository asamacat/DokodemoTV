package com.example.dokodemotv.repository

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.dokodemotv.model.AppSetting
import com.example.dokodemotv.model.ChannelHistory
import com.example.dokodemotv.model.FavoriteChannel
import com.example.dokodemotv.model.UserProfile
import com.example.dokodemotv.data.local.entity.EpgChannel
import com.example.dokodemotv.data.local.entity.EpgProgram
import com.example.dokodemotv.data.local.entity.ChannelMapping
import com.example.dokodemotv.data.local.dao.EpgDao


@Database(
    entities = [
        UserProfile::class,
        ChannelHistory::class,
        FavoriteChannel::class,
        AppSetting::class,
        EpgChannel::class,
        EpgProgram::class,
        ChannelMapping::class
    ],

    version = 2,
    exportSchema = false
)
abstract class DokodemoDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun channelHistoryDao(): ChannelHistoryDao
    abstract fun favoriteChannelDao(): FavoriteChannelDao
    abstract fun appSettingDao(): AppSettingDao
    abstract fun epgDao(): EpgDao


    companion object {
        @Volatile
        private var INSTANCE: DokodemoDatabase? = null

        fun getDatabase(context: Context): DokodemoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DokodemoDatabase::class.java,
                    "dokodemo_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
