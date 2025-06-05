package com.hereliesaz.cuedetat

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import android.util.Log
import com.hereliesaz.cuedetat.config.AppConfig // For TAG
// REMOVE THIS LINE: import org.opencv.android.OpenCVLoader // Remove OpenCVLoader import

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.i(AppConfig.TAG, "MyApplication onCreate: Forcing Light Theme (MODE_NIGHT_NO).")
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // REMOVED BLOCK - No longer needed for OpenCV initialization
        /*
        if (!OpenCVLoader.initDebug()) { // initDebug is for development, initAsync is better for production
            Log.e(AppConfig.TAG, "Failed to initialize OpenCV!")
            // Consider showing a user-friendly message or disabling features if OpenCV is crucial
        } else {
            Log.i(AppConfig.TAG, "OpenCV initialized successfully.")
        }
        */
    }
}