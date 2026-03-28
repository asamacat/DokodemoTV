import re

with open('app/src/main/java/com/example/dokodemotv/MainActivity.kt', 'r') as f:
    content = f.read()

# Add SettingsScreen import
imports = """import com.example.dokodemotv.ui.settings.SettingsScreen
import com.example.dokodemotv.data.preferences.SettingsRepository
import com.example.dokodemotv.service.RecordingForegroundService
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
"""
content = re.sub(r'(import androidx\.compose\.runtime\.\*)', r'\1\n' + imports, content)

# Change Load Folder button and add Settings button
old_buttons = """                    ElevatedButton(
                        onClick = { safeLaunchFolderPicker() },
                        shape = MaterialTheme.shapes.medium,
                        modifier = if (sources.isEmpty()) Modifier.focusRequester(initialFocusRequester) else Modifier
                    ) {
                        Text("📁 Load Folder / Settings")
                    }"""

new_buttons = """                    var showSettings by remember { mutableStateOf(false) }
                    ElevatedButton(
                        onClick = { showSettings = true },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("⚙️ Settings")
                    }
                    ElevatedButton(
                        onClick = { safeLaunchFolderPicker() },
                        shape = MaterialTheme.shapes.medium,
                        modifier = if (sources.isEmpty()) Modifier.focusRequester(initialFocusRequester) else Modifier
                    ) {
                        Text("📁 Load Folder")
                    }"""

content = content.replace(old_buttons, new_buttons)

# Add SettingsScreen rendering
if "var showSettings by remember { mutableStateOf(false) }" not in content:
    print("Warning: Failed to replace buttons")

settings_screen_code = """
    var showSettings by remember { mutableStateOf(false) }

    if (showSettings) {
        SettingsScreen(
            onNavigateBack = { showSettings = false },
            settingsRepository = SettingsRepository(context)
        )
        return
    }
"""

# Insert SettingsScreen logic right after DokodemoTVApp start
content = re.sub(r'(fun DokodemoTVApp\([^)]*\)\s*\{[^\n]*)', r'\1\n' + settings_screen_code, content)

# Modify ChannelListItem to include Record Button
old_channel_list_item = """@Composable
fun ChannelListItem(channel: ChannelItem, isSelected: Boolean, onClick: () -> Unit) {"""

new_channel_list_item = """@Composable
fun ChannelListItem(channel: ChannelItem, isSelected: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current"""

content = content.replace(old_channel_list_item, new_channel_list_item)


# Add record button to ChannelListItem row
old_row_content = """                Text(
                    text = channel.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor
                )"""

new_row_content = """                Text(
                    text = channel.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                    modifier = Modifier.weight(1f)
                )

                var isRecording by remember { mutableStateOf(false) }
                IconButton(onClick = {
                    val intent = Intent(context, RecordingForegroundService::class.java).apply {
                        if (isRecording) {
                            action = RecordingForegroundService.ACTION_STOP_RECORDING
                        } else {
                            action = RecordingForegroundService.ACTION_START_RECORDING
                            putExtra(RecordingForegroundService.EXTRA_URL, channel.streamUrl)
                            val cleanTitle = channel.title.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
                            putExtra(RecordingForegroundService.EXTRA_FILENAME, "${cleanTitle}_${System.currentTimeMillis()}.ts")
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                    isRecording = !isRecording
                }) {
                    Text(if (isRecording) "⏹️" else "⏺️")
                }"""

content = content.replace(old_row_content, new_row_content)

with open('app/src/main/java/com/example/dokodemotv/MainActivity.kt', 'w') as f:
    f.write(content)
