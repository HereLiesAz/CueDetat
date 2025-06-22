// app/src/main/java/com/hereliesaz/cuedetat/domain/StateReducer.kt
package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.ui.ZoomMapping
import com.hereliesaz.cuedetat.view.model.ActualCueBall
// import com.hereliesaz.cuedetat.view.model.Perspective // Not directly used in this version of reducer
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
        if (stateWidth == 0 || stateHeight == 0) return 1f // Default if view not initialized
        val zoomFactor = ZoomMapping.sliderToZoom(zoomSliderPos)
        return (min(stateWidth, stateHeight) * 0.30f / 2f) * zoomFactor
    }

    private fun adjustBallForCenteredZoom(
        currentBall: ActualCueBall?,
        viewCenterX: Float,
        viewCenterY: Float,
        oldZoomFactorFromSlider: Float,
        newZoomFactorFromSlider: Float
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

        return when (event) {
            is MainScreenEvent.SizeChanged -> {
                if (currentState.viewWidth == 0 && currentState.viewHeight == 0) {
                    createInitialState(event.width, event.height)
                } else {
                    val newLogicalRadius = getCurrentLogicalRadius(
                        event.width,
                        event.height,
                        currentState.zoomSliderPosition
                    )
                    var updatedActualCueBall =
                        currentState.actualCueBall?.copy(radius = newLogicalRadius)
                    if (currentState.isBankingMode && updatedActualCueBall != null) {
                        updatedActualCueBall = updatedActualCueBall.copy(
                            center = PointF(
                                event.width / 2f,
                                event.height / 2f
                            )
                        )
                    }
                    currentState.copy(
                        viewWidth = event.width,
                        viewHeight = event.height,
                        protractorUnit = currentState.protractorUnit.copy(
                            radius = newLogicalRadius,
                            center = PointF(event.width / 2f, event.height / 2f)
                        ),
                        actualCueBall = updatedActualCueBall
                    )
                }
            }

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

                    else -> oldZoomSliderPos
                }
                val newLogicalRadius = getCurrentLogicalRadius(
                    currentState.viewWidth,
                    currentState.viewHeight,
                    newSliderPos
                )
                val newZoomFactor = ZoomMapping.sliderToZoom(newSliderPos)

                var updatedActualCueBall =
                    currentState.actualCueBall?.copy(radius = newLogicalRadius)
                if (currentState.isBankingMode && updatedActualCueBall != null) {
                    updatedActualCueBall = adjustBallForCenteredZoom(
                        updatedActualCueBall, viewCenterX, viewCenterY, oldZoomFactor, newZoomFactor
                    )
                }
                currentState.copy(
                    protractorUnit = currentState.protractorUnit.copy(radius = newLogicalRadius),
                    actualCueBall = updatedActualCueBall,
                    zoomSliderPosition = newSliderPos,
                    valuesChangedSinceReset = true
                )
            }

            is MainScreenEvent.RotationChanged -> { // For ProtractorUnit rotation
                var normAng = event.newRotation % 360f
                if (normAng < 0) normAng += 360f
                currentState.copy(
                    protractorUnit = currentState.protractorUnit.copy(rotationDegrees = normAng),
                    valuesChangedSinceReset = true
                )
            }

            is MainScreenEvent.UpdateLogicalUnitPosition -> { // For ProtractorUnit center
                currentState.copy(
                    protractorUnit = currentState.protractorUnit.copy(center = event.logicalPoint),
                    valuesChangedSinceReset = true
                )
            }

            is MainScreenEvent.UpdateLogicalActualCueBallPosition -> { // For ActualCueBall (either mode)
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

            is MainScreenEvent.ToggleActualCueBall -> { // For optional ActualCueBall in Protractor mode
                if (currentState.isBankingMode) return currentState
                if (currentState.actualCueBall == null) {
                    // Simplified default position logic for protractor's optional cue ball
                    val newRadius = getCurrentLogicalRadius(
                        currentState.viewWidth,
                        currentState.viewHeight,
                        currentState.zoomSliderPosition
                    )
                    val initialCenter = PointF(
                        viewCenterX,
                        viewCenterY + newRadius * 4 // Place it somewhat below center
                    )
                    currentState.copy(
                        actualCueBall = ActualCueBall(center = initialCenter, radius = newRadius),
                        valuesChangedSinceReset = true
                    )
                } else {
                    currentState.copy(actualCueBall = null, valuesChangedSinceReset = true)
                }
            }

            is MainScreenEvent.ToggleBankingMode -> {
                val bankingEnabled = !currentState.isBankingMode
                if (bankingEnabled) {
                    val newSliderPosForTableFit =
                        ZoomMapping.zoomToSlider(ZoomMapping.DEFAULT_ZOOM) // Start with default zoom
                    val newLogicalRadius = getCurrentLogicalRadius(
                        currentState.viewWidth,
                        currentState.viewHeight,
                        newSliderPosForTableFit
                    )
                    val bankingBallCenter = PointF(viewCenterX, viewCenterY)
                    val newBankingBall =
                        ActualCueBall(center = bankingBallCenter, radius = newLogicalRadius)
                    val initialAimTarget =
                        calculateInitialBankingAimTarget(newBankingBall, 0f, newLogicalRadius)
                    currentState.copy(
                        isBankingMode = true,
                        actualCueBall = newBankingBall,
                        zoomSliderPosition = newSliderPosForTableFit,
                        tableRotationDegrees = 0f,
                        bankingAimTarget = initialAimTarget,
                        protractorUnit = currentState.protractorUnit.copy(radius = newLogicalRadius),
                        warningText = null,
                        valuesChangedSinceReset = true
                    )
                } else {
                    val defaultSliderPos = ZoomMapping.zoomToSlider(ZoomMapping.DEFAULT_ZOOM)
                    val defaultLogicalRadius = getCurrentLogicalRadius(
                        currentState.viewWidth,
                        currentState.viewHeight,
                        defaultSliderPos
                    )
                    currentState.copy(
                        isBankingMode = false,
                        bankingAimTarget = null,
                        actualCueBall = null, // Clear actual cue ball when exiting banking
                        zoomSliderPosition = defaultSliderPos,
                        protractorUnit = currentState.protractorUnit.copy(
                            radius = defaultLogicalRadius,
                            center = PointF(viewCenterX, viewCenterY)
                        ),
                        tableRotationDegrees = 0f,
                        valuesChangedSinceReset = true
                    )
                }
            }

            is MainScreenEvent.Reset -> createInitialState(
                currentState.viewWidth,
                currentState.viewHeight
            )

            is MainScreenEvent.ToggleHelp -> currentState.copy(areHelpersVisible = !currentState.areHelpersVisible)
            is MainScreenEvent.ToggleMoreHelp -> currentState.copy(isMoreHelpVisible = !currentState.isMoreHelpVisible)

            // Events handled by ViewModel or don't directly change state passed to reducer
            is MainScreenEvent.UnitMoved,
            is MainScreenEvent.ActualCueBallMoved,
            is MainScreenEvent.BankingAimTargetDragged -> currentState

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

    private fun createInitialState(viewWidth: Int, viewHeight: Int): OverlayState {
        val initialSliderPos = ZoomMapping.zoomToSlider(ZoomMapping.DEFAULT_ZOOM)
        val initialLogicalRadius = getCurrentLogicalRadius(viewWidth, viewHeight, initialSliderPos)
        val initialCenter = PointF(viewWidth / 2f, viewHeight / 2f)
        return OverlayState(
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            protractorUnit = ProtractorUnit(
                center = initialCenter,
                radius = initialLogicalRadius,
                rotationDegrees = 0f
            ),
            actualCueBall = null,
            zoomSliderPosition = initialSliderPos,
            isBankingMode = false,
            tableRotationDegrees = 0f,
            bankingAimTarget = null,
            valuesChangedSinceReset = false,
            areHelpersVisible = false,
            isMoreHelpVisible = false
        )
    }
}