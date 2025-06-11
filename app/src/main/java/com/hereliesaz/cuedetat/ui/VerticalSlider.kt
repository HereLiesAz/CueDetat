package com.hereliesaz.cuedetat.ui

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

/**
 * A vertical slider implementation that uses standard Compose state flow.
 */
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
            .width(48.dp) // A reasonable default width for touch targets
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val height = size.height.toFloat()
                    val range = valueRange.endInclusive - valueRange.start
                    if (range <= 0) return@detectDragGestures

                    val rawValue =
                        valueRange.start + ((height - change.position.y) / height) * range
                    onValueChange(rawValue.coerceIn(valueRange))
                    change.consume()
                }
            }
    ) {
        val trackWidthPx = with(LocalDensity.current) { 6.dp.toPx() }
        val thumbRadiusPx = with(LocalDensity.current) { 12.dp.toPx() }
        val height = constraints.maxHeight.toFloat()

        val valueRangeTotal = valueRange.endInclusive - valueRange.start
        val normalizedValue = if (valueRangeTotal > 0) {
            (value - valueRange.start) / valueRangeTotal
        } else {
            0f
        }
        val thumbY = height - (normalizedValue * height)

        Canvas(modifier = Modifier.matchParentSize()) {
            val centerX = size.width / 2f

            drawLine(
                color = colors.inactiveTrackColor,
                start = Offset(centerX, 0f),
                end = Offset(centerX, height),
                strokeWidth = trackWidthPx,
                cap = StrokeCap.Round
            )

            drawLine(
                color = colors.activeTrackColor,
                start = Offset(centerX, height),
                end = Offset(centerX, thumbY),
                strokeWidth = trackWidthPx,
                cap = StrokeCap.Round
            )

            drawCircle(
                color = colors.thumbColor,
                radius = thumbRadiusPx,
                center = Offset(centerX, thumbY)
            )
        }
    }
}
