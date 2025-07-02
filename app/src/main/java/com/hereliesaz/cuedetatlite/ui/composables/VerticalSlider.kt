package com.hereliesaz.cuedetatlite.ui.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

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
            .width(56.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val height = size.height.toFloat()
                    val valueRangeTotal = valueRange.endInclusive - valueRange.start
                    if (valueRangeTotal <= 0) return@detectDragGestures

                    val rawValue =
                        valueRange.start + ((height - change.position.y) / height) * valueRangeTotal
                    onValueChange(rawValue.coerceIn(valueRange))
                    change.consume()
                }
            }
    ) {
        val valueRangeTotal = valueRange.endInclusive - valueRange.start
        val normalizedValue = if (valueRangeTotal > 0f) {
            ((value - valueRange.start) / valueRangeTotal).coerceIn(0f, 1f)
        } else {
            0f
        }

        val height = constraints.maxHeight.toFloat()
        val thumbY = height * (1 - normalizedValue)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val trackWidthPx = 6.dp.toPx()
            val thumbRadiusPx = 12.dp.toPx()
            val centerX = size.width / 2f

            drawLine(
                color = colors.inactiveTrackColor.copy(alpha = 0.5f),
                start = Offset(centerX, 0f),
                end = Offset(centerX, size.height),
                strokeWidth = trackWidthPx,
                cap = StrokeCap.Round
            )
            drawLine(
                color = colors.activeTrackColor,
                start = Offset(centerX, size.height),
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
        var surfaceSize by remember { mutableStateOf(IntSize.Zero) }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = colors.thumbColor,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .onGloballyPositioned { coordinates ->
                    surfaceSize = coordinates.size
                }
                .absoluteOffset {
                    if (surfaceSize.height > 0) { // Ensure surfaceSize is initialized
                        IntOffset(
                            0,
                            (thumbY - surfaceSize.height / 2)
                                .roundToInt()
                                .coerceIn(0, constraints.maxHeight - surfaceSize.height)
                        )
                    } else {
                        IntOffset.Zero
                    }
                }
        ) {
            Text(
                text = value.roundToInt().toString(),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}