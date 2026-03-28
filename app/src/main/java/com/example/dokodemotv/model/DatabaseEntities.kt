package com.example.dokodemotv.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val isDefault: Boolean = false
)

@Entity(tableName = "channel_history")
data class ChannelHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val profileId: Int,
    val channelUrl: String,
    val lastWatchedTimestamp: Long
)

@Entity(tableName = "favorite_channels")
data class FavoriteChannel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val profileId: Int,
    val channelUrl: String,
    val addedTimestamp: Long
)

@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey val key: String,
    val value: String
)
