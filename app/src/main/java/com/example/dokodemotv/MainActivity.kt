package com.example.dokodemotv

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import coil.compose.rememberAsyncImagePainter
import com.example.dokodemotv.data.preferences.SettingsRepository
import com.example.dokodemotv.model.ChannelItem
import com.example.dokodemotv.service.RecordingForegroundService
import com.example.dokodemotv.ui.settings.SettingsScreen
import com.example.dokodemotv.ui.theme.DokodemoTVTheme
import com.example.dokodemotv.viewmodel.PlayerViewModel
import java.io.File
import kotlin.OptIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DokodemoTVTheme {
                DokodemoTVApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DokodemoTVApp(viewModel: PlayerViewModel = viewModel()) {
    val context = LocalContext.current
    val sources by viewModel.sources.collectAsState()
    var selectedUrl by remember { mutableStateOf<String?>(viewModel.initialUrl) }

    // Flat list of all channels for zapping
    val allChannels = remember(sources) {
        sources.flatMap { it.channels }
    }

    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableStateOf(0) }
    var showMenuButton by remember { mutableStateOf(true) }

    var zapMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(zapMessage) {
        if (zapMessage != null) {
            delay(2000)
            zapMessage = null
        }
    }

    // Auto-hide menu button after 15 seconds if a channel is selected and bottom sheet is closed
    LaunchedEffect(selectedUrl, showBottomSheet) {
        if (!showBottomSheet && selectedUrl != null) {
            showMenuButton = true
            delay(15000)
            showMenuButton = false
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                viewModel.loadPlaylists(it)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "設定を保存できませんでした", Toast.LENGTH_SHORT).show()
            }
        }
    }

