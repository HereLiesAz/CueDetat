package com.hereliesaz.cuedetat.ui.composables.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent

@Composable
fun LuminanceAdjustmentDialog(
    uiState: CueDetatState,
    onEvent: (MainScreenEvent) -> Unit,
    onDismiss: () -> Unit
) {
    if (uiState.showLuminanceDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Adjust Drawn Elements Luminance", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.66f),
            text = {
                Column {
                    Text("Current: ${"%.2f".format(uiState.luminanceAdjustment)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = uiState.luminanceAdjustment,
                        onValueChange = { onEvent(MainScreenEvent.AdjustLuminance(it)) },
                        valueRange = -0.4f..0.4f,
                        steps = 79,
                        modifier = Modifier.height(32.dp),
                        colors = SliderDefaults.colors(
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            thumbColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Done", color = MaterialTheme.colorScheme.primary) } }
        )
    }
}