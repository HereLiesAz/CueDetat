package com.hereliesaz.cuedetat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Session
import com.google.ar.core.exceptions.CameraNotAvailableException
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        if (arSession == null) {
            if (!hasCameraPermission()) {
                requestCameraPermission()
                return
            }
            if (!isArCoreAvailableAndInstalled()) {
                return
            }
            arSession = Session(this).also {
                viewModel.onEvent(UiEvent.SetSession(it))
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

    private fun isArCoreAvailableAndInstalled(): Boolean {
        return try {
            when (ArCoreApk.getInstance().requestInstall(this, userRequestedInstall)) {
                ArCoreApk.InstallStatus.INSTALLED -> true
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                    userRequestedInstall = false
                    false
                }
                else -> {
                    Toast.makeText(this, "ARCore is not available on this device.", Toast.LENGTH_LONG).show()
                    finish()
                    false
                }
            }
        } catch (e: UnavailableUserDeclinedInstallationException) {
            Toast.makeText(this, "ARCore is required. Please install it.", Toast.LENGTH_LONG).show()
            finish()
            false
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to create AR session: $e", Toast.LENGTH_LONG).show()
            finish()
            false
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            0
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (!hasCameraPermission()) {
            Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_LONG).show()
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                // User has selected "Don't ask again".
            }
            finish()
        }
    }
}