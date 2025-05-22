package com.hereliesaz.poolprotractor

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import android.util.Log

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("PoolProtractorApp", "MyApplication onCreate: Forcing Light Theme (MODE_NIGHT_NO).")
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}