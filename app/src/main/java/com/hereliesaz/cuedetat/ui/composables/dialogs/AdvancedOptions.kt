// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/dialogs/AdvancedOptionsDialog.kt

package com.hereliesaz.cuedetat.ui.composables.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.OverlayState

@Composable
fun AdvancedOptionsDialog(
    uiState: OverlayState,
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
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Show CV Mask:", modifier = Modifier.weight(1f))
                        TextButton(onClick = { onEvent(MainScreenEvent.ToggleCvMask) }) {
                            Text(if (uiState.showCvMask) "On" else "Off")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Active AI Model:", modifier = Modifier.weight(1f))
                        TextButton(onClick = { onEvent(MainScreenEvent.ToggleCvModel) }) {
                            Text(if (uiState.useCustomModel) "Custom" else "Generic")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

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
                        valueRange = 50f..250f
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Hough P2 (Accumulator): ${uiState.houghP2.toInt()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = uiState.houghP2,
                        onValueChange = { onEvent(MainScreenEvent.UpdateHoughP2(it)) },
                        valueRange = 10f..100f
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Canny T1: ${uiState.cannyThreshold1.toInt()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = uiState.cannyThreshold1,
                        onValueChange = { onEvent(MainScreenEvent.UpdateCannyT1(it)) },
                        valueRange = 10f..200f
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Canny T2: ${uiState.cannyThreshold2.toInt()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = uiState.cannyThreshold2,
                        onValueChange = { onEvent(MainScreenEvent.UpdateCannyT2(it)) },
                        valueRange = 50f..300f
                    )
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { onEvent(MainScreenEvent.EnterCalibrationMode) }) {
                        Text("Calibrate Felt", color = MaterialTheme.colorScheme.tertiary)
                    }
                    TextButton(onClick = { onEvent(MainScreenEvent.EnterCvMaskTestMode) }) {
                        Text("Test Mask", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Done", color = MaterialTheme.colorScheme.primary) } }
        )
    }
}