// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/GestureReducer.kt

package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import androidx.compose.ui.geometry.Offset
import com.hereliesaz.cuedetat.domain.ReducerUtils
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.ui.ZoomMapping
import com.hereliesaz.cuedetat.view.state.ExperienceMode
import com.hereliesaz.cuedetat.view.state.InteractionMode
import com.hereliesaz.cuedetat.view.state.OverlayState
import com.hereliesaz.cuedetat.view.state.SnapCandidate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

@Singleton
class GestureReducer @Inject constructor(private val reducerUtils: ReducerUtils) {
    fun reduce(currentState: OverlayState, event: MainScreenEvent): OverlayState {
        return when(event) {
            is MainScreenEvent.LogicalGestureStarted -> handleLogicalGestureStarted(currentState, event)
            is MainScreenEvent.LogicalDragApplied -> handleLogicalDragApplied(currentState, event)
            is MainScreenEvent.GestureEnded -> handleGestureEnded(currentState)
            else -> currentState
        }
    }

    private fun handleLogicalGestureStarted(currentState: OverlayState, event: MainScreenEvent.LogicalGestureStarted): OverlayState {
        if (currentState.experienceMode == ExperienceMode.BEGINNER && currentState.isBeginnerViewLocked) {
            return currentState // Do not allow any interaction in locked beginner mode
        }

        val logicalPoint = event.logicalPoint
        val onPlaneBall = currentState.onPlaneBall
        val spinControlCenter = currentState.spinControlCenter
        val protractorUnit = currentState.protractorUnit


        // Define a constant touch radius in screen pixels (e.g., ~32dp)
        val screenTouchRadiusPx = 96f
        // Calculate the current zoom factor from the slider position
        val (minZoom, maxZoom) = ZoomMapping.getZoomRange(
            currentState.experienceMode,
            currentState.isBeginnerViewLocked
        )
        val zoomFactor = ZoomMapping.sliderToZoom(currentState.zoomSliderPosition, minZoom, maxZoom)
        // Convert the screen-space radius to a dynamic logical-space radius.
        // As zoom increases, the logical radius needed to cover the same screen area decreases.
        val dynamicLogicalTouchRadius = screenTouchRadiusPx / zoomFactor


        // Banking Mode Logic
        if (currentState.isBankingMode) {
            return if (onPlaneBall != null && getDistance(
                    logicalPoint,
                    onPlaneBall.center
                ) < dynamicLogicalTouchRadius
            ) {
                currentState.copy(
                    interactionMode = InteractionMode.MOVING_ACTUAL_CUE_BALL,
                    hasCueBallBeenMoved = true,
                    isMagnifierVisible = true,
                    magnifierSourceCenter = event.screenOffset,
                    isWorldLocked = false
                )
            } else {
                currentState.copy(interactionMode = InteractionMode.AIMING_BANK_SHOT)
            }
        }

        // Protractor Mode Logic
        val spinControlTouchRadius = (onPlaneBall?.radius ?: protractorUnit.radius) * 1.5f
        if (currentState.isSpinControlVisible && spinControlCenter != null && getDistance(event.screenOffset, spinControlCenter) < spinControlTouchRadius) {
            return currentState.copy(interactionMode = InteractionMode.MOVING_SPIN_CONTROL, isMagnifierVisible = true, magnifierSourceCenter = event.screenOffset)
        }

        currentState.obstacleBalls.forEachIndexed { index, obstacle ->
            if (getDistance(logicalPoint, obstacle.center) < dynamicLogicalTouchRadius) {
                return currentState.copy(
                    interactionMode = InteractionMode.MOVING_OBSTACLE_BALL,
                    movingObstacleBallIndex = index,
                    isMagnifierVisible = true,
                    magnifierSourceCenter = event.screenOffset,
                    isWorldLocked = false
                )
            }
        }

        if (onPlaneBall != null && getDistance(
                logicalPoint,
                onPlaneBall.center
            ) < dynamicLogicalTouchRadius
        ) {
            return currentState.copy(
                interactionMode = InteractionMode.MOVING_ACTUAL_CUE_BALL,
                hasCueBallBeenMoved = true,
                isMagnifierVisible = true,
                magnifierSourceCenter = event.screenOffset,
                isWorldLocked = false
            )
        }

        val distToTargetBall = getDistance(logicalPoint, protractorUnit.center)
        if (distToTargetBall < dynamicLogicalTouchRadius) {
            return currentState.copy(
                interactionMode = InteractionMode.MOVING_PROTRACTOR_UNIT,
                hasTargetBallBeenMoved = true,
                isMagnifierVisible = true,
                magnifierSourceCenter = event.screenOffset,
                isWorldLocked = false
            )
        }

        val distToGhostBall = getDistance(logicalPoint, protractorUnit.ghostCueBallCenter)
        if (distToGhostBall < dynamicLogicalTouchRadius) {
            return currentState.copy(
                interactionMode = InteractionMode.MOVING_PROTRACTOR_UNIT,
                hasTargetBallBeenMoved = true,
                isMagnifierVisible = true,
                magnifierSourceCenter = event.screenOffset,
                isWorldLocked = false
            )
        }

        return currentState.copy(interactionMode = InteractionMode.ROTATING_PROTRACTOR, isMagnifierVisible = false)
    }

