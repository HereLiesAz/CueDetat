// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/AdvancedOptionsDialog.kt
package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent

@Composable
fun AdvancedOptionsDialog(
    state: CueDetatState,
    onEvent: (MainScreenEvent) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("CV Refinement Parameters") },
        text = {
            Column {
                Text("Canny Threshold 1: ${state.cannyThreshold1.toInt()}")
                Slider(
                    value = state.cannyThreshold1,
                    onValueChange = { onEvent(MainScreenEvent.UpdateCannyT1(it)) },
                    valueRange = 0f..255f
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Canny Threshold 2: ${state.cannyThreshold2.toInt()}")
                Slider(
                    value = state.cannyThreshold2,
                    onValueChange = { onEvent(MainScreenEvent.UpdateCannyT2(it)) },
                    valueRange = 0f..255f
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}