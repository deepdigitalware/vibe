package com.vibe

import android.app.Application
import android.content.Context
import com.vibe.util.AdManager

class VibeApplication : Application() {

    companion object {
        lateinit var instance: VibeApplication
            private set
            
        fun getContext(): Context = instance.applicationContext
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        AdManager.initialize(this)
        com.vibe.util.NotificationHelper.createNotificationChannel(this)
        com.vibe.util.SocketManager.init(this)
        // Load App Open Ad using ID from strings.xml
        AdManager.loadAppOpenAd(this, getString(R.string.admob_app_open_id))
    }
}
