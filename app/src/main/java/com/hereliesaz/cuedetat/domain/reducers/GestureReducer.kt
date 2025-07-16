// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/GestureReducer.kt

package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import androidx.compose.ui.geometry.Offset
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.domain.ReducerUtils
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.InteractionMode
import com.hereliesaz.cuedetat.view.state.OverlayState
import com.hereliesaz.cuedetat.view.state.SnapCandidate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.hypot
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
        val logicalPoint = event.logicalPoint
        val onPlaneBall = currentState.onPlaneBall
        val spinControlCenter = currentState.spinControlCenter
        val protractorUnit = currentState.protractorUnit

        val constantTouchRadius = LOGICAL_BALL_RADIUS * 3.5f // Increased trigger area

        val spinControlTouchRadius = (onPlaneBall?.radius ?: protractorUnit.radius) * 1.5f
        if (currentState.isSpinControlVisible && spinControlCenter != null && getDistance(event.screenOffset, spinControlCenter) < spinControlTouchRadius) {
            return currentState.copy(interactionMode = InteractionMode.MOVING_SPIN_CONTROL, isMagnifierVisible = true, magnifierSourceCenter = event.screenOffset)
        }

        currentState.obstacleBalls.forEachIndexed { index, obstacle ->
            if (getDistance(logicalPoint, obstacle.center) < constantTouchRadius) {
                return currentState.copy(interactionMode = InteractionMode.MOVING_OBSTACLE_BALL, movingObstacleBallIndex = index, isMagnifierVisible = true, magnifierSourceCenter = event.screenOffset)
            }
        }

        if (onPlaneBall != null && getDistance(logicalPoint, onPlaneBall.center) < constantTouchRadius) {
            return currentState.copy(interactionMode = InteractionMode.MOVING_ACTUAL_CUE_BALL, hasCueBallBeenMoved = true, isMagnifierVisible = true, magnifierSourceCenter = event.screenOffset)
        }

        val distToTargetBall = getDistance(logicalPoint, protractorUnit.center)
        val distToGhostBall = getDistance(logicalPoint, protractorUnit.ghostCueBallCenter)

        if (distToTargetBall < constantTouchRadius || distToGhostBall < constantTouchRadius) {
            return currentState.copy(interactionMode = InteractionMode.MOVING_PROTRACTOR_UNIT, hasTargetBallBeenMoved = true, isMagnifierVisible = true, magnifierSourceCenter = event.screenOffset)
        }

        return currentState.copy(interactionMode = InteractionMode.ROTATING_PROTRACTOR, isMagnifierVisible = false)
    }

    private fun handleLogicalDragApplied(currentState: OverlayState, event: MainScreenEvent.LogicalDragApplied): OverlayState {
        val logicalDelta = event.logicalDelta

        val newState = when (currentState.interactionMode) {
            InteractionMode.MOVING_PROTRACTOR_UNIT -> {
                val newCenter = PointF(currentState.protractorUnit.center.x + logicalDelta.x, currentState.protractorUnit.center.y + logicalDelta.y)
                currentState.copy(protractorUnit = currentState.protractorUnit.copy(center = newCenter), valuesChangedSinceReset = true)
            }
            InteractionMode.ROTATING_PROTRACTOR -> {
                val combinedDelta = event.logicalDelta.x + event.logicalDelta.y
                val rotationAmount = combinedDelta * 0.5f
                val newRotation = currentState.protractorUnit.rotationDegrees - rotationAmount
                currentState.copy(
                    protractorUnit = currentState.protractorUnit.copy(rotationDegrees = newRotation),
                    valuesChangedSinceReset = true
                )
            }
            InteractionMode.MOVING_ACTUAL_CUE_BALL -> {
                currentState.onPlaneBall?.let {
                    val newCenter = PointF(it.center.x + logicalDelta.x, it.center.y + logicalDelta.y)
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
                    val newCenter = PointF(obstacle.center.x + logicalDelta.x, obstacle.center.y + logicalDelta.y)
                    val newObstacles = currentState.obstacleBalls.toMutableList()
                    newObstacles[index] = obstacle.copy(center = newCenter)
                    currentState.copy(obstacleBalls = newObstacles, valuesChangedSinceReset = true)
                } else {
                    currentState
                }
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

        return state.snapCandidates
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