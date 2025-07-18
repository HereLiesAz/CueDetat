// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/gestures/GestureHandler.kt

package com.hereliesaz.cuedetat.view.gestures

import android.graphics.PointF
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import kotlin.math.abs

fun Modifier.detectManualGestures(onEvent: (MainScreenEvent) -> Unit) =
    this.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            onEvent(MainScreenEvent.ScreenGestureStarted(PointF(down.position.x, down.position.y)))
            down.consume()

            var lastCentroid: Offset = down.position

            do {
                val event = awaitPointerEvent()
                val canceled = event.changes.any { it.isConsumed }
                if (!canceled) {
                    val zoom = event.calculateZoom()
                    val rotation = event.calculateRotation()
                    val pan = event.calculatePan()
                    val centroid = event.calculateCentroid()

                    if (event.changes.size > 1) {
                        // Multi-finger gestures: Process all transformations.
                        if (zoom != 1f) {
                            onEvent(MainScreenEvent.ZoomScaleChanged(zoom))
                        }
                        if (rotation != 0f) {
                            onEvent(MainScreenEvent.TableRotationApplied(rotation))
                        }
                        // As commanded, only vertical pan is enacted.
                        if (abs(pan.y) > 0.1f) { // Use a small threshold to ignore noise
                            onEvent(MainScreenEvent.PanView(PointF(0f, pan.y)))
                        }

                    } else if (event.changes.size == 1) {
                        // Single-finger drag
                        if (pan != Offset.Zero) {
                            val previousPosition = lastCentroid
                            val currentPosition = centroid
                            onEvent(
                                MainScreenEvent.Drag(
                                    previousPosition = PointF(previousPosition.x, previousPosition.y),
                                    currentPosition = PointF(currentPosition.x, currentPosition.y)
                                )
                            )
                        }
                    }
                    lastCentroid = centroid
                }

                event.changes.forEach { if (it.pressed) it.consume() }
            } while (!canceled && event.changes.any { it.pressed })

            onEvent(MainScreenEvent.GestureEnded)
        }
    }