    private fun handleLogicalDragApplied(currentState: OverlayState, event: MainScreenEvent.LogicalDragApplied): OverlayState {
        val newState = when (currentState.interactionMode) {
            InteractionMode.MOVING_PROTRACTOR_UNIT -> {
                val dx = event.currentLogicalPoint.x - event.previousLogicalPoint.x
                val dy = event.currentLogicalPoint.y - event.previousLogicalPoint.y
                val newCenter = PointF(currentState.protractorUnit.center.x + dx, currentState.protractorUnit.center.y + dy)
                currentState.copy(protractorUnit = currentState.protractorUnit.copy(center = newCenter), valuesChangedSinceReset = true)
            }
            InteractionMode.ROTATING_PROTRACTOR -> {
                // HERESY CORRECTED: Reverted to relative angle calculation.
                // This provides a smooth rotation that "picks up where it left off."
                val center = currentState.protractorUnit.center
                val prevAngle = atan2(event.previousLogicalPoint.y - center.y, event.previousLogicalPoint.x - center.x)
                val currAngle = atan2(event.currentLogicalPoint.y - center.y, event.currentLogicalPoint.x - center.x)
                val angleDelta = Math.toDegrees(currAngle.toDouble() - prevAngle.toDouble()).toFloat()

                val newRotation = currentState.protractorUnit.rotationDegrees + angleDelta
                currentState.copy(
                    protractorUnit = currentState.protractorUnit.copy(rotationDegrees = newRotation),
                    valuesChangedSinceReset = true
                )
            }
            InteractionMode.MOVING_ACTUAL_CUE_BALL -> {
                currentState.onPlaneBall?.let {
                    val dx = event.currentLogicalPoint.x - event.previousLogicalPoint.x
                    val dy = event.currentLogicalPoint.y - event.previousLogicalPoint.y
                    val newCenter = PointF(it.center.x + dx, it.center.y + dy)
                    currentState.copy(onPlaneBall = it.copy(center = newCenter), valuesChangedSinceReset = true)
                } ?: currentState
            }
            InteractionMode.MOVING_SPIN_CONTROL -> {
                currentState.spinControlCenter?.let {
                    val newCenter = PointF(it.x + event.screenDelta.x, it.y + event.screenDelta.y)
                    currentState.copy(spinControlCenter = newCenter)
                } ?: currentState
            }
            InteractionMode.MOVING_OBSTACLE_BALL -> {
                val index = currentState.movingObstacleBallIndex
                if (index != null && index < currentState.obstacleBalls.size) {
                    val obstacle = currentState.obstacleBalls[index]
                    val dx = event.currentLogicalPoint.x - event.previousLogicalPoint.x
                    val dy = event.currentLogicalPoint.y - event.previousLogicalPoint.y
                    val newCenter = PointF(obstacle.center.x + dx, obstacle.center.y + dy)
                    val newObstacles = currentState.obstacleBalls.toMutableList()
                    newObstacles[index] = obstacle.copy(center = newCenter)
                    currentState.copy(obstacleBalls = newObstacles, valuesChangedSinceReset = true)
                } else {
                    currentState
                }
            }
            InteractionMode.AIMING_BANK_SHOT -> {
                currentState.copy(
                    bankingAimTarget = event.currentLogicalPoint,
                    valuesChangedSinceReset = true
                )
            }
            else -> currentState
        }

        return if (currentState.isMagnifierVisible) {
            newState.copy(magnifierSourceCenter = event.screenDelta + (currentState.magnifierSourceCenter ?: event.screenDelta))
        } else {
            newState
        }
    }

    private fun handleGestureEnded(currentState: OverlayState): OverlayState {
        var finalState = currentState.copy(interactionMode = InteractionMode.NONE, movingObstacleBallIndex = null, isMagnifierVisible = false)

        if (finalState.isSnappingEnabled) {
            val closestCandidate = findClosestSnapCandidate(finalState)
            if (closestCandidate != null) {
                finalState = when (currentState.interactionMode) {
                    InteractionMode.MOVING_PROTRACTOR_UNIT -> {
                        finalState.copy(protractorUnit = finalState.protractorUnit.copy(center = closestCandidate.detectedPoint))
                    }
                    InteractionMode.MOVING_ACTUAL_CUE_BALL -> {
                        finalState.onPlaneBall?.let {
                            finalState.copy(onPlaneBall = it.copy(center = closestCandidate.detectedPoint))
                        } ?: finalState
                    }
                    else -> finalState
                }
                // A snap occurred, so automatically lock the world.
                finalState = finalState.copy(isWorldLocked = true)
            }
        }

        return reducerUtils.snapViolatingBalls(finalState)
    }

    private fun findClosestSnapCandidate(state: OverlayState): SnapCandidate? {
        val referencePoint = when (state.interactionMode) {
            InteractionMode.MOVING_PROTRACTOR_UNIT -> state.protractorUnit.center
            InteractionMode.MOVING_ACTUAL_CUE_BALL -> state.onPlaneBall?.center
            else -> null
        } ?: return null

        val snapRadius = (state.onPlaneBall?.radius ?: state.protractorUnit.radius) * 1.5f

        return (state.snapCandidates ?: emptyList())
            .filter { getDistance(referencePoint, it.detectedPoint) < snapRadius }
            .minByOrNull { getDistance(referencePoint, it.detectedPoint) }
    }


    private fun getDistance(p1: PointF, p2: PointF): Float {
        return sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))
    }

    private fun getDistance(p1: Offset, p2: PointF): Float {
        return sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))
    }
}