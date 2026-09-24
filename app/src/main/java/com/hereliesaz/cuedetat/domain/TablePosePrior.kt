package com.hereliesaz.cuedetat.domain

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Chooses the starting guess for the table's on-screen pose from three memories, each trusted
 * to a different degree:
 *
 * 1. **This session**: the last confirmed pose, carried to the phone's current heading. Trusted
 *    most while fresh; fades over [SESSION_FADE_MS].
 * 2. **This location**: every pose confirmed within [SAME_TABLE_METERS] of here, fed to
 *    [TableOrientationLearner].
 * 3. **The last table anywhere**: the most recently used table's poses, at half trust. Right
 *    when the user plays at one table; a weak hint otherwise.
 *
 * The most confident wins. Pure Kotlin; the Android side only supplies the samples.
 */
object TablePosePrior {

    const val SESSION_FADE_MS = 10 * 60 * 1000L
    const val SAME_TABLE_METERS = 75.0
    const val LAST_TABLE_TRUST = 0.5f

    enum class Source { SESSION, LOCATION, LAST_TABLE }

    data class Prior(val prediction: TableOrientationLearner.Prediction, val source: Source)

    fun choose(
        session: TablePoseSample?,
        atLocation: List<TablePoseSample>?,
        lastTable: List<TablePoseSample>?,
        yawDeg: Float,
        pitchDeg: Float,
        nowMs: Long,
    ): Prior? {
        val candidates = ArrayList<Prior>(3)

        session?.let { s ->
            val age = (nowMs - s.timestampMs).coerceAtLeast(0L)
            if (age < SESSION_FADE_MS) {
                // The table stays put; the phone turned. Same rule as the learner (sign -1).
                val rotation = TableOrientationLearner.normalize180(s.rotationDeg - (yawDeg - s.yawDeg))
                val trust = 1f - age.toFloat() / SESSION_FADE_MS
                candidates += Prior(TableOrientationLearner.Prediction(rotation, s.zoom, trust), Source.SESSION)
            }
        }
        atLocation?.let { samples ->
            TableOrientationLearner.predict(samples, yawDeg, pitchDeg)?.let { candidates += Prior(it, Source.LOCATION) }
        }
        lastTable?.let { samples ->
            TableOrientationLearner.predict(samples, yawDeg, pitchDeg)?.let {
                candidates += Prior(it.copy(confidence = it.confidence * LAST_TABLE_TRUST), Source.LAST_TABLE)
            }
        }
        return candidates.maxByOrNull { it.prediction.confidence }
    }

    /** Great-circle distance in metres. */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * r * asin(sqrt(a))
    }
}
