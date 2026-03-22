package com.example.dokodemotv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.example.dokodemotv.viewmodel.PlayerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // PoC用: AbemaTVの低画質/高画質ストリーム等の一部ハードコードした初期実装
                    VideoPlayerContent(url = "https://linear-abematv.akamaized.net/preview/channel/onepiece/playlist.m3u8")
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerContent(url: String, viewModel: PlayerViewModel = viewModel()) {
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
                useController = true // TV向けの場合、D-Padで操作しやすいコントローラーUIへの最適化が必要
            }
        }
    )
}
