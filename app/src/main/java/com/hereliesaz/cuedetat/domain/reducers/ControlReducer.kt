// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/ControlReducer.kt

package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.domain.ReducerUtils
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.ui.ZoomMapping
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ControlReducer @Inject constructor(private val reducerUtils: ReducerUtils) {

    fun reduce(currentState: OverlayState, event: MainScreenEvent): OverlayState {
        val updatedState = when (event) {
            is MainScreenEvent.ZoomSliderChanged -> handleZoomSliderChanged(currentState, event)
            is MainScreenEvent.ZoomScaleChanged -> handleZoomScaleChanged(currentState, event)
            is MainScreenEvent.TableRotationApplied -> currentState.copy(tableRotationDegrees = currentState.tableRotationDegrees + event.degrees, valuesChangedSinceReset = true)
            is MainScreenEvent.TableRotationChanged -> currentState.copy(tableRotationDegrees = event.degrees, valuesChangedSinceReset = true)
            is MainScreenEvent.AdjustLuminance -> currentState.copy(luminanceAdjustment = event.adjustment.coerceIn(-0.4f, 0.4f), valuesChangedSinceReset = true)
            is MainScreenEvent.AdjustGlow -> currentState.copy(glowStickValue = event.value.coerceIn(-1f, 1f), valuesChangedSinceReset = true)
            else -> currentState
        }

        return if (event is MainScreenEvent.ZoomSliderChanged || event is MainScreenEvent.ZoomScaleChanged) {
            reducerUtils.snapViolatingBalls(updatedState)
        } else {
            updatedState
        }
    }

    private fun handleZoomSliderChanged(currentState: OverlayState, event: MainScreenEvent.ZoomSliderChanged): OverlayState {
        val newSliderPos = event.position.coerceIn(-50f, 50f)
        val newLogicalRadius = reducerUtils.getCurrentLogicalRadius(currentState.viewWidth, currentState.viewHeight, newSliderPos)

        val updatedOnPlaneBall = currentState.onPlaneBall?.copy(radius = newLogicalRadius)
        val updatedObstacles = currentState.obstacleBalls.map { it.copy(radius = newLogicalRadius) }

        return currentState.copy(
            protractorUnit = currentState.protractorUnit.copy(radius = newLogicalRadius),
            onPlaneBall = updatedOnPlaneBall,
            obstacleBalls = updatedObstacles,
            zoomSliderPosition = newSliderPos,
            valuesChangedSinceReset = true
        )
    }

    private fun handleZoomScaleChanged(currentState: OverlayState, event: MainScreenEvent.ZoomScaleChanged): OverlayState {
        val oldZoomSliderPos = currentState.zoomSliderPosition
        val currentZoomValue = ZoomMapping.sliderToZoom(oldZoomSliderPos)
        val newZoomValue = (currentZoomValue * event.scaleFactor).coerceIn(ZoomMapping.MIN_ZOOM, ZoomMapping.MAX_ZOOM)
        val newSliderPos = ZoomMapping.zoomToSlider(newZoomValue)
        val newLogicalRadius = reducerUtils.getCurrentLogicalRadius(currentState.viewWidth, currentState.viewHeight, newSliderPos)

        val updatedOnPlaneBall = currentState.onPlaneBall?.copy(radius = newLogicalRadius)
        val updatedObstacles = currentState.obstacleBalls.map { it.copy(radius = newLogicalRadius) }


        return currentState.copy(
            protractorUnit = currentState.protractorUnit.copy(radius = newLogicalRadius),
            onPlaneBall = updatedOnPlaneBall,
            obstacleBalls = updatedObstacles,
            zoomSliderPosition = newSliderPos,
            valuesChangedSinceReset = true
        )
    }
}