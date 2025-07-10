package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.domain.ReducerUtils
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.ui.ZoomMapping
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import javax.inject.Singleton

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
        val newSliderPos = event.position.coerceIn(-50f, 50f)
        val newLogicalRadius = ReducerUtils.getCurrentLogicalRadius(currentState.viewWidth, currentState.viewHeight, newSliderPos)

        val updatedOnPlaneBall = currentState.onPlaneBall?.copy(radius = newLogicalRadius)

        return currentState.copy(
            protractorUnit = currentState.protractorUnit.copy(radius = newLogicalRadius),
            onPlaneBall = updatedOnPlaneBall,
            zoomSliderPosition = newSliderPos,
            valuesChangedSinceReset = true
        )
    }

    private fun handleZoomScaleChanged(currentState: OverlayState, event: MainScreenEvent.ZoomScaleChanged): OverlayState {
        val oldZoomSliderPos = currentState.zoomSliderPosition
        val currentZoomValue = ZoomMapping.sliderToZoom(oldZoomSliderPos)
        val newZoomValue = (currentZoomValue * event.scaleFactor).coerceIn(ZoomMapping.MIN_ZOOM, ZoomMapping.MAX_ZOOM)
        val newSliderPos = ZoomMapping.zoomToSlider(newZoomValue)
        val newLogicalRadius = ReducerUtils.getCurrentLogicalRadius(currentState.viewWidth, currentState.viewHeight, newSliderPos)

        val updatedOnPlaneBall = currentState.onPlaneBall?.copy(radius = newLogicalRadius)

        return currentState.copy(
            protractorUnit = currentState.protractorUnit.copy(radius = newLogicalRadius),
            onPlaneBall = updatedOnPlaneBall,
            zoomSliderPosition = newSliderPos,
            valuesChangedSinceReset = true
        )
    }
}