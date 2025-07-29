package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.CueDetatAction
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.ui.ZoomMapping

internal fun reduceControlAction(state: CueDetatState, action: CueDetatAction): CueDetatState {
    return when (action) {
        is CueDetatAction.ZoomSliderChanged -> {
            val newSliderPos = action.position.coerceIn(-50f, 50f)
            state.copy(zoomSliderPosition = newSliderPos, valuesChangedSinceReset = true)
        }

        is CueDetatAction.ZoomScaleChanged -> {
            val (minZoom, maxZoom) = ZoomMapping.getZoomRange(state.experienceMode)
            val oldZoomSliderPos = state.zoomSliderPosition
            val currentZoomValue = ZoomMapping.sliderToZoom(oldZoomSliderPos, minZoom, maxZoom)
            val newZoomValue = (currentZoomValue * action.scaleFactor).coerceIn(minZoom, maxZoom)
            val newSliderPos = ZoomMapping.zoomToSlider(newZoomValue, minZoom, maxZoom)
            state.copy(zoomSliderPosition = newSliderPos, valuesChangedSinceReset = true)
        }

        is CueDetatAction.TableRotationApplied -> state.copy(
            worldRotationDegrees = state.worldRotationDegrees + action.degrees,
            valuesChangedSinceReset = true
        )

        is CueDetatAction.TableRotationChanged -> state.copy(
            worldRotationDegrees = action.degrees,
            valuesChangedSinceReset = true
        )

        is CueDetatAction.AdjustLuminance -> state.copy(
            luminanceAdjustment = action.adjustment.coerceIn(
                -0.4f,
                0.4f
            ), valuesChangedSinceReset = true
        )

        is CueDetatAction.AdjustGlow -> state.copy(
            glowStickValue = action.value.coerceIn(-1f, 1f),
            valuesChangedSinceReset = true
        )

        is CueDetatAction.PanView -> {
            val currentOffset = state.viewOffset
            val newY = currentOffset.y + action.delta.y
            val newX = currentOffset.x + action.delta.x
            state.copy(viewOffset = PointF(newX, newY))
        }

        is CueDetatAction.ApplyQuickAlign -> {
            val (minZoom, maxZoom) = ZoomMapping.getZoomRange(state.experienceMode)
            val newZoomSliderPos = ZoomMapping.zoomToSlider(action.scale, minZoom, maxZoom)
            state.copy(
                viewOffset = PointF(action.translation.x, action.translation.y),
                worldRotationDegrees = action.rotation,
                zoomSliderPosition = newZoomSliderPos,
                isWorldLocked = true,
                valuesChangedSinceReset = true
            )
        }

        else -> state
    }
}