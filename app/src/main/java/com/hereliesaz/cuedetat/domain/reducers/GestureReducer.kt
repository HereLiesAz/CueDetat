// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/GestureReducer.kt

package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import android.util.Log
import androidx.compose.ui.geometry.Offset
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.model.Perspective
import com.hereliesaz.cuedetat.view.state.InteractionMode
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.hypot

private const val GESTURE_TAG = "GestureDebug"

@Singleton
class GestureReducer @Inject constructor() {

    fun reduce(currentState: OverlayState, event: MainScreenEvent): OverlayState {
        return when (event) {
            is MainScreenEvent.LogicalGestureStarted -> handleGestureStarted(currentState, event)
            is MainScreenEvent.LogicalDragApplied -> handleDrag(currentState, event)
            is MainScreenEvent.GestureEnded -> {
                Log.d(GESTURE_TAG, "REDUCER: GestureEnded. Resetting interaction mode.")
                currentState.copy(
                    interactionMode = InteractionMode.NONE,
                    isMagnifierVisible = false,
                    magnifierSourceCenter = null
                )
            }
            is MainScreenEvent.AimBankShot -> {
                currentState.copy(bankingAimTarget = event.logicalTarget, valuesChangedSinceReset = true)
            }
            else -> currentState
        }
    }

    private fun handleGestureStarted(currentState: OverlayState, event: MainScreenEvent.LogicalGestureStarted): OverlayState {
        val logicalPoint = event.logicalPoint
        val screenPoint = PointF(event.screenOffset.x, event.screenOffset.y)
        var newMode = InteractionMode.NONE

        // THE FIX: Check for spin control interaction first, as it exists in screen space.
        currentState.spinControlCenter?.let {
            val spinControlScreenRadius = 100f // Effective radius in screen pixels (50dp * density, roughly)
            if (distance(screenPoint, it) < spinControlScreenRadius) {
                newMode = InteractionMode.MOVING_SPIN_CONTROL
            }
        }

        if (newMode == InteractionMode.NONE) {
            if (currentState.isBankingMode) {
                currentState.onPlaneBall?.let {
                    val touchSlop = it.radius * 1.5f
                    if (distance(logicalPoint, it.center) < it.radius + touchSlop) {
                        newMode = InteractionMode.MOVING_ACTUAL_CUE_BALL
                    }
                }
                if (newMode == InteractionMode.NONE) {
                    newMode = InteractionMode.AIMING_BANK_SHOT
                }
            } else {
                currentState.onPlaneBall?.let {
                    val touchSlop = it.radius * 1.5f
                    if (distance(logicalPoint, it.center) < it.radius + touchSlop) {
                        newMode = InteractionMode.MOVING_ACTUAL_CUE_BALL
                    }
                }
                if (newMode == InteractionMode.NONE) {
                    val touchSlop = currentState.protractorUnit.radius * 1.5f
                    if (distance(logicalPoint, currentState.protractorUnit.center) < currentState.protractorUnit.radius + touchSlop) {
                        newMode = InteractionMode.MOVING_PROTRACTOR_UNIT
                    }
                }
                if (newMode == InteractionMode.NONE) {
                    newMode = InteractionMode.ROTATING_PROTRACTOR
                }
            }
        }

        Log.d(GESTURE_TAG, "REDUCER: GestureStarted. Determined Mode: $newMode")

        val showMagnifier = newMode == InteractionMode.MOVING_ACTUAL_CUE_BALL || newMode == InteractionMode.MOVING_PROTRACTOR_UNIT
        return currentState.copy(
            interactionMode = newMode,
            warningText = null,
            isMagnifierVisible = showMagnifier,
            magnifierSourceCenter = if (showMagnifier) event.screenOffset else null
        )
    }

    private fun handleDrag(currentState: OverlayState, event: MainScreenEvent.LogicalDragApplied): OverlayState {
        val logicalDelta = event.logicalDelta
        val screenDelta = event.screenDelta
        Log.d(GESTURE_TAG, "REDUCER: handleDrag with mode=${currentState.interactionMode}")

        val newMagnifierCenter = currentState.magnifierSourceCenter?.plus(screenDelta)
        val stateWithUpdatedMagnifier = currentState.copy(magnifierSourceCenter = newMagnifierCenter)

        return when (stateWithUpdatedMagnifier.interactionMode) {
            InteractionMode.ROTATING_PROTRACTOR -> {
                val angleRad = Math.toRadians(-stateWithUpdatedMagnifier.tableRotationDegrees.toDouble())
                val cosAngle = cos(angleRad).toFloat()
                val sinAngle = sin(angleRad).toFloat()

                val screenDx = screenDelta.x
                val screenDy = screenDelta.y

                val unrotatedDx = screenDx * cosAngle - screenDy * sinAngle
                val unrotatedDy = screenDx * sinAngle + screenDy * cosAngle

                val rotationDelta = (unrotatedDx - unrotatedDy) * 0.2f
                val newRotation = stateWithUpdatedMagnifier.protractorUnit.rotationDegrees - rotationDelta

                stateWithUpdatedMagnifier.copy(protractorUnit = stateWithUpdatedMagnifier.protractorUnit.copy(rotationDegrees = newRotation), valuesChangedSinceReset = true)
            }
            InteractionMode.MOVING_PROTRACTOR_UNIT -> {
                var newCenterX = stateWithUpdatedMagnifier.protractorUnit.center.x + logicalDelta.x
                var newCenterY = stateWithUpdatedMagnifier.protractorUnit.center.y + logicalDelta.y

                if (stateWithUpdatedMagnifier.showTable || stateWithUpdatedMagnifier.isBankingMode) {
                    val (left, top, right, bottom) = getTableBoundaries(stateWithUpdatedMagnifier)
                    newCenterX = newCenterX.coerceIn(left, right)
                    newCenterY = newCenterY.coerceIn(top, bottom)
                }

                val newCenter = PointF(newCenterX, newCenterY)
                Log.d(GESTURE_TAG, "REDUCER: MOVING_PROTRACTOR_UNIT to $newCenter")
                stateWithUpdatedMagnifier.copy(protractorUnit = stateWithUpdatedMagnifier.protractorUnit.copy(center = newCenter), valuesChangedSinceReset = true)
            }
            InteractionMode.MOVING_ACTUAL_CUE_BALL -> {
                stateWithUpdatedMagnifier.onPlaneBall?.let {
                    var newCenterX = it.center.x + logicalDelta.x
                    var newCenterY = it.center.y + logicalDelta.y

                    if (stateWithUpdatedMagnifier.showTable || stateWithUpdatedMagnifier.isBankingMode) {
                        val (left, top, right, bottom) = getTableBoundaries(stateWithUpdatedMagnifier)
                        newCenterX = newCenterX.coerceIn(left, right)
                        newCenterY = newCenterY.coerceIn(top, bottom)
                    }

                    val newCenter = PointF(newCenterX, newCenterY)
                    Log.d(GESTURE_TAG, "REDUCER: MOVING_ACTUAL_CUE_BALL to $newCenter")
                    stateWithUpdatedMagnifier.copy(onPlaneBall = it.copy(center = newCenter), valuesChangedSinceReset = true)
                } ?: stateWithUpdatedMagnifier
            }
            else -> stateWithUpdatedMagnifier
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
        return hypot(dx.toDouble(), dy.toDouble()).toFloat()
    }
}