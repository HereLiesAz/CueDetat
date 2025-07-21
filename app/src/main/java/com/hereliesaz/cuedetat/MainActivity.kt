// FILE: app/src/main/java/com/hereliesaz/cuedetat/MainActivity.kt

package com.hereliesaz.cuedetat

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
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
import com.hereliesaz.cuedetat.ui.MainScreen
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.ui.MainViewModel
import com.hereliesaz.cuedetat.ui.composables.SplashScreen
import com.hereliesaz.cuedetat.ui.composables.calibration.CalibrationViewModel
import com.hereliesaz.cuedetat.ui.composables.quickalign.QuickAlignViewModel
import com.hereliesaz.cuedetat.ui.theme.CueDetatTheme
import com.hereliesaz.cuedetat.view.state.OverlayState
import com.hereliesaz.cuedetat.view.state.SingleEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val calibrationViewModel: CalibrationViewModel by viewModels()
    private val quickAlignViewModel: QuickAlignViewModel by viewModels()

    @Inject
    lateinit var calibrationRepository: CalibrationRepository

    private lateinit var calibrationAnalyzer: CalibrationAnalyzer

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                setContent {
                    AppContent(
                        mainViewModel,
                        calibrationViewModel,
                        quickAlignViewModel,
                        calibrationAnalyzer
                    )
                }
            } else {
                // Heresy is not tolerated. The user will comply or they will not use the app.
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        calibrationAnalyzer = CalibrationAnalyzer(calibrationViewModel)

        when {
            hasCameraPermission() -> {
                setContent {
                    AppContent(
                        mainViewModel,
                        calibrationViewModel,
                        quickAlignViewModel,
                        calibrationAnalyzer
                    )
                }
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
        observeSingleEvents()
    }

    private fun observeSingleEvents() {
        mainViewModel.singleEvent.onEach { event ->
            when (event) {
                is SingleEvent.OpenUrl -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(event.url))
                    startActivity(intent)
                    mainViewModel.onEvent(MainScreenEvent.SingleEventConsumed)
                }
                is SingleEvent.SendFeedbackEmail -> {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:") // Only email apps should handle this
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(event.email))
                        putExtra(Intent.EXTRA_SUBJECT, event.subject)
                    }
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                    }
                    mainViewModel.onEvent(MainScreenEvent.SingleEventConsumed)
                }
                null -> { /* Do nothing */ }
            }
        }.launchIn(lifecycleScope)
    }

    @Composable
    private fun AppContent(
        mainViewModel: MainViewModel,
        calibrationViewModel: CalibrationViewModel,
        quickAlignViewModel: QuickAlignViewModel,
        calibrationAnalyzer: CalibrationAnalyzer
    ) {
        var showSplashScreen by rememberSaveable { mutableStateOf(true) }
        val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(uiState.orientationLock) {
            requestedOrientation = when (uiState.orientationLock) {
                OverlayState.OrientationLock.AUTOMATIC -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                OverlayState.OrientationLock.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                OverlayState.OrientationLock.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        }

        CueDetatTheme {
            if (showSplashScreen) {
                SplashScreen(onTimeout = { showSplashScreen = false })
            } else {
                val currentAppControlColorScheme = MaterialTheme.colorScheme
                LaunchedEffect(currentAppControlColorScheme) {
                    mainViewModel.onEvent(MainScreenEvent.ThemeChanged(currentAppControlColorScheme))
                }
                MainScreen(
                    mainViewModel = mainViewModel,
                    calibrationViewModel = calibrationViewModel,
                    quickAlignViewModel = quickAlignViewModel,
                    calibrationAnalyzer = calibrationAnalyzer
                )
            }
        }
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
}