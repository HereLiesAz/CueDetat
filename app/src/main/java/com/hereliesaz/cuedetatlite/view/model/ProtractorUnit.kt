package com.hereliesaz.cuedetatlite.view.model

import android.graphics.PointF
import kotlin.math.cos
import kotlin.math.sin

data class ProtractorUnit(
    val targetBall: LogicalBall,
    val aimingAngleDegrees: Float = 0f,
) {
    data class LogicalBall(
        override val logicalPosition: PointF,
        override val radius: Float
    ) : ILogicalBall

    val ghostCueBall: LogicalBall
        get() {
            val angleRad = Math.toRadians(aimingAngleDegrees.toDouble()).toFloat()
            val totalRadius = targetBall.radius * 2
            val ghostBallX = targetBall.logicalPosition.x - cos(angleRad) * totalRadius
            val ghostBallY = targetBall.logicalPosition.y - sin(angleRad) * totalRadius
            return LogicalBall(PointF(ghostBallX, ghostBallY), targetBall.radius)
        }
}
