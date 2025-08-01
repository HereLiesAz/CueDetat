// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/dialogs/AdvancedOptionsDialog.kt

package com.hereliesaz.cuedetat.ui.composables.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent

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
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Auto-Snap Balls:", modifier = Modifier.weight(1f))
                        TextButton(onClick = { onEvent(MainScreenEvent.ToggleSnapping) }) {
                            Text(if (uiState.isSnappingEnabled) "On" else "Off")
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Show CV Mask:", modifier = Modifier.weight(1f))
                        TextButton(onClick = { onEvent(MainScreenEvent.ToggleCvMask) }) {
                            Text(if (uiState.showCvMask) "On" else "Off")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { onEvent(MainScreenEvent.EnterCvMaskTestMode) }) {
                            Text("Test Mask", color = MaterialTheme.colorScheme.secondary)
                        }
                    }


                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Active AI Model:", modifier = Modifier.weight(1f))
                        TextButton(onClick = { onEvent(MainScreenEvent.ToggleCvModel) }) {
                            Text(if (uiState.useCustomModel) "Custom" else "Generic")
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Refinement:", modifier = Modifier.weight(1f))
                        TextButton(onClick = { onEvent(MainScreenEvent.ToggleCvRefinementMethod) }) {
                            Text(uiState.cvRefinementMethod.name)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Hough P1 (Canny Edge): ${uiState.houghP1.toInt()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = uiState.houghP1,
                        onValueChange = { onEvent(MainScreenEvent.UpdateHoughP1(it)) },
                        valueRange = 50f..250f,
                        modifier = Modifier.height(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Hough P2 (Accumulator): ${uiState.houghP2.toInt()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = uiState.houghP2,
                        onValueChange = { onEvent(MainScreenEvent.UpdateHoughP2(it)) },
                        valueRange = 10f..100f,
                        modifier = Modifier.height(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Canny T1: ${uiState.cannyThreshold1.toInt()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = uiState.cannyThreshold1,
                        onValueChange = { onEvent(MainScreenEvent.UpdateCannyT1(it)) },
                        valueRange = 10f..200f,
                        modifier = Modifier.height(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Canny T2: ${uiState.cannyThreshold2.toInt()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = uiState.cannyThreshold2,
                        onValueChange = { onEvent(MainScreenEvent.UpdateCannyT2(it)) },
                        valueRange = 50f..300f,
                        modifier = Modifier.height(32.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
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
                    Divider()
                    TextButton(onClick = { onEvent(MainScreenEvent.EnterCalibrationMode) }) {
                        Text("Calibrate Felt Color", color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            },
            dismissButton = {
                // This is intentionally left blank to allow the confirm button to be the only one in the standard action row.
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Done", color = MaterialTheme.colorScheme.primary) } }
        )
    }
}