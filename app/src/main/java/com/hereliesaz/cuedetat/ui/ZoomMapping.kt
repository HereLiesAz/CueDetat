package com.hereliesaz.cuedetat.ui

import kotlin.math.ln
import kotlin.math.pow

internal object ZoomMapping {
    const val MIN_ZOOM = 0.2f
    const val MAX_ZOOM = 4.0f
    const val DEFAULT_ZOOM = 0.4f
    private const val B = 1.0069555f
    fun sliderToZoom(sliderValue: Float): Float = MIN_ZOOM * B.pow(sliderValue)
    fun zoomToSlider(zoomFactor: Float): Float =
        if (zoomFactor <= MIN_ZOOM) 0f else (ln(zoomFactor / MIN_ZOOM) / ln(B))
}
