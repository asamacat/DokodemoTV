package com.example.dokodemotv.data.recording

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.example.dokodemotv.data.storage.StorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class Media3Downloader(private val context: Context, private val downloadManager: DownloadManager) : RecordingEngine {

    private val _state = MutableStateFlow(RecordingState.IDLE)
    override val state: StateFlow<RecordingState> = _state

    private var downloadId: String? = null

    override suspend fun startRecording(url: String, fileName: String) {
        // Media3 HlsDownloader internally handles fetching segments,
        // but it strictly writes to the configured cache (exoplayer database)
        // We cannot directly pipe it to our arbitrary StorageManager (like NAS) easily
        // This is a known limitation when using ExoPlayer's DownloadManager vs a custom solution.

        try {
            downloadId = url
            val request = DownloadRequest.Builder(downloadId!!, Uri.parse(url)).build()
            downloadManager.addDownload(request)
            _state.value = RecordingState.RECORDING

            // Note: Since we need to support NAS/StorageManager and Media3 doesn't easily output to custom OutputStreams,
            // we primarily rely on the CustomTsDownloader for arbitrary storage.
            // For Media3, we trigger the download manager which stores in app cache.
        } catch (e: Exception) {
            _state.value = RecordingState.ERROR
        }
    }

    override suspend fun stopRecording() {
        if (downloadId != null) {
            downloadManager.removeDownload(downloadId!!)
            downloadId = null
        }
        _state.value = RecordingState.COMPLETED
    }
}
