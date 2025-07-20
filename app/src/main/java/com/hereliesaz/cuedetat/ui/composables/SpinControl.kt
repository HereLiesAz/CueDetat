// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/SpinControl.kt

package com.hereliesaz.cuedetat.ui.composables

import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.renderer.util.SpinColorUtils
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
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
    var isMoveModeActive by remember { mutableStateOf(false) }
    val moveIconPainter = rememberVectorPainter(image = Icons.Default.OpenWith)

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    val firstUp = waitForUpOrCancellation()

                    if (firstUp != null) {
                        firstUp.consume()
                        val secondDown = withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
                            awaitFirstDown(requireUnconsumed = false)
                        }

                        if (secondDown != null) {
                            secondDown.consume()
                            isMoveModeActive = true
                            var pointerId = secondDown.id

                            try {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val dragChange = event.changes.find { it.id == pointerId }

                                    if (dragChange == null || !dragChange.pressed) {
                                        break
                                    }
                                    if (dragChange.isConsumed) {
                                        continue
                                    }
                                    val pan = dragChange.positionChange()
                                    if (pan != Offset.Zero) {
                                        onEvent(
                                            MainScreenEvent.DragSpinControl(
                                                PointF(
                                                    pan.x,
                                                    pan.y
                                                )
                                            )
                                        )
                                        dragChange.consume()
                                    }
                                }
                            } finally {
                                isMoveModeActive = false
                            }
                        }
                    }
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .size(120.dp)
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
            val scaleFactor = if (isMoveModeActive) 1.1f else 1.0f

            withTransform({
                scale(scaleFactor, scaleFactor, center)
            }) {
                if (isMoveModeActive) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.2f),
                        radius = radius,
                        center = center
                    )
                }

                val numArcs = 72
                val arcAngle = 360f / numArcs
                for (i in 0 until numArcs) {
                    val startAngle = i * arcAngle
                    val colorSampleAngle = startAngle + (arcAngle / 2)
                    val color = SpinColorUtils.getColorFromAngleAndDistance(colorSampleAngle, 1.0f)

                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = arcAngle,
                        useCenter = true,
                        alpha = spinPathAlpha
                    )
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, Color.Transparent),
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

                lingeringSpinOffset?.let {
                    drawIndicator(
                        it,
                        center,
                        radius,
                        Color.White.copy(alpha = 0.6f * spinPathAlpha)
                    )
                }

                selectedSpinOffset?.let {
                    drawIndicator(it, center, radius, Color.White)
                }

                if (isMoveModeActive) {
                    with(moveIconPainter) {
                        translate(
                            left = center.x - intrinsicSize.width / 2,
                            top = center.y - intrinsicSize.height / 2
                        ) {
                            draw(
                                size = intrinsicSize,
                                colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.8f))
                            )
                        }
                    }
                }
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