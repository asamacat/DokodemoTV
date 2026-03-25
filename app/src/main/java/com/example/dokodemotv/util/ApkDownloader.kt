package com.example.dokodemotv.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log

object ApkDownloader {
    private const val TAG = "ApkDownloader"

    fun downloadApk(context: Context, url: String, fileName: String = "dokodemotv_update.apk"): Long {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

            val uri = Uri.parse(url)
            val request = DownloadManager.Request(uri)
                .setTitle("DokodemoTV Update")
                .setDescription("Downloading latest version...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            Log.d(TAG, "Starting download from $url")
            return downloadManager.enqueue(request)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start download", e)
            return -1L
        }
    }
}
