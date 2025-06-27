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

    fun reduce(currentState: OverlayState, event: MainScreenEvent): OverlayState {
        if (currentState.isSpatiallyLocked) {
            when (event) {
                is MainScreenEvent.ToggleSpatialLock,
                is MainScreenEvent.ZoomSliderChanged,
                is MainScreenEvent.ZoomScaleChanged,
                is MainScreenEvent.ToggleBankingMode,
                is MainScreenEvent.FullOrientationChanged,
                is MainScreenEvent.ToggleHelp,
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
                is MainScreenEvent.ShowToast,
                is MainScreenEvent.ArAnchorPlaced,
                is MainScreenEvent.ToastShown,
                is MainScreenEvent.ArAvailabilityChecked -> {
                    // Allowed events proceed
                }
                else -> return currentState
            }
        }

        return when (event) {
            is MainScreenEvent.SizeChanged -> {
                if (currentState.viewWidth == 0 && currentState.viewHeight == 0) {
                    createInitialState(event.width, event.height, currentState.appControlColorScheme)
                } else {
                    val newLogicalRadius = getCurrentLogicalRadius(event.width, event.height, currentState.zoomSliderPosition)
                    currentState.copy(
                        viewWidth = event.width, viewHeight = event.height,
                        protractorUnit = currentState.protractorUnit.copy(
                            radius = newLogicalRadius,
                            logicalPosition = PointF(event.width / 2f, event.height / 2f)
                        ),
                        actualCueBall = currentState.actualCueBall?.copy(radius = newLogicalRadius)
                    )
                }
            }
            is MainScreenEvent.ZoomSliderChanged -> {
                val newSliderPos = event.position.coerceIn(0f, 100f)
                val newLogicalRadius = getCurrentLogicalRadius(currentState.viewWidth, currentState.viewHeight, newSliderPos)
                currentState.copy(
                    protractorUnit = currentState.protractorUnit.copy(radius = newLogicalRadius),
                    actualCueBall = currentState.actualCueBall?.copy(radius = newLogicalRadius),
                    zoomSliderPosition = newSliderPos,
                    valuesChangedSinceReset = true
                )
            }
            is MainScreenEvent.ZoomScaleChanged -> {
                val currentZoomValue = ZoomMapping.sliderToZoom(currentState.zoomSliderPosition)
                val newZoomValue = (currentZoomValue * event.scaleFactor).coerceIn(ZoomMapping.MIN_ZOOM, ZoomMapping.MAX_ZOOM)
                val newSliderPos = ZoomMapping.zoomToSlider(newZoomValue)
                val newLogicalRadius = getCurrentLogicalRadius(currentState.viewWidth, currentState.viewHeight, newSliderPos)
                currentState.copy(
                    protractorUnit = currentState.protractorUnit.copy(radius = newLogicalRadius),
                    actualCueBall = currentState.actualCueBall?.copy(radius = newLogicalRadius),
                    zoomSliderPosition = newSliderPos,
                    valuesChangedSinceReset = true
                )
            }
            is MainScreenEvent.FullOrientationChanged -> {
                currentState.copy(currentOrientation = event.orientation)
            }
            is MainScreenEvent.ToggleSpatialLock -> {
                if (event.isLocked) {
                    currentState.copy(anchorPlacementRequested = true)
                } else {
                    currentState.arAnchor?.detach()
                    currentState.copy(
                        isSpatiallyLocked = false,
                        anchorOrientation = null,
                        arAnchor = null
                    )
                }
            }
            is MainScreenEvent.ArAnchorPlaced -> {
                if (event.anchor != null) {
                    currentState.copy(
                        isSpatiallyLocked = true,
                        anchorOrientation = currentState.currentOrientation,
                        arAnchor = event.anchor,
                        anchorPlacementRequested = false
                    )
                } else {
                    currentState.copy(anchorPlacementRequested = false)
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
                currentState.copy(protractorUnit = currentState.protractorUnit.copy(logicalPosition = event.logicalPoint), valuesChangedSinceReset = true)
            }
            is MainScreenEvent.UpdateLogicalActualCueBallPosition -> {
                currentState.actualCueBall?.let {
                    currentState.copy(actualCueBall = it.copy(logicalPosition = event.logicalPoint), valuesChangedSinceReset = true)
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
                    val initialCenter = PointF(currentState.viewWidth / 2f, currentState.viewHeight / 2f + newRadius * 4)
                    currentState.copy(actualCueBall = ActualCueBall(radius = newRadius, logicalPosition = initialCenter), valuesChangedSinceReset = true)
                } else {
                    currentState.copy(actualCueBall = null, valuesChangedSinceReset = true)
                }
            }
            is MainScreenEvent.ToggleBankingMode -> {
                val bankingEnabled = !currentState.isBankingMode
                val viewCenterX = currentState.viewWidth / 2f
                val viewCenterY = currentState.viewHeight / 2f
                val newState = if (bankingEnabled) {
                    val bankingZoomSliderPos = ZoomMapping.zoomToSlider(ZoomMapping.DEFAULT_ZOOM * 0.8f)
                    val newLogicalRadius = getCurrentLogicalRadius(currentState.viewWidth, currentState.viewHeight, bankingZoomSliderPos)
                    val bankingBallLogicalCenter = PointF(viewCenterX, viewCenterY)
                    val newBankingBall = ActualCueBall(radius = newLogicalRadius, logicalPosition = bankingBallLogicalCenter)
                    val initialAimTarget = PointF(viewCenterX, viewCenterY - (newLogicalRadius * defaultBankingAimDistanceFactor))
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
                            logicalPosition = PointF(viewCenterX, viewCenterY)
                        ),
                        tableRotationDegrees = 0f, warningText = null
                    )
                }
                return newState.copy(
                    isSpatiallyLocked = false, anchorOrientation = null,
                    valuesChangedSinceReset = true,
                    showLuminanceDialog = false, showTutorialOverlay = false,
                    isMoreHelpVisible = false
                )
            }
            is MainScreenEvent.ToggleForceTheme -> {
                val newMode = when (currentState.isForceLightMode) { null -> true; true -> false; else -> null }
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
            is MainScreenEvent.ThemeChanged -> currentState.copy(appControlColorScheme = event.scheme)
            is MainScreenEvent.ArAvailabilityChecked -> currentState.copy(isArSupported = event.isSupported)
            else -> currentState
        }
    }

    private fun createInitialState(
        viewWidth: Int,
        viewHeight: Int,
        appColorScheme: ColorScheme?
    ): OverlayState {
        val initialSliderPos = ZoomMapping.zoomToSlider(ZoomMapping.DEFAULT_ZOOM)
        val initialLogicalRadius = getCurrentLogicalRadius(viewWidth, viewHeight, initialSliderPos)
        val initialLogicalCenter = PointF(viewWidth / 2f, viewHeight / 2f)
        return OverlayState(
            viewWidth = viewWidth, viewHeight = viewHeight,
            protractorUnit = ProtractorUnit(radius = initialLogicalRadius, rotationDegrees = 0f, logicalPosition = initialLogicalCenter),
            actualCueBall = null, zoomSliderPosition = initialSliderPos,
            isBankingMode = false, tableRotationDegrees = 0f, bankingAimTarget = null,
            valuesChangedSinceReset = false, areHelpersVisible = true, isMoreHelpVisible = false,
            isForceLightMode = null, luminanceAdjustment = 0f, showLuminanceDialog = false,
            showTutorialOverlay = false, currentTutorialStep = 0,
            appControlColorScheme = appColorScheme,
            isSpatiallyLocked = false,
            currentOrientation = FullOrientation(0f, 0f, 0f),
            anchorOrientation = null
        )
    }
}
