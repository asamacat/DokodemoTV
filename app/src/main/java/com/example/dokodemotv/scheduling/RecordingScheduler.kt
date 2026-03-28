package com.example.dokodemotv.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.dokodemotv.service.RecordingForegroundService
import java.io.BufferedReader
import java.io.InputStreamReader

data class ScheduledRecording(
    val id: Int,
    val startTimeMs: Long,
    val url: String,
    val fileName: String
)

class RecordingScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleRecording(recording: ScheduledRecording) {
        val intent = Intent(context, RecordingForegroundService::class.java).apply {
            action = RecordingForegroundService.ACTION_START_RECORDING
            putExtra(RecordingForegroundService.EXTRA_URL, recording.url)
            putExtra(RecordingForegroundService.EXTRA_FILENAME, recording.fileName)
        }

        val pendingIntent = PendingIntent.getService(
            context,
            recording.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                recording.startTimeMs,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // Android 12+ requires SCHEDULE_EXACT_ALARM permission
            // If denied, fallback or prompt user to grant permission
            e.printStackTrace()
        }
    }

    fun cancelRecording(recordingId: Int) {
        val intent = Intent(context, RecordingForegroundService::class.java)
        val pendingIntent = PendingIntent.getService(
            context,
            recordingId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    // TODO: Full EPG integration
    // This parses a simple rec.csv from assets for initial testing.
    // Format: startTimeMs,url,fileName
    fun syncSchedulesFromCsv() {
        try {
            val inputStream = context.assets.open("rec.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))

            var idCounter = 1000
            reader.forEachLine { line ->
                val parts = line.split(",")
                if (parts.size >= 3) {
                    val timeMs = parts[0].toLongOrNull()
                    val url = parts[1]
                    val fileName = parts[2]

                    if (timeMs != null && timeMs > System.currentTimeMillis()) {
                        scheduleRecording(
                            ScheduledRecording(
                                id = idCounter++,
                                startTimeMs = timeMs,
                                url = url,
                                fileName = fileName
                            )
                        )
                    }
                }
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
