package com.example.dokodemotv.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {
    private const val TAG = "UpdateChecker"
    private const val GITHUB_API_URL = "https://api.github.com/repos/asamacat/DokodemoTV/releases/latest"

    data class GitHubRelease(
        val tag_name: String,
        val name: String,
        val assets: List<GitHubAsset>
    )

    data class GitHubAsset(
        val name: String,
        val browser_download_url: String
    )

    data class UpdateInfo(
        val isUpdateAvailable: Boolean,
        val latestVersion: String,
        val downloadUrl: String?
    )

    suspend fun checkForUpdates(context: Context): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = InputStreamReader(connection.inputStream)
                val release = Gson().fromJson(reader, GitHubRelease::class.java)
                reader.close()

                val currentVersion = getCurrentVersionName(context)
                val latestVersion = release.tag_name.removePrefix("v") // assuming tags are like "v1.0.1"

                Log.d(TAG, "Current version: $currentVersion, Latest version: $latestVersion")

                val isUpdateAvailable = isVersionGreater(latestVersion, currentVersion)

                // Find APK asset
                val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }

                return@withContext UpdateInfo(
                    isUpdateAvailable = isUpdateAvailable,
                    latestVersion = latestVersion,
                    downloadUrl = apkAsset?.browser_download_url
                )
            } else {
                Log.e(TAG, "Failed to check for updates. HTTP Code: ${connection.responseCode}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates", e)
        }

        return@withContext UpdateInfo(false, "", null)
    }

    private fun getCurrentVersionName(context: Context): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "0.0"
        } catch (e: PackageManager.NameNotFoundException) {
            "0.0"
        }
    }

    // Simple version comparison assuming semantic versioning like "1.0.0"
    private fun isVersionGreater(latest: String, current: String): Boolean {
        try {
            val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
            val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }

            val maxLength = maxOf(latestParts.size, currentParts.size)

            for (i in 0 until maxLength) {
                val l = latestParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }

                if (l > c) return true
                if (l < c) return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error comparing versions", e)
        }
        return false
    }
}
