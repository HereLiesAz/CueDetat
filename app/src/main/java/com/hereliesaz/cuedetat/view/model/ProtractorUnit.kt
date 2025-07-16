// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/model/ProtractorUnit.kt
package com.hereliesaz.cuedetat.view.model

import android.graphics.PointF

data class ProtractorUnit(
    override val center: PointF = PointF(0f, 0f),
    override val radius: Float = 100f,
    val rotationDegrees: Float = 0f,
) : LogicalCircular {
    val ghostCueBallCenter: PointF
        get() {
            val angleRad = Math.toRadians(rotationDegrees.toDouble())
            return PointF(
                center.x - (radius * 2 * kotlin.math.cos(angleRad)).toFloat(),
                center.y - (radius * 2 * kotlin.math.sin(angleRad)).toFloat()
            )
        }

    fun asOnPlaneBall(): OnPlaneBall {
        return OnPlaneBall(
            center = this.center,
            radius = this.radius
        )
    }
}