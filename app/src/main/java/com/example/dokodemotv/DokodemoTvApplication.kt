package com.example.dokodemotv

import android.app.Application
import com.example.dokodemotv.epg.EpgManager

class DokodemoTvApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        EpgManager.schedulePeriodicEpgUpdate(this)
    }
}
