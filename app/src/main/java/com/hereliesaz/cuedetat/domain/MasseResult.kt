package com.hereliesaz.cuedetat.domain

import android.graphics.PointF

data class MasseResult(
    val points: List<PointF>,
    val pocketIndex: Int?,          // null = no pocket reached; stays nullable
    val impactPoints: List<PointF> = emptyList()
)
