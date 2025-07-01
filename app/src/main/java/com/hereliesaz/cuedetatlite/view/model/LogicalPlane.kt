package com.hereliesaz.cuedetatlite.view.model

import android.graphics.PointF
import kotlin.math.cos
import kotlin.math.sin

object LogicalPlane {

    data class ProtractorUnit(
        val targetBallCenter: PointF = PointF(540f, 960f), // Default to center of a 1080x1920 screen
        override val radius: Float = 100f,
        val rotationDegrees: Float = 0f
    ) : ILogicalBall {
        override val center: PointF
            get() = targetBallCenter

        val protractorCueBallCenter: PointF
            get() {
                val angleRad = Math.toRadians(rotationDegrees.toDouble())
                return PointF(
                    targetBallCenter.x + (2 * radius * cos(angleRad)).toFloat(),
                    targetBallCenter.y + (2 * radius * sin(angleRad)).toFloat()
                )
            }
    }

    data class ActualCueBall(
        override val center: PointF = PointF(540f, 1500f),
        override val radius: Float = 100f
    ) : ILogicalBall
}