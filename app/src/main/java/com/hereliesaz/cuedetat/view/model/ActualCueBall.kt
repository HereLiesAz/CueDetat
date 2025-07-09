package com.hereliesaz.cuedetat.view.model

import android.graphics.PointF

/**
 * The user-positioned aiming sight.
 */
data class ActualCueBall(
    override val center: PointF,
    override val radius: Float
) : LogicalCircular