package com.hereliesaz.cuedetat

import android.app.Application
import android.content.Context
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
     * SplitCompat must be installed at application startup for Play on-demand
     * modules. Use reflection so the FOSS flavor stays free of Play Core.
     */
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        try {
            val splitCompat = Class.forName("com.google.android.play.core.splitcompat.SplitCompat")
            val installed = splitCompat
                .getMethod("install", Context::class.java)
                .invoke(null, this) as? Boolean
            if (installed == false) {
                Log.e(TAG, "SplitCompat reported startup installation failure")
            }
        } catch (_: ClassNotFoundException) {
            // Expected in the FOSS flavor: there are no on-demand Play splits.
        } catch (t: Throwable) {
            Log.e(TAG, "SplitCompat startup installation failed", t)
        }
    }

    /**
     * Injected rather than referenced directly: the vendor SDK is play-flavour
     * only, and `src/main` must not name it (see [MetaWearableRepository]).
     */
    @javax.inject.Inject
    lateinit var metaWearableRepository: MetaWearableRepository

    override fun onCreate() {
        super.onCreate()
        
        // Workaround for AEADBadTagException in Meta SDK's EncryptedSharedPreferences.
        // If the KeyStore becomes out of sync with the files (e.g. after a restore),
        // the SDK fails to initialize its storage. We clear the potentially 
        // corrupted files to allow a fresh start.
        clearMetaWearableStorageWorkaround()

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


    private fun clearMetaWearableStorageWorkaround() {
        // Known file names used by Meta SDK for encrypted storage.
        // Deleting these forces the SDK to recreate them with the current KeyStore.
        val sdkFiles = listOf(
            "ManifestRecordStore",
            "DeviceRecordStore",
            "acdc_manifest_store",
            "acdc_device_store"
        )
        sdkFiles.forEach { fileName ->
            deleteSharedPreferences(fileName)
        }
    }

    companion object {
        private const val TAG = "CueDetatApp"
    }
}
