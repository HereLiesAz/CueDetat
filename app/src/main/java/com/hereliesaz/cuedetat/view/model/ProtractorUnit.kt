// app/src/main/java/com/hereliesaz/cuedetat/view/model/ProtractorUnit.kt
package com.hereliesaz.cuedetat.view.model

import android.graphics.PointF
import kotlin.math.cos
import kotlin.math.sin

/**
 * Represents the protractor unit, including its center (target ball), radius, and rotation.
 * It holds both screen-space and logical (table-space) coordinates for its center.
 * It also defines the position of the ghost cue ball relative to its logical center.
 */
data class ProtractorUnit(
    override val radius: Float = 100f, // Radius of the protractor (Target Ball) in logical units
    val rotationDegrees: Float = 0f, // Rotation of the protractor
    override val logicalPosition: PointF = PointF(0f, 0f) // Logical (table-space) coordinates of the protractor's center
) : ILogicalBall {
    // The ghost cue ball's logical position is always a fixed distance along the logical Y-axis
    // from the protractor's logical center in its unrotated local space, then rotated.
    val protractorCueBallLogicalCenter: PointF
        get() {
            // Start with the local position of the ghost cue ball relative to the target ball center (0,0)
            // when rotation is 0. It's 2 * radius 'down' the y-axis.
            val angleRad = Math.toRadians((rotationDegrees - 90).toDouble()) // -90 to align with canvas rotation
            val offsetX = (2 * radius * cos(angleRad)).toFloat()
            val offsetY = (2 * radius * sin(angleRad)).toFloat()

            // Add the rotated offset to the logical center of the target ball.
            return PointF(logicalPosition.x + offsetX, logicalPosition.y + offsetY)
        }
}