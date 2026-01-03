package com.hereliesaz.cuedetat.di

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CueDetatApplication : Application() {
    // This class can be left empty. Its purpose is to hold the
    // @HiltAndroidApp annotation, which triggers Hilt's code generation.
}