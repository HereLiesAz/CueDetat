package com.hereliesaz.cuedetat.ui.composables.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
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
    if (uiState.isCvParamMenuVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Advanced Options") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Auto-Snap Balls:", modifier = Modifier.weight(1f))
                        TextButton(onClick = { onEvent(MainScreenEvent.ToggleSnapping) }) {
                            Text(if (uiState.isSnappingEnabled) "On" else "Off")
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Active AI Model:", modifier = Modifier.weight(1f))
                        TextButton(onClick = { onEvent(MainScreenEvent.ToggleCvModel) }) {
                            Text(uiState.cvModel)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Refinement:", modifier = Modifier.weight(1f))
                        TextButton(onClick = { onEvent(MainScreenEvent.ToggleCvRefinementMethod) }) {
                            Text(uiState.cvRefinement)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Hough P1 (Canny Edge): ${uiState.houghP1.toInt()}")
                    Slider(value = uiState.houghP1, onValueChange = { onEvent(MainScreenEvent.UpdateHoughP1(it)) }, valueRange = 1f..300f)
                    Text("Hough P2 (Accumulator): ${uiState.houghP2.toInt()}")
                    Slider(value = uiState.houghP2, onValueChange = { onEvent(MainScreenEvent.UpdateHoughP2(it)) }, valueRange = 1f..100f)
                    Text("Canny T1: ${uiState.cannyThreshold1.toInt()}")
                    Slider(value = uiState.cannyThreshold1, onValueChange = { onEvent(MainScreenEvent.UpdateCannyT1(it)) }, valueRange = 1f..200f)
                    Text("Canny T2: ${uiState.cannyThreshold2.toInt()}")
                    Slider(value = uiState.cannyThreshold2, onValueChange = { onEvent(MainScreenEvent.UpdateCannyT2(it)) }, valueRange = 1f..400f)
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Done")
                }
            }
        )
    }
}