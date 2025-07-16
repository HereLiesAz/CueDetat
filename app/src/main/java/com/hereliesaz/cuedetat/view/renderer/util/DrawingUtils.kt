package com.hereliesaz.cuedetat.view.renderer.util

import android.graphics.Matrix
import android.graphics.PointF
import com.hereliesaz.cuedetat.view.state.OverlayState
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sin

object DrawingUtils {

    data class PerspectiveRadiusInfo(val radius: Float, val lift: Float)

    fun getPerspectiveRadiusAndLift(
        logicalCenter: PointF,
        logicalRadius: Float,
        state: OverlayState,
        matrix: Matrix? = null // Allow overriding the matrix
    ): PerspectiveRadiusInfo {
        // Use the final, zoom-and-pitch-affected matrix to determine the on-screen radius.
        // This ensures the 3D ghost ball's size perfectly matches the 2D shadow's size.
        val usedMatrix = matrix ?: state.pitchMatrix
        if (!state.hasInverseMatrix && matrix == null) return PerspectiveRadiusInfo(logicalRadius, 0f)

        // CORRECT METHOD: Project two points (center and edge) and measure the screen distance.
        // This correctly accounts for perspective and zoom.
        val p1 = floatArrayOf(logicalCenter.x, logicalCenter.y)
        val p2 = floatArrayOf(logicalCenter.x + logicalRadius, logicalCenter.y)
        usedMatrix.mapPoints(p1)
        usedMatrix.mapPoints(p2)
        val radiusOnScreen = hypot((p1[0] - p2[0]).toDouble(), (p1[1] - p2[1]).toDouble()).toFloat()

        // The lift remains a function of the on-screen radius and pitch angle.
        val lift = radiusOnScreen * abs(sin(Math.toRadians(state.pitchAngle.toDouble()))).toFloat()
        return PerspectiveRadiusInfo(radiusOnScreen, lift)
    }

    fun mapPoint(p: PointF, m: Matrix): PointF {
        val arr = floatArrayOf(p.x, p.y)
        m.mapPoints(arr)
        return PointF(arr[0], arr[1])
    }
}