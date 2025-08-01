package com.hereliesaz.cuedetat.view.model

import android.graphics.PointF

/**
 * The data model for the main aiming tool, consisting of a Target Ball and a Ghost Cue Ball.
 * Its position is defined by the Target Ball's center.
 */
data class ProtractorUnit(
    override val center: PointF,
    override val radius: Float,
    val rotationDegrees: Float
) : LogicalCircular {

    /**
     * Calculates the position of the Ghost Cue Ball based on the unit's center and rotation.
     */
    val ghostCueBallCenter: PointF
        get() {
            val angleRad = Math.toRadians(rotationDegrees.toDouble())
            val distance = 2 * radius
            return PointF(
                center.x - (distance * kotlin.math.sin(angleRad)).toFloat(),
                center.y + (distance * kotlin.math.cos(angleRad)).toFloat()
            )
        }
}
