// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/sliders/TableRotationSlider.kt

package com.hereliesaz.cuedetat.ui.composables.sliders

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * A horizontal slider for manually rotating the virtual table relative to the camera view.
 * Useful when the device orientation sensors are drifting or inaccurate.
 *
 * @param isVisible Visibility flag.
 * @param worldRotationDegrees Current rotation in degrees.
 * @param onRotationChange Callback for value changes.
 * @param modifier Styling modifier.
 */
@Composable
fun TableRotationSlider(
    isVisible: Boolean,
    worldRotationDegrees: Float,
    onRotationChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isVisible) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Table Rotation: ${-worldRotationDegrees.toInt()}°",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Slider(
                value = -worldRotationDegrees,
                onValueChange = { onRotationChange(-it) },
                // Range covers full 360 degrees.
                valueRange = -179f..180f,
                // Granular control.
                steps = 358,
                modifier = Modifier
                    .semantics { contentDescription = "Table Rotation" }
                    .fillMaxWidth()
                    .height(32.dp),
                colors = SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    thumbColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
