package com.hereliesaz.cuedetat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
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
import com.google.ar.core.exceptions.*
import com.hereliesaz.cuedetat.ui.MainScreen
import com.hereliesaz.cuedetat.ui.MainViewModel
import com.hereliesaz.cuedetat.ui.theme.CueDetatTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    val viewModel: MainViewModel by viewModels()
    var arSession: Session? = null
        private set

    private var userRequestedInstall = true
    private var isFlashlightOn = false

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Re-trigger onResume to continue setup
                this.recreate()
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (hasCameraPermission()) {
            setupContent()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasCameraPermission()) {
            // Session creation and resume is now handled inside setupContent to avoid lifecycle issues
            if (arSession == null) {
                setupContent()
            }
            arSession?.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        arSession?.pause()
        if (isFlashlightOn) {
            toggleFlashlight(true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        arSession?.close()
    }

    private fun setupContent() {
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
                config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                arSession?.configure(config)
            } catch (e: Exception) {
                handleSessionCreationException(e)
                return
            }
        }

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            CueDetatTheme(darkTheme = uiState.isDarkMode) {
                MainScreen(
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                    activity = this
                )
            }
        }
    }

    fun toggleFlashlight(forceOff: Boolean = false) {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = cameraManager.cameraIdList[0]
            val newState = if (forceOff) false else !isFlashlightOn
            cameraManager.setTorchMode(cameraId, newState)
            isFlashlightOn = newState
        } catch (e: Exception) {
            isFlashlightOn = false
            Toast.makeText(this, "Flashlight not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasCameraPermission(): Boolean = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

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