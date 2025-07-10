package com.hereliesaz.cuedetat.view.model

import android.graphics.PointF

/**
 * The data model for the user-controlled, draggable ball on the logical plane.
 * It represents the ball placed by the user, whether it's acting as the
 * cue ball in Protractor mode or the object ball in Banking mode.
 */
data class OnPlaneBall(
    override val center: PointF,
    override val radius: Float
) : LogicalCircular