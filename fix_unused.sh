#!/bin/bash
sed -i.bak 's/val cache = CacheManager.getCache(app, prefs.cacheSizeMb)/\/\/ val cache = CacheManager.getCache(app, prefs.cacheSizeMb)/g' app/src/main/java/com/example/dokodemotv/viewmodel/PlayerViewModel.kt
