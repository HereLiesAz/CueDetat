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
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.view.renderer.util.SpinColorUtils
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MasseControl(
    modifier: Modifier = Modifier,
    selectedSpinOffset: PointF?,
    lingeringSpinOffset: PointF?,
    spinPathAlpha: Float,
    elevationAngle: Float,
    onEvent: (MainScreenEvent) -> Unit
) {
    var isMoveModeActive by remember { mutableStateOf(false) }
    val moveIconPainter = rememberVectorPainter(image = Icons.Default.OpenWith)
    val viewConfig = LocalViewConfiguration.current

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    val firstUp = waitForUpOrCancellation()
                    if (firstUp != null) {
                        firstUp.consume()
                        val secondDown = withTimeoutOrNull(viewConfig.doubleTapTimeoutMillis) {
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
                                    if (dragChange == null || !dragChange.pressed) break
                                    val pan = dragChange.positionChange()
                                    if (pan != Offset.Zero) {
                                        onEvent(MainScreenEvent.DragSpinControl(PointF(pan.x, pan.y)))
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
                            onEvent(MainScreenEvent.SpinApplied(PointF(offset.x, offset.y)))
                        },
                        onDragEnd = { onEvent(MainScreenEvent.SpinSelectionEnded) },
                        onDragCancel = { onEvent(MainScreenEvent.SpinSelectionEnded) }
                    ) { change, _ ->
                        val pos = change.position
                        onEvent(MainScreenEvent.SpinApplied(PointF(pos.x, pos.y)))
                        change.consume()
                    }
                }
        ) {
            val radius = size.minDimension / 2f
            val center = Offset(radius, radius)
            val scaleFactor = if (isMoveModeActive) 1.1f else 1.0f

            withTransform({ scale(scaleFactor, scaleFactor, center) }) {
                if (isMoveModeActive) {
                    drawCircle(color = Color.White.copy(alpha = 0.2f), radius = radius, center = center)
                }

                // Draw the color wheel
                val numArcs = 72
                val arcAngle = 360f / numArcs
                for (i in 0 until numArcs) {
                    val startAngle = i * arcAngle
                    val color = SpinColorUtils.getColorFromAngleAndDistance(startAngle + (arcAngle / 2), 1.0f)
                    drawArc(color = color, startAngle = startAngle, sweepAngle = arcAngle, useCenter = true, alpha = spinPathAlpha)
                }

                drawCircle(brush = Brush.radialGradient(colors = listOf(Color.White, Color.Transparent), center = center, radius = radius), radius = radius, center = center, alpha = spinPathAlpha)
                drawCircle(color = Color.White.copy(alpha = 0.5f * spinPathAlpha), radius = radius, center = center, style = Stroke(width = 2.dp.toPx()))

                // Draw the indicators (dots)
                lingeringSpinOffset?.let { drawLogicalIndicator(it, center, radius, Color.White.copy(alpha = 0.6f * spinPathAlpha)) }

                selectedSpinOffset?.let { activeOffset ->
                    drawLogicalIndicator(activeOffset, center, radius, Color.White)

                    // --- THE POOL STICK ANGLE (MASSE STICK) ---
                    // The stick is drawn as a foreshortened line based on phone tilt (elevationAngle).
                    // If elevation is 90 (vertical), the stick is a short tip. If 0 (flat), it is full length.
                    val stickBaseLen = radius * 1.5f
                    val stickWidth = 6.dp.toPx()
                    val foreshortenedLen = stickBaseLen * cos(Math.toRadians(elevationAngle.toDouble())).toFloat()

                    val angleToCenter = atan2(activeOffset.y - center.y, activeOffset.x - center.x)

                    withTransform({
                        // Rotate stick to align with the vector from the wheel center to the impact point
                        rotate(Math.toDegrees(angleToCenter.toDouble()).toFloat(), pivot = Offset(activeOffset.x, activeOffset.y))
                    }) {
                        // Draw the cue stick body (tapered line)
                        drawLine(
                            color = Color.White.copy(alpha = 0.8f),
                            start = Offset(activeOffset.x, activeOffset.y),
                            end = Offset(activeOffset.x + foreshortenedLen, activeOffset.y),
                            strokeWidth = stickWidth
                        )
                        // Draw the cue tip
                        drawCircle(
                            color = Color.Black,
                            radius = stickWidth / 2f,
                            center = Offset(activeOffset.x, activeOffset.y)
                        )
                    }
                }

                if (isMoveModeActive) {
                    with(moveIconPainter) {
                        translate(left = center.x - intrinsicSize.width / 2, top = center.y - intrinsicSize.height / 2) {
                            draw(size = intrinsicSize, colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.8f)))
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawLogicalIndicator(offset: PointF, center: Offset, radius: Float, color: Color) {
    drawCircle(color = color, radius = 5.dp.toPx(), center = Offset(offset.x, offset.y))
    drawCircle(color = color, radius = 5.dp.toPx(), center = Offset(offset.x, offset.y), style = Stroke(width = 2.dp.toPx()))
}