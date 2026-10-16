package com.hereliesaz.cuedetat.ui.composables.dialogs

import androidx.compose.ui.res.stringResource
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
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent

/**
 * A dialog allowing the user to adjust the global luminance (brightness) of the virtual overlay.
 *
 * This helps integrate the AR elements with the real-world lighting conditions (e.g. dimming them in a dark pool hall).
 *
 * @param uiState Current application state.
 * @param onEvent Callback to update luminance.
 * @param onDismiss Callback to close dialog.
 */
@Composable
fun LuminanceAdjustmentDialog(
    uiState: CueDetatState,
    onEvent: (MainScreenEvent) -> Unit,
    onDismiss: () -> Unit
) {
    val sliderA11y = stringResource(R.string.luminance_slider_a11y)
    if (uiState.showLuminanceDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.luminance_dialog_title), color = MaterialTheme.colorScheme.onSurfaceVariant) },
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.66f),
            text = {
                Column {
                    Text(stringResource(R.string.dialog_current_value, "%.2f".format(uiState.luminanceAdjustment)), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = uiState.luminanceAdjustment,
                        onValueChange = { onEvent(MainScreenEvent.AdjustLuminance(it)) },
                        // Range from -40% to +40% brightness.
                        valueRange = -0.4f..0.4f,
                        // Granular steps.
                        steps = 79,
                        modifier = Modifier
                            .semantics { contentDescription = sliderA11y }
                            .height(48.dp), // 48 dp minimum touch target
                        colors = SliderDefaults.colors(
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            thumbColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done), color = MaterialTheme.colorScheme.primary) } }
        )
    }
}
