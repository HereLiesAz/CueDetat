package com.hereliesaz.cuedetat.domain

import android.graphics.PointF

data class MasseResult(
    val points: List<PointF>,
    val pocketIndex: Int?,          // null = no pocket reached; stays nullable
    val impactPoints: List<PointF> = emptyList(),
    // v1.4 scaffold — both inert until jump activation
    val isAirborne: Boolean = false,
    val peakHeight: Float = 0f
)
