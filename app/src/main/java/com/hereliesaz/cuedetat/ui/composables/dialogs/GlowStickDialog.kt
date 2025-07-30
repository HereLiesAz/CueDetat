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
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.OverlayState

@Composable
fun GlowStickDialog(
    uiState: OverlayState,
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
                    val glowLabel = when {
                        uiState.glowStickValue > 0.05f -> "White Glow: ${"%.0f".format(uiState.glowStickValue * 100)}%"
                        uiState.glowStickValue < -0.05f -> "Black Glow: ${"%.0f".format(kotlin.math.abs(uiState.glowStickValue) * 100)}%"
                        else -> "No Glow"
                    }
                    Text(glowLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = uiState.glowStickValue,
                        onValueChange = { onEvent(MainScreenEvent.AdjustGlow(it)) },
                        valueRange = -1f..1f,
                        steps = 199,
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