// app/src/main/java/com/hereliesaz/cuedetat/domain/StateReducer.kt
package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import androidx.compose.material3.ColorScheme
import com.hereliesaz.cuedetat.data.FullOrientation
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.ui.ZoomMapping
import com.hereliesaz.cuedetat.view.model.ActualCueBall
import com.hereliesaz.cuedetat.view.model.ProtractorUnit
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import kotlin.math.min

class StateReducer @Inject constructor() {

    private val tableToBallRatioLong = 88f
    private val defaultBankingAimDistanceFactor = 15f

    private fun getCurrentLogicalRadius(
        stateWidth: Int,
        stateHeight: Int,
        zoomSliderPos: Float
    ): Float {
        if (stateWidth == 0 || stateHeight == 0) return 1f
        val zoomFactor = ZoomMapping.sliderToZoom(zoomSliderPos)
        return (min(stateWidth, stateHeight) * 0.30f / 2f) * zoomFactor
    }

    private fun adjustBallForCenteredZoom(
        currentBall: ActualCueBall?, viewCenterX: Float, viewCenterY: Float,
        oldZoomFactorFromSlider: Float, newZoomFactorFromSlider: Float
    ): ActualCueBall? {
        if (currentBall == null || oldZoomFactorFromSlider.ほぼEquals(0f) || newZoomFactorFromSlider.ほぼEquals(
                0f
            ) || oldZoomFactorFromSlider.ほぼEquals(newZoomFactorFromSlider)
        ) {
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

    private fun Float.ほぼEquals(other: Float, tolerance: Float = 0.00001f): Boolean {
        return kotlin.math.abs(this - other) < tolerance
    }

    fun reduce(currentState: OverlayState, event: MainScreenEvent): OverlayState {
        val viewCenterX = currentState.viewWidth / 2f
        val viewCenterY = currentState.viewHeight / 2f

        if (currentState.isSpatiallyLocked) {
            when (event) {
                is MainScreenEvent.ToggleSpatialLock,
                is MainScreenEvent.ZoomSliderChanged,
                is MainScreenEvent.ZoomScaleChanged,
                is MainScreenEvent.ToggleBankingMode,
                is MainScreenEvent.FullOrientationChanged,
                is MainScreenEvent.ToggleHelp,
                is MainScreenEvent.ToggleMoreHelp,
                is MainScreenEvent.ToggleForceTheme,
                is MainScreenEvent.ToggleLuminanceDialog,
                is MainScreenEvent.AdjustLuminance,
                is MainScreenEvent.StartTutorial,
                is MainScreenEvent.NextTutorialStep,
                is MainScreenEvent.EndTutorial,
                is MainScreenEvent.Reset,
                is MainScreenEvent.ThemeChanged,
                is MainScreenEvent.GestureStarted,
                is MainScreenEvent.GestureEnded,
                is MainScreenEvent.CheckForUpdate,
                is MainScreenEvent.ViewArt,
                is MainScreenEvent.FeatureComingSoon,
                is MainScreenEvent.ShowDonationOptions,
                is MainScreenEvent.SingleEventConsumed,
                is MainScreenEvent.ToastShown -> {
                    // Allowed events proceed
                }
                // All other events are IGNORED when locked by returning current state immediately
                else -> return currentState
            }
        }

        // This is the main `when` block (around line 126) that needs to be exhaustive
        return when (event) {
            is MainScreenEvent.SizeChanged -> {
                if (currentState.viewWidth == 0 && currentState.viewHeight == 0) {
                    createInitialState(event.width, event.height, currentState.appControlColorScheme)
                } else {
                    val newLogicalRadius = getCurrentLogicalRadius(event.width, event.height, currentState.zoomSliderPosition)
                    var updatedActualCueBall = currentState.actualCueBall?.copy(radius = newLogicalRadius)
                    var protractorNewCenter = currentState.protractorUnit.center
                    if (!currentState.isSpatiallyLocked) {
                        if (currentState.protractorUnit.center.x.ほぼEquals(currentState.viewWidth / 2f) &&
                            currentState.protractorUnit.center.y.ほぼEquals(currentState.viewHeight / 2f)) {
                            protractorNewCenter = PointF(event.width / 2f, event.height / 2f)
                        }
                        if (currentState.isBankingMode && updatedActualCueBall != null) {
                            if (updatedActualCueBall.center.x.ほぼEquals(currentState.viewWidth/2f) && updatedActualCueBall.center.y.ほぼEquals(currentState.viewHeight/2f)) {
                                updatedActualCueBall = updatedActualCueBall.copy(center = PointF(event.width / 2f, event.height / 2f))
                            }
                        }
                    }
                    currentState.copy(
                        viewWidth = event.width, viewHeight = event.height,
                        protractorUnit = currentState.protractorUnit.copy(
                            radius = newLogicalRadius,
                            center = protractorNewCenter
                        ),
                        actualCueBall = updatedActualCueBall
                    )
                }
            }
            is MainScreenEvent.ZoomSliderChanged -> {
                val oldZoomSliderPos = currentState.zoomSliderPosition
                val oldZoomFactor = ZoomMapping.sliderToZoom(oldZoomSliderPos)
                val newSliderPos = event.position.coerceIn(0f, 100f) // Directly use event.position
                val newLogicalRadius = getCurrentLogicalRadius(currentState.viewWidth, currentState.viewHeight, newSliderPos)
                val newZoomFactor = ZoomMapping.sliderToZoom(newSliderPos)
                var updatedActualCueBall = currentState.actualCueBall?.copy(radius = newLogicalRadius)
                if (currentState.isBankingMode && updatedActualCueBall != null) {
                    updatedActualCueBall = adjustBallForCenteredZoom(updatedActualCueBall, viewCenterX, viewCenterY, oldZoomFactor, newZoomFactor)
                }
                currentState.copy(
                    protractorUnit = currentState.protractorUnit.copy(radius = newLogicalRadius),
                    actualCueBall = updatedActualCueBall,
                    zoomSliderPosition = newSliderPos,
                    valuesChangedSinceReset = true
                )
            }
            is MainScreenEvent.ZoomScaleChanged -> {
                val oldZoomSliderPos = currentState.zoomSliderPosition
                val oldZoomFactor = ZoomMapping.sliderToZoom(oldZoomSliderPos)
                val currentZoomValue = ZoomMapping.sliderToZoom(oldZoomSliderPos)
                val newZoomValue = (currentZoomValue * event.scaleFactor).coerceIn(ZoomMapping.MIN_ZOOM, ZoomMapping.MAX_ZOOM)
                val newSliderPos = ZoomMapping.zoomToSlider(newZoomValue) // Convert back to slider position
                val newLogicalRadius = getCurrentLogicalRadius(currentState.viewWidth, currentState.viewHeight, newSliderPos)
                val newZoomFactor = newZoomValue // Use the direct zoom value
                var updatedActualCueBall = currentState.actualCueBall?.copy(radius = newLogicalRadius)
                if (currentState.isBankingMode && updatedActualCueBall != null) {
                    updatedActualCueBall = adjustBallForCenteredZoom(updatedActualCueBall, viewCenterX, viewCenterY, oldZoomFactor, newZoomFactor)
                }
                currentState.copy(
                    protractorUnit = currentState.protractorUnit.copy(radius = newLogicalRadius),
                    actualCueBall = updatedActualCueBall,
                    zoomSliderPosition = newSliderPos,
                    valuesChangedSinceReset = true
                )
            }
            is MainScreenEvent.FullOrientationChanged -> {
                currentState.copy(currentOrientation = event.orientation)
            }
            is MainScreenEvent.ToggleSpatialLock -> {
                val newLockState = !currentState.isSpatiallyLocked
                if (newLockState) {
                    currentState.copy(
                        isSpatiallyLocked = true,
                        anchorOrientation = currentState.currentOrientation,
                        valuesChangedSinceReset = true
                    )
                } else {
                    currentState.copy(
                        isSpatiallyLocked = false,
                        anchorOrientation = null,
                        valuesChangedSinceReset = true
                    )
                }
            }
            is MainScreenEvent.Reset -> {
                createInitialState(currentState.viewWidth, currentState.viewHeight, currentState.appControlColorScheme)
            }
            is MainScreenEvent.RotationChanged -> {
                var normAng = event.newRotation % 360f; if (normAng < 0) normAng += 360f
                currentState.copy(protractorUnit = currentState.protractorUnit.copy(rotationDegrees = normAng), valuesChangedSinceReset = true)
            }
            is MainScreenEvent.UpdateLogicalUnitPosition -> {
                currentState.copy(protractorUnit = currentState.protractorUnit.copy(center = event.logicalPoint), valuesChangedSinceReset = true)
            }
            is MainScreenEvent.UpdateLogicalActualCueBallPosition -> {
                currentState.actualCueBall?.let {
                    currentState.copy(actualCueBall = it.copy(center = event.logicalPoint), valuesChangedSinceReset = true)
                } ?: currentState
            }
            is MainScreenEvent.TableRotationChanged -> {
                currentState.copy(tableRotationDegrees = event.degrees, valuesChangedSinceReset = true)
            }
            is MainScreenEvent.UpdateLogicalBankingAimTarget -> {
                currentState.copy(bankingAimTarget = event.logicalPoint, valuesChangedSinceReset = true)
            }
            is MainScreenEvent.ToggleActualCueBall -> {
                if (currentState.isBankingMode) return currentState
                if (currentState.actualCueBall == null) {
                    val newRadius = getCurrentLogicalRadius(currentState.viewWidth, currentState.viewHeight, currentState.zoomSliderPosition)
                    val initialCenter = PointF(viewCenterX, viewCenterY + newRadius * 4)
                    currentState.copy(actualCueBall = ActualCueBall(center = initialCenter, radius = newRadius), valuesChangedSinceReset = true)
                } else {
                    currentState.copy(actualCueBall = null, valuesChangedSinceReset = true)
                }
            }
            is MainScreenEvent.ToggleBankingMode -> {
                val bankingEnabled = !currentState.isBankingMode
                val newState = if (bankingEnabled) {
                    val bankingZoomSliderPos = ZoomMapping.zoomToSlider(ZoomMapping.DEFAULT_ZOOM * 0.8f)
                    val newLogicalRadius = getCurrentLogicalRadius(currentState.viewWidth, currentState.viewHeight, bankingZoomSliderPos)
                    val bankingBallCenter = PointF(viewCenterX, viewCenterY)
                    val newBankingBall = ActualCueBall(center = bankingBallCenter, radius = newLogicalRadius)
                    val initialAimTarget = calculateInitialBankingAimTarget(newBankingBall, 0f, newLogicalRadius)
                    currentState.copy(
                        isBankingMode = true, actualCueBall = newBankingBall,
                        zoomSliderPosition = bankingZoomSliderPos, tableRotationDegrees = 0f,
                        bankingAimTarget = initialAimTarget,
                        protractorUnit = currentState.protractorUnit.copy(radius = newLogicalRadius),
                        warningText = null
                    )
                } else {
                    val defaultSliderPos = ZoomMapping.zoomToSlider(ZoomMapping.DEFAULT_ZOOM)
                    val defaultLogicalRadius = getCurrentLogicalRadius(currentState.viewWidth, currentState.viewHeight, defaultSliderPos)
                    currentState.copy(
                        isBankingMode = false, bankingAimTarget = null, actualCueBall = null,
                        zoomSliderPosition = defaultSliderPos,
                        protractorUnit = currentState.protractorUnit.copy(
                            radius = defaultLogicalRadius,
                            center = PointF(viewCenterX, viewCenterY)
                        ),
                        tableRotationDegrees = 0f, warningText = null
                    )
                }
                return newState.copy(
                    isSpatiallyLocked = false, anchorOrientation = null,
                    valuesChangedSinceReset = true,
                    showLuminanceDialog = false, showTutorialOverlay = false
                )
            }
            is MainScreenEvent.ToggleForceTheme -> {
                val newMode = when (currentState.isForceLightMode) { null -> true; true -> false; false -> null }
                currentState.copy(isForceLightMode = newMode, valuesChangedSinceReset = true)
            }
            is MainScreenEvent.ToggleLuminanceDialog -> currentState.copy(showLuminanceDialog = !currentState.showLuminanceDialog)
            is MainScreenEvent.AdjustLuminance -> currentState.copy(luminanceAdjustment = event.adjustment.coerceIn(-0.4f, 0.4f), valuesChangedSinceReset = true)
            is MainScreenEvent.StartTutorial -> currentState.copy(
                showTutorialOverlay = true, currentTutorialStep = 0, valuesChangedSinceReset = true,
                areHelpersVisible = false, showLuminanceDialog = false, isMoreHelpVisible = false,
                isSpatiallyLocked = false, anchorOrientation = null
            )
            is MainScreenEvent.NextTutorialStep -> currentState.copy(currentTutorialStep = currentState.currentTutorialStep + 1, valuesChangedSinceReset = true)
            is MainScreenEvent.EndTutorial -> currentState.copy(showTutorialOverlay = false, currentTutorialStep = 0)
            is MainScreenEvent.ToggleHelp -> currentState.copy(areHelpersVisible = !currentState.areHelpersVisible)
            is MainScreenEvent.ToggleMoreHelp -> currentState.copy(isMoreHelpVisible = !currentState.isMoreHelpVisible)

            // Explicitly handle pass-through or VM-handled events
            is MainScreenEvent.ThemeChanged -> currentState
            is MainScreenEvent.CheckForUpdate -> currentState
            is MainScreenEvent.ViewArt -> currentState
            is MainScreenEvent.FeatureComingSoon -> currentState
            is MainScreenEvent.ShowDonationOptions -> currentState
            is MainScreenEvent.SingleEventConsumed -> currentState
            is MainScreenEvent.ToastShown -> currentState
            is MainScreenEvent.GestureStarted -> currentState
            is MainScreenEvent.GestureEnded -> currentState

            // These are the screen-based events that ViewModel converts to Logical.
            // If they reach here, it means ViewModel didn't convert them or they are
            // new events not yet handled by ViewModel's conversion logic.
            // For safety, just return current state if they are not meant to be reduced directly.
            is MainScreenEvent.UnitMoved -> currentState
            is MainScreenEvent.ActualCueBallMoved -> currentState
            is MainScreenEvent.BankingAimTargetDragged -> currentState
        }
    }

    private fun calculateInitialBankingAimTarget(
        cueBall: ActualCueBall,
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

    private fun createInitialState(
        viewWidth: Int,
        viewHeight: Int,
        appColorScheme: ColorScheme
    ): OverlayState {
        val initialSliderPos = ZoomMapping.zoomToSlider(ZoomMapping.DEFAULT_ZOOM)
        val initialLogicalRadius = getCurrentLogicalRadius(viewWidth, viewHeight, initialSliderPos)
        val initialCenter = PointF(viewWidth / 2f, viewHeight / 2f)
        return OverlayState(
            viewWidth = viewWidth, viewHeight = viewHeight,
            protractorUnit = ProtractorUnit(center = initialCenter, radius = initialLogicalRadius, rotationDegrees = 0f),
            actualCueBall = null, zoomSliderPosition = initialSliderPos,
            isBankingMode = false, tableRotationDegrees = 0f, bankingAimTarget = null,
            valuesChangedSinceReset = false, areHelpersVisible = false, isMoreHelpVisible = false,
            isForceLightMode = null, luminanceAdjustment = 0f, showLuminanceDialog = false,
            showTutorialOverlay = false, currentTutorialStep = 0,
            appControlColorScheme = appColorScheme,
            isSpatiallyLocked = false,
            currentOrientation = FullOrientation(0f, 0f, 0f),
            anchorOrientation = null
        )
    }
}