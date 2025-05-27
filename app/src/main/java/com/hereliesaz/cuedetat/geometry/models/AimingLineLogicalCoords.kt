package com.hereliesaz.cuedetat.geometry.models

import android.graphics.PointF // Not strictly needed here but good for consistency

data class AimingLineLogicalCoords(
    val startX: Float, val startY: Float,
    val cueX: Float, val cueY: Float,
    val endX: Float, val endY: Float,
    val normDirX: Float, val normDirY: Float
)