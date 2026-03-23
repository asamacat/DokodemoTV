package com.example.dokodemotv.util

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

object CacheManager {
    private var cache: SimpleCache? = null

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun getCache(context: Context, cacheSizeMb: Int): SimpleCache {
        val currentCache = cache
        if (currentCache != null) {
            return currentCache
        }

        val cacheDir = File(context.cacheDir, "media")
        val evictor = LeastRecentlyUsedCacheEvictor((cacheSizeMb * 1024 * 1024).toLong())
        val databaseProvider = StandaloneDatabaseProvider(context)
        val newCache = SimpleCache(cacheDir, evictor, databaseProvider)
        cache = newCache
        return newCache
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun releaseCache() {
        cache?.release()
        cache = null
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun reinitializeCache(context: Context, newSizeMb: Int) {
        releaseCache()
        getCache(context, newSizeMb)
    }
}
