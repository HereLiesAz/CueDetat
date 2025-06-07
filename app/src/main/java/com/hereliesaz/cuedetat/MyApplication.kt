package com.hereliesaz.cuedetat

import android.app.Application
import android.util.Log
import androidx.multidex.MultiDex
import dagger.hilt.android.HiltAndroidApp


@HiltAndroidApp

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MultiDex.install(this)
        Log.d("PoolProtractorApp", "MyApplication onCreate.")
        // Theming is now handled entirely by Jetpack Compose with Material 3.
        // No need to force a light or dark mode here via AppCompatDelegate.
        // The PoolProtractorTheme composable will respect the system setting.
    }
}