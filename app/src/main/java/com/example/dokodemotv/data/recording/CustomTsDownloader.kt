package com.example.dokodemotv.data.recording

import com.example.dokodemotv.data.storage.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.io.OutputStream
import java.net.URI

class CustomTsDownloader(private val storageManager: StorageManager) : RecordingEngine {

    private val _state = MutableStateFlow(RecordingState.IDLE)
    override val state: StateFlow<RecordingState> = _state

    private val client = OkHttpClient()
    private var recordingJob: Job? = null
    private var isRecording = false

    // Keep track of downloaded segments to avoid redownloading
    private val downloadedSegments = mutableSetOf<String>()

    override suspend fun startRecording(url: String, fileName: String) {
        if (isRecording) return
        isRecording = true
        _state.value = RecordingState.RECORDING

        withContext(Dispatchers.IO) {
            try {
                // Main recording loop
                while (isActive && isRecording) {
                    val playlistContent = downloadText(url)
                    if (playlistContent != null) {
                        // Check if it's a master playlist or a media playlist
                        if (playlistContent.contains("#EXT-X-STREAM-INF")) {
                            // Find highest quality variant (simplified: just take first variant)
                            val variantUrl = extractVariantUrl(playlistContent, url)
                            if (variantUrl != null) {
                                processMediaPlaylist(variantUrl, fileName)
                            }
                        } else {
                            // It's a media playlist
                            processMediaPlaylist(url, fileName)
                        }
                    }
                    // Wait before fetching playlist again (e.g. 5 seconds)
                    delay(5000)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = RecordingState.ERROR
            } finally {
                isRecording = false
                if (_state.value != RecordingState.ERROR) {
                    _state.value = RecordingState.COMPLETED
                }
            }
        }
    }

    private suspend fun processMediaPlaylist(playlistUrl: String, fileName: String) {
        val playlistContent = downloadText(playlistUrl) ?: return
        val lines = playlistContent.lines()

        val segments = mutableListOf<String>()
        for (line in lines) {
            if (line.isNotBlank() && !line.startsWith("#")) {
                segments.add(line)
            }
        }

        for (segment in segments) {
            if (!isRecording) break

            // Resolve relative URLs
            val segmentUrl = resolveUrl(playlistUrl, segment)

            if (!downloadedSegments.contains(segmentUrl)) {
                downloadAndAppendSegment(segmentUrl, fileName)
                downloadedSegments.add(segmentUrl)

                // keep memory footprint small
                if (downloadedSegments.size > 1000) {
                    downloadedSegments.clear()
                }
            }
        }
    }

    private suspend fun downloadAndAppendSegment(segmentUrl: String, fileName: String) {
        val request = Request.Builder().url(segmentUrl).build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val inputStream = response.body?.byteStream()
                    if (inputStream != null) {
                        val outputStream = storageManager.getOutputStream(fileName)
                        if (outputStream != null) {
                            inputStream.copyTo(outputStream)
                            outputStream.close()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun extractVariantUrl(masterPlaylist: String, baseUrl: String): String? {
        val lines = masterPlaylist.lines()
        for (i in lines.indices) {
            if (lines[i].startsWith("#EXT-X-STREAM-INF") && i + 1 < lines.size) {
                return resolveUrl(baseUrl, lines[i + 1])
            }
        }
        return null
    }

    private fun resolveUrl(baseUrl: String, relativeUrl: String): String {
        if (relativeUrl.startsWith("http")) return relativeUrl
        return try {
            val baseUri = URI(baseUrl)
            baseUri.resolve(relativeUrl).toString()
        } catch (e: Exception) {
            relativeUrl
        }
    }

    private fun downloadText(url: String): String? {
        val request = Request.Builder().url(url).build()
        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun stopRecording() {
        isRecording = false
        recordingJob?.cancel()
        _state.value = RecordingState.IDLE
    }
}
