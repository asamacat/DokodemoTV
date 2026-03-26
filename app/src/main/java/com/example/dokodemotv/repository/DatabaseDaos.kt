package com.example.dokodemotv.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.dokodemotv.model.AppSetting
import com.example.dokodemotv.model.ChannelHistory
import com.example.dokodemotv.model.FavoriteChannel
import com.example.dokodemotv.model.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles")
    fun getAllProfiles(): Flow<List<UserProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfile): Long
}

@Dao
interface ChannelHistoryDao {
    @Query("SELECT * FROM channel_history WHERE profileId = :profileId ORDER BY lastWatchedTimestamp DESC")
    fun getHistoryForProfile(profileId: Int): Flow<List<ChannelHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: ChannelHistory)
}

@Dao
interface FavoriteChannelDao {
    @Query("SELECT * FROM favorite_channels WHERE profileId = :profileId")
    fun getFavoritesForProfile(profileId: Int): Flow<List<FavoriteChannel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteChannel)

    @Query("DELETE FROM favorite_channels WHERE profileId = :profileId AND channelUrl = :url")
    suspend fun deleteFavorite(profileId: Int, url: String)
}

@Dao
interface AppSettingDao {
    @Query("SELECT * FROM app_settings WHERE `key` = :key LIMIT 1")
    fun getSettingFlow(key: String): Flow<AppSetting?>

    @Query("SELECT * FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): AppSetting?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetting(setting: AppSetting)
}
