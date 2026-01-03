// app/src/main/java/com/hereliesaz/cuedetat/MainActivity.kt
package com.hereliesaz.cuedetat

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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

/**
 * The main and only activity of the application.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

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
     */
    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // If permission is granted, recreate the activity to initialize the camera and UI.
                recreate()
            } else {
                Toast.makeText(
                    this,
                    "Camera permission is required. The app will now close.",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Allow the app to draw behind the system bars for an immersive, full-screen experience.
        WindowCompat.setDecorFitsSystemWindows(window, false)

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
     */
    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
}
