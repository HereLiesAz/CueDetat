// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/SpinControl.kt

package com.hereliesaz.cuedetat.ui.composables

import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.ui.theme.RebelYellow
import com.hereliesaz.cuedetat.ui.theme.WarningRed
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun SpinControl(
    modifier: Modifier = Modifier,
    centerPosition: PointF,
    selectedSpinOffset: PointF?,
    lingeringSpinOffset: PointF?,
    spinPathAlpha: Float,
    onEvent: (MainScreenEvent) -> Unit
) {
    Box(
        modifier = modifier
            .offset { IntOffset(centerPosition.x.roundToInt(), centerPosition.y.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onEvent(MainScreenEvent.DragSpinControl(PointF(dragAmount.x, dragAmount.y)))
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .size(100.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val eventOffset = PointF(offset.x, offset.y)
                            onEvent(MainScreenEvent.SpinApplied(eventOffset))
                        },
                        onDragEnd = { onEvent(MainScreenEvent.SpinSelectionEnded) },
                        onDragCancel = { onEvent(MainScreenEvent.SpinSelectionEnded) }
                    ) { change, _ ->
                        val eventOffset = PointF(change.position.x, change.position.y)
                        onEvent(MainScreenEvent.SpinApplied(eventOffset))
                        change.consume()
                    }
                }
        ) {
            val radius = size.minDimension / 2f
            val center = Offset(radius, radius)

            val colorStops = arrayOf(
                0.0f to Color.White,
                0.4f to RebelYellow.copy(alpha = 0.7f),
                0.7f to WarningRed.copy(alpha = 0.8f),
                1.0f to Color.Blue.copy(alpha = 0.9f)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = colorStops.map { it.second },
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center,
                alpha = spinPathAlpha
            )

            drawCircle(
                color = Color.White.copy(alpha = 0.5f * spinPathAlpha),
                radius = radius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Draw the lingering indicator first, so the live one is on top
            lingeringSpinOffset?.let {
                drawIndicator(it, center, radius, Color.White.copy(alpha = 0.6f * spinPathAlpha))
            }

            // Draw live selection indicator
            selectedSpinOffset?.let {
                drawIndicator(it, center, radius, Color.White)
            }
        }
    }
}

private fun DrawScope.drawIndicator(
    offset: PointF,
    center: Offset,
    radius: Float,
    color: Color
) {
    val dragX = offset.x - center.x
    val dragY = offset.y - center.y
    val distance = hypot(dragX, dragY)
    val clampedDistance = distance.coerceAtMost(radius)

    val angle = atan2(dragY, dragX)
    val indicatorX = center.x + clampedDistance * cos(angle)
    val indicatorY = center.y + clampedDistance * sin(angle)

    drawCircle(
        color = color,
        radius = 5.dp.toPx(),
        center = Offset(indicatorX, indicatorY)
    )
    drawCircle(
        color = color,
        radius = 5.dp.toPx(),
        center = Offset(indicatorX, indicatorY),
        style = Stroke(width = 2.dp.toPx())
    )
}