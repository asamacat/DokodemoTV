package com.example.dokodemotv.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.dokodemotv.data.local.entity.ChannelMapping
import com.example.dokodemotv.data.local.entity.EpgChannel
import com.example.dokodemotv.data.local.entity.EpgProgram
import kotlinx.coroutines.flow.Flow

@Dao
interface EpgDao {
    // EPG Channels
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpgChannels(channels: List<EpgChannel>)

    @Query("SELECT * FROM epg_channels")
    fun getAllEpgChannels(): Flow<List<EpgChannel>>

    @Query("DELETE FROM epg_channels")
    suspend fun deleteAllEpgChannels()

    // EPG Programs
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEpgPrograms(programs: List<EpgProgram>)

    @Query("SELECT * FROM epg_programs WHERE channelId = :channelId AND endTime >= :currentTime ORDER BY startTime ASC")
    fun getProgramsForChannel(channelId: String, currentTime: Long): Flow<List<EpgProgram>>

    @Query("SELECT * FROM epg_programs WHERE endTime >= :currentTime ORDER BY startTime ASC")
    fun getAllActivePrograms(currentTime: Long): Flow<List<EpgProgram>>

    @Query("DELETE FROM epg_programs WHERE endTime < :cutoffTime")
    suspend fun deleteOldPrograms(cutoffTime: Long)

    @Query("DELETE FROM epg_programs")
    suspend fun deleteAllEpgPrograms()

    // Manual Channel Mappings
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannelMapping(mapping: ChannelMapping)

    @Query("SELECT * FROM channel_mappings")
    fun getAllChannelMappings(): Flow<List<ChannelMapping>>

    @Query("SELECT mappedChannelId FROM channel_mappings WHERE streamUrl = :streamUrl LIMIT 1")
    suspend fun getMappedChannelId(streamUrl: String): String?

    @Query("DELETE FROM channel_mappings WHERE streamUrl = :streamUrl")
    suspend fun deleteChannelMapping(streamUrl: String)

    // Combined operations
    @Transaction
    suspend fun clearAndInsertEpgData(channels: List<EpgChannel>, programs: List<EpgProgram>) {
        deleteAllEpgChannels()
        deleteAllEpgPrograms()
        insertEpgChannels(channels)
        insertEpgPrograms(programs)
    }
}
