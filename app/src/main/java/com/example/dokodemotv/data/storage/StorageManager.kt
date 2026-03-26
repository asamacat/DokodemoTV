package com.example.dokodemotv.data.storage

import android.content.Context
import java.io.InputStream
import java.io.OutputStream

interface StorageManager {
    suspend fun getOutputStream(fileName: String): OutputStream?
    suspend fun fileExists(fileName: String): Boolean
    suspend fun deleteFile(fileName: String): Boolean
}
