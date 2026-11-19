package com.hereliesaz.cuedetat.domain

import com.hereliesaz.cuedetat.data.FullOrientation
import kotlin.math.PI
import kotlin.math.abs

/**
 * Smooths the device orientation: heavily while the phone is nearly still, hardly at all while it is
 * being moved. A One Euro filter on each axis: the cutoff frequency rises with how fast *that axis* is
 * turning, so the smoothing follows the motion instead of a guess about it.
 *
 * It replaces a per-sample exponential average whose strength was a step function of how far the
 * output lagged (and only on pitch and roll), applied once per *sample*. That made it a different
 * filter at the 15 Hz and the 50 Hz the sensor runs at, a slow multi-second drag on a turn that
 * changed only yaw, and a filter that could switch tiers in the middle of a slow pan.
 *
 * Here everything is a function of time: `alpha = dt / (dt + 1 / (2 pi cutoff))`, so the same motion
 * gives the same output at any sample rate, and yaw has its own speed and its own cutoff.
 *
 * Constants, and what they mean at the phone:
 *  - [minCutoffHz] 0.1: nearly still, a time constant of about 1.6 s. Noise on a phone resting on a
 *    rail is smoothed to nothing; slow settling is invisible.
 *  - [beta] 0.25: the cutoff grows by 0.25 Hz for each degree per second. 5 degrees a second (a slow,
 *    careful tilt) gives 1.35 Hz, a 0.12 s time constant and about half a degree of lag; 20 degrees a
 *    second (a deliberate tilt) gives 5.1 Hz, 31 ms. The old filter lagged about 1.3 degrees at 5
 *    degrees a second. Chosen on a synthetic sweep: noise at rest barely changes between 0.1 and 0.25
 *    (0.006 against 0.005 degrees for 0.05 degrees of input noise) while the lag halves.
 *  - [maxCutoffHz] 8: a flick is passed with under 20 ms of lag.
 *  - [derivativeCutoffHz] 1: the speed estimate is itself smoothed, so noise does not look like motion.
 *
 * A gap of more than [RESTART_AFTER_SECONDS] between samples (the sensor was off) restarts the filter
 * at the raw value. Yaw and roll wrap at 180 degrees and are filtered along the shortest arc.
 *
 * Pitch is returned as given (the caller applies the app's sign convention).
 */
class OrientationFilter(
    private val minCutoffHz: Float = 0.1f,
    private val beta: Float = 0.25f,
    private val maxCutoffHz: Float = 8f,
    private val derivativeCutoffHz: Float = 1f,
) {

    private class Axis(val wraps: Boolean) {
        var value = 0f
        var previousRaw = 0f
        var speed = 0f
        var lastNanos = 0L
        var started = false
    }

    init {
        require(minCutoffHz > 0f && maxCutoffHz >= minCutoffHz && derivativeCutoffHz > 0f && beta >= 0f) {
            "cutoffs must be positive and ordered"
        }
    }

    private val yaw = Axis(wraps = true)
    private val pitch = Axis(wraps = false)
    private val roll = Axis(wraps = true)

    /** The fastest any axis is turning right now, in degrees a second (smoothed). */
    val speedDegPerSecond: Float
        get() = maxOf(abs(yaw.speed), abs(pitch.speed), abs(roll.speed))

    /** The filtered orientation for a raw one taken at [timestampNanos] (any clock, only differences count). */
    fun update(rawYaw: Float, rawPitch: Float, rawRoll: Float, timestampNanos: Long): FullOrientation =
        FullOrientation(
            yaw = step(yaw, rawYaw, timestampNanos),
            pitch = step(pitch, rawPitch, timestampNanos),
            roll = step(roll, rawRoll, timestampNanos),
        )

    private fun step(axis: Axis, raw: Float, nanos: Long): Float {
        // A reading that is not a number is no reading: keep what we have rather than lodge it forever.
        if (!raw.isFinite()) return if (axis.started) axis.value else 0f
        val dt = (nanos - axis.lastNanos) / 1e9f
        // The same timestamp twice (or one out of order) is not a new sample: change nothing.
        if (axis.started && dt <= 0f) return axis.value
        if (!axis.started || dt > RESTART_AFTER_SECONDS) {
            axis.started = true
            axis.value = raw
            axis.previousRaw = raw
            axis.speed = 0f
            axis.lastNanos = nanos
            return raw
        }
        val rawSpeed = difference(axis, raw, axis.previousRaw) / dt
        axis.speed += smoothing(derivativeCutoffHz, dt) * (rawSpeed - axis.speed)

        val cutoff = (minCutoffHz + beta * abs(axis.speed)).coerceAtMost(maxCutoffHz)
        axis.value = wrap(axis, axis.value + smoothing(cutoff, dt) * difference(axis, raw, axis.value))

        axis.previousRaw = raw
        axis.lastNanos = nanos
        return axis.value
    }

    /** `a - b`, along the shortest arc for an axis that wraps. */
    private fun difference(axis: Axis, a: Float, b: Float): Float {
        var d = a - b
        if (axis.wraps) {
            d %= 360f
            if (d > 180f) d -= 360f
            if (d < -180f) d += 360f
        }
        return d
    }

    private fun wrap(axis: Axis, value: Float): Float {
        if (!axis.wraps) return value
        var v = value
        if (v > 180f) v -= 360f
        if (v <= -180f) v += 360f
        return v
    }

    /** The share of the way to the raw value taken in [dt] seconds at [cutoffHz]. */
    private fun smoothing(cutoffHz: Float, dt: Float): Float {
        val tau = 1f / (2f * PI.toFloat() * cutoffHz)
        return dt / (dt + tau)
    }

    companion object {
        /** A gap this long between samples means the sensor was off: start again from the raw value. */
        const val RESTART_AFTER_SECONDS = 1.0f
    }
}
