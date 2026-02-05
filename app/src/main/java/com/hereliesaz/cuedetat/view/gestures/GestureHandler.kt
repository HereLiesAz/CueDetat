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

/**
 * A custom Compose modifier that handles multi-touch gestures for the main canvas.
 *
 * It detects:
 * - Single-finger Drag: Moves objects on the table (or pans the camera if locked).
 * - Two-finger Pinch: Controls Zoom.
 * - Two-finger Rotation: Rotates the table/camera.
 * - Two-finger Pan: Pans the camera view.
 *
 * @param uiState The current state (used to gate specific gestures based on mode).
 * @param onEvent Callback to dispatch detected gesture events to the ViewModel.
 */
fun Modifier.detectManualGestures(uiState: CueDetatState, onEvent: (MainScreenEvent) -> Unit) =
    this.pointerInput(Unit) {
        // Wait for each gesture sequence (down -> move -> up/cancel).
        awaitEachGesture {
            // Wait for the first pointer to touch the screen.
            val down = awaitFirstDown(requireUnconsumed = true)

            // Notify system that a gesture has started at the touch point.
            onEvent(MainScreenEvent.ScreenGestureStarted(PointF(down.position.x, down.position.y)))

            // Consume the down event so other components don't steal it.
            down.consume()

            // Track the centroid (center point of fingers) to calculate movement.
            var lastCentroid: Offset = down.position

            // Loop to handle drag/move events until all fingers are lifted.
            do {
                val event = awaitPointerEvent()
                val canceled = event.changes.any { it.isConsumed }
                if (!canceled) {
                    val centroid = event.calculateCentroid()

                    // Check how many pointers are active.
                    if (event.changes.size > 1) {
                        // MULTI-TOUCH DETECTED (2+ fingers)

                        // 1. Zoom (Pinch)
                        // Allowed in all modes.
                        val zoom = event.calculateZoom()
                        if (zoom != 1f) {
                            onEvent(MainScreenEvent.ZoomScaleChanged(zoom))
                        }

                        // 2. Rotation & Pan (Two-finger drag)
                        // Only allowed when the Table is visible (Expert mode/Banking).
                        // In Beginner mode, the view is simplified/locked.
                        if (uiState.table.isVisible) {
                            // Rotation
                            val rotation = event.calculateRotation()
                            if (rotation != 0f) {
                                onEvent(MainScreenEvent.TableRotationApplied(rotation))
                            }
                            // Pan (using two fingers to move the camera view)
                            val pan = event.calculatePan()
                            if (abs(pan.y) > 0.1f) {
                                // Dispatch pan event. Note: implementation might only use Y or X/Y depending on needs.
                                onEvent(MainScreenEvent.PanView(PointF(0f, pan.y)))
                            }
                        }
                    } else if (event.changes.size == 1) {
                        // SINGLE-TOUCH DETECTED (1 finger)

                        val pan = event.calculatePan()
                        // Ensure there was actual movement.
                        if (pan != Offset.Zero) {
                            // Special Case: "World Locked" mode (AR tracking suspended).
                            // If world is locked, single finger might PAN the view instead of dragging objects,
                            // UNLESS we are in Beginner mode which locks the view entirely.
                            if (uiState.isWorldLocked && (uiState.experienceMode != ExperienceMode.BEGINNER || !uiState.isBeginnerViewLocked)) {
                                onEvent(MainScreenEvent.PanView(PointF(pan.x, pan.y)))
                            } else {
                                // Standard Case: Dragging virtual objects (Ball, Slider, Spin control).
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
                    // Update the last known centroid for the next delta calculation.
                    lastCentroid = centroid
                }

                // Consume all pointer changes to indicate we handled them.
                event.changes.forEach { if (it.pressed) it.consume() }
            } while (!canceled && event.changes.any { it.pressed })

            // Gesture sequence ended (all fingers up).
            onEvent(MainScreenEvent.GestureEnded)
        }
    }
