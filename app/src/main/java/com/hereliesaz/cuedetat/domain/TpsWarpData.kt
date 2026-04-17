// app/src/main/java/com/hereliesaz/cuedetat/domain/TpsWarpData.kt
package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import androidx.annotation.Keep

/**
 * Serializable TPS residual control points.
 *
 * srcPoints: 6 homography-estimated logical positions (where H maps image points to)
 * dstPoints: 6 true logical positions (known from table model)
 *
 * Used in two directions:
 * - Forward (src→dst): CV pipeline — corrects homography-estimated logical → true logical
 * - Inverse (dst→src): Rendering — corrects true logical point for drawing inside pitchMatrix
 *
 * Weights are NOT stored here. ThinPlateSpline solves them lazily and caches in a WeakHashMap.
 *
 * Serialized via Gson default reflection. PointF has public float fields x/y which Gson handles
 * without a custom adapter (same implicit behavior used by viewOffset, bankingAimTarget, etc. in
 * CueDetatState). Note: if Gson is replaced with a stricter library, a PointF adapter will be needed.
 */
@Keep
data class TpsWarpData(
    val srcPoints: List<PointF>,
    val dstPoints: List<PointF>
)
