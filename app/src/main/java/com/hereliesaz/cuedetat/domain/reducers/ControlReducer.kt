package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.ui.ZoomMapping
import com.hereliesaz.cuedetat.view.model.OnPlaneBall
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class ControlReducer @Inject constructor() {

    fun reduce(currentState: OverlayState, event: MainScreenEvent): OverlayState {
        return when (event) {
            is MainScreenEvent.ZoomSliderChanged -> handleZoomSliderChanged(currentState, event)
            is MainScreenEvent.ZoomScaleChanged -> handleZoomScaleChanged(currentState, event)
            is MainScreenEvent.TableRotationApplied -> currentState.copy(tableRotationDegrees = currentState.tableRotationDegrees + event.degrees, valuesChangedSinceReset = true)
            is MainScreenEvent.TableRotationChanged -> currentState.copy(tableRotationDegrees = event.degrees, valuesChangedSinceReset = true)
            is MainScreenEvent.AdjustLuminance -> currentState.copy(luminanceAdjustment = event.adjustment.coerceIn(-0.4f, 0.4f), valuesChangedSinceReset = true)
            else -> currentState
        }
    }

    private fun handleZoomSliderChanged(currentState: OverlayState, event: MainScreenEvent.ZoomSliderChanged): OverlayState {
        val viewCenterX = currentState.viewWidth / 2f
        val viewCenterY = currentState.viewHeight / 2f
        val oldZoomSliderPos = currentState.zoomSliderPosition
        val oldZoomFactor = ZoomMapping.sliderToZoom(oldZoomSliderPos)
        val newSliderPos = event.position.coerceIn(-50f, 50f)
        val newLogicalRadius = ReducerUtils.getCurrentLogicalRadius(currentState.viewWidth, currentState.viewHeight, newSliderPos)
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
        val newLogicalRadius = ReducerUtils.getCurrentLogicalRadius(currentState.viewWidth, currentState.viewHeight, newSliderPos)
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
}