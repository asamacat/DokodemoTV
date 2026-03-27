package com.example.dokodemotv.epg

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.dokodemotv.data.local.EpgDatabase

class EpgUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "EpgUpdateWorker"
        const val KEY_URLS = "epg_urls"
    }

    override suspend fun doWork(): Result {
        val urlsArray = inputData.getStringArray(KEY_URLS) ?: return Result.failure()

        Log.d(TAG, "Starting EPG update for \${urlsArray.size} URLs")

        val database = EpgDatabase.getDatabase(applicationContext)
        val repository = EpgRepository(database.epgDao(), applicationContext)

        var allSuccess = true
        for (url in urlsArray) {
            val success = repository.updateEpgFromUrl(url)
            if (!success) {
                allSuccess = false
            }
        }

        return if (allSuccess) {
            Log.d(TAG, "EPG update completed successfully")
            Result.success()
        } else {
            Log.w(TAG, "EPG update completed with some failures")
            Result.retry() // Or Result.success() depending on retry strategy
        }
    }
}
