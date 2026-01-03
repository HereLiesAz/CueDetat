package com.hereliesaz.cuedetat.di

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.opencv.android.OpenCVLoader

@HiltAndroidApp
class CueDetatApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        OpenCVLoader.initDebug()
    }
}