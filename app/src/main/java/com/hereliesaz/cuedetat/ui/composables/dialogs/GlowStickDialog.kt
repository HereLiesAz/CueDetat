package com.hereliesaz.cuedetat.ui.composables.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.OverlayState

@Composable
fun GlowStickDialog(
    uiState: OverlayState,
    onEvent: (MainScreenEvent) -> Unit,
    onDismiss: () -> Unit
) {
    if (uiState.glowStickValue != 0f) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Glow Stick") },
            text = {
                Column {
                    Slider(
                        value = uiState.glowStickValue,
                        onValueChange = { onEvent(MainScreenEvent.AdjustGlow(it)) },
                        valueRange = -1f..1f
                    )
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