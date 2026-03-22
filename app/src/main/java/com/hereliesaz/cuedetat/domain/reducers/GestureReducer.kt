package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import androidx.compose.ui.geometry.Offset
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.domain.ReducerUtils
import com.hereliesaz.cuedetat.view.state.InteractionMode
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

@Singleton
class GestureReducer @Inject constructor(private val reducerUtils: ReducerUtils) {
    fun reduce(currentState: CueDetatState, event: MainScreenEvent): CueDetatState {
        return when(event) {
            is MainScreenEvent.LogicalGestureStarted -> handleLogicalGestureStarted(currentState, event)
            is MainScreenEvent.LogicalDragApplied -> handleLogicalDragApplied(currentState, event)
            is MainScreenEvent.GestureEnded -> handleGestureEnded(currentState)
            else -> currentState
        }
    }

    private fun handleLogicalGestureStarted(
        currentState: CueDetatState,
        event: MainScreenEvent.LogicalGestureStarted
    ): CueDetatState {
        if (currentState.experienceMode == ExperienceMode.BEGINNER && currentState.isBeginnerViewLocked) {
            return currentState
        }

        val spinControlCenter = currentState.spinControlCenter
        val onPlaneBall = currentState.onPlaneBall
        val protractorUnit = currentState.protractorUnit
        val touchRadius = 25f * 4.0f

        // 1. Relocate UI Widget (Double Tap + Drag on Center)
        if (currentState.isSpinControlVisible || currentState.isMasseModeActive) {
            val distToControl = spinControlCenter?.let { getDistance(event.screenOffset, it) } ?: Float.MAX_VALUE
            if (event.isDoubleTap && distToControl < (60f * currentState.screenDensity)) {
                return currentState.copy(
                    interactionMode = InteractionMode.MOVING_SPIN_CONTROL,
                    isMagnifierVisible = true,
                    magnifierSourceCenter = event.screenOffset
                )
            }
        }

        // 2. Ball Movement (Priority check for Cue Ball and Ghost Ball area)
        val ghostPos = currentState.shotGuideImpactPoint
        val isHitOnBall = onPlaneBall != null && getDistance(event.logicalPoint, onPlaneBall.center) < touchRadius
        val isHitOnGhost = ghostPos != null && getDistance(event.logicalPoint, ghostPos) < touchRadius

        if (isHitOnBall || isHitOnGhost) {
            return currentState.copy(
                interactionMode = InteractionMode.MOVING_ACTUAL_CUE_BALL,
                isMagnifierVisible = true,
                magnifierSourceCenter = event.screenOffset
            )
        }

        // 3. Bank Mode or Default Shot Line Rotation
        return if (currentState.isBankingMode) {
            currentState.copy(interactionMode = InteractionMode.AIMING_BANK_SHOT)
        } else {
            currentState.copy(interactionMode = InteractionMode.ROTATING_PROTRACTOR)
        }
    }

    private fun handleLogicalDragApplied(currentState: CueDetatState, event: MainScreenEvent.LogicalDragApplied): CueDetatState {
        return when (currentState.interactionMode) {
            InteractionMode.MOVING_SPIN_CONTROL -> {
                val currentCenter = currentState.spinControlCenter ?: return currentState
                currentState.copy(spinControlCenter = PointF(currentCenter.x + event.screenDelta.x, currentCenter.y + event.screenDelta.y))
            }
            InteractionMode.MOVING_ACTUAL_CUE_BALL -> {
                currentState.onPlaneBall?.let {
                    val dx = event.currentLogicalPoint.x - event.previousLogicalPoint.x
                    val dy = event.currentLogicalPoint.y - event.previousLogicalPoint.y
                    currentState.copy(onPlaneBall = it.copy(center = PointF(it.center.x + dx, it.center.y + dy)), valuesChangedSinceReset = true)
                } ?: currentState
            }
            InteractionMode.AIMING_BANK_SHOT -> {
                currentState.copy(bankingAimTarget = event.currentLogicalPoint, valuesChangedSinceReset = true)
            }
            InteractionMode.ROTATING_PROTRACTOR -> {
                val center = currentState.protractorUnit.center
                val prevAngle = atan2(event.previousLogicalPoint.y - center.y, event.previousLogicalPoint.x - center.x)
                val currAngle = atan2(event.currentLogicalPoint.y - center.y, event.currentLogicalPoint.x - center.x)
                val angleDelta = Math.toDegrees(currAngle.toDouble() - prevAngle.toDouble()).toFloat()
                currentState.copy(
                    protractorUnit = currentState.protractorUnit.copy(rotationDegrees = currentState.protractorUnit.rotationDegrees + angleDelta),
                    valuesChangedSinceReset = true
                )
            }
            else -> currentState
        }
    }

    private fun handleGestureEnded(currentState: CueDetatState): CueDetatState {
        return currentState.copy(interactionMode = InteractionMode.NONE, isMagnifierVisible = false)
    }

    private fun getDistance(p1: Offset, p2: PointF) = sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))
    private fun getDistance(p1: PointF, p2: PointF) = sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))
}