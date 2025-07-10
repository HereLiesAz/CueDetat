package com.hereliesaz.cuedetat.view.model

import android.graphics.PointF
import kotlin.math.cos
import kotlin.math.sin

/**
 * The data model for the main aiming tool, consisting of a Target Ball and a Ghost Cue Ball.
 * Its position is defined by the Target Ball's center.
 */
data class ProtractorUnit(
    override val center: PointF,
    override val radius: Float,
    val rotationDegrees: Float
) : LogicalCircular {

    companion object {
        const val LOGICAL_BALL_RADIUS = 30f // A fixed logical size for all balls
    }

    /**
     * Calculates the position of the Ghost Cue Ball based on the unit's center and rotation.
     */
    val ghostCueBallCenter: PointF
        get() {
            val angleRad = Math.toRadians(rotationDegrees.toDouble())
            val distance = 2 * radius
            return PointF(
                center.x - (sin(angleRad)).toFloat() * distance,
                center.y + (cos(angleRad)).toFloat() * distance
            )
        }
}