// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/ControlReducer.kt

package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.ReducerUtils
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.ui.ZoomMapping
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ControlReducer @Inject constructor(private val reducerUtils: ReducerUtils) {

    fun reduce(currentState: OverlayState, event: MainScreenEvent): OverlayState {
        return when (event) {
            is MainScreenEvent.ZoomSliderChanged -> handleZoomSliderChanged(currentState, event)
            is MainScreenEvent.ZoomScaleChanged -> handleZoomScaleChanged(currentState, event)
            is MainScreenEvent.TableRotationApplied -> currentState.copy(
                worldRotationDegrees = currentState.worldRotationDegrees + event.degrees,
                valuesChangedSinceReset = true
            )

            is MainScreenEvent.TableRotationChanged -> currentState.copy(
                worldRotationDegrees = event.degrees,
                valuesChangedSinceReset = true
            )
            is MainScreenEvent.AdjustLuminance -> currentState.copy(luminanceAdjustment = event.adjustment.coerceIn(-0.4f, 0.4f), valuesChangedSinceReset = true)
            is MainScreenEvent.AdjustGlow -> currentState.copy(glowStickValue = event.value.coerceIn(-1f, 1f), valuesChangedSinceReset = true)
            is MainScreenEvent.PanView -> handlePanView(currentState, event)
            is MainScreenEvent.ApplyQuickAlign -> handleApplyQuickAlign(currentState, event)
            else -> currentState
        }
    }

    private fun handleApplyQuickAlign(
        currentState: OverlayState,
        event: MainScreenEvent.ApplyQuickAlign
    ): OverlayState {
        val newZoomSliderPos = ZoomMapping.zoomToSlider(event.scale)

        return currentState.copy(
            viewOffset = PointF(event.translation.x, event.translation.y),
            worldRotationDegrees = event.rotation,
            zoomSliderPosition = newZoomSliderPos,
            isWorldLocked = true,
            valuesChangedSinceReset = true
        )
    }

    private fun handlePanView(currentState: OverlayState, event: MainScreenEvent.PanView): OverlayState {
        val currentOffset = currentState.viewOffset
        // The reducer simply applies the delta. The UseCase will enforce limits.
        val newY = currentOffset.y + event.delta.y
        val newX = currentOffset.x + event.delta.x
        return currentState.copy(viewOffset = PointF(newX, newY))
    }

    private fun handleZoomSliderChanged(currentState: OverlayState, event: MainScreenEvent.ZoomSliderChanged): OverlayState {
        val newSliderPos = event.position.coerceIn(-50f, 50f)
        return currentState.copy(
            zoomSliderPosition = newSliderPos,
            valuesChangedSinceReset = true
        )
    }

    private fun handleZoomScaleChanged(currentState: OverlayState, event: MainScreenEvent.ZoomScaleChanged): OverlayState {
        val oldZoomSliderPos = currentState.zoomSliderPosition
        val currentZoomValue = ZoomMapping.sliderToZoom(oldZoomSliderPos)
        val newZoomValue = (currentZoomValue * event.scaleFactor).coerceIn(ZoomMapping.MIN_ZOOM, ZoomMapping.MAX_ZOOM)
        val newSliderPos = ZoomMapping.zoomToSlider(newZoomValue)
        return currentState.copy(
            zoomSliderPosition = newSliderPos,
            valuesChangedSinceReset = true
        )
    }
}