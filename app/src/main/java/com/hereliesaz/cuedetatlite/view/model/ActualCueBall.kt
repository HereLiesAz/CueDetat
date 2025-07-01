package com.hereliesaz.cuedetatlite.view.model

import android.graphics.PointF

/**
 * Represents the cue ball as detected in the camera feed.
 */
data class ActualCueBall(
    val center: PointF,
    val radius: Float
)
