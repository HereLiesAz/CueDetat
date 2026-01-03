// app/src/main/java/com/hereliesaz/cuedetat/MainActivity.kt
package com.hereliesaz.cuedetat

import android.Manifest
<<<<<<< HEAD
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
=======
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.MotionEvent
>>>>>>> origin/CueDetatAR
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
<<<<<<< HEAD
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.hereliesaz.cuedetat.data.CalibrationAnalyzer
import com.hereliesaz.cuedetat.data.CalibrationRepository
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.ui.MainViewModel
import com.hereliesaz.cuedetat.ui.ProtractorScreen
import com.hereliesaz.cuedetat.ui.composables.SplashScreen
import com.hereliesaz.cuedetat.ui.composables.calibration.CalibrationViewModel
import com.hereliesaz.cuedetat.ui.composables.quickalign.QuickAlignViewModel
import com.hereliesaz.cuedetat.ui.hatemode.HaterEvent
import com.hereliesaz.cuedetat.ui.hatemode.HaterScreen
import com.hereliesaz.cuedetat.ui.hatemode.HaterViewModel
import com.hereliesaz.cuedetat.ui.theme.CueDetatTheme
import com.hereliesaz.cuedetat.utils.SecurityUtils
import com.hereliesaz.cuedetat.view.state.SingleEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
=======
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.*
import com.hereliesaz.cuedetat.ar.jetpack.ArRenderer
import com.hereliesaz.cuedetat.ar.jetpack.helpers.DisplayRotationHelper
import com.hereliesaz.cuedetat.ui.MainScreen
import com.hereliesaz.cuedetat.ui.MainViewModel
import com.hereliesaz.cuedetat.ui.state.UiEvent
import com.hereliesaz.cuedetat.ui.theme.CueDetatTheme
import dagger.hilt.android.AndroidEntryPoint
>>>>>>> origin/CueDetatAR

