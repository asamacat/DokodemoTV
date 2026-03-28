package com.example.dokodemotv.repository

import com.example.dokodemotv.model.ChannelItem
import java.util.regex.Pattern

object ChannelRepository {
    fun parseChannelList(content: String): List<ChannelItem> {
        val channels = mutableListOf<ChannelItem>()
        val lines = content.lines().map { it.trim() }.filter { it.isNotBlank() }

        // Check if content is standard M3U/M3U8
        if (lines.firstOrNull()?.startsWith("#EXTM3U") == true) {
            var currentName = ""
            var currentIconUrl: String? = null
            var currentTvgId: String? = null

            for (line in lines) {
                if (line.startsWith("#EXTINF")) {
                    // Extract tvg-id if present
                    val tvgIdMatcher = Pattern.compile("tvg-id=\"([^\"]+)\"").matcher(line)
                    if (tvgIdMatcher.find()) {
                        currentTvgId = tvgIdMatcher.group(1)
                    } else {
                        currentTvgId = null
                    }

                    // Extract tvg-logo if present
                    val logoMatcher = Pattern.compile("tvg-logo=\"([^\"]+)\"").matcher(line)
                    if (logoMatcher.find()) {
                        currentIconUrl = logoMatcher.group(1)
                    } else {
                        currentIconUrl = null
                    }

                    // Extract name (after the last comma)
                    val lastCommaIndex = line.lastIndexOf(",")
                    if (lastCommaIndex != -1 && lastCommaIndex < line.length - 1) {
                        currentName = line.substring(lastCommaIndex + 1).trim()
                    } else {
                        currentName = "Unknown Channel"
                    }
                } else if (!line.startsWith("#") && line.startsWith("http")) {
                    // It's a stream URL
                    channels.add(
                        ChannelItem(
                            name = currentName.takeIf { it.isNotEmpty() } ?: "Unknown Channel",
                            iconUrl = currentIconUrl,
                            streamUrl = line,
                            tvgId = currentTvgId
                        )
                    )
                    // Reset for next
                    currentName = ""
                    currentIconUrl = null
                    currentTvgId = null
                }
            }
        } else {
            // Assume extended CSV format: Name,IconUrl,StreamUrl,TvgId
            lines.forEachIndexed { index, line ->
                val parts = line.split(",").map { it.trim() }
                when {
                    parts.size >= 4 -> {
                        channels.add(ChannelItem(
                            name = parts[0],
                            iconUrl = parts[1].takeIf { it.isNotEmpty() },
                            streamUrl = parts[2],
                            tvgId = parts[3].takeIf { it.isNotEmpty() }
                        ))
                    }
                    parts.size == 3 -> {
                        channels.add(ChannelItem(
                            name = parts[0],
                            iconUrl = parts[1].takeIf { it.isNotEmpty() },
                            streamUrl = parts[2]
                        ))
                    }
                    parts.size == 2 -> {
                        channels.add(ChannelItem(
                            name = parts[0],
                            iconUrl = null,
                            streamUrl = parts[1]
                        ))
                    }
                    parts.size == 1 -> {
                        channels.add(ChannelItem(
                            name = "Channel ${index + 1}",
                            iconUrl = null,
                            streamUrl = parts[0]
                        ))
                    }
                }
            }
        }
        return channels
    }
}
