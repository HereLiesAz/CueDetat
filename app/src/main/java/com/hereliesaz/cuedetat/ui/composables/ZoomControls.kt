// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/ZoomControls.kt

package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp


/**
 * A dedicated vertical slider component for controlling zoom levels.
 *
 * Includes +/- buttons for fine adjustment and a vertical slider for coarse adjustment.
 * Note: Material3 Slider is horizontal by default. We rotate it 270 degrees to make it vertical,
 * and adjust layout measurement to compensate for the rotation.
 *
 * @param zoomSliderPosition Current zoom value (normalized -50 to 50).
 * @param onZoomChange Callback for value changes.
 * @param modifier Styling modifier.
 */
@Composable
fun ZoomControls(
    zoomSliderPosition: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Zoom In Button (+).
        IconButton(onClick = { onZoomChange(zoomSliderPosition + 1) }) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Zoom In",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Vertical Slider.
        Slider(
            value = zoomSliderPosition,
            onValueChange = { onZoomChange(it) },
            valueRange = -50f..50f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .semantics {
                    contentDescription = "Zoom Level"
                }
                .graphicsLayer {
                    // Rotate the horizontal slider to be vertical.
                    rotationZ = 270f
                    transformOrigin = TransformOrigin(0f, 0f)
                }
                .layout { measurable, constraints ->
                    // Swap width and height constraints to accommodate the rotation.
                    val placeable = measurable.measure(
                        Constraints(
                            minWidth = constraints.minHeight,
                            maxWidth = constraints.maxHeight,
                            minHeight = constraints.minWidth,
                            maxHeight = constraints.maxHeight,
                        )
                    )
                    // Layout the component with swapped dimensions and offset.
                    layout(placeable.height, placeable.width) {
                        placeable.place(-placeable.width, 0)
                    }
                }
                .fillMaxHeight(0.7f) // Occupy 70% of the available height.
                .height(32.dp) // Thickness of the slider track/thumb area.
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Zoom Out Button (-).
        IconButton(onClick = { onZoomChange(zoomSliderPosition - 1) }) {
            Icon(
                Icons.Default.Remove,
                contentDescription = "Zoom Out",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
