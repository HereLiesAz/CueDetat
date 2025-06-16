// app/src/main/java/com/hereliesaz/cuedetat/view/model/LogicalPlane.kt
package com.hereliesaz.cuedetat.view.model

import android.graphics.PointF

// A standard billiard ball is 2.25 inches in diameter.
const val STANDARD_BALL_DIAMETER = 2.25f
const val STANDARD_BALL_RADIUS = STANDARD_BALL_DIAMETER / 2f

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
    val rotationDegrees: Float,
    // Radius is now constant based on real-world dimensions
    override val radius: Float = STANDARD_BALL_RADIUS
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
    // Radius is now constant based on real-world dimensions
    override val radius: Float = STANDARD_BALL_RADIUS
) : ILogicalBall
