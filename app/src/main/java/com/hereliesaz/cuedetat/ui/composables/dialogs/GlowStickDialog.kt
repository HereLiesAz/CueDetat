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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent

/**
 * A dialog allowing the user to control the "Glow Stick" effect on virtual lines.
 *
 * When non-zero, the drawn lines switch from their standard color to a white/black
 * glowing neon style — useful in dark pool halls or for visual preference.
 *
 * @param uiState Current application state.
 * @param onEvent Callback to update glow value.
 * @param onDismiss Callback to close dialog.
 */
@Composable
fun GlowStickDialog(
    uiState: CueDetatState,
    onEvent: (MainScreenEvent) -> Unit,
    onDismiss: () -> Unit
) {
    if (uiState.showGlowStickDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Glow Stick", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.66f),
            text = {
                Column {
                    Text(
                        "Current: ${"%.2f".format(uiState.glowStickValue)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = uiState.glowStickValue,
                        onValueChange = { onEvent(MainScreenEvent.AdjustGlow(it)) },
                        valueRange = -1f..1f,
                        steps = 199,
                        modifier = Modifier
                            .semantics { contentDescription = "Glow Stick Intensity" }
                            .height(32.dp),
                        colors = SliderDefaults.colors(
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            thumbColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Done", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}
