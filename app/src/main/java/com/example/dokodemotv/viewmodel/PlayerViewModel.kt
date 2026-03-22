package com.example.dokodemotv.viewmodel

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.dokodemotv.model.ChannelItem
import com.example.dokodemotv.repository.ChannelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ChannelSource(val name: String, val channels: List<ChannelItem>)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(application).build()
    private var currentUrl: String? = null

    private val _sources = MutableStateFlow<List<ChannelSource>>(emptyList())
    val sources = _sources.asStateFlow()

    fun preparePlayer(url: String) {
        if (currentUrl == url) return
        currentUrl = url
        val mediaItem = MediaItem.fromUri(url)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    fun loadPlaylists(folderUri: Uri) {
        val rootDoc = DocumentFile.fromTreeUri(getApplication(), folderUri) ?: return
        val files = rootDoc.listFiles()
        val newSources = mutableListOf<ChannelSource>()

        files.filter { it.name?.endsWith(".txt") == true || it.name?.endsWith(".csv") == true }.forEach { file ->
            val content = getApplication<Application>().contentResolver.openInputStream(file.uri)?.bufferedReader()?.use { it.readText() }
            if (content != null) {
                val channels = ChannelRepository.parseChannelList(content)
                val categoryName = file.name?.substringBeforeLast(".") ?: "Unknown"
                newSources.add(ChannelSource(categoryName, channels))
            }
        }
        _sources.value = newSources
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer.release()
    }
}
