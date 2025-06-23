// app/src/main/java/com/hereliesaz/cuedetat/domain/StateReducer.kt
package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import androidx.compose.material3.ColorScheme
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

        // If spatially locked, many user interactions for placement should be disabled.
        // Zoom and mode changes might still be allowed.
        if (currentState.isSpatiallyLocked && event !is MainScreenEvent.ToggleSpatialLock &&
            event !is MainScreenEvent.ZoomSliderChanged && event !is MainScreenEvent.ZoomScaleChanged &&
            event !is MainScreenEvent.ToggleBankingMode && event !is MainScreenEvent.PitchAngleChanged && /* allow sensor updates */
            event !is MainScreenEvent.ToggleHelp && event !is MainScreenEvent.ToggleMoreHelp && /* UI toggles */
            event !is MainScreenEvent.ToggleForceTheme && event !is MainScreenEvent.ToggleLuminanceDialog &&
            event !is MainScreenEvent.AdjustLuminance && event !is MainScreenEvent.StartTutorial &&
            event !is MainScreenEvent.NextTutorialStep && event !is MainScreenEvent.EndTutorial &&
            event !is MainScreenEvent.Reset && /* Reset should probably unlock */
            event !is MainScreenEvent.ThemeChanged /* Allow theme changes */
        ) {
            // Potentially allow Reset to also unlock, or handle it specially.
            if (event is MainScreenEvent.Reset) {
                return createInitialState(currentState.viewWidth, currentState.viewHeight, currentState.appControlColorScheme)
                    .copy(isSpatiallyLocked = false) // Ensure reset also unlocks
            }
            return currentState // Ignore most placement/rotation events when locked
        }


        return when (event) {
            is MainScreenEvent.SizeChanged -> {
                // ... (existing SizeChanged logic) ...
                if (currentState.viewWidth == 0 && currentState.viewHeight == 0) {
                    createInitialState(
                        event.width,
                        event.height,
                        currentState.appControlColorScheme
                    )
                } else {
                    val newLogicalRadius = getCurrentLogicalRadius(
                        event.width,
                        event.height,
                        currentState.zoomSliderPosition
                    )
                    var updatedActualCueBall =
                        currentState.actualCueBall?.copy(radius = newLogicalRadius)
                    if (currentState.isBankingMode && updatedActualCueBall != null && !currentState.isSpatiallyLocked) { // only recenter if not locked
                        updatedActualCueBall = updatedActualCueBall.copy(
                            center = PointF(
                                event.width / 2f,
                                event.height / 2f
                            )
                        )
                    }
                    currentState.copy(
                        viewWidth = event.width, viewHeight = event.height,
                        protractorUnit = currentState.protractorUnit.copy(
                            radius = newLogicalRadius,
                            // Only recenter protractor if not locked, or if it was never moved from center
                            center = if (!currentState.isSpatiallyLocked || currentState.protractorUnit.center == PointF(currentState.viewWidth/2f, currentState.viewHeight/2f))
                                PointF(event.width / 2f, event.height / 2f)
                            else currentState.protractorUnit.center
                        ),
                        actualCueBall = updatedActualCueBall
                    )
                }
            }
            // ... (ZoomSliderChanged, ZoomScaleChanged - existing logic seems okay, zoom affects visual scale not logical locked position) ...
            is MainScreenEvent.ZoomSliderChanged, is MainScreenEvent.ZoomScaleChanged -> {
                val oldZoomSliderPos = currentState.zoomSliderPosition
                val oldZoomFactor = ZoomMapping.sliderToZoom(oldZoomSliderPos)
                val newSliderPos = when (event) {
                    is MainScreenEvent.ZoomSliderChanged -> event.position.coerceIn(0f, 100f)
                    is MainScreenEvent.ZoomScaleChanged -> {
                        val currentZoomValue = ZoomMapping.sliderToZoom(oldZoomSliderPos)
                        val newZoomValue = (currentZoomValue * event.scaleFactor).coerceIn(
                            ZoomMapping.MIN_ZOOM,
                            ZoomMapping.MAX_ZOOM
                        )
                        ZoomMapping.zoomToSlider(newZoomValue)
                    }
                    else -> oldZoomSliderPos // Should not happen due to when condition
                }
                val newLogicalRadius = getCurrentLogicalRadius(
                    currentState.viewWidth,
                    currentState.viewHeight,
                    newSliderPos
                )
                val newZoomFactor = ZoomMapping.sliderToZoom(newSliderPos)
                var updatedActualCueBall =
                    currentState.actualCueBall?.copy(radius = newLogicalRadius)

                // If spatially locked, the logical center of the ball doesn't change due to zoom.
                // The visual effect of zoom is handled by the radius change and perspective.
                // However, if in BANKING mode and NOT spatially locked, we might want adjustBallForCenteredZoom.
                // For now, let's keep it simple: zoom changes radius. If locked, logical position is fixed.
                // If banking mode AND locked, we adjust the *visual position* of the ball relative to table center when zooming.
                if (currentState.isBankingMode && updatedActualCueBall != null && currentState.isSpatiallyLocked) {
                    // When spatially locked in banking mode, the ball's logical position relative to the
                    // *table* should be maintained. Since the table itself is always view-centered,
                    // we can use adjustBallForCenteredZoom, as it scales the vector from view center.
                    updatedActualCueBall = adjustBallForCenteredZoom(
                        updatedActualCueBall,
                        viewCenterX,
                        viewCenterY,
                        oldZoomFactor,
                        newZoomFactor
                    )
                } else if (currentState.isBankingMode && updatedActualCueBall != null && !currentState.isSpatiallyLocked) {
                    // Default behavior for banking mode zoom (recenter if ball is at center, otherwise scales from center)
                    updatedActualCueBall = adjustBallForCenteredZoom(
                        updatedActualCueBall,
                        viewCenterX,
                        viewCenterY,
                        oldZoomFactor,
                        newZoomFactor
                    )
                }


                currentState.copy(
                    protractorUnit = currentState.protractorUnit.copy(radius = newLogicalRadius),
                    actualCueBall = updatedActualCueBall,
                    zoomSliderPosition = newSliderPos, valuesChangedSinceReset = true
                )
            }


            is MainScreenEvent.RotationChanged -> {
                var normAng = event.newRotation % 360f; if (normAng < 0) normAng += 360f
                currentState.copy(
                    protractorUnit = currentState.protractorUnit.copy(rotationDegrees = normAng),
                    valuesChangedSinceReset = true
                )
            }
            is MainScreenEvent.UpdateLogicalUnitPosition -> {
                currentState.copy(
                    protractorUnit = currentState.protractorUnit.copy(center = event.logicalPoint),
                    valuesChangedSinceReset = true
                )
            }
            is MainScreenEvent.UpdateLogicalActualCueBallPosition -> {
                currentState.actualCueBall?.let {
                    currentState.copy(
                        actualCueBall = it.copy(center = event.logicalPoint),
                        valuesChangedSinceReset = true
                    )
                } ?: currentState
            }
            is MainScreenEvent.TableRotationChanged -> {
                currentState.copy(
                    tableRotationDegrees = event.degrees,
                    valuesChangedSinceReset = true
                )
            }
            is MainScreenEvent.UpdateLogicalBankingAimTarget -> {
                currentState.copy(
                    bankingAimTarget = event.logicalPoint,
                    valuesChangedSinceReset = true
                )
            }
            is MainScreenEvent.PitchAngleChanged -> currentState.copy(pitchAngle = event.pitch)
            is MainScreenEvent.ToggleActualCueBall -> {
                if (currentState.isBankingMode) return currentState // No separate toggle in banking
                if (currentState.actualCueBall == null) {
                    val newRadius = getCurrentLogicalRadius(
                        currentState.viewWidth,
                        currentState.viewHeight,
                        currentState.zoomSliderPosition
                    )
                    // Initial position slightly offset if not locked, otherwise keep its last known or default.
                    val initialCenter = if (!currentState.isSpatiallyLocked) {
                        PointF(viewCenterX, viewCenterY + newRadius * 4)
                    } else {
                        // If locked and actualCueBall was null, where should it appear?
                        // For now, let's use the offset position. If spatial lock is active
                        // this ball effectively "appears" at this location in space.
                        PointF(viewCenterX, viewCenterY + newRadius * 4)
                    }
                    currentState.copy(
                        actualCueBall = ActualCueBall(
                            center = initialCenter,
                            radius = newRadius
                        ), valuesChangedSinceReset = true
                    )
                } else {
                    currentState.copy(actualCueBall = null, valuesChangedSinceReset = true)
                }
            }
            is MainScreenEvent.ToggleBankingMode -> {
                // ... (existing ToggleBankingMode logic) ...
                // When toggling mode, spatial lock should be disengaged.
                val bankingEnabled = !currentState.isBankingMode
                if (bankingEnabled) {
                    val bankingZoomSliderPos =
                        ZoomMapping.zoomToSlider(ZoomMapping.DEFAULT_ZOOM * 0.8f)
                    val newLogicalRadius = getCurrentLogicalRadius(
                        currentState.viewWidth,
                        currentState.viewHeight,
                        bankingZoomSliderPos
                    )
                    val bankingBallCenter = PointF(viewCenterX, viewCenterY)
                    val newBankingBall =
                        ActualCueBall(center = bankingBallCenter, radius = newLogicalRadius)
                    val initialAimTarget =
                        calculateInitialBankingAimTarget(newBankingBall, 0f, newLogicalRadius)
                    currentState.copy(
                        isBankingMode = true, actualCueBall = newBankingBall,
                        zoomSliderPosition = bankingZoomSliderPos, tableRotationDegrees = 0f,
                        bankingAimTarget = initialAimTarget,
                        protractorUnit = currentState.protractorUnit.copy(radius = newLogicalRadius),
                        warningText = null, valuesChangedSinceReset = true,
                        showLuminanceDialog = false, showTutorialOverlay = false,
                        isSpatiallyLocked = false // Unlock when changing mode
                    )
                } else {
                    val defaultSliderPos = ZoomMapping.zoomToSlider(ZoomMapping.DEFAULT_ZOOM)
                    val defaultLogicalRadius = getCurrentLogicalRadius(
                        currentState.viewWidth,
                        currentState.viewHeight,
                        defaultSliderPos
                    )
                    currentState.copy(
                        isBankingMode = false, bankingAimTarget = null, actualCueBall = null,
                        zoomSliderPosition = defaultSliderPos,
                        protractorUnit = currentState.protractorUnit.copy(
                            radius = defaultLogicalRadius,
                            center = PointF(viewCenterX, viewCenterY)
                        ),
                        tableRotationDegrees = 0f, valuesChangedSinceReset = true,
                        showLuminanceDialog = false, showTutorialOverlay = false,
                        isSpatiallyLocked = false // Unlock when changing mode
                    )
                }
            }

            is MainScreenEvent.ToggleSpatialLock -> {
                val newLockState = !currentState.isSpatiallyLocked
                if (newLockState) {
                    // When locking, we would ideally store current sensor readings (pitch, roll, yaw)
                    // and the current logical positions/rotations of all elements.
                    // For now, we just set the flag.
                    // The "anchoring" happens by virtue of not changing logical positions on user input.
                    // Sensor-based adjustments to *keep* them looking fixed will be in UpdateStateUseCase.
                    currentState.copy(
                        isSpatiallyLocked = true,
                        valuesChangedSinceReset = true
                        // Example: lockedPitch = currentState.pitchAngle (if we store this)
                    )
                } else {
                    // When unlocking, elements become freely movable again.
                    // No specific position reset needed here unless desired.
                    currentState.copy(
                        isSpatiallyLocked = false,
                        valuesChangedSinceReset = true
                        // Example: lockedPitch = null
                    )
                }
            }

            is MainScreenEvent.ToggleForceTheme -> {
                // ... (existing logic) ...
                val newMode = when (currentState.isForceLightMode) {
                    null -> true; true -> false; false -> null
                }
                currentState.copy(isForceLightMode = newMode, valuesChangedSinceReset = true)
            }
            is MainScreenEvent.ToggleLuminanceDialog -> {
                // ... (existing logic) ...
                currentState.copy(showLuminanceDialog = !currentState.showLuminanceDialog)
            }
            is MainScreenEvent.AdjustLuminance -> {
                // ... (existing logic) ...
                currentState.copy(
                    luminanceAdjustment = event.adjustment.coerceIn(-0.4f, 0.4f),
                    valuesChangedSinceReset = true
                )
            }
            is MainScreenEvent.StartTutorial -> {
                // ... (existing logic) ...
                currentState.copy(
                    showTutorialOverlay = true,
                    currentTutorialStep = 0,
                    valuesChangedSinceReset = true,
                    areHelpersVisible = false,
                    showLuminanceDialog = false,
                    isMoreHelpVisible = false,
                    isSpatiallyLocked = false // Exit lock for tutorial
                )
            }
            is MainScreenEvent.NextTutorialStep -> {
                // ... (existing logic) ...
                currentState.copy(
                    currentTutorialStep = currentState.currentTutorialStep + 1,
                    valuesChangedSinceReset = true
                )
            }
            is MainScreenEvent.EndTutorial -> {
                // ... (existing logic) ...
                currentState.copy(showTutorialOverlay = false, currentTutorialStep = 0)
            }
            is MainScreenEvent.Reset -> {
                // Resetting should also turn off spatial lock.
                createInitialState(
                    currentState.viewWidth,
                    currentState.viewHeight,
                    currentState.appControlColorScheme
                ).copy(isSpatiallyLocked = false)
            }
            is MainScreenEvent.ToggleHelp -> currentState.copy(areHelpersVisible = !currentState.areHelpersVisible)
            is MainScreenEvent.ToggleMoreHelp -> currentState.copy(isMoreHelpVisible = !currentState.isMoreHelpVisible)


            is MainScreenEvent.UnitMoved,
            is MainScreenEvent.ActualCueBallMoved,
            is MainScreenEvent.BankingAimTargetDragged,
            is MainScreenEvent.ThemeChanged -> currentState // These are handled by specific cases above or have no direct state change here

            // Make sure all event types are covered.
            // If an event is meant to be ignored when locked, the top-level if handles it.
            // Otherwise, it falls through to its specific handler or the default.
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
            protractorUnit = ProtractorUnit(
                center = initialCenter,
                radius = initialLogicalRadius,
                rotationDegrees = 0f
            ),
            actualCueBall = null, zoomSliderPosition = initialSliderPos,
            isBankingMode = false, tableRotationDegrees = 0f, bankingAimTarget = null,
            valuesChangedSinceReset = false, areHelpersVisible = false, isMoreHelpVisible = false,
            isForceLightMode = null, luminanceAdjustment = 0f, showLuminanceDialog = false,
            showTutorialOverlay = false, currentTutorialStep = 0,
            appControlColorScheme = appColorScheme,
            isSpatiallyLocked = false // Ensure unlocked on initial state
        )
    }
}