package com.example.dokodemotv.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "epg_programs",
    indices = [Index(value = ["channelId", "startTime", "endTime"])]
)
data class EpgProgram(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelId: String,
    val title: String,
    val description: String?,
    val startTime: Long, // timestamp in ms
    val endTime: Long    // timestamp in ms
)
