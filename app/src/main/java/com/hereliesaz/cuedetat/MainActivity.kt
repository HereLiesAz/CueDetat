package com.hereliesaz.cuedetat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.core.content.ContextCompat
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.utils.SecurityUtils
import com.hereliesaz.cuedetat.view.state.SingleEvent
import androidx.core.view.WindowCompat
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.*
import com.hereliesaz.cuedetat.ar.jetpack.ArRenderer
import com.hereliesaz.cuedetat.ar.jetpack.helpers.DisplayRotationHelper
import com.hereliesaz.cuedetat.ui.MainScreen
import com.hereliesaz.cuedetat.ui.MainViewModel
import com.hereliesaz.cuedetat.ui.theme.CueDetatTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.net.toUri

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    val viewModel: MainViewModel by viewModels()
    var arSession: Session? = null
        private set
    lateinit var glSurfaceView: GLSurfaceView
    private var renderer: ArRenderer? = null

    private var userRequestedInstall = true
    private var isFlashlightOn = false
    private var backCameraId: String? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                this.recreate()
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        initializeCameraId()

        if (hasCameraPermission()) {
            setupArSessionAndSetContent()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!this::glSurfaceView.isInitialized) return

        try {
            arSession?.resume()
            glSurfaceView.onResume()
        } catch (e: CameraNotAvailableException) {
            handleSessionCreationException(e)
        }
    }

    override fun onPause() {
        super.onPause()
        if (!this::glSurfaceView.isInitialized) return

        glSurfaceView.onPause()
        arSession?.pause()
        if (isFlashlightOn) {
            toggleFlashlight(forceOff = true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (arSession != null) {
            arSession?.close()
            arSession = null
        }
    }

    private fun setupArSessionAndSetContent() {
        if (arSession != null) return

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
            config.lightEstimationMode = Config.LightEstimationMode.AMBIENT_INTENSITY
            config.depthMode = Config.DepthMode.AUTOMATIC
            arSession?.configure(config)

            glSurfaceView = GLSurfaceView(this).apply {
                preserveEGLContextOnPause = true
                setEGLContextClientVersion(2)
                setEGLConfigChooser(8, 8, 8, 8, 16, 0)

                // The renderer is created with a lambda that calls the ViewModel event handler.
                renderer = ArRenderer(
                    session = arSession!!,
                    displayRotationHelper = DisplayRotationHelper(this@MainActivity),
                    onTrackingStateChanged = { _, _ ->
                        // viewModel.onEvent(MainScreenEvent.ArTrackingStateUpdate(trackingState, failureReason))
                    }
                ).also { setRenderer(it) }

                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            }
            glSurfaceView.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    // viewModel.onEvent(MainScreenEvent.ArTap(Offset(event.x, event.y)))
                }
                true
            }

        } catch (e: Exception) {
            handleSessionCreationException(e)
            return
        }

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val singleEvent by viewModel.singleEvent.collectAsState(initial = null)

            LaunchedEffect(singleEvent) {
                singleEvent?.let { event ->
                    when (event) {
                        is SingleEvent.OpenUrl -> {
                            if (SecurityUtils.isSafeUrl(event.url)) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(event.url))
                                startActivity(intent)
                            } else {
                                Log.e("MainActivity", "Blocked unsafe URL: ${event.url}")
                            }
                        }
                        is SingleEvent.SendFeedbackEmail -> {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = "mailto:".toUri()
                                putExtra(Intent.EXTRA_EMAIL, arrayOf(event.email))
                                putExtra(Intent.EXTRA_SUBJECT, event.subject)
                            }
                            try {
                                startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(this@MainActivity, "No email app found", Toast.LENGTH_SHORT).show()
                            }
                        }
                        is SingleEvent.InitiateHaterMode -> {
                            Toast.makeText(this@MainActivity, "Hater Mode Initiated!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    viewModel.onEvent(MainScreenEvent.SingleEventConsumed)
                }
            }

            // renderer?.updateState(uiState)

            CueDetatTheme(darkTheme = false) {
                MainScreen(
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                    activity = this,
                    glSurfaceView = glSurfaceView
                )
            }
        }
    }

    private fun initializeCameraId() {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
                if (facing == CameraCharacteristics.LENS_FACING_BACK && hasFlash == true) {
                    backCameraId = id
                    return
                }
            }
        } catch (e: CameraAccessException) {
            Log.e("MainActivity", "Failed to access camera", e)
        }
    }

    fun toggleFlashlight(forceOff: Boolean = false) {
        if (backCameraId == null) {
            Toast.makeText(this, "Flashlight not available", Toast.LENGTH_SHORT).show()
            return
        }
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val newState = if (forceOff) false else !isFlashlightOn
            cameraManager.setTorchMode(backCameraId!!, newState)
            isFlashlightOn = newState
        } catch (e: Exception) {
            isFlashlightOn = false
            Toast.makeText(this, "Flashlight unavailable while AR is active", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasCameraPermission(): Boolean = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun handleSessionCreationException(e: Exception) {
        val message = when (e) {
            is UnavailableApkTooOldException -> "Please update ARCore"
            is UnavailableSdkTooOldException -> "Please update this app"
            is UnavailableDeviceNotCompatibleException -> "This device does not support AR"
            is UnavailableUserDeclinedInstallationException -> "ARCore installation is required"
            is CameraNotAvailableException -> "Camera is not available. Please try again."
            else -> "Failed to create AR session: $e"
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }
}
