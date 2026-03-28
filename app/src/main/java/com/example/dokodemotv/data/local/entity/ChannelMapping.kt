package com.example.dokodemotv.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channel_mappings")
data class ChannelMapping(
    @PrimaryKey val streamUrl: String,
    val mappedChannelId: String
)
