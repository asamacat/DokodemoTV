package com.example.dokodemotv.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.dokodemotv.data.local.dao.EpgDao
import com.example.dokodemotv.data.local.entity.ChannelMapping
import com.example.dokodemotv.data.local.entity.EpgChannel
import com.example.dokodemotv.data.local.entity.EpgProgram

@Database(
    entities = [EpgChannel::class, EpgProgram::class, ChannelMapping::class],
    version = 1,
    exportSchema = false
)
abstract class EpgDatabase : RoomDatabase() {

    abstract fun epgDao(): EpgDao

    companion object {
        @Volatile
        private var INSTANCE: EpgDatabase? = null

        fun getDatabase(context: Context): EpgDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EpgDatabase::class.java,
                    "epg_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
