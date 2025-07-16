package com.hereliesaz.cuedetat.ui.composables.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
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
    if (uiState.isAdvancedOptionsDialogVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Advanced Options") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Enable Snapping")
                        Spacer(Modifier.weight(1f))
                        Switch(checked = uiState.isSnappingEnabled, onCheckedChange = { onEvent(MainScreenEvent.ToggleSnapping) })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("CV Model: ${uiState.cvModel.name}")
                        Spacer(Modifier.weight(1f))
                        Button(onClick = { onEvent(MainScreenEvent.ToggleCvModel) }) { Text("Cycle") }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("CV Refinement: ${uiState.cvRefinement.name}")
                        Spacer(Modifier.weight(1f))
                        Button(onClick = { onEvent(MainScreenEvent.ToggleCvRefinementMethod) }) { Text("Cycle") }
                    }
                    Text("Hough P1: ${uiState.houghP1}")
                    Slider(value = uiState.houghP1, onValueChange = { onEvent(MainScreenEvent.UpdateHoughP1(it)) }, valueRange = 1f..200f)
                    Text("Hough P2: ${uiState.houghP2}")
                    Slider(value = uiState.houghP2, onValueChange = { onEvent(MainScreenEvent.UpdateHoughP2(it)) }, valueRange = 1f..200f)
                    Text("Canny Thresh 1: ${uiState.cannyThreshold1}")
                    Slider(value = uiState.cannyThreshold1, onValueChange = { onEvent(MainScreenEvent.UpdateCannyT1(it)) }, valueRange = 1f..200f)
                    Text("Canny Thresh 2: ${uiState.cannyThreshold2}")
                    Slider(value = uiState.cannyThreshold2, onValueChange = { onEvent(MainScreenEvent.UpdateCannyT2(it)) }, valueRange = 1f..200f)
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        )
    }
}