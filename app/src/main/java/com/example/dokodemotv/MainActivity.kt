package com.example.dokodemotv

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import kotlin.OptIn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.view.KeyEvent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val sources by viewModel.sources.collectAsState()
    var selectedUrl by remember { mutableStateOf("https://linear-abematv.akamaized.net/preview/channel/onepiece/playlist.m3u8") }

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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { launcher.launch(null) },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Select Playlist Folder")
                }
                Divider()
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(sources) { source ->
                        Text(
                            text = "Source: ${source.name}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        source.channels.forEachIndexed { index, channelUrl ->
                            NavigationDrawerItem(
                                label = { Text("Channel ${index + 1}: ${channelUrl.takeLast(40)}") },
                                selected = selectedUrl == channelUrl,
                                onClick = {
                                    selectedUrl = channelUrl
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("DokodemoTV PoC") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { padding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                color = MaterialTheme.colorScheme.background
            ) {
                VideoPlayerContent(
                    url = selectedUrl,
                    viewModel = viewModel,
                    onChannelUp = {
                        val allChannels = sources.flatMap { it.channels }
                        val currentIndex = allChannels.indexOf(selectedUrl)
                        if (currentIndex != -1 && allChannels.isNotEmpty()) {
                            val nextIndex = (currentIndex + 1) % allChannels.size
                            selectedUrl = allChannels[nextIndex]
                        }
                    },
                    onChannelDown = {
                        val allChannels = sources.flatMap { it.channels }
                        val currentIndex = allChannels.indexOf(selectedUrl)
                        if (currentIndex != -1 && allChannels.isNotEmpty()) {
                            val prevIndex = if (currentIndex - 1 < 0) allChannels.size - 1 else currentIndex - 1
                            selectedUrl = allChannels[prevIndex]
                        }
                    }
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerContent(
    url: String,
    viewModel: PlayerViewModel,
    onChannelUp: () -> Unit = {},
    onChannelDown: () -> Unit = {}
) {
    val context = LocalContext.current
    val exoPlayer = viewModel.exoPlayer
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(url) {
        viewModel.preparePlayer(url)
        focusRequester.requestFocus()
    }

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester),
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = true
                isFocusable = true
                isFocusableInTouchMode = true

                setOnKeyListener { _, keyCode, event ->
                    if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                        when (keyCode) {
                            android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                onChannelUp()
                                true
                            }
                            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                onChannelDown()
                                true
                            }
                            android.view.KeyEvent.KEYCODE_DPAD_LEFT, android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                if (!isControllerFullyVisible) {
                                    showController()
                                }
                                false
                            }
                            android.view.KeyEvent.KEYCODE_DPAD_CENTER, android.view.KeyEvent.KEYCODE_ENTER -> {
                                if (!isControllerFullyVisible) {
                                    showController()
                                }
                                false
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                }
            }
        }
    )
}
