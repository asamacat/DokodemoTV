package com.example.dokodemotv.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ApkInstaller(private val context: Context, private val downloadId: Long) : BroadcastReceiver() {
    private val TAG = "ApkInstaller"

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadId) {
                installApk(context, downloadId)
                // Unregister receiver after download completes
                try {
                    context.unregisterReceiver(this)
                } catch (e: IllegalArgumentException) {
                    Log.w(TAG, "Receiver not registered", e)
                }
            }
        }
    }

    private fun installApk(context: Context, downloadId: Long) {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val query = DownloadManager.Query()
            query.setFilterById(downloadId)

            val cursor = downloadManager.query(query)
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val status = cursor.getInt(columnIndex)

                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    val apkUri = downloadManager.getUriForDownloadedFile(downloadId)

                    if (apkUri != null) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error launching install intent", e)
                        }
                    } else {
                        Log.e(TAG, "APK URI is null")
                    }
                } else {
                    Log.e(TAG, "Download failed with status: $status")
                }
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error installing APK", e)
        }
    }
}
