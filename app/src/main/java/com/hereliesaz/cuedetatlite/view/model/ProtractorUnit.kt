package com.hereliesaz.cuedetatlite.view.model

import android.graphics.PointF

data class ProtractorUnit(
    val targetBall: ILogicalBall = LogicalBall(PointF(100f, 100f), 30f),
    val aimingAngleDegrees: Float = 0f // This now controls the ghost ball's position
) {
    data class LogicalBall(
        override val logicalPosition: PointF,
        override val radius: Float
    ) : ILogicalBall
}