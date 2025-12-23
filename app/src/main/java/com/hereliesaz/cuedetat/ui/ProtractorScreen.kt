// app/src/main/java/com/hereliesaz/cuedetat/ui/ProtractorScreen.kt
package com.hereliesaz.cuedetat.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hereliesaz.cuedetat.data.CalibrationAnalyzer
import com.hereliesaz.cuedetat.ui.composables.CameraBackground
import com.hereliesaz.cuedetat.ui.composables.calibration.CalibrationScreen
import com.hereliesaz.cuedetat.ui.composables.calibration.CalibrationViewModel
import com.hereliesaz.cuedetat.ui.composables.quickalign.QuickAlignAnalyzer
import com.hereliesaz.cuedetat.ui.composables.quickalign.QuickAlignScreen
import com.hereliesaz.cuedetat.ui.composables.quickalign.QuickAlignViewModel
import com.hereliesaz.cuedetat.view.ProtractorOverlay

/**
 * The main screen of the application for the "Expert" and "Beginner" experience modes.
 *
 * This composable serves as a container that orchestrates the display of various UI layers:
 * - The live camera feed as the background.
 * - The main protractor overlay with all the aiming guides.
 * - Conditional screens for camera calibration and quick alignment, which are displayed on top
 *   of the other layers when active.
 *
 * @param mainViewModel The primary [MainViewModel] that holds the main application state.
 * @param calibrationViewModel The [CalibrationViewModel] for managing the camera calibration process.
 * @param quickAlignViewModel The [QuickAlignViewModel] for managing the quick alignment process.
 * @param calibrationAnalyzer The [CalibrationAnalyzer] responsible for processing camera frames during calibration.
 */
@Composable
fun ProtractorScreen(
    mainViewModel: MainViewModel,
    calibrationViewModel: CalibrationViewModel,
    quickAlignViewModel: QuickAlignViewModel,
    calibrationAnalyzer: CalibrationAnalyzer
) {
    // Observe the main UI state from the MainViewModel.
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val systemIsDark = isSystemInDarkTheme()

    // Remember a QuickAlignAnalyzer instance, which is tied to the lifecycle of the QuickAlignViewModel.
    val quickAlignAnalyzer =
        remember(quickAlignViewModel) { QuickAlignAnalyzer(quickAlignViewModel) }

    // A Box layout allows layering composables on top of each other.
    Box(modifier = Modifier.fillMaxSize()) {

        // Display the camera feed as the background if it's set to be visible.
        if (uiState.isCameraVisible) {
            // Determine which analyzer should be active based on the current UI state.
            // This ensures that the correct image processing logic is applied to the camera feed.
            val activeAnalyzer = when {
                uiState.showCalibrationScreen -> calibrationAnalyzer
                uiState.showQuickAlignScreen -> quickAlignAnalyzer
                else -> mainViewModel.visionAnalyzer // Default analyzer for general computer vision tasks.
            }
            CameraBackground(
                modifier = Modifier.fillMaxSize(),
                analyzer = activeAnalyzer
            )
        }

        // Conditionally display the appropriate screen on top of the camera background.
        when {
            // If the calibration screen should be shown, display it.
            uiState.showCalibrationScreen -> {
                CalibrationScreen(
                    uiState = uiState,
                    onEvent = mainViewModel::onEvent,
                    viewModel = calibrationViewModel,
                    analyzer = calibrationAnalyzer
                )
            }

            // If the quick align screen should be shown, display it.
            uiState.showQuickAlignScreen -> {
                QuickAlignScreen(
                    onEvent = mainViewModel::onEvent,
                    viewModel = quickAlignViewModel,
                    analyzer = quickAlignAnalyzer
                )
            }

            // If the AR screen should be shown, display it.
            uiState.showArScreen -> {
                ArScreen(
                    uiState = uiState,
                    visionRepository = mainViewModel.visionAnalyzer.visionRepository, // Access the repo via analyzer
                    onEvent = mainViewModel::onEvent
                )
            }

            // Otherwise, display the main layout with the protractor overlay.
            else -> {
                MainLayout(uiState = uiState, onEvent = mainViewModel::onEvent) {
                    ProtractorOverlay(
                        uiState = uiState,
                        systemIsDark = systemIsDark,
                        isTestingCvMask = uiState.isTestingCvMask,
                        onEvent = mainViewModel::onEvent
                    )
                }
            }
        }
    }
}
