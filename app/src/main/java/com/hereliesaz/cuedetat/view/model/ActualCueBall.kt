// app/src/main/java/com/hereliesaz/cuedetat/view/model/ActualCueBall.kt
package com.hereliesaz.cuedetat.view.model

import android.graphics.PointF

/**
 * Represents the actual cue ball's state.
 * It holds both screen-space and logical (table-space) coordinates.
 */
data class ActualCueBall(
    val screenCenter: PointF, // Screen coordinates of the ball's center
    override val radius: Float, // Radius of the ball in logical units (pixels based on zoom)
    override val logicalPosition: PointF // Logical (table-space) coordinates of the ball's center
) : ILogicalBall