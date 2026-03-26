#!/bin/bash
sed -i.bak '/val cacheDataSourceFactory = CacheDataSource.Factory()/,/\.setFlags(CacheDataSource\.FLAG_IGNORE_CACHE_ON_ERROR)/ { s/^/\/\/ / }' app/src/main/java/com/example/dokodemotv/viewmodel/PlayerViewModel.kt
sed -i.bak 's/\.setDataSourceFactory(cacheDataSourceFactory)/\.setDataSourceFactory(httpDataSourceFactory) \/\/ .setDataSourceFactory(cacheDataSourceFactory)/g' app/src/main/java/com/example/dokodemotv/viewmodel/PlayerViewModel.kt
