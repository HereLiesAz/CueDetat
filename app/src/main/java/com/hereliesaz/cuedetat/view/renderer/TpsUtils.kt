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

/**
 * The single lens-warp gate for every renderer.
 *
 * Returns null (no residual TPS warp) when:
 * - the camera is in LITE_AR: the residual TPS was fitted against the full-AR projection, and
 *   LITE_AR draws the plane without it;
 * - the beginner view is locked: that view draws on the flat logicalPlaneMatrix, not the
 *   camera projection the TPS was fitted to.
 *
 * Table, rails, pockets, balls, labels and all lines must use this so they agree.
 */
fun renderLensTps(state: com.hereliesaz.cuedetat.domain.CueDetatState): TpsWarpData? {
    if (state.cameraMode == com.hereliesaz.cuedetat.domain.CameraMode.LITE_AR) return null
    if (state.experienceMode == com.hereliesaz.cuedetat.domain.ExperienceMode.BEGINNER &&
        state.isBeginnerViewLocked) return null
    return state.lensWarpTps
}
