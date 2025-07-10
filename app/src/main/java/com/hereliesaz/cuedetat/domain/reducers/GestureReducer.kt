package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import android.util.Log
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.InteractionMode
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

private const val GESTURE_TAG = "GestureDebug"

@Singleton
class GestureReducer @Inject constructor() {

    fun reduce(currentState: OverlayState, event: MainScreenEvent): OverlayState {
        return when (event) {
            is MainScreenEvent.LogicalGestureStarted -> handleGestureStarted(currentState, event)
            is MainScreenEvent.LogicalDragApplied -> handleDrag(currentState, event)
            is MainScreenEvent.GestureEnded -> {
                Log.d(GESTURE_TAG, "REDUCER: GestureEnded. Resetting interaction mode.")
                currentState.copy(interactionMode = InteractionMode.NONE)
            }
            else -> currentState
        }
    }

    private fun handleGestureStarted(currentState: OverlayState, event: MainScreenEvent.LogicalGestureStarted): OverlayState {
        val touchPoint = event.logicalPoint
        var newMode = InteractionMode.NONE

        if (currentState.isBankingMode) {
            currentState.onPlaneBall?.let {
                val touchSlop = max(30f, it.radius * 0.5f)
                if (distance(touchPoint, it.center) < it.radius + touchSlop) {
                    newMode = InteractionMode.MOVING_ACTUAL_CUE_BALL
                }
            }
            if (newMode == InteractionMode.NONE) {
                newMode = InteractionMode.AIMING_BANK_SHOT
            }
        } else {
            currentState.onPlaneBall?.let {
                val touchSlop = max(30f, it.radius * 0.5f)
                if (distance(touchPoint, it.center) < it.radius + touchSlop) {
                    newMode = InteractionMode.MOVING_ACTUAL_CUE_BALL
                }
            }
            if (newMode == InteractionMode.NONE) {
                val touchSlop = max(30f, currentState.protractorUnit.radius * 0.5f)
                if (distance(touchPoint, currentState.protractorUnit.center) < currentState.protractorUnit.radius + touchSlop) {
                    newMode = InteractionMode.MOVING_PROTRACTOR_UNIT
                }
            }
            if (newMode == InteractionMode.NONE) {
                newMode = InteractionMode.ROTATING_PROTRACTOR
            }
        }
        Log.d(GESTURE_TAG, "REDUCER: GestureStarted. Determined Mode: $newMode")
        return currentState.copy(interactionMode = newMode, warningText = null)
    }

    private fun handleDrag(currentState: OverlayState, event: MainScreenEvent.LogicalDragApplied): OverlayState {
        val logicalDelta = event.logicalDelta
        val screenDelta = event.screenDelta
        Log.d(GESTURE_TAG, "REDUCER: handleDrag with mode=${currentState.interactionMode}")

        return when (currentState.interactionMode) {
            InteractionMode.ROTATING_PROTRACTOR -> {
                val rotationDelta = (screenDelta.x - screenDelta.y) * 0.2f
                Log.d(GESTURE_TAG, "REDUCER: ROTATING. screenDelta.x=${screenDelta.x}, screenDelta.y=${screenDelta.y}, rotationDelta=$rotationDelta")
                val newRotation = currentState.protractorUnit.rotationDegrees - rotationDelta
                currentState.copy(protractorUnit = currentState.protractorUnit.copy(rotationDegrees = newRotation), valuesChangedSinceReset = true)
            }
            InteractionMode.MOVING_PROTRACTOR_UNIT -> {
                val newCenter = PointF(currentState.protractorUnit.center.x + logicalDelta.x, currentState.protractorUnit.center.y + logicalDelta.y)
                Log.d(GESTURE_TAG, "REDUCER: MOVING_PROTRACTOR_UNIT to $newCenter")
                currentState.copy(protractorUnit = currentState.protractorUnit.copy(center = newCenter), valuesChangedSinceReset = true)
            }
            InteractionMode.MOVING_ACTUAL_CUE_BALL -> {
                currentState.onPlaneBall?.let {
                    val newCenter = PointF(it.center.x + logicalDelta.x, it.center.y + logicalDelta.y)
                    Log.d(GESTURE_TAG, "REDUCER: MOVING_ACTUAL_CUE_BALL to $newCenter")
                    currentState.copy(onPlaneBall = it.copy(center = newCenter), valuesChangedSinceReset = true)
                } ?: currentState
            }
            InteractionMode.AIMING_BANK_SHOT -> {
                currentState.bankingAimTarget?.let {
                    val newTarget = PointF(it.x + logicalDelta.x, it.y + logicalDelta.y)
                    Log.d(GESTURE_TAG, "REDUCER: AIMING_BANK_SHOT to $newTarget")
                    currentState.copy(bankingAimTarget = newTarget, valuesChangedSinceReset = true)
                } ?: currentState
            }
            else -> currentState
        }
    }

    private fun distance(p1: PointF, p2: PointF): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}