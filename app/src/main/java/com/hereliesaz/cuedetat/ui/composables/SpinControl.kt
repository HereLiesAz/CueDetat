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
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.view.renderer.util.SpinColorUtils
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * A custom composable for controlling Spin (English) on the cue ball.
 *
 * It features two modes of interaction:
 * 1. **Apply Spin:** Dragging from the center outward sets the spin value.
 * 2. **Move Control:** Double-tap and drag moves the control widget itself around the screen.
 *
 * @param modifier Styling modifier.
 * @param centerPosition The current screen coordinates of the control center.
 * @param selectedSpinOffset The active spin vector being applied (dragging).
 * @param lingeringSpinOffset The last applied spin vector (released).
 * @param spinPathAlpha Opacity multiplier for the spin visualization.
 * @param onEvent Callback for dispatching spin or drag events.
 */
@Composable
fun SpinControl(
    modifier: Modifier = Modifier,
    centerPosition: PointF, // Note: Passed but used by parent layout, logical here is stateless position-wise except via modifier/event.
    selectedSpinOffset: PointF?,
    lingeringSpinOffset: PointF?,
    spinPathAlpha: Float,
    onEvent: (MainScreenEvent) -> Unit
) {
    // Local state to track if we are in "Move Widget" mode (vs "Apply Spin" mode).
    var isMoveModeActive by remember { mutableStateOf(false) }
    // Icon to display when moving the widget.
    val moveIconPainter = rememberVectorPainter(image = Icons.Default.OpenWith)
    // Access system configuration for double-tap timeout.
    val viewConfiguration = LocalViewConfiguration.current

    Box(
        modifier = modifier
            // Gesture detector for "Move Widget" (Double Tap + Drag).
            .pointerInput(Unit) {
                awaitEachGesture {
                    // Wait for initial touch.
                    awaitFirstDown(requireUnconsumed = false)
                    // Wait for release.
                    val firstUp = waitForUpOrCancellation()

                    if (firstUp != null) {
                        firstUp.consume()
                        // Wait for a second touch within the timeout window.
                        val secondDown = withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
                            awaitFirstDown(requireUnconsumed = false)
                        }

                        if (secondDown != null) {
                            secondDown.consume()
                            // Double tap detected: Enter move mode.
                            isMoveModeActive = true
                            var pointerId = secondDown.id

                            try {
                                // Loop to handle the drag associated with the second tap.
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val dragChange = event.changes.find { it.id == pointerId }

                                    // Check if finger lifted or cancelled.
                                    if (dragChange == null || !dragChange.pressed) {
                                        break
                                    }
                                    if (dragChange.isConsumed) {
                                        continue
                                    }
                                    // Calculate movement delta.
                                    val pan = dragChange.positionChange()
                                    if (pan != Offset.Zero) {
                                        // Dispatch event to move the control's position in state.
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
                                // Exit move mode when drag ends.
                                isMoveModeActive = false
                            }
                        }
                    }
                }
            }
    ) {
        // Inner Canvas for drawing the control and handling Spin gestures.
        Canvas(
            modifier = Modifier
                .size(120.dp)
                // Gesture detector for "Apply Spin" (Single Drag).
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
            // Slightly enlarge the control visually when in "Move Mode".
            val scaleFactor = if (isMoveModeActive) 1.1f else 1.0f

            withTransform({
                scale(scaleFactor, scaleFactor, center)
            }) {
                // Background visual for Move Mode.
                if (isMoveModeActive) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.2f),
                        radius = radius,
                        center = center
                    )
                }

                // Draw the color wheel segments.
                val numArcs = 72
                val arcAngle = 360f / numArcs
                for (i in 0 until numArcs) {
                    val startAngle = i * arcAngle
                    val colorSampleAngle = startAngle + (arcAngle / 2)
                    // Get color corresponding to this angle from the shared utility.
                    val color = SpinColorUtils.getColorFromAngleAndDistance(colorSampleAngle, 1.0f)

                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = arcAngle,
                        useCenter = true,
                        alpha = spinPathAlpha // Fade out when idle if configured.
                    )
                }

                // Draw gradient overlay to make center white (neutral spin).
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

                // Draw outer border.
                drawCircle(
                    color = Color.White.copy(alpha = 0.5f * spinPathAlpha),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Draw lingering indicator (ghost of last setting).
                lingeringSpinOffset?.let {
                    drawIndicator(
                        it,
                        center,
                        radius,
                        Color.White.copy(alpha = 0.6f * spinPathAlpha)
                    )
                }

                // Draw active indicator (current touch).
                selectedSpinOffset?.let {
                    drawIndicator(it, center, radius, Color.White)
                }

                // Draw "Move" icon overlay when mode is active.
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

/**
 * Helper to draw the small circle indicator for the spin position.
 */
private fun DrawScope.drawIndicator(
    offset: PointF,
    center: Offset,
    radius: Float,
    color: Color
) {
    // Calculate vector from center.
    val dragX = offset.x - center.x
    val dragY = offset.y - center.y
    val distance = hypot(dragX, dragY)
    // Clamp to the radius of the control.
    val clampedDistance = distance.coerceAtMost(radius)

    // Calculate position based on clamped distance.
    val angle = atan2(dragY, dragX)
    val indicatorX = center.x + clampedDistance * cos(angle)
    val indicatorY = center.y + clampedDistance * sin(angle)

    // Draw solid circle.
    drawCircle(
        color = color,
        radius = 5.dp.toPx(),
        center = Offset(indicatorX, indicatorY)
    )
    // Draw ring outline.
    drawCircle(
        color = color,
        radius = 5.dp.toPx(),
        center = Offset(indicatorX, indicatorY),
        style = Stroke(width = 2.dp.toPx())
    )
}
