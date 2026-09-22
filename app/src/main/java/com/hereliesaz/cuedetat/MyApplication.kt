package com.hereliesaz.cuedetat

import android.app.Application
import android.util.Log
import com.hereliesaz.cuedetat.data.MetaWearableRepository
import dagger.hilt.android.HiltAndroidApp
import org.opencv.android.OpenCVLoader

/**
 * The Application class, annotated with @HiltAndroidApp, is the entry point for Hilt
 * and is responsible for creating the top-level dependency container.
 */
@HiltAndroidApp
class MyApplication : Application() {

    /**
     * Injected rather than referenced directly: the vendor SDK is play-flavour
     * only, and `src/main` must not name it (see [MetaWearableRepository]).
     */
    @javax.inject.Inject
    lateinit var metaWearableRepository: MetaWearableRepository

    override fun onCreate() {
        super.onCreate()

        // The AEADBadTagException workaround (clearing the Meta SDK's encrypted
        // storage files when the KeyStore falls out of sync with them) now lives
        // in PlayMetaWearableRepository.initialize(), gated on that specific
        // failure, instead of running unconditionally here on every cold start.
        // See PlayMetaWearableRepository for the guarded clear-and-retry.

        // Only initialize Wearables if we have the necessary permissions,
        // or let it fail gracefully if called here. On first launch, 
        // MainActivity will re-trigger this after permissions are granted.
        if (hasBluetoothPermissions()) {
            initializeWearables()
        }

        if (OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "OpenCV loaded successfully!")
        } else {
            Log.e("OpenCV", "Unable to load OpenCV!")
        }
    }

    private fun hasBluetoothPermissions(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.BLUETOOTH_CONNECT
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun initializeWearables() {
        metaWearableRepository.initialize()
    }
}
