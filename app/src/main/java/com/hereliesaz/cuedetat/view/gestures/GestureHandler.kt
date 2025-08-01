// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/gestures/GestureHandler.kt

package com.hereliesaz.cuedetat.view.gestures

import android.graphics.PointF
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import kotlin.math.abs

fun Modifier.detectManualGestures(uiState: CueDetatState, onEvent: (MainScreenEvent) -> Unit) =
    this.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = true)
            onEvent(MainScreenEvent.ScreenGestureStarted(PointF(down.position.x, down.position.y)))
            down.consume()

            var lastCentroid: Offset = down.position

            do {
                val event = awaitPointerEvent()
                val canceled = event.changes.any { it.isConsumed }
                if (!canceled) {
                    val centroid = event.calculateCentroid()

                    if (event.changes.size > 1) {
                        // Zoom is allowed in all modes.
                        val zoom = event.calculateZoom()
                        if (zoom != 1f) {
                            onEvent(MainScreenEvent.ZoomScaleChanged(zoom))
                        }

                        // Rotation and Pan are only allowed when the table is visible (i.e., not in Beginner Mode).
                        if (uiState.table.isVisible) {
                            val rotation = event.calculateRotation()
                            if (rotation != 0f) {
                                onEvent(MainScreenEvent.TableRotationApplied(rotation))
                            }
                            val pan = event.calculatePan()
                            if (abs(pan.y) > 0.1f) {
                                onEvent(MainScreenEvent.PanView(PointF(0f, pan.y)))
                            }
                        }
                    } else if (event.changes.size == 1) {
                        val pan = event.calculatePan()
                        // Single-finger drag behavior
                        if (pan != Offset.Zero) {
                            // Pan is disabled in locked beginner mode
                            if (uiState.isWorldLocked && (uiState.experienceMode != ExperienceMode.BEGINNER || !uiState.isBeginnerViewLocked)) {
                                onEvent(MainScreenEvent.PanView(PointF(pan.x, pan.y)))
                            } else {
                                // Perform a logical drag on the plane.
                                val previousPosition = lastCentroid
                                val currentPosition = centroid
                                onEvent(
                                    MainScreenEvent.Drag(
                                        previousPosition = PointF(
                                            previousPosition.x,
                                            previousPosition.y
                                        ),
                                        currentPosition = PointF(
                                            currentPosition.x,
                                            currentPosition.y
                                        )
                                    )
                                )
                            }
                        }
                    }
                    lastCentroid = centroid
                }

                event.changes.forEach { if (it.pressed) it.consume() }
            } while (!canceled && event.changes.any { it.pressed })

            onEvent(MainScreenEvent.GestureEnded)
        }
    }