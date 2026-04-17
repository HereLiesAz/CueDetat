package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.ui.ZoomMapping
import android.graphics.PointF
import androidx.compose.ui.geometry.Offset
import com.hereliesaz.cuedetat.domain.BallSelectionPhase
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.domain.ReducerUtils
import com.hereliesaz.cuedetat.view.model.OnPlaneBall
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

        val zoomLimits = ZoomMapping.getZoomRange(currentState.experienceMode, currentState.isBeginnerViewLocked)
        val currentZoom = ZoomMapping.sliderToZoom(currentState.zoomSliderPosition, zoomLimits.first, zoomLimits.second)
        val touchRadius = (25f * 4.0f) / currentZoom

        // 0. Ball Selection Phase: tap near a confirmed snap candidate to attach a virtual ball
        if (currentState.tableScanModel != null &&
            currentState.ballSelectionPhase != BallSelectionPhase.NONE) {
            val confirmed = currentState.snapCandidates?.filter { it.isConfirmed } ?: emptyList()
            val snapTapRadius = touchRadius * 2f
            val closest = confirmed.minByOrNull { getDistance(event.logicalPoint, it.detectedPoint) }
            if (closest != null && getDistance(event.logicalPoint, closest.detectedPoint) < snapTapRadius) {
                return when (currentState.ballSelectionPhase) {
                    BallSelectionPhase.AWAITING_CUE -> currentState.copy(
                        onPlaneBall = currentState.onPlaneBall?.copy(center = closest.detectedPoint)
                            ?: OnPlaneBall(center = closest.detectedPoint, radius = LOGICAL_BALL_RADIUS),
                        cueBallCvAnchor = closest.detectedPoint,
                        ballSelectionPhase = BallSelectionPhase.AWAITING_TARGET
                    )
                    BallSelectionPhase.AWAITING_TARGET -> currentState.copy(
                        protractorUnit = currentState.protractorUnit.copy(center = closest.detectedPoint),
                        targetCvAnchor = closest.detectedPoint,
                        ballSelectionPhase = BallSelectionPhase.NONE
                    )
                    BallSelectionPhase.NONE -> currentState
                }
            }
            // Tap missed all candidates — fall through to normal interaction
        }
        val onPlaneBall = currentState.onPlaneBall
        val protractorUnit = currentState.protractorUnit

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

        // 2b. Target Ball Movement (Protractor Unit center and Ghost Cue Ball contact point)
        if (!currentState.isBankingMode) {
            val targetPos = currentState.protractorUnit.center
            val ghostCuePos = currentState.protractorUnit.ghostCueBallCenter
            val isHitOnTarget = getDistance(event.logicalPoint, targetPos) < touchRadius
            val isHitOnGhostCue = getDistance(event.logicalPoint, ghostCuePos) < touchRadius

            if (isHitOnTarget || isHitOnGhostCue) {
                return currentState.copy(
                    interactionMode = InteractionMode.MOVING_PROTRACTOR_UNIT,
                    isMagnifierVisible = true,
                    magnifierSourceCenter = event.screenOffset
                )
            }
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
            InteractionMode.MOVING_PROTRACTOR_UNIT -> {
                val dx = event.currentLogicalPoint.x - event.previousLogicalPoint.x
                val dy = event.currentLogicalPoint.y - event.previousLogicalPoint.y
                currentState.copy(
                    protractorUnit = currentState.protractorUnit.copy(
                        center = PointF(
                            currentState.protractorUnit.center.x + dx,
                            currentState.protractorUnit.center.y + dy
                        )
                    ),
                    valuesChangedSinceReset = true
                )
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
                if (currentState.isMasseModeActive) {
                    val cuePos = currentState.onPlaneBall?.center ?: return currentState
                    val prevAngle = atan2(
                        event.previousLogicalPoint.y - cuePos.y,
                        event.previousLogicalPoint.x - cuePos.x
                    )
                    val currAngle = atan2(
                        event.currentLogicalPoint.y - cuePos.y,
                        event.currentLogicalPoint.x - cuePos.x
                    )
                    val delta = Math.toDegrees(currAngle.toDouble() - prevAngle.toDouble()).toFloat()
                    currentState.copy(masseShotAngleDeg = currentState.masseShotAngleDeg + delta)
                } else {
                    val center = currentState.protractorUnit.center
                    val prevAngle = atan2(event.previousLogicalPoint.y - center.y, event.previousLogicalPoint.x - center.x)
                    val currAngle = atan2(event.currentLogicalPoint.y - center.y, event.currentLogicalPoint.x - center.x)
                    val angleDelta = Math.toDegrees(currAngle.toDouble() - prevAngle.toDouble()).toFloat()
                    currentState.copy(
                        protractorUnit = currentState.protractorUnit.copy(rotationDegrees = currentState.protractorUnit.rotationDegrees + angleDelta),
                        valuesChangedSinceReset = true
                    )
                }
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