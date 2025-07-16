// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/util/DrawingUtils.kt
package com.hereliesaz.cuedetat.view.renderer.util

import android.graphics.Matrix
import android.graphics.PointF
import com.hereliesaz.cuedetat.view.state.OverlayState
import kotlin.math.abs
import kotlin.math.sin

object DrawingUtils {

    data class RadiusInfo(val radius: Float, val lift: Float)

    fun getPerspectiveRadiusAndLift(
        logicalCenter: PointF,
        logicalRadius: Float,
        state: OverlayState,
        matrix: Matrix? = null
    ): RadiusInfo {
        val m = matrix ?: state.pitchMatrix
        if (!state.hasInverseMatrix) return RadiusInfo(logicalRadius, 0f)

        val screenCenter = mapPoint(logicalCenter, m)
        val screenTop = mapPoint(PointF(logicalCenter.x, logicalCenter.y - logicalRadius), m)

        val screenRadius = screenCenter.y - screenTop.y
        val liftAmount = screenRadius * abs(sin(Math.toRadians(state.pitchAngle.toDouble()))).toFloat()

        return RadiusInfo(screenRadius, liftAmount)
    }

    fun mapPoint(point: PointF, matrix: Matrix): PointF {
        val pts = floatArrayOf(point.x, point.y)
        matrix.mapPoints(pts)
        return PointF(pts[0], pts[1])
    }
}