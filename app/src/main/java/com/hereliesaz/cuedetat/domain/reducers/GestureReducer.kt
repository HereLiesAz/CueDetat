package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import android.util.Log
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.InteractionMode
import com.hereliesaz.cuedetat.view.state.OverlayState
import com.hereliesaz.cuedetat.view.state.TableSize
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

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
            is MainScreenEvent.AimBankShot -> {
                currentState.copy(bankingAimTarget = event.logicalTarget, valuesChangedSinceReset = true)
            }
            else -> currentState
        }
    }

    private fun handleGestureStarted(currentState: OverlayState, event: MainScreenEvent.LogicalGestureStarted): OverlayState {
        val touchPoint = event.logicalPoint
        var newMode = InteractionMode.NONE

        if (currentState.isBankingMode) {
            currentState.onPlaneBall?.let {
                val touchSlop = it.radius * 1.5f // Increased slop for easier grabbing
                if (distance(touchPoint, it.center) < it.radius + touchSlop) {
                    newMode = InteractionMode.MOVING_ACTUAL_CUE_BALL
                }
            }
            if (newMode == InteractionMode.NONE) {
                newMode = InteractionMode.AIMING_BANK_SHOT
            }
        } else {
            currentState.onPlaneBall?.let {
                val touchSlop = it.radius * 1.5f // Increased slop
                if (distance(touchPoint, it.center) < it.radius + touchSlop) {
                    newMode = InteractionMode.MOVING_ACTUAL_CUE_BALL
                }
            }
            if (newMode == InteractionMode.NONE) {
                val touchSlop = currentState.protractorUnit.radius * 1.5f // Increased slop
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
                val angleRad = Math.toRadians(-currentState.tableRotationDegrees.toDouble())
                val cosAngle = cos(angleRad).toFloat()
                val sinAngle = sin(angleRad).toFloat()

                val screenDx = screenDelta.x
                val screenDy = screenDelta.y

                // Un-rotate the screen delta to align with the logical plane
                val unrotatedDx = screenDx * cosAngle - screenDy * sinAngle
                val unrotatedDy = screenDx * sinAngle + screenDy * cosAngle

                val rotationDelta = (unrotatedDx - unrotatedDy) * 0.2f
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
                    var newCenterX = it.center.x + logicalDelta.x
                    var newCenterY = it.center.y + logicalDelta.y

                    if (currentState.showTable || currentState.isBankingMode) {
                        val (left, top, right, bottom) = getTableBoundaries(currentState)
                        newCenterX = newCenterX.coerceIn(left, right)
                        newCenterY = newCenterY.coerceIn(top, bottom)
                    }

                    val newCenter = PointF(newCenterX, newCenterY)
                    Log.d(GESTURE_TAG, "REDUCER: MOVING_ACTUAL_CUE_BALL to $newCenter")
                    currentState.copy(onPlaneBall = it.copy(center = newCenter), valuesChangedSinceReset = true)
                } ?: currentState
            }
            // AIMING_BANK_SHOT is now handled by AimBankShot event, so this case is removed from drag.
            else -> currentState
        }
    }

    private fun getTableBoundaries(state: OverlayState): FloatArray {
        val referenceRadius = state.onPlaneBall?.radius ?: state.protractorUnit.radius
        val tableToBallRatioLong = state.tableSize.getTableToBallRatioLong()
        val tableToBallRatioShort = tableToBallRatioLong / state.tableSize.aspectRatio
        val tablePlayingSurfaceWidth = tableToBallRatioLong * referenceRadius
        val tablePlayingSurfaceHeight = tableToBallRatioShort * referenceRadius

        val canvasCenterX = state.viewWidth / 2f
        val canvasCenterY = state.viewHeight / 2f
        val left = canvasCenterX - tablePlayingSurfaceWidth / 2
        val top = canvasCenterY - tablePlayingSurfaceHeight / 2
        val right = canvasCenterX + tablePlayingSurfaceWidth / 2
        val bottom = canvasCenterY + tablePlayingSurfaceHeight / 2

        return floatArrayOf(left, top, right, bottom)
    }

    private fun distance(p1: PointF, p2: PointF): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}