// app/src/main/java/com/hereliesaz/cuedetat/domain/StateReducer.kt
package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import android.util.Log
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.geometry.Offset
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.ui.ZoomMapping
import com.hereliesaz.cuedetat.view.model.OnPlaneBall
import com.hereliesaz.cuedetat.view.model.ProtractorUnit
import com.hereliesaz.cuedetat.view.state.DistanceUnit
import com.hereliesaz.cuedetat.view.state.InteractionMode
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val GESTURE_TAG = "GestureDebug"

class StateReducer @Inject constructor() {

    private val defaultBankingAimDistanceFactor = 15f

    fun reduce(currentState: OverlayState, event: MainScreenEvent): OverlayState {
        return when (event) {
            is MainScreenEvent.LogicalGestureStarted -> handleGestureStarted(currentState, event)
            is MainScreenEvent.LogicalDragApplied -> handleDrag(currentState, event)
            is MainScreenEvent.GestureEnded -> {
                Log.d(GESTURE_TAG, "REDUCER: GestureEnded. Resetting interaction mode.")
                currentState.copy(interactionMode = InteractionMode.NONE)
            }
            is MainScreenEvent.SizeChanged -> handleSizeChanged(currentState, event)
            is MainScreenEvent.ZoomSliderChanged -> handleZoomSliderChanged(currentState, event)
            is MainScreenEvent.ZoomScaleChanged -> handleZoomScaleChanged(currentState, event)
            is MainScreenEvent.FullOrientationChanged -> currentState.copy(currentOrientation = event.orientation)
            is MainScreenEvent.Reset -> handleReset(currentState)
            is MainScreenEvent.TableRotationChanged -> currentState.copy(tableRotationDegrees = event.degrees, valuesChangedSinceReset = true)
            is MainScreenEvent.ToggleOnPlaneBall -> handleToggleOnPlaneBall(currentState)
            is MainScreenEvent.ToggleBankingMode -> handleToggleBankingMode(currentState)
            is MainScreenEvent.ToggleTable -> currentState.copy(showTable = !currentState.showTable, valuesChangedSinceReset = true)
            is MainScreenEvent.ToggleForceTheme -> {
                val newMode = when (currentState.isForceLightMode) { null -> true; true -> false; false -> null }
                currentState.copy(isForceLightMode = newMode, valuesChangedSinceReset = true)
            }
            is MainScreenEvent.ToggleDistanceUnit -> currentState.copy(
                distanceUnit = if (currentState.distanceUnit == DistanceUnit.METRIC) DistanceUnit.IMPERIAL else DistanceUnit.METRIC,
                valuesChangedSinceReset = true
            )
            is MainScreenEvent.ToggleLuminanceDialog -> currentState.copy(showLuminanceDialog = !currentState.showLuminanceDialog)
            is MainScreenEvent.AdjustLuminance -> currentState.copy(luminanceAdjustment = event.adjustment.coerceIn(-0.4f, 0.4f), valuesChangedSinceReset = true)
            is MainScreenEvent.StartTutorial -> currentState.copy(
                showTutorialOverlay = true, currentTutorialStep = 0, valuesChangedSinceReset = true,
                areHelpersVisible = false, showLuminanceDialog = false, isMoreHelpVisible = false
            )
            is MainScreenEvent.NextTutorialStep -> currentState.copy(currentTutorialStep = currentState.currentTutorialStep + 1, valuesChangedSinceReset = true)
            is MainScreenEvent.EndTutorial -> currentState.copy(showTutorialOverlay = false, currentTutorialStep = 0)
            is MainScreenEvent.ToggleHelp -> currentState.copy(areHelpersVisible = !currentState.areHelpersVisible)
            is MainScreenEvent.ToggleMoreHelp -> currentState.copy(isMoreHelpVisible = !currentState.isMoreHelpVisible)
            else -> currentState
        }
    }

