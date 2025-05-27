package com.hereliesaz.cuedetat.geometry.models

import android.graphics.PointF

data class ProjectedCoords(
    val targetProjected: PointF,
    val cueProjected: PointF,
    val targetScreenRadius: Float,
    val cueScreenRadius: Float
)