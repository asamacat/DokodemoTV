package com.example.dokodemotv.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dokodemotv.data.local.EpgDatabase
import com.example.dokodemotv.data.local.entity.ChannelMapping
import com.example.dokodemotv.data.local.entity.EpgChannel
import com.example.dokodemotv.data.local.entity.EpgProgram
import com.example.dokodemotv.epg.EpgManager
import com.example.dokodemotv.epg.EpgRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EpgViewModel(application: Application) : AndroidViewModel(application) {
    private val epgDao = EpgDatabase.getDatabase(application).epgDao()
    private val repository = EpgRepository(epgDao, application)

    val epgChannels: StateFlow<List<EpgChannel>> = repository.getAllEpgChannels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activePrograms: StateFlow<List<EpgProgram>> = epgDao.getAllActivePrograms(System.currentTimeMillis())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val channelMappings = repository.getAllChannelMappings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun refreshEpgData() {
        EpgManager.triggerImmediateUpdate(getApplication())
    }

    fun mapChannel(streamUrl: String, tvgId: String) {
        viewModelScope.launch {
            epgDao.insertChannelMapping(ChannelMapping(streamUrl, tvgId))
        }
    }

    fun unmapChannel(streamUrl: String) {
        viewModelScope.launch {
            epgDao.deleteChannelMapping(streamUrl)
        }
    }
}
