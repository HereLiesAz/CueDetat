package com.hereliesaz.cuedetat.domain

import com.hereliesaz.cuedetat.data.FullOrientation
import kotlin.math.abs

/**
 * Whether a new device orientation is worth a reduce, a matrix rebuild, an aim recompute and a
 * redraw. The sensor filter's output moves a hair on every sample even with the phone on a table,
 * and every one of those used to run the whole pipeline.
 *
 * The comparison is against the orientation last *applied*, not the last one seen, so slow drift
 * still gets through once it adds up to more than [EPSILON_DEG]; it arrives as a step that small.
 *
 * [EPSILON_DEG] is 0.03 degrees. A phone camera covers roughly 60 degrees over about 2000 pixels, so
 * 0.03 degrees is about one pixel: below it nothing on screen could move by a visible amount.
 * Yaw and roll are compared along the shortest arc (they wrap at 180 degrees).
 */
object OrientationGate {

    const val EPSILON_DEG = 0.03f

    /** True when [next] differs enough from [applied] to be worth applying (always true with none applied). */
    fun worthApplying(applied: FullOrientation?, next: FullOrientation): Boolean {
        if (applied == null) return true
        return abs(arc(next.yaw, applied.yaw)) > EPSILON_DEG ||
            abs(next.pitch - applied.pitch) > EPSILON_DEG ||
            abs(arc(next.roll, applied.roll)) > EPSILON_DEG
    }

    /** Signed shortest-arc difference `a - b` in degrees, in [-180, 180]. */
    private fun arc(a: Float, b: Float): Float {
        var d = a - b
        while (d > 180f) d -= 360f
        while (d < -180f) d += 360f
        return d
    }
}
