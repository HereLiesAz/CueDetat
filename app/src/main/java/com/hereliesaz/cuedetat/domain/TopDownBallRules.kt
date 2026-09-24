package com.hereliesaz.cuedetat.domain

import kotlin.math.asin
import kotlin.math.tan

/**
 * Ball geometry in the top-down (rectified) view of the table.
 *
 * Rectifying the camera frame onto the table plane is exact for the felt, but a ball stands off
 * the felt, so its image is stretched along the direction pointing away from the camera:
 *
 * - **Across** that direction it stays one ball wide (2R, magnified by only a few percent).
 * - **Along** it, it grows by the local stretch `k = 1 / sin(e)`, where `e` is the camera's
 *   elevation above the table at that spot. `k` is read off the rectification itself: the ratio
 *   of table distance covered by one screen pixel up versus one screen pixel across.
 * - Its **near end** is not the contact point. The lowest ray grazing the sphere meets the felt
 *   `R · tan(e / 2)` in front of the contact point (a circle of radius R resting on a line, cut by
 *   a tangent at angle e). So contact = near end + `R · tan(e / 2)` along the away direction.
 *
 * Pure Kotlin; runs in plain JUnit. Lengths in top-down pixels throughout.
 */
object TopDownBallRules {

    enum class Verdict { REJECT, SINGLE, PAIR_ALONG, PAIR_ACROSS }

    /** Camera elevation (radians) from the local stretch k = 1 / sin(e). */
    fun elevationFromStretch(k: Float): Float = asin((1f / k.coerceAtLeast(1f)).coerceIn(0f, 1f))

    /** Distance from a ball's near end to its contact point, in the same units as [radius]. */
    fun contactOffset(radius: Float, elevationRad: Float): Float = radius * tan(elevationRad / 2f)

    /** Longest a single ball's image may be along the away direction. */
    fun maxSingleLength(radius: Float, stretch: Float): Float = 2f * radius * stretch * 1.3f

    /**
     * @param width island extent across the away direction
     * @param length island extent along it
     * @param area island pixel count
     * @param radius ball radius in top-down pixels
     * @param stretch local stretch k (>= 1)
     */
    fun judge(width: Float, length: Float, area: Int, radius: Float, stretch: Float): Verdict {
        if (radius <= 0f || width <= 0f || length <= 0f) return Verdict.REJECT
        if (area / (width * length) < 0.45f) return Verdict.REJECT
        val oneWide = width in 1.2f * radius..2.8f * radius
        val twoWide = width > 2.8f * radius && width <= 5.2f * radius
        val maxOne = maxSingleLength(radius, stretch)
        val oneLong = length in 1.6f * radius..maxOne
        return when {
            oneWide && oneLong -> Verdict.SINGLE
            // One ball partly behind another: the commonest merge at a low angle.
            oneWide && length > maxOne && length <= 2f * maxOne -> Verdict.PAIR_ALONG
            twoWide && oneLong -> Verdict.PAIR_ACROSS
            else -> Verdict.REJECT
        }
    }
}
