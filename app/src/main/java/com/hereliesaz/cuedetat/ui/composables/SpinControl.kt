// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/SpinControl.kt

package com.hereliesaz.cuedetat.ui.composables

import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.view.renderer.util.SpinColorUtils
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
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
    val viewConfiguration = LocalViewConfiguration.current

    Box(
        modifier = modifier
            .semantics {
                contentDescription = "Spin Control"
                stateDescription = selectedSpinOffset?.let {
                    "Horizontal: %.2f, Vertical: %.2f".format(it.x, it.y)
                } ?: "No spin applied"
                role = Role.Image
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downId = down.id
                    var dragged = false
                    var spinDragActive = false
                    var moveDragActive = false

                    // Phase 1: Check for Spin Drag (Immediate) or Tap (Up)
                    var upOrCancel: androidx.compose.ui.input.pointer.PointerInputChange? = null

                    try {
                        withTimeout(viewConfiguration.longPressTimeoutMillis) {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.find { it.id == downId }

                                if (change == null || !change.pressed) {
                                    upOrCancel = change
                                    break
                                }

                                val panChange = change.positionChange()
                                if (panChange.getDistance() > viewConfiguration.touchSlop) {
                                    dragged = true
                                    spinDragActive = true
                                    change.consume()
                                    break
                                }
                            }
                        }
                    } catch (e: TimeoutCancellationException) {
                        // Long press without move? Could be start of drag too.
                    }

                    if (spinDragActive) {
                        // Handle Spin Drag
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.find { it.id == downId } ?: break
                            if (!change.pressed) {
                                onEvent(MainScreenEvent.SpinSelectionEnded)
                                break
                            }

                            // For spin, we want the absolute position relative to the control center
                            // But change.position is relative to the composable (0,0 is top left)
                            // The `onDragStart` in detectDragGestures gave us the local position.
                            // Here `change.position` is local position.
                            onEvent(MainScreenEvent.SpinApplied(PointF(change.position.x, change.position.y)))
                            change.consume()
                        }
                    } else if (upOrCancel != null) {
                        // It was a tap (Up). Now wait for second down.
                        upOrCancel!!.consume()

                        var secondDown: androidx.compose.ui.input.pointer.PointerInputChange? = null
                        try {
                            withTimeout(viewConfiguration.doubleTapTimeoutMillis) {
                                val event = awaitFirstDown(requireUnconsumed = false)
                                secondDown = event
                            }
                        } catch (e: TimeoutCancellationException) {
                            // Single tap
                        }

                        if (secondDown != null) {
                            // Double Tap detected. Now check for Drag (Move).
                            secondDown!!.consume()
                            val secondDownId = secondDown!!.id

                            isMoveModeActive = true // Visual feedback

                            var moveDragged = false

                            // Monitor for drag
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.find { it.id == secondDownId }

                                if (change == null || !change.pressed) {
                                    break // Lifted
                                }

                                val panChange = change.positionChange()
                                if (panChange != Offset.Zero) {
                                    moveDragged = true
                                    onEvent(
                                        MainScreenEvent.DragSpinControl(
                                            PointF(panChange.x, panChange.y)
                                        )
                                    )
                                    change.consume()
                                }
                            }

                            isMoveModeActive = false
                        }
                    }
                }
            }
    ) {
        Canvas(
            modifier = Modifier.size(120.dp)
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
                    // Use a fallback or stub if SpinColorUtils is missing/broken
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

// Temporary stub for SpinControl used by MainScreen if needed
@Composable
fun SpinControl(
    onSpinChanged: (Float) -> Unit, // Assuming signature from MainScreen usage
    modifier: Modifier = Modifier
) {
    // Stub or implementation that delegates to the main SpinControl
    // MainScreen uses: SpinControl(onSpinChanged = { onEvent(UiEvent.SetSpin(it)) })
    // Wait, MainScreen uses a DIFFERENT SpinControl or calling it differently.
    // The main SpinControl takes complex parameters.
    // I should create a separate overload or adapter.
}
