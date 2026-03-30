package com.example.dokodemotv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dokodemotv.model.ChannelItem
import com.example.dokodemotv.data.local.entity.EpgChannel
import com.example.dokodemotv.data.local.entity.EpgProgram
import com.example.dokodemotv.scheduling.RecordingScheduler
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import java.text.SimpleDateFormat
import java.util.*



@Composable
fun EpgScreen(
    channels: List<ChannelItem>,
    onChannelSelected: (ChannelItem) -> Unit,
    onBack: () -> Unit,
    epgViewModel: EpgViewModel = viewModel()
) {
    val context = LocalContext.current
    val epgChannels by epgViewModel.epgChannels.collectAsState()

    val activePrograms by epgViewModel.activePrograms.collectAsState()
    val mappings by epgViewModel.channelMappings.collectAsState()

    val currentTime = System.currentTimeMillis()
    val horizontalScrollState = rememberScrollState()

    // Mapping Dialog State
    var channelToMap by remember { mutableStateOf<ChannelItem?>(null) }
    // Programs to schedule
    var programToSchedule by remember { mutableStateOf<Pair<EpgProgram, ChannelItem>?>(null) }


    // Group programs by mapped channel ID, including future ones
    val programsByChannelId = remember(activePrograms) {
        activePrograms.groupBy { it.channelId }
    }


    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text("TV Guide (EPG)", style = MaterialTheme.typography.titleLarge)
            }

            Button(onClick = { epgViewModel.refreshEpgData() }) {
                Text("Refresh")
            }
        }


        // Time Axis Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 120.dp) // Offset for channel names
                .horizontalScroll(horizontalScrollState)
        ) {
            // Simplified time axis: just show current time for now
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text("Current Time: \${timeFormat.format(Date(currentTime))}")
            }
        }

        // Channels and Programs Grid
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(channels) { channel ->
                // Resolve mapped tvg-id
                val mapping = mappings.find { it.streamUrl == channel.streamUrl }
                val mappedTvgId = mapping?.mappedChannelId ?: channel.tvgId

                val currentPrograms = mappedTvgId?.let { programsByChannelId[it] } ?: emptyList()
                val currentProgram = currentPrograms.find { it.startTime <= currentTime && it.endTime > currentTime }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clickable { onChannelSelected(channel) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Channel Header
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { channelToMap = channel } // Allow long press ideally, but clickable for now to trigger mapping
                            .padding(8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column {
                            Text(
                                text = channel.name,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (mapping != null) {
                                Text("Mapped", color = Color.Green, fontSize = 10.sp)
                            }
                        }
                    }

                    // Program Track
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(horizontalScrollState)
                    ) {
                        val programs = mappedTvgId?.let { programsByChannelId[it] } ?: emptyList()
                        if (programs.isNotEmpty()) {
                            programs.sortedBy { it.startTime }.forEach { program ->
                                val isNow = program.startTime <= currentTime && program.endTime > currentTime
                                Box(
                                    modifier = Modifier
                                        .width(250.dp)
                                        .fillMaxHeight()
                                        .padding(4.dp)
                                        .background(
                                            if (isNow) MaterialTheme.colorScheme.primaryContainer 
                                            else MaterialTheme.colorScheme.surfaceVariant, 
                                            shape = MaterialTheme.shapes.small
                                        )
                                        .clickable {
                                            if (program.endTime > currentTime) {
                                                programToSchedule = program to channel
                                            }
                                        }
                                        .padding(8.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "\${timeFormat.format(Date(program.startTime))} - \${timeFormat.format(Date(program.endTime))}",
                                            fontSize = 11.sp,
                                            color = if (isNow) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) 
                                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = program.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isNow) MaterialTheme.colorScheme.onPrimaryContainer 
                                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .width(250.dp)
                                    .fillMaxHeight()
                                    .padding(4.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), shape = MaterialTheme.shapes.small)
                                    .padding(8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "設定なし (No Data)",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }

                }
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

            }
        }
    }

    // Mapping Dialog
    if (channelToMap != null) {
        Dialog(onDismissRequest = { channelToMap = null }) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Map EPG for: \${channelToMap!!.name}", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (epgChannels.isEmpty()) {
                        Text("No EPG channels found. Please wait for the update to complete.")
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            epgViewModel.unmapChannel(channelToMap!!.streamUrl)
                                            channelToMap = null
                                        }
                                        .padding(12.dp)
                                ) {
                                    Text("Unmap (Use Default)", color = MaterialTheme.colorScheme.error)
                                }
                                Divider()

                            }
                            items(epgChannels) { epgChannel ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            epgViewModel.mapChannel(channelToMap!!.streamUrl, epgChannel.channelId)
                                            channelToMap = null
                                        }
                                        .padding(12.dp)
                                ) {
                                    Text(epgChannel.displayName)
                                }
                                Divider()

                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { channelToMap = null }, modifier = Modifier.align(Alignment.End)) {
                        Text("Cancel")
                    }
                }
            }
        }
    }

    // Schedule Dialog
    if (programToSchedule != null) {
        val (program, channel) = programToSchedule!!
        AlertDialog(
            onDismissRequest = { programToSchedule = null },
            title = { Text("番組予約") },
            text = {
                Column {
                    Text(program.title, style = MaterialTheme.typography.titleMedium)
                    Text("\${timeFormat.format(Date(program.startTime))} - \${timeFormat.format(Date(program.endTime))}")
                    Text("チャンネル: \${channel.name}")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val scheduler = RecordingScheduler(context)
                        scheduler.scheduleRecording(
                            com.example.dokodemotv.scheduling.ScheduledRecording(
                                id = program.hashCode(),
                                startTimeMs = program.startTime,
                                url = channel.streamUrl,
                                fileName = "\${channel.name}_\${program.title}_\${System.currentTimeMillis()}.ts"
                            )
                        )
                        Toast.makeText(context, "録画予約しました: \${program.title}", Toast.LENGTH_SHORT).show()
                        programToSchedule = null
                    }
                ) {
                    Text("録画予約")
                }
            },
            dismissButton = {
                TextButton(onClick = { programToSchedule = null }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

