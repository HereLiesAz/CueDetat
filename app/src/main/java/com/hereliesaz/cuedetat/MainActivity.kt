package com.hereliesaz.cuedetat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import com.hereliesaz.cuedetat.ui.MainScreen
import com.hereliesaz.cuedetat.ui.MainViewModel
import com.hereliesaz.cuedetat.ui.state.UiEvent
import com.hereliesaz.cuedetat.ui.theme.CueDetatTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var arSession: Session? = null
    private var userRequestedInstall = true

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. onResume will now handle the setup.
            } else {
                Toast.makeText(this, "Camera permission is required to run this application", Toast.LENGTH_LONG).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    override fun onResume() {
        super.onResume()

        if (!hasCameraPermission()) {
            requestCameraPermission()
            return
        }

        if (arSession == null) {
            try {
                when (ArCoreApk.getInstance().requestInstall(this, userRequestedInstall)) {
                    ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                        userRequestedInstall = false
                        return
                    }
                    ArCoreApk.InstallStatus.INSTALLED -> {}
                }

                arSession = Session(this)

                val config = Config(arSession)

                // *** THIS IS THE CRUCIAL FIX ***
                // Tell ARCore to analyze the latest camera image on every frame.
                // This is required for reliable plane detection.
                config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE

                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                arSession?.configure(config)

                viewModel.onEvent(UiEvent.SetSession(arSession))

            } catch (e: Exception) {
                handleSessionCreationException(e)
                return
            }
        }

        try {
            arSession?.resume()
        } catch (e: CameraNotAvailableException) {
            Toast.makeText(this, "Camera not available. Try restarting.", Toast.LENGTH_LONG).show()
            arSession = null
            return
        }

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            CueDetatTheme(darkTheme = uiState.isDarkMode) {
                MainScreen(
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                    arSession = arSession
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        arSession?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        arSession?.close()
        arSession = null
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun handleSessionCreationException(e: Exception) {
        val message = when (e) {
            is UnavailableApkTooOldException -> "Please update ARCore"
            is UnavailableSdkTooOldException -> "Please update this app"
            is UnavailableDeviceNotCompatibleException -> "This device does not support AR"
            is UnavailableUserDeclinedInstallationException -> "ARCore installation is required"
            else -> "Failed to create AR session: $e"
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }
}