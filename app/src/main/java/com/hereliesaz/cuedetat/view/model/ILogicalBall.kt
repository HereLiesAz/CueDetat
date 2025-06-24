// app/src/main/java/com/hereliesaz/cuedetat/view/model/ILogicalBall.kt
package com.hereliesaz.cuedetat.view.model

import android.graphics.PointF

/**
 * Interface for objects that represent a ball in the logical (table) space.
 * 'logicalPosition' here refers to the center of the ball in the logical coordinate system.
 */
interface ILogicalBall {
    val logicalPosition: PointF // The logical (table-space) center of the ball
    val radius: Float // The radius of the ball in logical units
}