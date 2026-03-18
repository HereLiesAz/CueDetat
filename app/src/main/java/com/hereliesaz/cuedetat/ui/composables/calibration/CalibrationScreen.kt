// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/calibration/CalibrationScreen.kt

package com.hereliesaz.cuedetat.ui.composables.calibration

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hereliesaz.cuedetat.data.CalibrationAnalyzer
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.ui.composables.CameraBackground
import com.hereliesaz.cuedetat.ui.composables.CuedetatButton

/**
 * A dedicated screen for performing Camera Calibration.
 *
 * It displays the live camera feed with an overlay visualizing detected calibration patterns.
 * Users capture multiple frames of a calibration board to compute lens distortion parameters.
 *
 * @param uiState Current app state.
 * @param onEvent Event dispatcher.
 * @param viewModel Hilt-injected ViewModel for calibration logic.
 * @param analyzer The ImageAnalysis.Analyzer used to detect patterns.
 */
@Composable
fun CalibrationScreen(
    uiState: CueDetatState,
    onEvent: (MainScreenEvent) -> Unit,
    viewModel: CalibrationViewModel = hiltViewModel(),
    analyzer: CalibrationAnalyzer
) {
    // Observe ViewModel state.
    val detectedPattern by viewModel.detectedPattern.collectAsState()
    val showSubmissionDialog by viewModel.showSubmissionDialog.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val context = LocalContext.current

    // Show Android Toasts when the ViewModel requests them.
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.onToastShown()
        }
    }

    // Show the optional submission dialog when calibration completes.
    if (showSubmissionDialog) {
        CalibrationSubmissionDialog(
            onDismiss = { viewModel.onDismissSubmission(); onEvent(MainScreenEvent.ToggleCalibrationScreen) },
            onSubmit = { viewModel.onSubmitData(); onEvent(MainScreenEvent.ToggleCalibrationScreen) }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Overlay canvas to draw the detected pattern grid.
        Canvas(modifier = Modifier.fillMaxSize()) {
            detectedPattern?.let { points ->
                if (points.isNotEmpty()) {
                    val path = Path()
                    points.forEachIndexed { index, point ->
                        val offset = Offset(point.x.toFloat(), point.y.toFloat())
                        if (index == 0) {
                            path.moveTo(offset.x, offset.y)
                        } else {
                            path.lineTo(offset.x, offset.y)
                        }
                    }
                    // Draw lines connecting detected points (visual verification).
                    drawPath(
                        path,
                        color = Color.Green,
                        style = Stroke(width = 2.dp.toPx()),
                        alpha = 0.7f
                    )
                    // Draw dots at each detected corner.
                    points.forEach { point ->
                        drawCircle(
                            Color.Green,
                            radius = 5.dp.toPx(),
                            center = Offset(point.x.toFloat(), point.y.toFloat()),
                            alpha = 0.8f
                        )
                    }
                }
            }
        }
    }
}
