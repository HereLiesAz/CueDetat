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
import com.hereliesaz.cuedetat.ui.AppRoot
import com.hereliesaz.cuedetat.ui.MainScreen
import com.hereliesaz.cuedetat.ui.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    var arSession: Session? = null
    private var userRequestedInstall = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            AppRoot(uiState = uiState) {
                MainScreen(
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                    arSession = arSession
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (arSession == null) {
            if (!requestCameraPermission()) {
                return
            }
            try {
                when (ArCoreApk.getInstance().requestInstall(this, userRequestedInstall)) {
                    ArCoreApk.InstallStatus.INSTALLED -> {
                        arSession = Session(this)
                    }
                    ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                        userRequestedInstall = false
                        return
                    }
                    else -> {
                        // Another state, just return for now
                        return
                    }
                }
            } catch (e: UnavailableUserDeclinedInstallationException) {
                Toast.makeText(this, "Please install ARCore", Toast.LENGTH_LONG).show()
                finish()
                return
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to create AR session: $e", Toast.LENGTH_LONG).show()
                finish()
                return
            }
        }

        try {
            arSession?.resume()
        } catch (e: CameraNotAvailableException) {
            Toast.makeText(this, "Camera not available. Try restarting the app.", Toast.LENGTH_LONG).show()
            arSession = null
            return
        }
        // Redraw the composable to pass the session
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            AppRoot(uiState = uiState) {
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

    private fun requestCameraPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 0)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, onResume will handle the rest.
            } else {
                Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG).show()
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    // User selected "Don't ask again". Direct them to settings.
                }
                finish()
            }
        }
    }
}
