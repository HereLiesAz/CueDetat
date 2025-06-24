// app/src/main/java/com/hereliesaz/cuedetat/view/model/LogicalPlane.kt
package com.hereliesaz.cuedetat.view.model

import android.graphics.PointF

<<<<<<<< HEAD:app/src/main/java/com/hereliesaz/cuedetat/view/model/ProtractorUnit.kt
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
========
// This file is now empty or contains only non-redundant code.
// The ILogicalBall interface definition has been moved to its own file.
>>>>>>>> 646ff5c (Having nightmARes.):app/src/main/java/com/hereliesaz/cuedetat/view/model/LogicalPlane.kt
