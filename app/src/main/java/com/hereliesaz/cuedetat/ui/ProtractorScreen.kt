package com.hereliesaz.cuedetat.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.hereliesaz.cuedetat.data.CalibrationAnalyzer
import com.hereliesaz.cuedetat.ui.composables.CameraBackground
import com.hereliesaz.cuedetat.ui.composables.calibration.CalibrationScreen
import com.hereliesaz.cuedetat.ui.composables.calibration.CalibrationViewModel
import com.hereliesaz.cuedetat.ui.composables.quickalign.QuickAlignScreen
import com.hereliesaz.cuedetat.ui.composables.quickalign.QuickAlignViewModel
import com.hereliesaz.cuedetat.view.ProtractorOverlay
import com.hereliesaz.cuedetat.view.gestures.detectManualGestures

@Composable
fun ProtractorScreen(
    mainViewModel: MainViewModel,
    calibrationViewModel: CalibrationViewModel,
    quickAlignViewModel: QuickAlignViewModel,
    calibrationAnalyzer: CalibrationAnalyzer
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val systemIsDark = isSystemInDarkTheme()

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isCameraVisible) {
            CameraBackground(
                modifier = Modifier.fillMaxSize(),
                analyzer = mainViewModel.visionAnalyzer
            )
        }

        ProtractorOverlay(
            uiState = uiState,
            systemIsDark = systemIsDark,
            isTestingCvMask = uiState.isTestingCvMask,
            onEvent = mainViewModel::onEvent,
            modifier = Modifier.detectManualGestures(uiState, mainViewModel::onEvent)
        )

        // Other UI components would go here, like buttons, sliders, etc.
        // This is a simplified version for the refactoring.
    }

    if (uiState.showCalibrationScreen) {
        CalibrationScreen(
            uiState = uiState,
            onEvent = mainViewModel::onEvent,
            viewModel = calibrationViewModel,
            analyzer = calibrationAnalyzer
        )
    }

    if (uiState.showQuickAlignScreen) {
        QuickAlignScreen(
            uiState = uiState,
            analyzer = mainViewModel.visionAnalyzer,
            onEvent = mainViewModel::onEvent,
            viewModel = quickAlignViewModel
        )
    }
}