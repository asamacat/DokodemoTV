package com.example.dokodemotv

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import kotlin.OptIn
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
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import coil.compose.rememberAsyncImagePainter
import com.example.dokodemotv.model.ChannelItem
import android.widget.Toast
import android.content.ActivityNotFoundException
import android.os.Environment
import android.Manifest
import java.io.File
import com.example.dokodemotv.viewmodel.PlayerViewModel
import com.example.dokodemotv.ui.theme.DokodemoTVTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

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

    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableStateOf(0) }
    var showMenuButton by remember { mutableStateOf(true) }

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

    val safeLaunchFolderPicker = {
        try {
            launcher.launch(null)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "標準のファイルマネージャが無いため、専用フォルダを探します", Toast.LENGTH_LONG).show()
            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
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
                onShowControls = { showMenuButton = true }
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
                    ElevatedButton(
                        onClick = { safeLaunchFolderPicker() },
                        shape = MaterialTheme.shapes.medium,
                        modifier = if (sources.isEmpty()) Modifier.focusRequester(initialFocusRequester) else Modifier
                    ) {
                        Text("📁 Load Folder / Settings")
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
    onShowControls: () -> Unit
) {
    val context = LocalContext.current
    val exoPlayer = viewModel.exoPlayer

    LaunchedEffect(url) {
        if (url != null) viewModel.preparePlayer(url)
    }

    AndroidView(
        modifier = Modifier.fillMaxSize()
            .clickable { onShowControls() },
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = true
                
                setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                    if (visibility == android.view.View.VISIBLE) {
                        onShowControls()
                    }
                })

                // Pass D-Pad Up events back up if the controller doesn't handle them
                setOnKeyListener { _, keyCode, event ->
                    if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        onDpadUp()
                        true
                    } else if (event.action == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
                        onShowControls()
                        false
                    } else {
                        false
                    }
                }
            }
        }
    )
}
