package com.hereliesaz.cuedetatlite.ui

import kotlin.math.ln
import kotlin.math.pow

internal object ZoomMapping {
    const val MIN_ZOOM = 0.084f
    const val DEFAULT_ZOOM = 0.4f // Restoring this for text scaling normalization
    const val MAX_ZOOM = 0.6f

    private val B = (MAX_ZOOM / MIN_ZOOM).pow(0.01f)

    fun sliderToZoom(sliderValue: Float): Float = MIN_ZOOM * B.pow(sliderValue)

    fun zoomToSlider(zoomFactor: Float): Float {
        if (zoomFactor <= MIN_ZOOM) return 0f
        if (zoomFactor >= MAX_ZOOM) return 100f
        return (ln(zoomFactor / MIN_ZOOM) / ln(B))
    }
}