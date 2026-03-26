package com.example.dokodemotv.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.dokodemotv.data.preferences.RecordingEngine
import com.example.dokodemotv.data.preferences.SettingsRepository
import com.example.dokodemotv.data.preferences.StorageType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    settingsRepository: SettingsRepository
) {
    val coroutineScope = rememberCoroutineScope()
    val settingsState by settingsRepository.settingsFlow.collectAsState(initial = null)

    val settings = settingsState ?: return // Wait for loading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    Button(onClick = onNavigateBack, modifier = Modifier.padding(start = 8.dp)) {
                        Text("Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Recording Engine", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = settings.engine == RecordingEngine.CUSTOM,
                    onClick = { coroutineScope.launch { settingsRepository.updateEngine(RecordingEngine.CUSTOM) } }
                )
                Text("Custom TS Downloader (Recommended)")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = settings.engine == RecordingEngine.MEDIA3,
                    onClick = { coroutineScope.launch { settingsRepository.updateEngine(RecordingEngine.MEDIA3) } }
                )
                Text("Media3 Downloader")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = settings.engine == RecordingEngine.OFF,
                    onClick = { coroutineScope.launch { settingsRepository.updateEngine(RecordingEngine.OFF) } }
                )
                Text("Off")
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text("Storage Type", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = settings.storageType == StorageType.LOCAL,
                    onClick = { coroutineScope.launch { settingsRepository.updateStorageType(StorageType.LOCAL) } }
                )
                Text("Local Storage")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = settings.storageType == StorageType.NAS,
                    onClick = { coroutineScope.launch { settingsRepository.updateStorageType(StorageType.NAS) } }
                )
                Text("SMB/NAS Direct Recording")
            }

            if (settings.storageType == StorageType.NAS) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("NAS Connection Details", style = MaterialTheme.typography.titleSmall)

                        var nasIp by remember { mutableStateOf(settings.nasIp) }
                        var nasShare by remember { mutableStateOf(settings.nasShare) }
                        var nasUser by remember { mutableStateOf(settings.nasUser) }
                        var nasPass by remember { mutableStateOf(settings.nasPass) }

                        OutlinedTextField(
                            value = nasIp,
                            onValueChange = { nasIp = it },
                            label = { Text("NAS IP Address") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = nasShare,
                            onValueChange = { nasShare = it },
                            label = { Text("Share Name (e.g., Public)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = nasUser,
                            onValueChange = { nasUser = it },
                            label = { Text("Username") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = nasPass,
                            onValueChange = { nasPass = it },
                            label = { Text("Password") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    settingsRepository.updateNasDetails(nasIp, nasShare, nasUser, nasPass)
                                }
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Save NAS Settings")
                        }
                    }
                }
            }
        }
    }
}
