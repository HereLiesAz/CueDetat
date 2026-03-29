// app/src/main/java/com/hereliesaz/cuedetat/view/renderer/TpsUtils.kt
package com.hereliesaz.cuedetat.view.renderer

import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.ThinPlateSpline
import com.hereliesaz.cuedetat.domain.TpsWarpData

/**
 * Applies the inverse residual TPS to a logical draw point.
 * Used inside canvas.withMatrix(pitchMatrix) blocks to correct for lens distortion.
 * If tps is null (no alignment performed), returns this unchanged.
 */
fun PointF.warpedBy(tps: TpsWarpData?): PointF {
    if (tps == null) return this
    val warped = ThinPlateSpline.applyInverseWarp(tps, this)
    return if (warped.x.isNaN() || warped.y.isNaN() || warped.x.isInfinite() || warped.y.isInfinite()) {
        this
    } else {
        warped
    }
}
