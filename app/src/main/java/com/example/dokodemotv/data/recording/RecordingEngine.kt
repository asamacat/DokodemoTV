package com.example.dokodemotv.data.recording

import kotlinx.coroutines.flow.Flow

enum class RecordingState {
    IDLE,
    RECORDING,
    ERROR,
    COMPLETED
}

interface RecordingEngine {
    val state: Flow<RecordingState>

    suspend fun startRecording(url: String, fileName: String)
    suspend fun stopRecording()
}
