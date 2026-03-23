package com.example.dokodemotv.util

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("dokodemotv_prefs", Context.MODE_PRIVATE)

    var savedFolderUri: String?
        get() = prefs.getString("saved_folder_uri", null)
        set(value) = prefs.edit().putString("saved_folder_uri", value).apply()

    var lastWatchedUrl: String?
        get() = prefs.getString("last_watched_url", null)
        set(value) = prefs.edit().putString("last_watched_url", value).apply()

    var cacheSizeMb: Int
        get() = prefs.getInt("cache_size_mb", 100) // Default 100MB
        set(value) = prefs.edit().putInt("cache_size_mb", value).apply()
}
