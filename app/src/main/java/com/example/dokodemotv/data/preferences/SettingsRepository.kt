package com.example.dokodemotv.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "recording_settings")

enum class RecordingEngine {
    CUSTOM, MEDIA3, OFF
}

enum class StorageType {
    LOCAL, NAS
}

data class RecordingSettings(
    val engine: RecordingEngine,
    val storageType: StorageType,
    val nasIp: String,
    val nasShare: String,
    val nasUser: String,
    val nasPass: String
)

class SettingsRepository(private val context: Context) {

    private val ENGINE_KEY = stringPreferencesKey("recording_engine")
    private val STORAGE_TYPE_KEY = stringPreferencesKey("storage_type")
    private val NAS_IP_KEY = stringPreferencesKey("nas_ip")
    private val NAS_SHARE_KEY = stringPreferencesKey("nas_share")
    private val NAS_USER_KEY = stringPreferencesKey("nas_user")
    private val NAS_PASS_KEY = stringPreferencesKey("nas_pass")

    val settingsFlow: Flow<RecordingSettings> = context.dataStore.data
        .map { preferences ->
            val engineStr = preferences[ENGINE_KEY] ?: RecordingEngine.CUSTOM.name
            val storageTypeStr = preferences[STORAGE_TYPE_KEY] ?: StorageType.LOCAL.name

            RecordingSettings(
                engine = runCatching { RecordingEngine.valueOf(engineStr) }.getOrDefault(RecordingEngine.CUSTOM),
                storageType = runCatching { StorageType.valueOf(storageTypeStr) }.getOrDefault(StorageType.LOCAL),
                nasIp = preferences[NAS_IP_KEY] ?: "",
                nasShare = preferences[NAS_SHARE_KEY] ?: "Public",
                nasUser = preferences[NAS_USER_KEY] ?: "guest",
                nasPass = preferences[NAS_PASS_KEY] ?: ""
            )
        }

    suspend fun updateEngine(engine: RecordingEngine) {
        context.dataStore.edit { preferences ->
            preferences[ENGINE_KEY] = engine.name
        }
    }

    suspend fun updateStorageType(storageType: StorageType) {
        context.dataStore.edit { preferences ->
            preferences[STORAGE_TYPE_KEY] = storageType.name
        }
    }

    suspend fun updateNasDetails(ip: String, share: String, user: String, pass: String) {
        context.dataStore.edit { preferences ->
            preferences[NAS_IP_KEY] = ip
            preferences[NAS_SHARE_KEY] = share
            preferences[NAS_USER_KEY] = user
            preferences[NAS_PASS_KEY] = pass
        }
    }
}
