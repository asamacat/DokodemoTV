package com.example.dokodemotv.repository

import com.example.dokodemotv.model.ChannelItem

object ChannelRepository {
    fun parseChannelList(content: String): List<ChannelItem> {
        val channels = mutableListOf<ChannelItem>()
        content.lines().filter { it.isNotBlank() }.forEachIndexed { index, line ->
            // Assume CSV format: Name,IconUrl,StreamUrl
            // If it doesn't contain comma, treat the whole line as StreamUrl and generate a default name
            val parts = line.split(",").map { it.trim() }
            if (parts.size >= 3) {
                channels.add(ChannelItem(name = parts[0], iconUrl = parts[1].takeIf { it.isNotEmpty() }, streamUrl = parts[2]))
            } else if (parts.size == 2) {
                channels.add(ChannelItem(name = parts[0], iconUrl = null, streamUrl = parts[1]))
            } else if (parts.size == 1) {
                channels.add(ChannelItem(name = "Channel ${index + 1}", iconUrl = null, streamUrl = parts[0]))
            }
        }
        return channels
    }
}
