// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/calibration/CalibrationScreen.kt

package com.hereliesaz.cuedetat.ui.composables.calibration

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun CalibrationScreen(
    uiState: CueDetatState,
    onEvent: (MainScreenEvent) -> Unit,
    viewModel: CalibrationViewModel = hiltViewModel(),
    analyzer: CalibrationAnalyzer
) {
    val detectedPattern by viewModel.detectedPattern.collectAsState()
    val capturedImageCount by viewModel.capturedImageCount.collectAsState()
    val showSubmissionDialog by viewModel.showSubmissionDialog.collectAsState()

    if (showSubmissionDialog) {
        CalibrationSubmissionDialog(
            onDismiss = { viewModel.onDismissSubmission(); onEvent(MainScreenEvent.ToggleCalibrationScreen) },
            onSubmit = { viewModel.onSubmitData(); onEvent(MainScreenEvent.ToggleCalibrationScreen) }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CameraBackground(modifier = Modifier.fillMaxSize(), analyzer = analyzer)

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
                    // Draw lines connecting all points for a spider-web effect
                    drawPath(
                        path,
                        color = Color.Green,
                        style = Stroke(width = 2.dp.toPx()),
                        alpha = 0.7f
                    )
                    // Draw circles at each detected point
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

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Instructions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Camera Calibration",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Show the camera a 4x11 circle grid pattern from various angles. " +
                            "Capture at least 10-15 images for an accurate calibration. " +
                            "A green overlay will indicate a successful pattern detection.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Bottom Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Images: $capturedImageCount/15",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                CuedetatButton(
                    onClick = { viewModel.capturePattern() },
                    text = "Capture",
                    // Enable button only when a pattern is detected
                    color = if (detectedPattern != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.5f
                    )
                )

                TextButton(onClick = { viewModel.onCalibrationFinished() }) {
                    Text("Finish")
                }
            }
        }
    }
}