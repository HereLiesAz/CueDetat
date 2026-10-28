// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/ZoomControls.kt

package com.hereliesaz.cuedetat.ui.composables

import androidx.compose.animation.core.animate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.hereliesaz.cuedetat.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.ui.semantics.stateDescription
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
import kotlin.math.roundToInt


/**
 * The right-edge control column: a zoom slider (with +/- buttons) on top and a table-height
 * (Z) slider below it, splitting the available vertical space evenly.
 *
 * Material3 Slider is horizontal by default; [VerticalSlider] rotates it 270° and swaps its
 * layout constraints to render vertically.
 *
 * With ARCore world tracking the zoom slider is a self-centring jog ([isJog]): zoom there moves the
 * table along the camera line by whatever it is told, so an absolute position means nothing and
 * would only run into its end stops. Dragging nudges ([onNudge], in slider units) and the thumb
 * springs back to centre on release; the +/- buttons nudge one unit.
 *
 * @param zoomSliderPosition Current zoom value (-50..50). Unused while [isJog].
 * @param onZoomChange Callback for zoom changes. Unused while [isJog].
 * @param isJog True under ARCore world tracking: the zoom slider and buttons nudge instead.
 * @param onNudge Relative zoom change, in slider units (positive = closer), while [isJog].
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
    modifier: Modifier = Modifier,
    isJog: Boolean = false,
    onNudge: (Float) -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Zoom In Button (+).
        IconButton(onClick = { if (isJog) onNudge(1f) else onZoomChange(zoomSliderPosition + 1) }) {
            Icon(
                Icons.Default.Add,
                contentDescription = stringResource(R.string.zoom_in_a11y),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // Zoom slider — takes half of the remaining space.
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (isJog) {
                JogSlider(onNudge = onNudge, description = stringResource(R.string.zoom_level_a11y))
            } else {
                VerticalSlider(
                    value = zoomSliderPosition,
                    onValueChange = onZoomChange,
                    description = stringResource(R.string.zoom_level_a11y)
                )
            }
        }

        // Zoom Out Button (-).
        IconButton(onClick = { if (isJog) onNudge(-1f) else onZoomChange(zoomSliderPosition - 1) }) {
            Icon(
                Icons.Default.Remove,
                contentDescription = stringResource(R.string.zoom_out_a11y),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Table-height (Z) slider — takes the other half of the remaining space.
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            VerticalSlider(
                value = tableHeight,
                onValueChange = onTableHeightChange,
                description = stringResource(R.string.table_height_a11y)
            )
        }
    }
}

/**
 * A [VerticalSlider] that rests at centre. Each drag step reports its change in slider units
 * through [onNudge]; on release the thumb springs back to centre without reporting, so what the
 * thumb shows is only the drag in progress and it can always be dragged further.
 */
@Composable
private fun JogSlider(onNudge: (Float) -> Unit, description: String) {
    var jog by remember { mutableFloatStateOf(0f) }
    var returning by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    VerticalSlider(
        value = jog,
        onValueChange = { v ->
            returning?.cancel()
            val delta = v - jog
            jog = v
            onNudge(delta)
        },
        description = description,
        onValueChangeFinished = {
            returning = scope.launch { animate(jog, 0f) { v, _ -> jog = v } }
        },
    )
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
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val valueText = value.roundToInt().toString()
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = -50f..50f,
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .semantics {
                contentDescription = description
                stateDescription = valueText
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
                layout(placeable.height, placeable.width) {
                    placeable.place(-placeable.width, 0)
                }
            }
            .fillMaxHeight()
            .height(48.dp) // Thickness of the slider track/thumb area; 48 dp minimum touch target.
    )
}
