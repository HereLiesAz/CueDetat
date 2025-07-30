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

@Composable
fun ProtractorScreen(
    mainViewModel: MainViewModel,
    calibrationViewModel: CalibrationViewModel,
    quickAlignViewModel: QuickAlignViewModel,
    calibrationAnalyzer: CalibrationAnalyzer
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val systemIsDark = isSystemInDarkTheme()
    val quickAlignAnalyzer =
        remember(quickAlignViewModel) { QuickAlignAnalyzer(quickAlignViewModel) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isCameraVisible) {
            val activeAnalyzer = when {
                uiState.showCalibrationScreen -> calibrationAnalyzer
                uiState.showQuickAlignScreen -> quickAlignAnalyzer
                else -> mainViewModel.visionAnalyzer
            }
            CameraBackground(
                modifier = Modifier.fillMaxSize(),
                analyzer = activeAnalyzer
            )
        }

        when {
            uiState.showCalibrationScreen -> {
                CalibrationScreen(
                    uiState = uiState,
                    onEvent = mainViewModel::onEvent,
                    viewModel = calibrationViewModel,
                    analyzer = calibrationAnalyzer
                )
            }

            uiState.showQuickAlignScreen -> {
                QuickAlignScreen(
                    onEvent = mainViewModel::onEvent,
                    viewModel = quickAlignViewModel,
                    analyzer = quickAlignAnalyzer
                )
            }

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