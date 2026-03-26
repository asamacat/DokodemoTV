package com.example.dokodemotv.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.dokodemotv.repository.DokodemoDatabase

class DokodemoViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlayerViewModel::class.java)) {
            val database = DokodemoDatabase.getDatabase(application)
            @Suppress("UNCHECKED_CAST")
            return PlayerViewModel(application, database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
