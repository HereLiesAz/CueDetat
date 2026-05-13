package com.hereliesaz.cuedetat.network

import androidx.annotation.Keep

/**
 * Output of the on-device MYRIAD student model. The package path is preserved
 * for backwards compatibility with existing imports across the codebase, even
 * though no network call is involved any more.
 */
@Keep
data class TrajectoryPoint(
    val x: Float,
    val y: Float,
)

@Keep
data class TrajectoryResponse(
    val points: List<TrajectoryPoint>,
    val confidence: Float,
)
