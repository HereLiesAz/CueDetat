package com.hereliesaz.cuedetat.domain

import androidx.annotation.Keep

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * One confirmed table pose: what the sensors read and where the user (or a confident snap)
 * put the virtual table. The training example for [TableOrientationLearner] and for any model
 * trained later from the on-disk log.
 *
 * @param yawDeg compass azimuth of the phone, degrees (magnetic north = 0, clockwise)
 * @param pitchDeg phone pitch as the app uses it (`FullOrientation.pitch`), degrees
 * @param rollDeg phone roll, degrees
 * @param rotationDeg `worldRotationDegrees` of the confirmed table
 * @param zoom zoom factor of the confirmed table
 * @param offsetX `viewOffset.x`
 * @param offsetY `viewOffset.y`
 */
@Keep // Serialised with Gson (TablePoseStore); R8 must keep field names.
data class TablePoseSample(
    val yawDeg: Float,
    val pitchDeg: Float,
    val rollDeg: Float,
    val rotationDeg: Float,
    val zoom: Float,
    val offsetX: Float,
    val offsetY: Float,
    val timestampMs: Long,
)

/**
 * Predicts where the table will sit on screen from the phone's sensors and past confirmed poses.
 *
 * A pool table doesn't move, so its compass heading is a constant of the room. The on-screen
 * rotation is that heading minus the phone's heading: turn the phone right and the table turns
 * left on screen. So `rotation = s * yaw + c`, where `c` is learned per table and `s` is -1 by
 * geometry (confirmed from the data once there is enough spread in yaw to tell).
 *
 * Tables look the same turned half way round, so angles are compared modulo 180° (averaged as
 * doubled angles).
 *
 * Zoom follows distance, and people tend to stand the same way at the same table, so it is
 * regressed on pitch when the samples vary enough in pitch; otherwise it is their mean.
 *
 * Pure Kotlin and deterministic: runs in plain JUnit.
 */
object TableOrientationLearner {

    /** Yaw spread (degrees) needed before the data may overrule the geometric sign. */
    const val MIN_YAW_SPREAD_FOR_SIGN = 20f

    /** Pitch spread (degrees) needed to fit zoom against pitch rather than average it. */
    const val MIN_PITCH_SPREAD_FOR_ZOOM_FIT = 5f

    /** Samples needed for full confidence. */
    const val FULL_CONFIDENCE_SAMPLES = 5

    data class Prediction(
        /** Predicted `worldRotationDegrees`. */
        val rotationDeg: Float,
        /** Predicted zoom factor. */
        val zoom: Float,
        /** 0..1: agreement of past samples times how many there are. */
        val confidence: Float,
    )

    fun predict(samples: List<TablePoseSample>, yawDeg: Float, pitchDeg: Float): Prediction? {
        if (samples.isEmpty()) return null

        val sign = chooseSign(samples)
        val (headingDeg, agreement) = circularMean180(samples.map { it.rotationDeg - sign * it.yawDeg })
        val rotation = normalize180(sign * yawDeg + headingDeg)

        val zoom = predictZoom(samples, pitchDeg)
        val count = (samples.size.toFloat() / FULL_CONFIDENCE_SAMPLES).coerceAtMost(1f)
        return Prediction(rotation, zoom, agreement * count)
    }

    /** -1 by geometry; +1 only if the data, spread widely enough in yaw, clearly prefer it. */
    internal fun chooseSign(samples: List<TablePoseSample>): Float {
        val yaws = samples.map { it.yawDeg }
        val spread = circularSpread(yaws)
        if (samples.size < 3 || spread < MIN_YAW_SPREAD_FOR_SIGN) return -1f
        val neg = circularMean180(samples.map { it.rotationDeg + it.yawDeg }).second
        val pos = circularMean180(samples.map { it.rotationDeg - it.yawDeg }).second
        return if (pos > neg + 0.1f) 1f else -1f
    }

    private fun predictZoom(samples: List<TablePoseSample>, pitchDeg: Float): Float {
        val meanZoom = samples.map { it.zoom }.average().toFloat()
        if (samples.size < 3) return meanZoom
        val pitches = samples.map { it.pitchDeg.toDouble() }
        val zooms = samples.map { it.zoom.toDouble() }
        val mp = pitches.average()
        val mz = zooms.average()
        val spread = (pitches.maxOrNull() ?: 0.0) - (pitches.minOrNull() ?: 0.0)
        if (spread < MIN_PITCH_SPREAD_FOR_ZOOM_FIT) return meanZoom
        var sxy = 0.0; var sxx = 0.0
        for (i in pitches.indices) {
            sxy += (pitches[i] - mp) * (zooms[i] - mz)
            sxx += (pitches[i] - mp) * (pitches[i] - mp)
        }
        if (sxx < 1e-9) return meanZoom
        val slope = sxy / sxx
        val predicted = mz + slope * (pitchDeg - mp)
        // Never extrapolate outside what has been seen.
        return predicted.coerceIn(zooms.minOrNull()!!, zooms.maxOrNull()!!).toFloat()
    }

    /**
     * Mean of angles taken modulo 180°, via doubled angles. Returns the mean in (-90, 90] and the
     * resultant length (1 = all agree, 0 = no agreement).
     */
    fun circularMean180(anglesDeg: List<Float>): Pair<Float, Float> {
        if (anglesDeg.isEmpty()) return 0f to 0f
        var s = 0.0; var c = 0.0
        for (a in anglesDeg) {
            val r = Math.toRadians(2.0 * a)
            s += sin(r); c += cos(r)
        }
        s /= anglesDeg.size; c /= anglesDeg.size
        val mean = Math.toDegrees(atan2(s, c)) / 2.0
        return normalize180(mean.toFloat()) to hypot(s, c).toFloat()
    }

    /** Spread of full-circle angles: the circular standard deviation, degrees. */
    private fun circularSpread(anglesDeg: List<Float>): Float {
        var s = 0.0; var c = 0.0
        for (a in anglesDeg) {
            val r = Math.toRadians(a.toDouble())
            s += sin(r); c += cos(r)
        }
        val rLen = (hypot(s, c) / anglesDeg.size).coerceIn(1e-9, 1.0)
        return Math.toDegrees(sqrt(-2.0 * kotlin.math.ln(rLen))).toFloat()
    }

    /** Angle folded into (-90, 90]. */
    fun normalize180(deg: Float): Float {
        var a = deg % 180f
        if (a <= -90f) a += 180f
        if (a > 90f) a -= 180f
        return a
    }

    /** Smallest difference between two angles taken modulo 180°, 0..90. */
    fun diff180(a: Float, b: Float): Float = abs(normalize180(a - b))
}
