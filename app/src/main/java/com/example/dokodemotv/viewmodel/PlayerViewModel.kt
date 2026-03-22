package com.example.dokodemotv.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(application).build()
    private var currentUrl: String? = null

    fun preparePlayer(url: String) {
        if (currentUrl == url) return
        currentUrl = url
        val mediaItem = MediaItem.fromUri(url)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer.release()
    }
}
