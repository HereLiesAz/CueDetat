package com.hereliesaz.cuedetatlite.view.model

import android.graphics.PointF

/**
 * Represents the cue ball as detected in the camera feed.
 */
data class ActualCueBall(
    override val logicalPosition: PointF, // Renamed from 'center' to match IlogicalBall
    override val radius: Float
) : IlogicalBall // Implement the interface