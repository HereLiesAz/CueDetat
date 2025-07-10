package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.ui.ZoomMapping
import kotlin.math.min

object ReducerUtils {
    fun getCurrentLogicalRadius(stateWidth: Int, stateHeight: Int, zoomSliderPos: Float): Float {
        if (stateWidth == 0 || stateHeight == 0) return 1f
        val zoomFactor = ZoomMapping.sliderToZoom(zoomSliderPos)
        return (min(stateWidth, stateHeight) * 0.30f / 2f) * zoomFactor
    }
}