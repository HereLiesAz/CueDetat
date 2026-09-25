package com.hereliesaz.cuedetat.domain

import kotlin.math.abs

/**
 * When the training-capture mode ([com.hereliesaz.cuedetat.data.CaptureRecorder]) keeps a frame,
 * and how raw camera pixels map to the saved (upright) image.
 *
 * A frame is kept when enough time has passed since the last one, the phone is steady (turning
 * slower than [MAX_TURN_DEG_PER_S]; a swinging phone means motion blur and a pose that doesn't
 * match the pixels), and the image is sharp (variance of the Laplacian at least [MIN_SHARPNESS]).
 *
 * Pure Kotlin: runs in plain JUnit.
 */
object CaptureSampler {

    /** Minimum time between kept frames. */
    const val INTERVAL_MS = 2_000L

    /** Fastest the phone may turn, on any axis, for a frame to count as steady. */
    const val MAX_TURN_DEG_PER_S = 10f

    /** Readings further apart than this say nothing about steadiness now. */
    const val MAX_READING_GAP_MS = 1_000L

    /** Variance of the Laplacian (grey, ~480 px wide) below which a frame is too blurred. */
    const val MIN_SHARPNESS = 60.0

    fun due(nowMs: Long, lastKeptMs: Long): Boolean = nowMs - lastKeptMs >= INTERVAL_MS

    /**
     * @param previous yaw, pitch, roll (degrees) at [previousMs]
     * @param current yaw, pitch, roll (degrees) at [nowMs]
     */
    fun steady(previous: FloatArray?, previousMs: Long, current: FloatArray, nowMs: Long): Boolean {
        if (previous == null) return false
        val dtMs = nowMs - previousMs
        if (dtMs <= 0 || dtMs > MAX_READING_GAP_MS) return false
        val maxTurn = (0 until 3).maxOf { angleDiff(previous[it], current[it]) }
        return maxTurn / (dtMs / 1000f) <= MAX_TURN_DEG_PER_S
    }

    fun sharpEnough(laplacianVariance: Double): Boolean = laplacianVariance >= MIN_SHARPNESS

    /** Smallest difference between two angles in degrees, 0..180 (yaw wraps at ±180). */
    fun angleDiff(a: Float, b: Float): Float {
        val d = abs(a - b) % 360f
        return if (d > 180f) 360f - d else d
    }

    /**
     * A raw-frame point (frame [width] x [height], sensor orientation) in the frame rotated
     * clockwise by [rotationDegrees] to upright.
     */
    fun toUpright(x: Float, y: Float, width: Int, height: Int, rotationDegrees: Int): Pair<Float, Float> =
        when (((rotationDegrees % 360) + 360) % 360) {
            90 -> (height - y) to x
            180 -> (width - x) to (height - y)
            270 -> y to (width - x)
            else -> x to y
        }

    /** Size of the upright image for a raw frame. */
    fun uprightSize(width: Int, height: Int, rotationDegrees: Int): Pair<Int, Int> =
        if (((rotationDegrees % 360) + 360) % 360 in setOf(90, 270)) height to width else width to height
}
