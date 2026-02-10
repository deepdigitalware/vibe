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
        AdManager.loadAppOpenAd(this, "ca-app-pub-2509091968835675/8562719984")
    }
}
