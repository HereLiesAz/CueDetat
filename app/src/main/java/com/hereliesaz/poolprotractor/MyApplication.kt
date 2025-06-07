package com.hereliesaz.poolprotractor

import android.app.Application
import android.util.Log

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("PoolProtractorApp", "MyApplication onCreate.")
        // Theming is now handled entirely by Jetpack Compose with Material 3.
        // No need to force a light or dark mode here via AppCompatDelegate.
        // The PoolProtractorTheme composable will respect the system setting.
    }
}