/**
 * The main and only activity of the application.
 *
 * This activity is the entry point for the user interface. It is responsible for:
 * - Handling camera permission requests.
 * - Setting up the core Jetpack Compose content.
 * - Observing and handling one-time events from the [MainViewModel].
 * - Managing the overall UI flow, including the splash screen and the main protractor/hater screens.
 * - Controlling screen orientation based on the current application state.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

<<<<<<< HEAD
    // ViewModels for different parts of the UI, injected by Hilt.
    private val viewModel: MainViewModel by viewModels()
    private val calibrationViewModel: CalibrationViewModel by viewModels()
    private val quickAlignViewModel: QuickAlignViewModel by viewModels()
    private val haterViewModel: HaterViewModel by viewModels()

    @Inject
    lateinit var calibrationRepository: CalibrationRepository

    private lateinit var calibrationAnalyzer: CalibrationAnalyzer

    /**
     * An ActivityResultLauncher that requests the CAMERA permission.
     * The result of this request determines the application's flow.
     */
    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // If permission is granted, recreate the activity to initialize the camera and UI.
                recreate()
            } else {
                // If permission is denied, the app is unusable. Inform the user and close the app.
                // This is a deliberate design choice: the app's core functionality depends on the camera.
                Toast.makeText(
                    this,
                    "Camera permission is required. The app will now close.",
                    Toast.LENGTH_LONG
                ).show()
=======
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
>>>>>>> origin/CueDetatAR
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Allow the app to draw behind the system bars for an immersive, full-screen experience.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        initializeCameraId()

<<<<<<< HEAD
        calibrationAnalyzer = CalibrationAnalyzer(calibrationViewModel)

        // Check for camera permission and either launch the permission request or set the content.
        when {
            hasCameraPermission() -> {
                setContent {
                    AppContent()
                }
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
        observeSingleEvents()
    }

    /**
     * Observes a [SharedFlow] in the [MainViewModel] for one-time events.
     * These are events that should only be handled once, such as navigation or showing a Toast.
     */
    private fun observeSingleEvents() {
        viewModel.singleEvent.onEach { event ->
            when (event) {
                is SingleEvent.OpenUrl -> {
                    // Before opening any URL, validate it to prevent security vulnerabilities.
                    if (SecurityUtils.isSafeUrl(event.url)) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(event.url))
                        startActivity(intent)
                    } else {
                        // Log or handle the attempt to open an unsafe URL.
                    }
                    // Consume the event to prevent it from being triggered again on configuration change.
                    viewModel.onEvent(MainScreenEvent.SingleEventConsumed)
                }
                is SingleEvent.SendFeedbackEmail -> {
                    // Create an email intent to allow the user to send feedback.
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(event.email))
                        putExtra(Intent.EXTRA_SUBJECT, event.subject)
                    }
                    // Ensure there is an app that can handle the intent before starting the activity.
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                    }
                    viewModel.onEvent(MainScreenEvent.SingleEventConsumed)
                }
                is SingleEvent.InitiateHaterMode -> {
                    // Trigger the hater mode from the main ViewModel.
                    haterViewModel.onEvent(HaterEvent.EnterHaterMode)
                    viewModel.onEvent(MainScreenEvent.SingleEventConsumed)
                }
                null -> { /* This state is expected when the flow is first collected. */ }
            }
        }.launchIn(lifecycleScope)
    }

    /**
     * The main composable function that defines the application's UI.
     * It observes the UI state and displays the appropriate screen.
     */
    @Composable
    private fun AppContent() {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val showSplashScreen = uiState.experienceMode == null
        // Remember the initial orientation when entering Hater Mode to lock it.
        var haterModeLockedOrientation by rememberSaveable { mutableStateOf(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) }

        // A side-effect that triggers when the experience mode or orientation lock setting changes.
        LaunchedEffect(uiState.experienceMode, uiState.orientationLock) {
            if (uiState.experienceMode == ExperienceMode.HATER) {
                // Lock the screen orientation to the current orientation when entering Hater Mode.
                if (haterModeLockedOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                    haterModeLockedOrientation = when (resources.configuration.orientation) {
                        Configuration.ORIENTATION_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }
                }
                requestedOrientation = haterModeLockedOrientation
            } else {
                // When not in Hater Mode, respect the user's selected orientation lock setting.
                haterModeLockedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                requestedOrientation = when (uiState.orientationLock) {
                    CueDetatState.OrientationLock.AUTOMATIC -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    CueDetatState.OrientationLock.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    CueDetatState.OrientationLock.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
            }
        }

        // The main theme of the application.
        CueDetatTheme(luminanceAdjustment = uiState.luminanceAdjustment) {
            if (showSplashScreen) {
                // Show the splash screen on first launch to let the user select an experience mode.
                SplashScreen(onRoleSelected = { selectedMode ->
                    viewModel.onEvent(MainScreenEvent.SetExperienceMode(selectedMode))
                })
            } else {
                // A side-effect to notify the ViewModel when the color scheme changes (e.g., dark/light mode).
                val currentAppControlColorScheme = MaterialTheme.colorScheme
                LaunchedEffect(currentAppControlColorScheme) {
                    viewModel.onEvent(MainScreenEvent.ThemeChanged(currentAppControlColorScheme))
                }

                // Display the appropriate screen based on the selected experience mode.
                when (uiState.experienceMode) {
                    ExperienceMode.HATER -> HaterScreen(
                        haterViewModel = haterViewModel,
                        uiState = uiState,
                        onEvent = viewModel::onEvent
                    )
                    else -> { // EXPERT and BEGINNER modes share the same main screen.
                        ProtractorScreen(
                            mainViewModel = viewModel,
                            calibrationViewModel = calibrationViewModel,
                            quickAlignViewModel = quickAlignViewModel,
                            calibrationAnalyzer = calibrationAnalyzer
                        )
                    }
                }
            }
        }
    }

    /**
     * A helper function to check if the CAMERA permission has been granted.
     * @return `true` if the permission is granted, `false` otherwise.
     */
    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
}
=======
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
                    onTrackingStateChanged = { trackingState, failureReason ->
                        viewModel.onEvent(UiEvent.OnTrackingStateUpdate(trackingState, failureReason))
                    }
                ).also { setRenderer(it) }

                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            }
            glSurfaceView.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    viewModel.onEvent(UiEvent.OnScreenTap(Offset(event.x, event.y)))
                }
                true
            }

        } catch (e: Exception) {
            handleSessionCreationException(e)
            return
        }

        setContent {
            val uiState by viewModel.uiState.collectAsState()

            // The renderer is only updated with state. It no longer needs a callback here.
            renderer?.updateState(uiState)

            CueDetatTheme(darkTheme = uiState.isDarkMode) {
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
            e.printStackTrace()
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
>>>>>>> origin/CueDetatAR
