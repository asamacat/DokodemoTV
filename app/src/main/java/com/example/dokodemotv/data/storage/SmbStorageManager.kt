package com.example.dokodemotv.data.storage

import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import jcifs.context.BaseContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import java.util.Properties
import jcifs.config.PropertyConfiguration

class SmbStorageManager(
    private val ip: String,
    private val share: String,
    private val user: String,
    private val pass: String
) : StorageManager {

    private val baseContext: jcifs.context.BaseContext by lazy {
        val prop = Properties()
        prop.setProperty("jcifs.smb.client.enableSMB2", "true")
        prop.setProperty("jcifs.smb.client.disableSMB1", "false")
        prop.setProperty("jcifs.resolveOrder", "BCAST,DNS")
        val config = PropertyConfiguration(prop)
        val auth = NtlmPasswordAuthenticator("", user, pass)
        BaseContext(config).withCredentials(auth) as BaseContext
    }

    private fun getSmbFile(fileName: String): SmbFile {
        val url = "smb://$ip/$share/$fileName"
        return SmbFile(url, baseContext)
    }

    override suspend fun getOutputStream(fileName: String): OutputStream? = withContext(Dispatchers.IO) {
        return@withContext try {
            val file = getSmbFile(fileName)
            if (!file.exists()) {
                file.createNewFile()
            }
            file.openOutputStream(true) // append = true
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun fileExists(fileName: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            getSmbFile(fileName).exists()
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun deleteFile(fileName: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val file = getSmbFile(fileName)
            if (file.exists()) {
                file.delete()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
