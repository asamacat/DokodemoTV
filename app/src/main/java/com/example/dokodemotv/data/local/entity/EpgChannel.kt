package com.example.dokodemotv.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "epg_channels")
data class EpgChannel(
    @PrimaryKey val channelId: String,
    val displayName: String,
    val iconUrl: String?
)
