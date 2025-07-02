package com.hereliesaz.cuedetatlite.view.model

import android.graphics.PointF

/**
 * Represents the cue ball as detected in the camera feed.
 */
data class ActualCueBall(
    override val logicalPosition: PointF,
    override val radius: Float
) : ILogicalBall // FIX: Implement the correctly capitalized interface