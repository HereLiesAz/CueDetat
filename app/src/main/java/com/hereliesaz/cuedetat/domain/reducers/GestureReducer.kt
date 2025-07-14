// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/GestureReducer.kt

package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.ReducerUtils
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.model.OnPlaneBall
import com.hereliesaz.cuedetat.view.model.ProtractorUnit
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
        val logicalPoint = event.logicalPoint
        val onPlaneBall = currentState.onPlaneBall
        val spinControlCenter = currentState.spinControlCenter
        val protractorUnit = currentState.protractorUnit

        // Check for interaction with the spin control first
        if (currentState.isSpinControlVisible && spinControlCenter != null) {
            val distToSpinControl = getDistance(logicalPoint, spinControlCenter)
            if (distToSpinControl < (onPlaneBall?.radius ?: protractorUnit.radius) * 1.5f) {
                return currentState.copy(interactionMode = InteractionMode.MOVING_SPIN_CONTROL)
            }
        }

        // Check for interaction with an obstacle ball
        currentState.obstacleBalls.forEachIndexed { index, obstacle ->
            if (getDistance(logicalPoint, obstacle.center) < obstacle.radius) {
                return currentState.copy(interactionMode = InteractionMode.MOVING_OBSTACLE_BALL, movingObstacleBallIndex = index)
            }
        }

        // Check for interaction with the OnPlaneBall (ActualCueBall)
        if (onPlaneBall != null && getDistance(logicalPoint, onPlaneBall.center) < onPlaneBall.radius) {
            return currentState.copy(interactionMode = InteractionMode.MOVING_ACTUAL_CUE_BALL, hasCueBallBeenMoved = true)
        }

        val distToGhostBall = getDistance(logicalPoint, protractorUnit.ghostCueBallCenter)
        val distToTargetBall = getDistance(logicalPoint, protractorUnit.center)

        return when {
            distToTargetBall < protractorUnit.radius -> currentState.copy(interactionMode = InteractionMode.MOVING_PROTRACTOR_UNIT, hasTargetBallBeenMoved = true)
            distToGhostBall < protractorUnit.radius -> currentState.copy(interactionMode = InteractionMode.ROTATING_PROTRACTOR)
            else -> {
                if(currentState.isBankingMode) {
                    currentState.copy(interactionMode = InteractionMode.AIMING_BANK_SHOT, bankingAimTarget = logicalPoint)
                } else {
                    currentState.copy(interactionMode = InteractionMode.SCALING)
                }
            }
        }.copy(isMagnifierVisible = true, magnifierSourceCenter = event.screenOffset)
    }

    private fun handleLogicalDragApplied(currentState: OverlayState, event: MainScreenEvent.LogicalDragApplied): OverlayState {
        val logicalDelta = event.logicalDelta

        val newState = when (currentState.interactionMode) {
            InteractionMode.MOVING_PROTRACTOR_UNIT -> {
                val newCenter = PointF(currentState.protractorUnit.center.x + logicalDelta.x, currentState.protractorUnit.center.y + logicalDelta.y)
                currentState.copy(protractorUnit = currentState.protractorUnit.copy(center = newCenter))
            }
            InteractionMode.ROTATING_PROTRACTOR -> {
                val center = currentState.protractorUnit.center
                val currentDragPoint = event.logicalPoint
                val newAngle = atan2((currentDragPoint.y - center.y).toDouble(), (currentDragPoint.x - center.x).toDouble())
                currentState.copy(protractorUnit = currentState.protractorUnit.copy(rotationDegrees = Math.toDegrees(newAngle).toFloat()))
            }
            InteractionMode.MOVING_ACTUAL_CUE_BALL -> {
                val onPlaneBall = currentState.onPlaneBall
                if (onPlaneBall != null) {
                    val newCenter = PointF(onPlaneBall.center.x + logicalDelta.x, onPlaneBall.center.y + logicalDelta.y)
                    currentState.copy(onPlaneBall = onPlaneBall.copy(center = newCenter))
                } else {
                    currentState
                }
            }
            InteractionMode.MOVING_SPIN_CONTROL -> {
                val spinControlCenter = currentState.spinControlCenter
                if (spinControlCenter != null) {
                    val newCenter = PointF(spinControlCenter.x + logicalDelta.x, spinControlCenter.y + logicalDelta.y)
                    currentState.copy(spinControlCenter = newCenter)
                } else {
                    currentState
                }
            }
            InteractionMode.MOVING_OBSTACLE_BALL -> {
                val index = currentState.movingObstacleBallIndex
                if (index != null && index < currentState.obstacleBalls.size) {
                    val obstacle = currentState.obstacleBalls[index]
                    val newCenter = PointF(obstacle.center.x + logicalDelta.x, obstacle.center.y + logicalDelta.y)
                    val newObstacles = currentState.obstacleBalls.toMutableList()
                    newObstacles[index] = obstacle.copy(center = newCenter)
                    currentState.copy(obstacleBalls = newObstacles)
                } else {
                    currentState
                }
            }
            else -> currentState
        }
        return newState.copy(magnifierSourceCenter = event.screenDelta + (currentState.magnifierSourceCenter ?: event.screenDelta))
    }

    private fun handleGestureEnded(currentState: OverlayState): OverlayState {
        val interactionMode = currentState.interactionMode
        var finalState = currentState.copy(interactionMode = InteractionMode.NONE, movingObstacleBallIndex = null, isMagnifierVisible = false)

        if (finalState.isSnappingEnabled) {
            val closestCandidate = findClosestSnapCandidate(finalState, interactionMode)
            if (closestCandidate != null) {
                finalState = when (interactionMode) {
                    InteractionMode.MOVING_PROTRACTOR_UNIT -> {
                        finalState.copy(protractorUnit = finalState.protractorUnit.copy(center = closestCandidate.detectedPoint))
                    }
                    InteractionMode.MOVING_ACTUAL_CUE_BALL -> {
                        val onPlaneBall = finalState.onPlaneBall
                        if (onPlaneBall != null) {
                            finalState.copy(onPlaneBall = onPlaneBall.copy(center = closestCandidate.detectedPoint))
                        } else {
                            finalState
                        }
                    }
                    else -> finalState
                }
            }
        }

        return confineAllBallsToTable(finalState)
    }

    private fun findClosestSnapCandidate(state: OverlayState, interactionMode: InteractionMode): SnapCandidate? {
        val referencePoint = when (interactionMode) {
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

    private fun confineAllBallsToTable(state: OverlayState): OverlayState {
        if (!state.showTable) {
            return state
        }
        val bounds = reducerUtils.getTableBoundaries(state)

        val confinedCueBall = state.onPlaneBall?.let {
            it.copy(center = confinePoint(it.center, bounds))
        }

        val confinedObstacles = state.obstacleBalls.map {
            it.copy(center = confinePoint(it.center, bounds))
        }

        val confinedTargetBall = state.protractorUnit.copy(
            center = confinePoint(state.protractorUnit.center, bounds)
        )

        return state.copy(
            onPlaneBall = confinedCueBall,
            obstacleBalls = confinedObstacles,
            protractorUnit = confinedTargetBall
        )
    }

    private fun confinePoint(point: PointF, bounds: android.graphics.Rect): PointF {
        return PointF(
            point.x.coerceIn(bounds.left.toFloat(), bounds.right.toFloat()),
            point.y.coerceIn(bounds.top.toFloat(), bounds.bottom.toFloat())
        )
    }
}