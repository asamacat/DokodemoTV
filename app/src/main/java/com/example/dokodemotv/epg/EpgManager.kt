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
import com.example.dokodemotv.data.preferences.SettingsRepository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


object EpgManager {
    private const val EPG_WORK_NAME = "epg_update_work"
    private const val TAG = "EpgManager"

    // Configured EPG Sources. Now fetched from SettingsRepository.
    private suspend fun getEpgSources(context: Context): List<String> {
        val settings = SettingsRepository(context).settingsFlow.first()
        return if (settings.epgUrl.isNotBlank()) {
            listOf(settings.epgUrl)
        } else {
            emptyList()
        }
    }


    fun schedulePeriodicEpgUpdate(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val sources = getEpgSources(context)
            if (sources.isEmpty()) return@launch

            Log.d(TAG, "Scheduling periodic EPG updates for \${sources.size} sources.")

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val inputData = Data.Builder()
                .putStringArray(EpgUpdateWorker.KEY_URLS, sources.toTypedArray())
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
                ExistingPeriodicWorkPolicy.UPDATE, // Update schedule to use new URLs
                epgWorkRequest
            )
        }
    }


    fun triggerImmediateUpdate(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val sources = getEpgSources(context)
            if (sources.isEmpty()) return@launch
            
            Log.d(TAG, "Triggering immediate EPG update for \${sources.size} sources.")

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val inputData = Data.Builder()
                .putStringArray(EpgUpdateWorker.KEY_URLS, sources.toTypedArray())
                .build()

            val oneTimeRequest = OneTimeWorkRequestBuilder<EpgUpdateWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context).enqueue(oneTimeRequest)
        }
    }

}
