package com.hereliesaz.cuedetat.geometry.models

import android.graphics.PointF // Not strictly needed here but good for consistency

data class AimingLineLogicalCoords(
    val startX: Float, val startY: Float, // This is the *actual* cue ball's projected screen position
    val cueX: Float, val cueY: Float,     // This is the *ghost* cue ball's logical position
    val endX: Float, val endY: Float,     // Extended end of the line
    val normDirX: Float, val normDirY: Float
)