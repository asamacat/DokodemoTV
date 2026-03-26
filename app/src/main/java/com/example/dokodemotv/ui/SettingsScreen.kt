package com.example.dokodemotv.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.dokodemotv.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val bufferSize by viewModel.bufferSizeFlow.collectAsState()
    val sleepTimer by viewModel.sleepTimerFlow.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                SettingsCategory("Playback & Network") {
                    var showBufferDialog by remember { mutableStateOf(false) }
                    SettingsItem(
                        title = "Buffer Size",
                        subtitle = bufferSize,
                        onClick = { showBufferDialog = true }
                    )
                    if (showBufferDialog) {
                        OptionsDialog(
                            title = "Select Buffer Size",
                            options = listOf("Small", "Medium", "Large"),
                            selected = bufferSize,
                            onSelect = {
                                coroutineScope.launch { viewModel.setBufferSize(it) }
                                showBufferDialog = false
                            },
                            onDismiss = { showBufferDialog = false }
                        )
                    }

                    var showTimerDialog by remember { mutableStateOf(false) }
                    SettingsItem(
                        title = "Sleep Timer",
                        subtitle = sleepTimer,
                        onClick = { showTimerDialog = true }
                    )
                    if (showTimerDialog) {
                        OptionsDialog(
                            title = "Select Sleep Timer",
                            options = listOf("Off", "15 minutes", "30 minutes", "60 minutes"),
                            selected = sleepTimer,
                            onSelect = {
                                coroutineScope.launch { viewModel.setSleepTimer(it) }
                                showTimerDialog = false
                            },
                            onDismiss = { showTimerDialog = false }
                        )
                    }
                }
                Divider()
            }
            item {
                SettingsCategory("Profiles & Data") {
                    SettingsItem("Manage Profiles (Coming Soon)", "", {})
                    SettingsItem("Backup / Restore (Coming Soon)", "", {})
                }
                Divider()
            }
            item {
                SettingsCategory("UI & Display") {
                    SettingsItem("Theme Settings (Coming Soon)", "", {})
                }
            }
        }
    }
}

@Composable
fun SettingsCategory(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)
        )
        content()
    }
}

@Composable
fun SettingsItem(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        if (subtitle.isNotEmpty()) {
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun OptionsDialog(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (option == selected),
                            onClick = { onSelect(option) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = option)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
