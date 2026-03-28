package com.example.dokodemotv.data.storage

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalStorageManager(private val context: Context) : StorageManager {

    private val recordDir: File by lazy {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "DokodemoTV_Records")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    override suspend fun getOutputStream(fileName: String): OutputStream? = withContext(Dispatchers.IO) {
        val file = File(recordDir, fileName)
        return@withContext try {
            FileOutputStream(file, true) // Append mode true for combining segments
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun fileExists(fileName: String): Boolean = withContext(Dispatchers.IO) {
        File(recordDir, fileName).exists()
    }

    override suspend fun deleteFile(fileName: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(recordDir, fileName)
        if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }
}
