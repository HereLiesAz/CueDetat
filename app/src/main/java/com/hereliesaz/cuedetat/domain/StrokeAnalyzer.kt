package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

data class StrokeProfile(
    val time: Long,
    val acceleration: FloatArray, // 3-axis accel
    val gyroscope: FloatArray // 3-axis gyro
)

data class AnalyticsResult(
    val fluidityScore: Float,
    val twistPenalty: Float,
    val isErratic: Boolean
)

/**
 * Domain-level engine to analyze the user's stroke mechanics via high-frequency IMU telemetry.
 * Leverages Dynamic Time Warping (DTW) to allow speed-invariant baseline comparisons, 
 * heavily penalizes lateral twist in the wrist (gyro deviation), and factors in HR state.
 */
class StrokeAnalyzer {

    // Simplified DTW to find distance between two 1D sequences
    private fun computeDTWDistance(seq1: FloatArray, seq2: FloatArray): Float {
        val n = seq1.size
        val m = seq2.size
        if (n == 0 || m == 0) return 0f

        val dtw = Array(n + 1) { FloatArray(m + 1) { Float.MAX_VALUE } }
        dtw[0][0] = 0f

        for (i in 1..n) {
            for (j in 1..m) {
                val cost = abs(seq1[i - 1] - seq2[j - 1])
                val minPrev = min(dtw[i - 1][j], min(dtw[i][j - 1], dtw[i - 1][j - 1]))
                dtw[i][j] = cost + minPrev
            }
        }
        return dtw[n][m] / (n + m).toFloat() // Normalized distance
    }

    fun analyzeStroke(
        baseline: List<StrokeProfile>,
        current: List<StrokeProfile>,
        heartRate: Float
    ): AnalyticsResult {
        // Extract forward acceleration (assuming Y-axis is along the cue stick)
        val baselineY = baseline.map { it.acceleration[1] }.toFloatArray()
        val currentY = current.map { it.acceleration[1] }.toFloatArray()

        // 1. Fluidity via DTW
        val dtwDist = computeDTWDistance(baselineY, currentY)
        // Convert distance to a 0.0 - 1.0 score (arbitrary scaling for the prototype)
        val fluidityScore = (1.0f - (dtwDist / 20f)).coerceIn(0f, 1f)

        // 2. Lateral Twist Penalty
        // Sum up absolute deviation on roll/yaw axes (X and Z gyro)
        var totalTwist = 0f
        current.forEach { 
            totalTwist += abs(it.gyroscope[0]) + abs(it.gyroscope[2])
        }
        val twistPenalty = (totalTwist / current.size.coerceAtLeast(1)).coerceIn(0f, 1f)

        // 3. Erratic/Shake check
        // High HR lowers the threshold for what we consider an erratic stroke
        val hrThreshold = if (heartRate > 120f) 0.5f else 1.5f
        val isErratic = twistPenalty > hrThreshold || dtwDist > (hrThreshold * 10f)

        return AnalyticsResult(
            fluidityScore = fluidityScore,
            twistPenalty = twistPenalty,
            isErratic = isErratic
        )
    }
}
