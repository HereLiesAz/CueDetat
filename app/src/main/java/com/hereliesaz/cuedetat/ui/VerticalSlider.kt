package com.hereliesaz.cuedetat.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * A vertical slider implementation that works with Material 3's SliderState.
 * It provides a draggable thumb and respects the provided SliderColors.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerticalSlider(
    state: SliderState,
    modifier: Modifier = Modifier,
    colors: SliderColors = SliderDefaults.colors()
) {
    BoxWithConstraints(
        modifier = modifier
            .width(48.dp) // A reasonable default width for touch targets
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val height = size.height.toFloat()
                    val range = state.valueRange.endInclusive - state.valueRange.start
                    if (range <= 0) return@detectDragGestures

                    // Convert y-position to a slider value.
                    // Y is 0 at the top, so we invert the calculation.
                    val rawValue =
                        state.valueRange.start + ((height - change.position.y) / height) * range
                    state.value = rawValue.coerceIn(state.valueRange)
                    change.consume()
                }
            }
    ) {
        val trackWidthPx = with(LocalDensity.current) { 6.dp.toPx() }
        val thumbRadiusPx = with(LocalDensity.current) { 12.dp.toPx() }
        val height = constraints.maxHeight.toFloat()

        // Calculate the thumb's Y position from the state's value
        val valueRange = state.valueRange.endInclusive - state.valueRange.start
        val normalizedValue = if (valueRange > 0) {
            (state.value - state.valueRange.start) / valueRange
        } else {
            0f
        }
        // Y is 0 at the top, so we invert the normalized value to find the thumb position
        val thumbY = height - (normalizedValue * height)

        Canvas(modifier = Modifier.matchParentSize()) {
            val centerX = size.width / 2f

            // Draw inactive track from top to bottom
            drawLine(
                color = colors.inactiveTrackColor,
                start = Offset(centerX, 0f),
                end = Offset(centerX, height),
                strokeWidth = trackWidthPx,
                cap = StrokeCap.Round
            )

            // Draw active track from the bottom up to the thumb's position
            drawLine(
                color = colors.activeTrackColor,
                start = Offset(centerX, height), // Start from bottom
                end = Offset(centerX, thumbY),   // End at thumb
                strokeWidth = trackWidthPx,
                cap = StrokeCap.Round
            )

            // Draw thumb
            drawCircle(
                color = colors.thumbColor,
                radius = thumbRadiusPx,
                center = Offset(centerX, thumbY)
            )
        }
    }
}