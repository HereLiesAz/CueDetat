package com.hereliesaz.cuedetat

import android.app.Application
import android.util.Log
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
    }
}