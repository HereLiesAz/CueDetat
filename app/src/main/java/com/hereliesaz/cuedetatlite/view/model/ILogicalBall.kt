// hereliesaz/cuedetat/CueDetat-CueDetatLite/app/src/main/java/com/hereliesaz/cuedetatlite/view/model/IlogicalBall.kt
package com.hereliesaz.cuedetatlite.view.model

import android.graphics.PointF

/**
 * Interface for objects that represent a ball in the logical (table) space.
 * 'logicalPosition' here refers to the center of the ball in the logical coordinate system.
 */
interface IlogicalBall {
    val logicalPosition: PointF // The logical (table-space) center of the ball
    val radius: Float // The radius of the ball in logical units
}
