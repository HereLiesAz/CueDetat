// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/gestures/GestureHandler.kt

package com.hereliesaz.cuedetat.view.gestures

import android.graphics.PointF
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.input.pointer.pointerInput
import com.hereliesaz.cuedetat.domain.ArProjectionPolicy
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import kotlin.math.abs

/**
 * A custom Compose modifier that handles multi-touch gestures for the main canvas.
 *
 * It detects:
 * - Single-finger Drag: Moves objects on the table (or pans the camera if world-locked,
 *   unless the table is AR-anchored, where panning does nothing).
 * - Two-finger Pinch: Controls Zoom (the table's size on screen). With ARCore world tracking the
 *   table's size is metric, and zoom moves it toward or away from the camera, so the pinch
 *   resizes the table in place instead ([MainScreenEvent.TableScaleChanged]); the zoom slider
 *   keeps the range.
 * - Two-finger Rotation: Rotates the table/camera.
 * - Two-finger Pan: Pans the camera view.
 * - Three-finger Drag: Moves the table up/down along its Z-axis. Only when three fingers are down
 *   and all of them are moving the same way ([isCoherentVerticalDrag]); a thumb resting on the
 *   screen edge during a two-finger gesture is not a third finger of this one.
 *
 * The pointer-input coroutine is keyed on `Unit` so it is never restarted mid-drag; the
 * latest [uiState] and [onEvent] are read through [rememberUpdatedState], so gating
 * (table visibility, world lock, ball-selection phase, beginner lock) always reflects the
 * current state rather than the state of the first composition.
 *
 * When the number of pressed pointers changes (a finger lands or lifts) the tracked
 * centroid is re-seeded and no movement is emitted for that event, so lifting one finger
 * of a pinch does not teleport a dragged ball by half the finger spread.
 *
 * @param uiState The current state (used to gate specific gestures based on mode).
 * @param onEvent Callback to dispatch detected gesture events to the ViewModel.
 */
@Composable
fun Modifier.detectManualGestures(uiState: CueDetatState, onEvent: (MainScreenEvent) -> Unit): Modifier {
    val currentState by rememberUpdatedState(uiState)
    val currentOnEvent by rememberUpdatedState(onEvent)
    return this.pointerInput(Unit) {
        val onEvent: (MainScreenEvent) -> Unit = { currentOnEvent(it) }
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
            var lastPressedCount = 1

            // Loop to handle drag/move events until all fingers are lifted.
            var hasMovedDramatically = false
            do {
                val event = awaitPointerEvent()
                val canceled = event.changes.any { it.isConsumed }
                if (!canceled) {
                    val uiState = currentState
                    val centroid = event.calculateCentroid()
                    val pressedCount = event.changes.count { it.pressed }
                    if (pressedCount != lastPressedCount) {
                        // Finger landed or lifted: re-seed and emit nothing this event.
                        lastPressedCount = pressedCount
                        if (centroid.isSpecified) lastCentroid = centroid
                        event.changes.forEach { if (it.pressed) it.consume() }
                        continue
                    }
                    val isDynamicBeginner = uiState.experienceMode == ExperienceMode.BEGINNER && !uiState.isBeginnerViewLocked

                    // Check how many pointers are active.
                    if (event.changes.size >= 3) {
                        hasMovedDramatically = true
                        // THREE-FINGER GESTURE: Z-axis table movement. Not gated on
                        // table.isVisible (that is EXPERT-only) so it also works in
                        // dynamic beginner mode.
                        val pan = event.calculatePan()
                        val pressed = event.changes.filter { it.pressed }
                        if (abs(pan.y) > 0.1f &&
                            isCoherentVerticalDrag(pressed.map { it.position.y - it.previousPosition.y })
                        ) {
                            onEvent(MainScreenEvent.MoveTableZ(-pan.y * 0.05f))
                        }
                    } else if (event.changes.size == 2) {
                        hasMovedDramatically = true
                        // TWO-FINGER GESTURE: zoom, rotation, pan
                        
                        // 1. Zoom (Pinch)
                        if (uiState.experienceMode != ExperienceMode.BEGINNER || !uiState.isBeginnerViewLocked) {
                            val zoom = event.calculateZoom()
                            if (zoom != 1f) {
                                if (ArProjectionPolicy.isWorldTracked(uiState.cameraMode, uiState.depthCapability)) {
                                    onEvent(MainScreenEvent.TableScaleChanged(zoom))
                                } else {
                                    onEvent(MainScreenEvent.ZoomScaleChanged(zoom))
                                }
                            }
                        }

                        // 2. Rotation & Pan
                        if (uiState.table.isVisible) {
                            val rotation = event.calculateRotation()
                            if (rotation != 0f) onEvent(MainScreenEvent.TableRotationApplied(rotation))
                            val pan = event.calculatePan()
                            if (abs(pan.y) > 0.1f || abs(pan.x) > 0.1f) onEvent(MainScreenEvent.PanView(PointF(pan.x, pan.y)))
                        }
                    } else if (event.changes.size == 1) {
                        val pan = event.calculatePan()
                        if (pan.getDistance() > 2f) hasMovedDramatically = true
                        
                        if (pan != Offset.Zero) {
                            // An AR-anchored table ignores viewOffset, so a pan would be a dead
                            // gesture; drags go to the balls instead.
                            if (uiState.isWorldLocked && !isDynamicBeginner &&
                                !ArProjectionPolicy.isTableAnchored(uiState)
                            ) {
                                onEvent(MainScreenEvent.PanView(PointF(pan.x, pan.y)))
                            } else {
                                val previousPosition = lastCentroid
                                val currentPosition = centroid
                                onEvent(MainScreenEvent.Drag(
                                    previousPosition = PointF(previousPosition.x, previousPosition.y),
                                    currentPosition = PointF(currentPosition.x, currentPosition.y)
                                ))
                            }
                        }
                    }
                    if (centroid.isSpecified) lastCentroid = centroid
                }
                event.changes.forEach { if (it.pressed) it.consume() }
            } while (!canceled && event.changes.any { it.pressed })

            // Gesture sequence ended (all fingers up).
            val uiState = currentState
            // A tap while a ball pick is armed (Cue / Target button, or first-run setup) places it.
            // Works with the camera off too: the pick is the user's explicit choice.
            if (!hasMovedDramatically &&
                uiState.ballSelectionPhase != com.hereliesaz.cuedetat.domain.BallSelectionPhase.NONE
            ) {
                onEvent(MainScreenEvent.ArSurfaceTapped(PointF(down.position.x, down.position.y)))
            }
            onEvent(MainScreenEvent.GestureEnded)
        }
    }
}

/** Per-finger vertical travel, in pixels, below which a finger counts as resting. */
private const val RESTING_FINGER_PX = 1f

/**
 * True when at least three fingers are down and every one of them moved vertically, all the same
 * way: the three-finger table-height drag. [verticalDeltas] is each pressed finger's change in y
 * since the last event. A finger that is not moving (a resting thumb) or moving against the others
 * makes this false, so it can't drive the height slider.
 */
internal fun isCoherentVerticalDrag(verticalDeltas: List<Float>): Boolean {
    if (verticalDeltas.size < 3) return false
    val down = verticalDeltas.all { it > RESTING_FINGER_PX }
    val up = verticalDeltas.all { it < -RESTING_FINGER_PX }
    return down || up
}
