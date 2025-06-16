package com.hereliesaz.cuedetat.view.model

import android.graphics.PointF

/**
 * Represents an object with a position and radius on the logical 2D plane.
 */
interface ILogicalBall {
    val center: PointF
    val radius: Float
}

/**
 * The main aiming tool, consisting of a Target Ball and a Protractor Cue Ball.
 * Its position is defined by the Target Ball's center.
 */
data class ProtractorUnit(
    override val center: PointF,
    override val radius: Float,
    val rotationDegrees: Float
) : ILogicalBall {

    /**
     * Calculates the position of the Protractor Cue Ball based on the unit's center and rotation.
     */
    val protractorCueBallCenter: PointF
        get() {
            val angleRad = Math.toRadians(rotationDegrees.toDouble())
            val distance = 2 * radius
            return PointF(
                center.x - (distance * kotlin.math.sin(angleRad)).toFloat(),
                center.y + (distance * kotlin.math.cos(angleRad)).toFloat()
            )
        }
}

/**
 * The user-positioned aiming sight.
 */
data class ActualCueBall(
    override val center: PointF,
    override val radius: Float
) : ILogicalBall
