package com.hereliesaz.cuedetat.ui.composables

import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun SpinControl(
    modifier: Modifier = Modifier,
    centerPosition: PointF,
    selectedSpinOffset: Offset?,
    lingeringSpinOffset: Offset?,
    spinPathAlpha: Float,
    onEvent: (MainScreenEvent) -> Unit
) {
    Canvas(
        modifier = modifier
            .size(120.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { onEvent(MainScreenEvent.SpinDragEnd) }
                ) { change, dragAmount ->
                    change.consume()
                    val currentPosition = change.position
                    val relativePosition = currentPosition - centerPosition
                    onEvent(MainScreenEvent.SpinDrag(relativePosition))
                }
            }
    ) {
        val radius = min(size.width, size.height) / 2f
        drawCircle(
            color = Color.White.copy(alpha = 0.2f),
            radius = radius,
            center = centerPosition,
            style = Stroke(width = 2.dp.toPx())
        )

        (selectedSpinOffset ?: lingeringSpinOffset)?.let { offset ->
            val angle = atan2(offset.y, offset.x)
            val distance = sqrt(offset.x * offset.x + offset.y * offset.y)
            val clampedDistance = min(distance, radius)

            val endPoint = Offset(
                centerPosition.x + clampedDistance * cos(angle),
                centerPosition.y + clampedDistance * sin(angle)
            )

            drawCircle(
                color = Color.Red.copy(alpha = spinPathAlpha),
                radius = 8.dp.toPx(),
                center = endPoint
            )
        }
    }
}