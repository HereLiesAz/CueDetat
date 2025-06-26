// app/src/main/java/com/hereliesaz/cuedetat/domain/StateReducer.kt
package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import android.util.Log
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
        // Use screenCenter explicitly here
        val vecX = currentBall.screenCenter.x - viewCenterX
        val vecY = currentBall.screenCenter.y - viewCenterY
        val newVecX = vecX * scaleEffectRatio
        val newVecY = vecY * scaleEffectRatio
        val newCenterX = viewCenterX + newVecX
        val newCenterY = viewCenterY + newVecY
        // Return with updated screenCenter
        return currentBall.copy(screenCenter = PointF(newCenterX, newCenterY))
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
                is MainScreenEvent.ToastShown -> {
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
                    var updatedActualCueBall = currentState.actualCueBall?.copy(radius = newLogicalRadius)
                    var protractorNewScreenCenter = currentState.protractorUnit.screenCenter
                    if (!currentState.isSpatiallyLocked) {
                        if (protractorNewScreenCenter.x.ほぼEquals(currentState.viewWidth / 2f) &&
                            protractorNewScreenCenter.y.ほぼEquals(currentState.viewHeight / 2f)) {
                            protractorNewScreenCenter = PointF(event.width / 2f, event.height / 2f)
                        }
                        if (currentState.isBankingMode && updatedActualCueBall != null) {
                            if (updatedActualCueBall.screenCenter.x.ほぼEquals(currentState.viewWidth/2f) && updatedActualCueBall.screenCenter.y.ほぼEquals(currentState.viewHeight/2f)) {
                                updatedActualCueBall = updatedActualCueBall.copy(screenCenter = PointF(event.width / 2f, event.height / 2f))
                            }
                        }
                    }
                    currentState.copy(
                        viewWidth = event.width, viewHeight = event.height,
                        protractorUnit = currentState.protractorUnit.copy(
                            radius = newLogicalRadius,
                            screenCenter = protractorNewScreenCenter,
                            logicalPosition = protractorNewScreenCenter
                        ),
                        actualCueBall = updatedActualCueBall
                    )
                }
            }
            is MainScreenEvent.ZoomSliderChanged -> {
                val oldZoomSliderPos = currentState.zoomSliderPosition
                val oldZoomFactor = ZoomMapping.sliderToZoom(oldZoomSliderPos)
                val newSliderPos = event.position.coerceIn(0f, 100f)
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
                val newSliderPos = ZoomMapping.zoomToSlider(newZoomValue)
                val newLogicalRadius = getCurrentLogicalRadius(currentState.viewWidth, currentState.viewHeight, newSliderPos)
                val newZoomFactor = newZoomValue
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
                Log.d("StateReducer", "FullOrientationChanged: ${event.orientation}, Locked: ${currentState.isSpatiallyLocked}")
                currentState.copy(currentOrientation = event.orientation)
            }
            is MainScreenEvent.ToggleSpatialLock -> {
                val newLockState = event.isLocked
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
            is MainScreenEvent.UnitMoved -> {
                currentState.copy(protractorUnit = currentState.protractorUnit.copy(screenCenter = event.position), valuesChangedSinceReset = true)
            }
            is MainScreenEvent.ActualCueBallMoved -> {
                currentState.actualCueBall?.let {
                    currentState.copy(actualCueBall = it.copy(screenCenter = event.position), valuesChangedSinceReset = true)
                } ?: currentState
            }
            is MainScreenEvent.TableRotationChanged -> {
                currentState.copy(tableRotationDegrees = event.degrees, valuesChangedSinceReset = true)
            }
            is MainScreenEvent.BankingAimTargetDragged -> {
                currentState.copy(bankingAimTarget = event.screenPoint, valuesChangedSinceReset = true)
            }
            is MainScreenEvent.ToggleActualCueBall -> {
                if (currentState.isBankingMode) return currentState
                if (currentState.actualCueBall == null) {
                    val newRadius = getCurrentLogicalRadius(currentState.viewWidth, currentState.viewHeight, currentState.zoomSliderPosition)
                    val initialCenter = PointF(viewCenterX, viewCenterY + newRadius * 4)
                    currentState.copy(actualCueBall = ActualCueBall(screenCenter = initialCenter, radius = newRadius, logicalPosition = initialCenter), valuesChangedSinceReset = true)
                } else {
                    currentState.copy(actualCueBall = null, valuesChangedSinceReset = true)
                }
            }
            is MainScreenEvent.ToggleBankingMode -> {
                val bankingEnabled = !currentState.isBankingMode
                val newState = if (bankingEnabled) {
                    val bankingZoomSliderPos = ZoomMapping.zoomToSlider(ZoomMapping.DEFAULT_ZOOM * 0.8f)
                    val newLogicalRadius = getCurrentLogicalRadius(currentState.viewWidth, currentState.viewHeight, bankingZoomSliderPos)
                    val bankingBallLogicalCenter = PointF(viewCenterX, viewCenterY)
                    val bankingBallScreenCenter = bankingBallLogicalCenter
                    val newBankingBall = ActualCueBall(screenCenter = bankingBallScreenCenter, radius = newLogicalRadius, logicalPosition = bankingBallLogicalCenter)
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
                            screenCenter = PointF(viewCenterX, viewCenterY),
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
            is MainScreenEvent.ThemeChanged -> currentState
            is MainScreenEvent.CheckForUpdate -> currentState
            is MainScreenEvent.ViewArt -> currentState
            is MainScreenEvent.FeatureComingSoon -> currentState
            is MainScreenEvent.ShowDonationOptions -> currentState
            is MainScreenEvent.SingleEventConsumed -> currentState
            is MainScreenEvent.ToastShown -> currentState
            is MainScreenEvent.GestureStarted -> currentState
            is MainScreenEvent.GestureEnded -> currentState
            is MainScreenEvent.ShowToast -> currentState

            else -> currentState
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
            cueBall.logicalPosition.x + (aimDistance * kotlin.math.cos(angleRad)).toFloat(),
            cueBall.logicalPosition.y + (aimDistance * kotlin.math.sin(angleRad)).toFloat()
        )
    }

    private fun createInitialState(
        viewWidth: Int,
        viewHeight: Int,
        appColorScheme: ColorScheme?
    ): OverlayState {
        val initialSliderPos = ZoomMapping.zoomToSlider(ZoomMapping.DEFAULT_ZOOM)
        val initialLogicalRadius = getCurrentLogicalRadius(viewWidth, viewHeight, initialSliderPos)
        val initialScreenCenter = PointF(viewWidth / 2f, viewHeight / 2f)
        val initialLogicalCenter = initialScreenCenter
        return OverlayState(
            viewWidth = viewWidth, viewHeight = viewHeight,
            protractorUnit = ProtractorUnit(screenCenter = initialScreenCenter, radius = initialLogicalRadius, rotationDegrees = 0f, logicalPosition = initialLogicalCenter),
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