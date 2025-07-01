// hereliesaz/cuedetat/CueDetat-CueDetatLite/app/src/main/java/com/hereliesaz/cuedetatlite/view/model/ProtractorUnit.kt
package com.hereliesaz.cuedetatlite.view.model

import android.graphics.PointF

data class ProtractorUnit(
    val cueBall: IlogicalBall = LogicalBall(PointF(0f, 0f), 30f),
    val targetBall: IlogicalBall = LogicalBall(PointF(100f, 100f), 30f)
) {
    data class LogicalBall(
        override val logicalPosition: PointF,
        override val radius: Float
    ) : IlogicalBall
}