    private fun handleReset(currentState: OverlayState): OverlayState {
        // If a pre-reset state exists, revert to it.
        currentState.preResetState?.let {
            return it.copy(preResetState = null) // Revert and clear the saved state
        }

        // Otherwise, this is the first press. Save the current positional state.
        val stateToSave = currentState.copy()

        // Create the default positional state
        val initialSliderPos = 0f
        val initialLogicalRadius = getCurrentLogicalRadius(currentState.viewWidth, currentState.viewHeight, initialSliderPos)
        val initialProtractorCenter = PointF(currentState.viewWidth / 2f, currentState.viewHeight / 2f)

        // Reset only positional and rotational properties, preserving toggles
        return currentState.copy(
            protractorUnit = ProtractorUnit(center = initialProtractorCenter, radius = initialLogicalRadius, rotationDegrees = 0f),
            zoomSliderPosition = initialSliderPos,
            tableRotationDegrees = 0f,
            bankingAimTarget = null, // This is positional
            luminanceAdjustment = 0f,
            preResetState = stateToSave // Save the original state for reverting
        )
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
                // Combine horizontal and vertical drag components for a more fluid rotation
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

    private fun createInitialState(viewWidth: Int, viewHeight: Int, appColorScheme: ColorScheme): OverlayState {
        val initialSliderPos = 0f // Centered
        val initialLogicalRadius = getCurrentLogicalRadius(viewWidth, viewHeight, initialSliderPos)
        val initialProtractorCenter = PointF(viewWidth / 2f, viewHeight / 2f)

        return OverlayState(
            viewWidth = viewWidth, viewHeight = viewHeight,
            protractorUnit = ProtractorUnit(center = initialProtractorCenter, radius = initialLogicalRadius, rotationDegrees = 0f),
            onPlaneBall = null, // Default to null
            zoomSliderPosition = initialSliderPos,
            isBankingMode = false, showTable = false, tableRotationDegrees = 0f, bankingAimTarget = null,
            valuesChangedSinceReset = false, areHelpersVisible = false, isMoreHelpVisible = false,
            isForceLightMode = null, luminanceAdjustment = 0f, showLuminanceDialog = false,
            showTutorialOverlay = false, currentTutorialStep = 0,
            appControlColorScheme = appColorScheme,
            interactionMode = InteractionMode.NONE
        )
    }

    private fun getCurrentLogicalRadius(stateWidth: Int, stateHeight: Int, zoomSliderPos: Float): Float {
        if (stateWidth == 0 || stateHeight == 0) return 1f
        val zoomFactor = ZoomMapping.sliderToZoom(zoomSliderPos)
        return (min(stateWidth, stateHeight) * 0.30f / 2f) * zoomFactor
    }

    private fun adjustBallForCenteredZoom(currentBall: OnPlaneBall?, viewCenterX: Float, viewCenterY: Float, oldZoomFactorFromSlider: Float, newZoomFactorFromSlider: Float): OnPlaneBall? {
        if (currentBall == null || oldZoomFactorFromSlider.roughlyEquals(0f) || newZoomFactorFromSlider.roughlyEquals(0f) || oldZoomFactorFromSlider.roughlyEquals(newZoomFactorFromSlider)) {
            return currentBall
        }
        val scaleEffectRatio = oldZoomFactorFromSlider / newZoomFactorFromSlider
        val vecX = currentBall.center.x - viewCenterX
        val vecY = currentBall.center.y - viewCenterY
        val newVecX = vecX * scaleEffectRatio
        val newVecY = vecY * scaleEffectRatio
        val newCenterX = viewCenterX + newVecX
        val newCenterY = viewCenterY + newVecY
        return currentBall.copy(center = PointF(newCenterX, newCenterY))
    }

    private fun Float.roughlyEquals(other: Float, tolerance: Float = 0.00001f): Boolean {
        return abs(this - other) < tolerance
    }

    private fun handleToggleOnPlaneBall(currentState: OverlayState): OverlayState {
        val viewCenterX = currentState.viewWidth / 2f
        val viewCenterY = currentState.viewHeight / 2f
        val viewBottomY = currentState.viewHeight.toFloat()

        if (currentState.isBankingMode) return currentState
        return if (currentState.onPlaneBall == null) {
            val newRadius = getCurrentLogicalRadius(currentState.viewWidth, currentState.viewHeight, currentState.zoomSliderPosition)
            // Position halfway between screen center and screen bottom
            val initialCenter = PointF(viewCenterX, (viewCenterY + viewBottomY) / 2f)
            currentState.copy(onPlaneBall = OnPlaneBall(center = initialCenter, radius = newRadius), valuesChangedSinceReset = true)
        } else {
            currentState.copy(onPlaneBall = null, valuesChangedSinceReset = true)
        }
    }

    private fun handleToggleBankingMode(currentState: OverlayState): OverlayState {
        val viewCenterX = currentState.viewWidth / 2f
        val viewCenterY = currentState.viewHeight / 2f
        val bankingEnabled = !currentState.isBankingMode
        val newState = if (bankingEnabled) {
            val bankingZoomSliderPos = 0f // Centered default
            val newLogicalRadius = getCurrentLogicalRadius(currentState.viewWidth, currentState.viewHeight, bankingZoomSliderPos)
            val bankingBallCenter = PointF(viewCenterX, viewCenterY)
            val newBankingBall = OnPlaneBall(center = bankingBallCenter, radius = newLogicalRadius)
            val initialAimTarget = calculateInitialBankingAimTarget(newBankingBall, 0f, newLogicalRadius)
            currentState.copy(
                isBankingMode = true, onPlaneBall = newBankingBall,
                zoomSliderPosition = bankingZoomSliderPos, tableRotationDegrees = 0f,
                bankingAimTarget = initialAimTarget,
                protractorUnit = currentState.protractorUnit.copy(radius = newLogicalRadius),
                showTable = true, // Always show table in banking mode
                warningText = null
            )
        } else {
            val defaultSliderPos = 0f // Centered default
            val defaultLogicalRadius = getCurrentLogicalRadius(currentState.viewWidth, currentState.viewHeight, defaultSliderPos)
            currentState.copy(
                isBankingMode = false, bankingAimTarget = null, onPlaneBall = null,
                zoomSliderPosition = defaultSliderPos,
                showTable = false,
                protractorUnit = currentState.protractorUnit.copy(
                    radius = defaultLogicalRadius,
                    center = PointF(viewCenterX, viewCenterY)
                ),
                tableRotationDegrees = 0f, warningText = null
            )
        }
        return newState.copy(
            valuesChangedSinceReset = true,
            showLuminanceDialog = false, showTutorialOverlay = false
        )
    }

    private fun handleSizeChanged(currentState: OverlayState, event: MainScreenEvent.SizeChanged): OverlayState {
        if (currentState.viewWidth == 0 && currentState.viewHeight == 0) {
            return createInitialState(event.width, event.height, currentState.appControlColorScheme)
        } else {
            val newLogicalRadius = getCurrentLogicalRadius(event.width, event.height, currentState.zoomSliderPosition)
            var updatedOnPlaneBall = currentState.onPlaneBall?.copy(radius = newLogicalRadius)
            var protractorNewCenter = currentState.protractorUnit.center

            if (currentState.protractorUnit.center.x.roughlyEquals(currentState.viewWidth / 2f) &&
                currentState.protractorUnit.center.y.roughlyEquals(currentState.viewHeight / 2f)) {
                protractorNewCenter = PointF(event.width / 2f, event.height / 2f)
            }
            if (currentState.isBankingMode && updatedOnPlaneBall != null) {
                if (updatedOnPlaneBall.center.x.roughlyEquals(currentState.viewWidth/2f) && updatedOnPlaneBall.center.y.roughlyEquals(currentState.viewHeight/2f)) {
                    updatedOnPlaneBall = updatedOnPlaneBall.copy(center = PointF(event.width / 2f, event.height / 2f))
                }
            }

            return currentState.copy(
                viewWidth = event.width, viewHeight = event.height,
                protractorUnit = currentState.protractorUnit.copy(
                    radius = newLogicalRadius,
                    center = protractorNewCenter
                ),
                onPlaneBall = updatedOnPlaneBall
            )
        }
    }

    private fun handleZoomSliderChanged(currentState: OverlayState, event: MainScreenEvent.ZoomSliderChanged): OverlayState {
        val viewCenterX = currentState.viewWidth / 2f
        val viewCenterY = currentState.viewHeight / 2f
        val oldZoomSliderPos = currentState.zoomSliderPosition
        val oldZoomFactor = ZoomMapping.sliderToZoom(oldZoomSliderPos)
        val newSliderPos = event.position.coerceIn(-50f, 50f)
        val newLogicalRadius = getCurrentLogicalRadius(currentState.viewWidth, currentState.viewHeight, newSliderPos)
        val newZoomFactor = ZoomMapping.sliderToZoom(newSliderPos)
        var updatedOnPlaneBall = currentState.onPlaneBall?.copy(radius = newLogicalRadius)
        if (currentState.isBankingMode && updatedOnPlaneBall != null) {
            updatedOnPlaneBall = adjustBallForCenteredZoom(updatedOnPlaneBall, viewCenterX, viewCenterY, oldZoomFactor, newZoomFactor)
        }
        return currentState.copy(
            protractorUnit = currentState.protractorUnit.copy(radius = newLogicalRadius),
            onPlaneBall = updatedOnPlaneBall,
            zoomSliderPosition = newSliderPos,
            valuesChangedSinceReset = true
        )
    }

    private fun handleZoomScaleChanged(currentState: OverlayState, event: MainScreenEvent.ZoomScaleChanged): OverlayState {
        val viewCenterX = currentState.viewWidth / 2f
        val viewCenterY = currentState.viewHeight / 2f
        val oldZoomSliderPos = currentState.zoomSliderPosition
        val oldZoomFactor = ZoomMapping.sliderToZoom(oldZoomSliderPos)
        val currentZoomValue = ZoomMapping.sliderToZoom(oldZoomSliderPos)
        val newZoomValue = (currentZoomValue * event.scaleFactor).coerceIn(ZoomMapping.MIN_ZOOM, ZoomMapping.MAX_ZOOM)
        val newSliderPos = ZoomMapping.zoomToSlider(newZoomValue)
        val newLogicalRadius = getCurrentLogicalRadius(currentState.viewWidth, currentState.viewHeight, newSliderPos)
        val newZoomFactor = newZoomValue
        var updatedOnPlaneBall = currentState.onPlaneBall?.copy(radius = newLogicalRadius)
        if (currentState.isBankingMode && updatedOnPlaneBall != null) {
            updatedOnPlaneBall = adjustBallForCenteredZoom(updatedOnPlaneBall, viewCenterX, viewCenterY, oldZoomFactor, newZoomFactor)
        }
        return currentState.copy(
            protractorUnit = currentState.protractorUnit.copy(radius = newLogicalRadius),
            onPlaneBall = updatedOnPlaneBall,
            zoomSliderPosition = newSliderPos,
            valuesChangedSinceReset = true
        )
    }

    private fun calculateInitialBankingAimTarget(
        cueBall: OnPlaneBall,
        tableRotationDegrees: Float,
        cueBallRadius: Float
    ): PointF {
        val aimDistance = cueBallRadius * defaultBankingAimDistanceFactor
        val angleRad = Math.toRadians((tableRotationDegrees - 90.0))
        return PointF(
            cueBall.center.x + (aimDistance * kotlin.math.cos(angleRad)).toFloat(),
            cueBall.center.y + (aimDistance * kotlin.math.sin(angleRad)).toFloat()
        )
    }
}