// app/src/main/java/com/hereliesaz/cuedetat/view/model/ProtractorUnit.kt
package com.hereliesaz.cuedetat.view.model

import android.graphics.PointF

/**
 * Represents the protractor unit, including its center (target ball), radius, and rotation.
 * It holds both screen-space and logical (table-space) coordinates for its center.
 * It also defines the position of the ghost cue ball relative to its logical center.
 */
data class ProtractorUnit(
    val screenCenter: PointF = PointF(0f, 0f), // Screen coordinates of the protractor's center (Target Ball)
    override val radius: Float = 100f, // Radius of the protractor (Target Ball) in logical units
    val rotationDegrees: Float = 0f, // Rotation of the protractor
    override val logicalPosition: PointF = PointF(0f, 0f) // Logical (table-space) coordinates of the protractor's center
) : ILogicalBall {
    // The ghost cue ball's logical position is always a fixed distance along the logical Y-axis
    // from the protractor's logical center in its unrotated local space.
    val protractorCueBallLogicalCenter: PointF
        get() = PointF(logicalPosition.x, logicalPosition.y + 2 * radius)
}