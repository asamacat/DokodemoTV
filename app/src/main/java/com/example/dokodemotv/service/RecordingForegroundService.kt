package com.example.dokodemotv.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.dokodemotv.data.preferences.SettingsRepository
import com.example.dokodemotv.data.preferences.StorageType
import com.example.dokodemotv.data.recording.CustomTsDownloader
import com.example.dokodemotv.data.recording.RecordingEngine
import com.example.dokodemotv.data.storage.LocalStorageManager
import com.example.dokodemotv.data.storage.SmbStorageManager
import com.example.dokodemotv.data.storage.StorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RecordingForegroundService : Service() {

    private val CHANNEL_ID = "RecordingServiceChannel"
    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private lateinit var settingsRepository: SettingsRepository
    private var activeEngine: RecordingEngine? = null

    companion object {
        const val ACTION_START_RECORDING = "ACTION_START_RECORDING"
        const val ACTION_STOP_RECORDING = "ACTION_STOP_RECORDING"
        const val EXTRA_URL = "EXTRA_URL"
        const val EXTRA_FILENAME = "EXTRA_FILENAME"
    }

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        when (action) {
            ACTION_START_RECORDING -> {
                val url = intent.getStringExtra(EXTRA_URL)
                val fileName = intent.getStringExtra(EXTRA_FILENAME) ?: "record_${System.currentTimeMillis()}.ts"

                if (url == null) {
                    stopSelf()
                    return START_NOT_STICKY
                }

                startForeground(1, createNotification("Recording started..."))
                acquireWakeLock()
                startRecording(url, fileName)
            }
            ACTION_STOP_RECORDING -> {
                stopRecording()
                stopForeground(true)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun startRecording(url: String, fileName: String) {
        serviceScope.launch {
            val settings = settingsRepository.settingsFlow.first()

            val storageManager: StorageManager = when (settings.storageType) {
                StorageType.LOCAL -> LocalStorageManager(this@RecordingForegroundService)
                StorageType.NAS -> SmbStorageManager(
                    settings.nasIp,
                    settings.nasShare,
                    settings.nasUser,
                    settings.nasPass
                )
            }

            activeEngine = CustomTsDownloader(storageManager)
            activeEngine?.startRecording(url, fileName)
        }
    }

    private fun stopRecording() {
        serviceScope.launch {
            activeEngine?.stopRecording()
            releaseWakeLock()
        }
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "DokodemoTV::RecordingWakeLock"
        )
        wakeLock?.acquire(10 * 60 * 1000L) // 10 minutes max for safety
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    private fun createNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DokodemoTV Recording")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Recording Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
        releaseWakeLock()
    }
}
