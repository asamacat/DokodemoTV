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
import com.example.dokodemotv.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
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
    var selectedUrl by remember { mutableStateOf("https://linear-abematv.akamaized.net/preview/channel/onepiece/playlist.m3u8") }

    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableStateOf(0) }
    var showOverlayMenu by remember { mutableStateOf(true) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.loadPlaylists(it)
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
                        true
                    } else if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                               keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        showOverlayMenu = !showOverlayMenu
                        true
                    } else {
                        false
                    }
                }
        ) {
            VideoPlayerContent(url = selectedUrl, viewModel = viewModel)

            if (showOverlayMenu) {
                // Top Right Action Buttons (Load Folder & Toggle Overlay)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = { launcher.launch(null) }) {
                        Text("Load Folder")
                    }
                    Button(onClick = { showOverlayMenu = !showOverlayMenu }) {
                        Text("Hide Menu")
                    }
                }

                // Top Left Menu Button
                IconButton(
                    onClick = { showBottomSheet = true },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.primary)
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
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (sources.isNotEmpty()) {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        edgePadding = 8.dp
                    ) {
                        sources.forEachIndexed { index, source ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { Text(source.name) },
                                modifier = Modifier.focusRequester(if (index == selectedTabIndex) initialFocusRequester else FocusRequester())
                            )
                        }
                    }

                    val selectedChannels = sources.getOrNull(selectedTabIndex)?.channels ?: emptyList()

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
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
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No channels loaded. Please load a folder.")
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelListItem(channel: ChannelItem, isSelected: Boolean, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(8.dp),
        color = if (isFocused) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
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
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .padding(end = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📺", style = MaterialTheme.typography.headlineSmall)
                }
            }

            Text(
                text = channel.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerContent(url: String, viewModel: PlayerViewModel) {
    val context = LocalContext.current
    val exoPlayer = viewModel.exoPlayer

    LaunchedEffect(url) {
        viewModel.preparePlayer(url)
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = true
                // Pass D-Pad Up events back up if the controller doesn't handle them
                setOnKeyListener { _, keyCode, event ->
                    if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        // Let the parent (Compose onKeyEvent) handle it
                        false
                    } else if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        false
                    } else {
                        false
                    }
                }
            }
        }
    )
}
