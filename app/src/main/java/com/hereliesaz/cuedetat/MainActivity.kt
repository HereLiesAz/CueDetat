// app/src/main/java/com/hereliesaz/cuedetat/MainActivity.kt
package com.hereliesaz.cuedetat

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Session
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import com.hereliesaz.cuedetat.ui.MainScreen
import com.hereliesaz.cuedetat.ui.MainViewModel
import com.hereliesaz.cuedetat.ui.theme.CueDetatTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.opencv.android.OpenCVLoader


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private var arSession: Session? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Attempt to enable ARCore if not already done, or check for its availability
        maybeEnableArCore()
        if (!OpenCVLoader.initDebug())
            Log.e("OpenCV", "Unable to load OpenCV!");
        else
            Log.d("OpenCV", "OpenCV loaded Successfully!");
        setContent {
            CueDetatTheme {
                MainScreen(viewModel = viewModel)
            }
        }

        // Observe the spatial lock state from the ViewModel
        // and manage the AR session accordingly.
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { uiState ->
                if (uiState.isSpatiallyLocked) {
                    // If switching to locked (AR) mode, ensure AR session is created and resumed
                    if (arSession == null) {
                        tryCreateArSession()
                    } else {
                        try {
                            arSession?.resume()
                        } catch (e: Exception) {
                            Log.e("ARCore", "Failed to resume AR session on lock enable", e)
                        }
                    }
                } else {
                    // If switching off locked (AR) mode, pause AR session
                    arSession?.pause()
                }
            }
        }
    }

    /**
     * Checks ARCore availability and attempts to install it if needed.
     * This function should be called early in the Activity lifecycle.
     */
    private fun maybeEnableArCore() {
        val availability = ArCoreApk.getInstance().checkAvailability(this)

        if (availability.isTransient) {
            // ARCore is not yet available, but might be soon. Retry later.
            Log.d("ARCore", "ARCore availability is transient. Retrying soon.")
            // Consider adding a delay or retry mechanism here if critical for initial load
            return
        }

        if (availability.isSupported) {
            Log.d("ARCore", "ARCore is supported on this device.")
            // If supported, attempt to create the AR session immediately
            tryCreateArSession()
        } else {
            Log.w("ARCore", "ARCore is NOT supported on this device.")
            // Handle cases where ARCore is permanently not supported on the device.
            // You might want to disable AR-related UI elements or show a message.
        }
    }

    /**
     * Attempts to create an ARCore session. Handles installation requests if necessary.
     */
    private fun tryCreateArSession() {
        if (arSession != null) {
            Log.d("ARCore", "AR Session already exists. Skipping creation.")
            return // Session already exists
        }

        try {
            when (ArCoreApk.getInstance().requestInstall(this, true)) { // `true` requests user installation if needed
                ArCoreApk.InstallStatus.INSTALLED -> {
                    // ARCore is installed. Create the AR session.
                    arSession = Session(this)
                    Log.d("ARCore", "AR Session created successfully.")
                    // Inform ViewModel about the created AR session
                    viewModel.onArSessionCreated(arSession!!)
                }
                // If 'INSTALL_REQUEST_NEEDED' remains unresolved, please verify your ARCore SDK setup in build.gradle.kts
                // and ensure Gradle caches are cleared. This is a build environment issue.
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                    Log.d("ARCore", "ARCore installation requested. Waiting for user interaction.")
                    // User will be prompted to install Google Play Services for AR.
                    // The activity will resume (via onResume) when installation is complete or cancelled.
                }
                else -> { // Added else branch to make 'when' exhaustive
                    Log.e("ARCore", "Unhandled ARCore installation status.")
                }
            }
        } catch (e: UnavailableUserDeclinedInstallationException) {
            Log.e("ARCore", "User declined ARCore installation.", e)
            // Inform ViewModel or show user-friendly message
        } catch (e: UnavailableDeviceNotCompatibleException) {
            Log.e("ARCore", "Device not compatible with ARCore.", e)
            // Inform ViewModel or show user-friendly message
        } catch (e: UnavailableArcoreNotInstalledException) {
            Log.e("ARCore", "ARCore not installed unexpectedly.", e)
            // This case should ideally be caught by requestInstall mostly, but here as a fallback
        } catch (e: UnavailableSdkTooOldException) {
            Log.e("ARCore", "ARCore SDK version too old.", e)
        } catch (e: UnavailableException) {
            Log.e("ARCore", "ARCore is unavailable for an unknown reason.", e)
        } catch (e: Exception) { // Catch any other unexpected exceptions during session creation
            Log.e("ARCore", "Unexpected error during AR session creation.", e)
        }
    }

    override fun onResume() {
        super.onResume()
        // Resume AR session only if it exists and the app is in spatially locked (AR) mode
        if (viewModel.uiState.value.isSpatiallyLocked) {
            try {
                arSession?.resume()
                Log.d("ARCore", "AR Session resumed from onResume.")
            } catch (e: Exception) {
                Log.e("ARCore", "Failed to resume AR session in onResume", e)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Pause AR session regardless of spatial lock state when activity pauses
        arSession?.pause()
        Log.d("ARCore", "AR Session paused from onPause.")
    }

    override fun onDestroy() {
        super.onDestroy()
        // Close AR session when activity is destroyed
        arSession?.close()
        arSession = null
        Log.d("ARCore", "AR Session closed from onDestroy.")
    }
}