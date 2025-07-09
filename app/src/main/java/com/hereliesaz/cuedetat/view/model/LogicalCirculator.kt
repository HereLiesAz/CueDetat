package com.hereliesaz.cuedetat.view.model

import android.graphics.PointF

/**
 * Defines the contract for any circular object on the logical 2D plane.
 * It ensures the object has a position (center) and a size (radius).
 */
interface LogicalCircular {
    val center: PointF
    val radius: Float
}