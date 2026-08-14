package com.cse489.landmarks

import android.app.Application
import org.osmdroid.config.Configuration

class LandmarkApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize osmdroid configuration
        Configuration.getInstance().userAgentValue = packageName
    }
}
