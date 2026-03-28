package com.example.dokodemotv.repository

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.dokodemotv.model.AppSetting
import com.example.dokodemotv.model.ChannelHistory
import com.example.dokodemotv.model.FavoriteChannel
import com.example.dokodemotv.model.UserProfile

@Database(
    entities = [
        UserProfile::class,
        ChannelHistory::class,
        FavoriteChannel::class,
        AppSetting::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DokodemoDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun channelHistoryDao(): ChannelHistoryDao
    abstract fun favoriteChannelDao(): FavoriteChannelDao
    abstract fun appSettingDao(): AppSettingDao

    companion object {
        @Volatile
        private var INSTANCE: DokodemoDatabase? = null

        fun getDatabase(context: Context): DokodemoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DokodemoDatabase::class.java,
                    "dokodemo_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
