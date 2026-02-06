// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/dialogs/AdvancedOptionsDialog.kt

package com.hereliesaz.cuedetat.ui.composables.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent

/**
 * A dialog containing advanced configuration options, primarily for debugging or fine-tuning Computer Vision.
 *
 * Exposes parameters for:
 * - Edge detection thresholds (Canny).
 * - Circle detection parameters (Hough Transform).
 * - Debug masks (CV binary view).
 * - Lens Calibration triggers.
 *
 * @param uiState Current application state.
 * @param onEvent Callback to update settings.
 * @param onDismiss Callback to close the dialog.
 */
@Composable
fun AdvancedOptionsDialog(
    uiState: CueDetatState,
    onEvent: (MainScreenEvent) -> Unit,
    onDismiss: () -> Unit
) {
    if (uiState.showAdvancedOptionsDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Too Advanced Options", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
            text = {
                Column {
                    // Toggle: Auto-snap virtual ball to detected ball.
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Auto-Snap Balls:", modifier = Modifier.weight(1f))
                        TextButton(onClick = { onEvent(MainScreenEvent.ToggleSnapping) }) {
                            Text(if (uiState.isSnappingEnabled) "On" else "Off")
                        }
                    }

                    // Toggle: Show the black-and-white CV mask overlay.
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Show CV Mask:", modifier = Modifier.weight(1f))
                        TextButton(onClick = { onEvent(MainScreenEvent.ToggleCvMask) }) {
                            Text(if (uiState.showCvMask) "On" else "Off")
                        }
                    }

                    // Action: Enter a dedicated mode for tuning the mask (live view).
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { onEvent(MainScreenEvent.EnterCvMaskTestMode) }) {
                            Text("Test Mask", color = MaterialTheme.colorScheme.secondary)
                        }
                    }

                    // Toggle: Select CV Model (Generic/Custom).
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Active AI Model:", modifier = Modifier.weight(1f))
                        TextButton(onClick = { onEvent(MainScreenEvent.ToggleCvModel) }) {
                            Text(if (uiState.useCustomModel) "Custom" else "Generic")
                        }
                    }

                    // Toggle: Cycle through CV refinement algorithms.
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Refinement:", modifier = Modifier.weight(1f))
                        TextButton(onClick = { onEvent(MainScreenEvent.ToggleCvRefinementMethod) }) {
                            Text(uiState.cvRefinementMethod.name)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- CV Parameters Sliders ---

                    // Hough P1: Canny edge threshold for the Hough Transform.
                    Text("Hough P1 (Canny Edge): ${uiState.houghP1.toInt()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = uiState.houghP1,
                        onValueChange = { onEvent(MainScreenEvent.UpdateHoughP1(it)) },
                        valueRange = 50f..250f,
                        modifier = Modifier
                            .semantics { contentDescription = "Hough P1 Canny Edge" }
                            .height(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Hough P2: Accumulator threshold for circle detection. Lower = more circles (and false positives).
                    Text("Hough P2 (Accumulator): ${uiState.houghP2.toInt()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = uiState.houghP2,
                        onValueChange = { onEvent(MainScreenEvent.UpdateHoughP2(it)) },
                        valueRange = 10f..100f,
                        modifier = Modifier
                            .semantics { contentDescription = "Hough P2 Accumulator" }
                            .height(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Canny T1: Lower threshold for hysteresis procedure.
                    Text("Canny T1: ${uiState.cannyThreshold1.toInt()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = uiState.cannyThreshold1,
                        onValueChange = { onEvent(MainScreenEvent.UpdateCannyT1(it)) },
                        valueRange = 10f..200f,
                        modifier = Modifier
                            .semantics { contentDescription = "Canny Threshold 1" }
                            .height(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Canny T2: Upper threshold for hysteresis procedure.
                    Text("Canny T2: ${uiState.cannyThreshold2.toInt()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = uiState.cannyThreshold2,
                        onValueChange = { onEvent(MainScreenEvent.UpdateCannyT2(it)) },
                        valueRange = 50f..300f,
                        modifier = Modifier
                            .semantics { contentDescription = "Canny Threshold 2" }
                            .height(32.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                    Spacer(modifier = Modifier.height(8.dp))

                    // --- Calibration Controls ---
                    Text("Fix Lens Warp", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(onClick = { onEvent(MainScreenEvent.ToggleQuickAlignScreen); onDismiss() }) {
                            Text("Table Alignment", color = MaterialTheme.colorScheme.tertiary)
                        }
                        TextButton(onClick = { onEvent(MainScreenEvent.ToggleCalibrationScreen); onDismiss() }) {
                            Text("Full Calibration", color = MaterialTheme.colorScheme.tertiary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                    // Action: Sample table color for masking.
                    TextButton(onClick = { onEvent(MainScreenEvent.EnterCalibrationMode) }) {
                        Text("Calibrate Felt Color", color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            },
            dismissButton = {
                // No secondary dismiss button needed.
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Done", color = MaterialTheme.colorScheme.primary) } }
        )
    }
}
