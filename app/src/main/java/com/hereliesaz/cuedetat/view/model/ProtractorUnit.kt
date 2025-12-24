package com.hereliesaz.cuedetat.view.model

import android.graphics.PointF
import kotlin.math.cos
import kotlin.math.sin
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS

data class ProtractorUnit(
    override val center: PointF,
    override val radius: Float,
    val rotationDegrees: Float
) : LogicalCircular {
    val ghostCueBallCenter: PointF
        get() {
            // Distance from target ball (center) to ghost ball is usually 2 * radius (touching)
            val distance = 2 * radius
            // rotationDegrees: 0 is straight shot.
            // Convert to radians
            val radians = Math.toRadians(rotationDegrees.toDouble())
            val dx = distance * sin(radians).toFloat()
            val dy = distance * cos(radians).toFloat()

            return PointF(
                center.x - dx,
                center.y + dy
            )
        }
}
