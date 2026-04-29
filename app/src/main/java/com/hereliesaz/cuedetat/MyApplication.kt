package com.hereliesaz.cuedetat

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import com.hereliesaz.cuedetat.service.OrientationTrackingService
import com.meta.wearable.dat.core.Wearables
import dagger.hilt.android.HiltAndroidApp
import org.opencv.android.OpenCVLoader

/**
 * The Application class, annotated with @HiltAndroidApp, is the entry point for Hilt
 * and is responsible for creating the top-level dependency container.
 */
@HiltAndroidApp
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Wearables.initialize(this)
        if (OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "OpenCV loaded successfully!")
        } else {
            Log.e("OpenCV", "Unable to load OpenCV!")
        }
        val channel = NotificationChannel(
            OrientationTrackingService.CHANNEL_ID,
            "Table Orientation",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "Tracks table orientation while screen is off"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}