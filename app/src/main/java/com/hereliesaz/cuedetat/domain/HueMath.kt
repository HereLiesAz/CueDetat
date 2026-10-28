package com.hereliesaz.cuedetat.domain

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Hue is an angle, not a number line: a red cloth has hues on both sides of the 0/180 seam
 * (OpenCV's 8-bit hue runs 0..180 for the colour wheel's 0..360 degrees), and the average of 170
 * and 10 is 0, not 90. Everything that averages a hue or builds a hue window goes through here.
 *
 * Pure Kotlin; no OpenCV types, so it runs in plain JUnit.
 */
object HueMath {

    /** Hue units in a full turn (OpenCV 8-bit hue). */
    const val PERIOD = 180

    /** Circular mean and standard deviation of a hue distribution, in hue units. */
    data class Circular(val mean: Double, val std: Double)

    /**
     * Circular mean and spread of a hue histogram ([PERIOD] bins, bin i counting hue i).
     * Null when it is empty or too evenly spread to have a mean (no hue stands out).
     */
    fun circularStats(histogram: FloatArray): Circular? {
        if (histogram.size != PERIOD) return null
        var sumCos = 0.0
        var sumSin = 0.0
        var total = 0.0
        for (i in 0 until PERIOD) {
            val n = histogram[i].toDouble()
            if (n <= 0.0) continue
            val angle = 2.0 * PI * i / PERIOD
            sumCos += n * cos(angle)
            sumSin += n * sin(angle)
            total += n
        }
        if (total <= 0.0) return null
        val resultant = hypot(sumCos, sumSin) / total
        if (resultant < MIN_RESULTANT) return null
        val mean = (atan2(sumSin, sumCos) * PERIOD / (2.0 * PI) + PERIOD) % PERIOD
        // Circular standard deviation, sqrt(-2 ln R) radians, in hue units.
        val std = sqrt(-2.0 * ln(resultant.coerceAtMost(1.0 - 1e-12))) * PERIOD / (2.0 * PI)
        return Circular(mean, std)
    }

    /**
     * The hue ranges (inclusive, within 0..[PERIOD]) covering [center] +/- [halfWidth], one range
     * normally, two when the window crosses the seam. A window of half a turn or more is the
     * whole wheel.
     */
    fun windows(center: Double, halfWidth: Double): List<ClosedFloatingPointRange<Double>> {
        if (halfWidth >= PERIOD / 2.0) return listOf(0.0..PERIOD.toDouble())
        val lo = center - halfWidth
        val hi = center + halfWidth
        return when {
            lo < 0.0 -> listOf(0.0..hi, (lo + PERIOD)..PERIOD.toDouble())
            hi > PERIOD -> listOf(lo..PERIOD.toDouble(), 0.0..(hi - PERIOD))
            else -> listOf(lo..hi)
        }
    }

    /** Shortest distance between two hues, 0..[PERIOD]/2. */
    fun distance(a: Double, b: Double): Double {
        val d = kotlin.math.abs(a - b) % PERIOD
        return minOf(d, PERIOD - d)
    }

    /** Below this the hues point every way and "the" hue means nothing. */
    private const val MIN_RESULTANT = 0.05
}
