package com.example.dokodemotv.epg

import android.content.Context
import android.util.Log
import com.example.dokodemotv.data.local.dao.EpgDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date

class EpgRepository(private val epgDao: EpgDao, private val context: Context) {

    companion object {
        private const val TAG = "EpgRepository"
        private const val PREFS_NAME = "epg_prefs"
        private const val KEY_LAST_MODIFIED = "last_modified_"
    }

    suspend fun updateEpgFromUrl(urlStr: String, force: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastModified = prefs.getLong(KEY_LAST_MODIFIED + urlStr, 0L)

        try {
            val url = URL(urlStr)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            if (!force && lastModified > 0) {
                connection.ifModifiedSince = lastModified
            }

            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
                Log.d(TAG, "EPG not modified since last check.")
                return@withContext true
            }

            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Failed to fetch EPG: HTTP $responseCode")
                return@withContext false
            }

            val newLastModified = connection.lastModified

            connection.inputStream.use { inputStream ->
                val parser = EpgParser()
                val (channels, programs) = parser.parse(inputStream)

                Log.d(TAG, "Parsed \${channels.size} channels and \${programs.size} programs.")

                // Insert into Room DB
                epgDao.clearAndInsertEpgData(channels, programs)

                // Update preferences
                if (newLastModified > 0) {
                    prefs.edit().putLong(KEY_LAST_MODIFIED + urlStr, newLastModified).apply()
                } else {
                    prefs.edit().putLong(KEY_LAST_MODIFIED + urlStr, System.currentTimeMillis()).apply()
                }
            }

            // Cleanup old data
            garbageCollect()

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating EPG from URL: $urlStr", e)
            false
        }
    }

    private suspend fun garbageCollect() = withContext(Dispatchers.IO) {
        val cutoffTime = System.currentTimeMillis() - 24 * 60 * 60 * 1000 // 24 hours ago
        epgDao.deleteOldPrograms(cutoffTime)
        Log.d(TAG, "Deleted programs older than \${Date(cutoffTime)}")
    }

    fun getEpgForChannel(tvgId: String) = epgDao.getProgramsForChannel(tvgId, System.currentTimeMillis())

    fun getAllEpgChannels() = epgDao.getAllEpgChannels()

    fun getAllChannelMappings() = epgDao.getAllChannelMappings()
}