val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val downloadFolder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "DokodemoTV")
            viewModel.loadPlaylists(downloadFolder)
            if (viewModel.sources.value.isEmpty()) {
                Toast.makeText(context, "Download/DokodemoTV フォルダ内にプレイリストが見つかりません。作成してください", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Download/DokodemoTV フォルダから読み込みました", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "権限拒否: Download/DokodemoTV フォルダを読み込めません", Toast.LENGTH_LONG).show()
        }
    }

    val manageStorageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                val downloadFolder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "DokodemoTV")
                viewModel.loadPlaylists(downloadFolder)
                if (viewModel.sources.value.isEmpty()) {
                    Toast.makeText(context, "Download/DokodemoTV フォルダ内にプレイリストが見つかりません。作成してください", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Download/DokodemoTV フォルダから読み込みました", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "権限拒否: すべてのファイルへのアクセス権限が必要です", Toast.LENGTH_LONG).show()
            }
        }
    }

    val safeLaunchFolderPicker = {
        try {
            launcher.launch(null)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "標準のファイルマネージャが無いため、権限を要求します", Toast.LENGTH_LONG).show()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        intent.data = Uri.parse("package:${context.packageName}")
                        manageStorageLauncher.launch(intent)
                    } catch (ex: Exception) {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        manageStorageLauncher.launch(intent)
                    }
                } else {
                    val downloadFolder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "DokodemoTV")
                    viewModel.loadPlaylists(downloadFolder)
                    if (viewModel.sources.value.isEmpty()) {
                        Toast.makeText(context, "Download/DokodemoTV フォルダ内にプレイリストが見つかりません", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Download/DokodemoTV フォルダから読み込みました", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (selectedUrl == null) {
            safeLaunchFolderPicker()
        }
    }

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .onKeyEvent { keyEvent ->
                    if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                        keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        showBottomSheet = true
                        showMenuButton = true
                        true
                    } else if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                               keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        showMenuButton = true
                        true
                    } else {
                        false
                    }
                }
        ) {
            VideoPlayerContent(
                url = selectedUrl, 
                viewModel = viewModel,
                onDpadUp = { showBottomSheet = true; showMenuButton = true },
                onShowControls = { showMenuButton = true },
                onZapNext = {
                    if (allChannels.isNotEmpty()) {
                        val currentIndex = allChannels.indexOfFirst { it.streamUrl == selectedUrl }
                        val nextIndex = if (currentIndex == -1 || currentIndex == allChannels.lastIndex) 0 else currentIndex + 1
                        val nextChannel = allChannels[nextIndex]
                        selectedUrl = nextChannel.streamUrl
                        zapMessage = nextChannel.name
                    }
                },
                onZapPrevious = {
                    if (allChannels.isNotEmpty()) {
                        val currentIndex = allChannels.indexOfFirst { it.streamUrl == selectedUrl }
                        val prevIndex = if (currentIndex <= 0) allChannels.lastIndex else currentIndex - 1
                        val prevChannel = allChannels[prevIndex]
                        selectedUrl = prevChannel.streamUrl
                        zapMessage = prevChannel.name
                    }
                }
            )

            if (showMenuButton) {
                // Top Left Menu Button
                FilledIconButton(
                    onClick = { showBottomSheet = true },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                }
            }

            // Zap Message Overlay
            if (zapMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = zapMessage ?: "",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
            }
        }
    }

    if (showBottomSheet) {
        val initialFocusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            // Request focus when the bottom sheet opens to allow D-Pad navigation
            initialFocusRequester.requestFocus()
        }

        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                
                // Top Action Bar inside Bottom Sheet for Settings / Load Folder
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    var showSettings by remember { mutableStateOf(false) }
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
                    }
                }

                if (sources.isNotEmpty()) {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        edgePadding = 8.dp,
                        containerColor = Color.Transparent,
                        divider = {}
                    ) {
                        sources.forEachIndexed { index, source ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = {
                                    Text(
                                        source.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                modifier = Modifier.focusRequester(if (index == selectedTabIndex) initialFocusRequester else FocusRequester())
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val selectedChannels = sources.getOrNull(selectedTabIndex)?.channels ?: emptyList()

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 500.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(selectedChannels) { channel ->
                            ChannelListItem(
                                channel = channel,
                                isSelected = channel.streamUrl == selectedUrl,
                                onClick = {
                                    selectedUrl = channel.streamUrl
                                    showBottomSheet = false
                                }
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No channels loaded. Please load a folder.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelListItem(channel: ChannelItem, isSelected: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    var isFocused by remember { mutableStateOf(false) }

    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isFocused -> MaterialTheme.colorScheme.surfaceVariant
        else -> Color.Transparent
    }

    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        isFocused -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        color = containerColor,
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (channel.iconUrl != null) {
                Image(
                    painter = rememberAsyncImagePainter(model = channel.iconUrl),
                    contentDescription = "${channel.name} icon",
                    modifier = Modifier
                        .size(48.dp)
                        .padding(end = 16.dp)
                )
            } else {
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .padding(end = 16.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("📺", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }

            Text(
                text = channel.name,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerContent(
    url: String?, 
    viewModel: PlayerViewModel,
    onDpadUp: () -> Unit,
    onShowControls: () -> Unit,
    onZapNext: () -> Unit = {},
    onZapPrevious: () -> Unit = {}
) {
    val context = LocalContext.current
    val exoPlayer = viewModel.exoPlayer

    val forwardingPlayer = remember(exoPlayer, onZapNext, onZapPrevious) {
        object : ForwardingPlayer(exoPlayer) {
            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .build()
            }

            override fun isCommandAvailable(command: @Player.Command Int): Boolean {
                if (command == Player.COMMAND_SEEK_TO_NEXT || command == Player.COMMAND_SEEK_TO_PREVIOUS) {
                    return true
                }
                return super.isCommandAvailable(command)
            }

            override fun seekToNext() {
                onZapNext()
            }

            override fun seekToPrevious() {
                onZapPrevious()
            }
        }
    }

    LaunchedEffect(url) {
        if (url != null) viewModel.preparePlayer(url)
    }

    AndroidView(
        modifier = Modifier.fillMaxSize()
            .clickable { onShowControls() },
        factory = {
            object : PlayerView(context) {
                override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                    if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        onDpadUp()
                        return true
                    } else if (event.action == KeyEvent.ACTION_DOWN && (event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER || event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                        onShowControls()
                        // let super handle it to show/hide controller or do default actions
                    }
                    return super.dispatchKeyEvent(event)
                }
            }.apply {
                player = forwardingPlayer
                useController = true
                
                setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                    if (visibility == android.view.View.VISIBLE) {
                        onShowControls()
                    }
                })
            }
        }
    )
}
