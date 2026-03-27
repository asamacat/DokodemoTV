package com.example.dokodemotv.epg

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import java.util.concurrent.TimeUnit

object EpgManager {
    private const val EPG_WORK_NAME = "epg_update_work"
    private const val TAG = "EpgManager"

    // Configured EPG Sources. In a real app, these would come from SharedPreferences or a local file.
    val epgSources = listOf(
        "https://example.com/epg.xml" // Placeholder
    )

    fun schedulePeriodicEpgUpdate(context: Context) {
        if (epgSources.isEmpty()) return

        Log.d(TAG, "Scheduling periodic EPG updates every 12 hours.")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putStringArray(EpgUpdateWorker.KEY_URLS, epgSources.toTypedArray())
            .build()

        val epgWorkRequest = PeriodicWorkRequestBuilder<EpgUpdateWorker>(
            12, TimeUnit.HOURS,
            30, TimeUnit.MINUTES // Flex interval
        )
        .setConstraints(constraints)
        .setInputData(inputData)
        .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            EPG_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing schedule if it's already running
            epgWorkRequest
        )
    }

    fun triggerImmediateUpdate(context: Context) {
        if (epgSources.isEmpty()) return
        Log.d(TAG, "Triggering immediate EPG update.")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putStringArray(EpgUpdateWorker.KEY_URLS, epgSources.toTypedArray())
            .build()

        val oneTimeRequest = OneTimeWorkRequestBuilder<EpgUpdateWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueue(oneTimeRequest)
    }
}
