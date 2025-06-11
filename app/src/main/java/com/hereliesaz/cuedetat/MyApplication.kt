package com.hereliesaz.cuedetat

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * The Application class, annotated with @HiltAndroidApp, is the entry point for Hilt
 * and is responsible for creating the top-level dependency container.
 */
@HiltAndroidApp
class MyApplication : Application() {
    // This class can be empty. Its presence and annotation are all that's needed for Hilt.
}
