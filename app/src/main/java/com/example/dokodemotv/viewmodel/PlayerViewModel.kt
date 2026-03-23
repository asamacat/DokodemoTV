package com.example.dokodemotv.viewmodel

import android.app.Application
import android.net.Uri
import android.content.Intent
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.dokodemotv.model.ChannelItem
import com.example.dokodemotv.repository.ChannelRepository
import com.example.dokodemotv.util.CacheManager
import com.example.dokodemotv.util.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ChannelSource(val name: String, val channels: List<ChannelItem>)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = PreferencesManager(application)
    var exoPlayer: ExoPlayer
    private var currentUrl: String? = null

    private val _sources = MutableStateFlow<List<ChannelSource>>(emptyList())
    val sources = _sources.asStateFlow()

    init {
        exoPlayer = buildPlayer()
        checkAndLoadSavedPlaylists()
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun buildPlayer(): ExoPlayer {
        val app = getApplication<Application>()
        val cache = CacheManager.getCache(app, prefs.cacheSizeMb)

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)

        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val mediaSourceFactory = DefaultMediaSourceFactory(app)
            .setDataSourceFactory(cacheDataSourceFactory)

        return ExoPlayer.Builder(app)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
    }

    fun preparePlayer(url: String?) {
        if (url == null) {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            currentUrl = null
            return
        }
        if (currentUrl == url) return
        currentUrl = url
        prefs.lastWatchedUrl = url

        val mediaItem = MediaItem.fromUri(url)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    fun reinitializePlayerWithNewCacheSize() {
        val currentPlayWhenReady = exoPlayer.playWhenReady
        exoPlayer.release()

        val app = getApplication<Application>()
        CacheManager.reinitializeCache(app, prefs.cacheSizeMb)

        exoPlayer = buildPlayer()
        currentUrl?.let {
            val mediaItem = MediaItem.fromUri(it)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = currentPlayWhenReady
        }
    }

    private fun checkAndLoadSavedPlaylists() {
        val savedUriStr = prefs.savedFolderUri
        if (savedUriStr != null) {
            try {
                val uri = Uri.parse(savedUriStr)
                // Check if we still have permission
                val contentResolver = getApplication<Application>().contentResolver
                val persistedUriPermissions = contentResolver.persistedUriPermissions

                val hasPermission = persistedUriPermissions.any { it.uri == uri && it.isReadPermission }
                if (hasPermission) {
                    loadPlaylists(uri)
                } else {
                    // Try to take permission just in case, though it's usually done in Activity
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    loadPlaylists(uri)
                }
            } catch (e: SecurityException) {
                // Permission revoked or not accessible
                prefs.savedFolderUri = null
            } catch (e: Exception) {
                // Other errors
            }
        }
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

        prefs.savedFolderUri = folderUri.toString()
        _sources.value = newSources
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer.release()
    }
}
