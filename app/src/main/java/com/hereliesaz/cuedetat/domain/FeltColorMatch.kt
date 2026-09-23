package com.hereliesaz.cuedetat.domain

import kotlin.math.abs
import kotlin.math.min

/**
 * Decides whether a sampled colour is the captured felt.
 *
 * Both colours are Android HSV (hue 0..360, saturation 0..1, value 0..1). Hue carries the
 * identity of the cloth and survives lighting changes, so it is held tight. Saturation and value
 * move with shadow and glare across a table, so they only have to stay in the felt's
 * neighbourhood. Pure Kotlin: no Android or OpenCV types, so it runs in plain JUnit.
 */
object FeltColorMatch {

    /** Max circular hue distance, in degrees. */
    const val HUE_TOLERANCE_DEG = 20f

    /** Sample saturation must be at least this fraction of the felt's. */
    const val MIN_SATURATION_RATIO = 0.5f

    /** Sample brightness must be at least this fraction of the felt's (shadowed cloth). */
    const val MIN_VALUE_RATIO = 0.3f

    /** Circular distance between two hues, 0..180. */
    fun hueDistance(a: Float, b: Float): Float {
        val d = abs(a - b) % 360f
        return min(d, 360f - d)
    }

    fun matches(sample: FloatArray, felt: FloatArray): Boolean {
        if (sample.size < 3 || felt.size < 3) return false
        return hueDistance(sample[0], felt[0]) <= HUE_TOLERANCE_DEG &&
            sample[1] >= felt[1] * MIN_SATURATION_RATIO &&
            sample[2] >= felt[2] * MIN_VALUE_RATIO
    }
}
