// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/ZoomControls.kt

package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
 * The right-edge control column: a zoom slider (with +/- buttons) on top and a table-height
 * (Z) slider below it, splitting the available vertical space evenly.
 *
 * Material3 Slider is horizontal by default; [VerticalSlider] rotates it 270° and swaps its
 * layout constraints to render vertically.
 *
 * @param zoomSliderPosition Current zoom value (-50..50).
 * @param onZoomChange Callback for zoom changes.
 * @param tableHeight Current table Z offset (-50..50).
 * @param onTableHeightChange Callback for table-height changes (absolute value).
 * @param modifier Styling modifier.
 */
@Composable
fun ZoomControls(
    zoomSliderPosition: Float,
    onZoomChange: (Float) -> Unit,
    tableHeight: Float,
    onTableHeightChange: (Float) -> Unit,
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

        // Zoom slider — takes half of the remaining space.
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            VerticalSlider(
                value = zoomSliderPosition,
                onValueChange = onZoomChange,
                description = "Zoom Level"
            )
        }

        // Zoom Out Button (-).
        IconButton(onClick = { onZoomChange(zoomSliderPosition - 1) }) {
            Icon(
                Icons.Default.Remove,
                contentDescription = "Zoom Out",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Table-height (Z) slider — takes the other half of the remaining space.
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            VerticalSlider(
                value = tableHeight,
                onValueChange = onTableHeightChange,
                description = "Table Height"
            )
        }
    }
}

/**
 * A vertical Material3 [Slider] (rotated 270°), filling the height of its parent box.
 * Value range is fixed to -50..50 to match the zoom and table-Z clamps.
 */
@Composable
private fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    description: String,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = -50f..50f,
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .semantics { contentDescription = description }
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
                layout(placeable.height, placeable.width) {
                    placeable.place(-placeable.width, 0)
                }
            }
            .fillMaxHeight()
            .height(32.dp) // Thickness of the slider track/thumb area.
    )
}
