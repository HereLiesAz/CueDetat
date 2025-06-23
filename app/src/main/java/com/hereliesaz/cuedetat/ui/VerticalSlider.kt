// app/src/main/java/com/hereliesaz/cuedetat/ui/VerticalSlider.kt
package com.hereliesaz.cuedetat.ui

import android.util.Log // Added for debugging
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    colors: SliderColors = SliderDefaults.colors()
) {
    BoxWithConstraints(
        modifier = modifier
            .width(48.dp)
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> Log.d("VerticalSlider", "onDragStart at $offset. Current value: $value") },
                    onDragEnd = { Log.d("VerticalSlider", "onDragEnd. Final value: $value") },
                    onDragCancel = { Log.d("VerticalSlider", "onDragCancel") }
                ) { change, dragAmount ->
                    val height = size.height.toFloat()
                    val range = valueRange.endInclusive - valueRange.start

                    Log.d("VerticalSlider", "Drag: change.pos.y=${change.position.y}, height=$height, dragAmount.y=${dragAmount.y}")

                    if (range <= 0 || height <= 0) {
                        Log.d("VerticalSlider", "Invalid range or height. Range: $range, Height: $height")
                        return@detectDragGestures
                    }

                    // Calculate rawValue based on the y-position of the touch within the slider's bounds
                    // Top of slider (y=0) should correspond to valueRange.endInclusive
                    // Bottom of slider (y=height) should correspond to valueRange.start
                    val rawValue = valueRange.start + ((height - change.position.y) / height) * range
                    val coercedValue = rawValue.coerceIn(valueRange)

                    Log.d("VerticalSlider", "Calculated: rawValue=$rawValue, coercedValue=$coercedValue")

                    if (value != coercedValue) { // Only call if value changed to prevent excess recompositions
                        onValueChange(coercedValue)
                    }
                    change.consume()
                }
            }
    ) {
        val trackWidthPx = with(LocalDensity.current) { 6.dp.toPx() }
        val thumbRadiusPx = with(LocalDensity.current) { 12.dp.toPx() }
        val currentHeight = constraints.maxHeight.toFloat() // Use constraints.maxHeight for drawing consistency

        val valueRangeTotal = valueRange.endInclusive - valueRange.start
        val normalizedValue = if (valueRangeTotal > 0) {
            (value - valueRange.start) / valueRangeTotal
        } else {
            0f // Default to start if range is invalid
        }
        // Thumb Y: 0 is top, currentHeight is bottom.
        // If value is max (normalizedValue = 1), thumbY should be 0.
        // If value is min (normalizedValue = 0), thumbY should be currentHeight.
        val thumbY = currentHeight * (1 - normalizedValue) // Simplified: (currentHeight - (normalizedValue * currentHeight))


        // Log drawing parameters
        // Avoid excessive logging in draw phase, but useful for one-time check if needed
        // Log.d("VerticalSliderDraw", "value=$value, normalizedValue=$normalizedValue, thumbY=$thumbY, currentHeight=$currentHeight")


        Canvas(modifier = Modifier.matchParentSize()) {
            val centerX = size.width / 2f

            // Ensure drawing respects the actual height obtained by the canvas
            val canvasActualHeight = size.height

            drawLine(
                color = colors.inactiveTrackColor.copy(alpha = 0.5f), // Ensure inactive is distinguishable
                start = Offset(centerX, 0f),
                end = Offset(centerX, canvasActualHeight),
                strokeWidth = trackWidthPx,
                cap = StrokeCap.Round
            )

            // Active track from bottom up to thumbY
            // thumbY calculation: if value = max, norm = 1, thumbY = 0 (top)
            // if value = min, norm = 0, thumbY = height (bottom)
            // So active track is from canvasActualHeight (bottom) to thumbY
            drawLine(
                color = colors.activeTrackColor,
                start = Offset(centerX, canvasActualHeight), // Start from bottom
                end = Offset(centerX, thumbY.coerceIn(0f, canvasActualHeight)), // End at thumb, clamped
                strokeWidth = trackWidthPx,
                cap = StrokeCap.Round
            )

            drawCircle(
                color = colors.thumbColor,
                radius = thumbRadiusPx,
                center = Offset(centerX, thumbY.coerceIn(0f, canvasActualHeight)) // Clamp thumb position
            )
        }
    }
}