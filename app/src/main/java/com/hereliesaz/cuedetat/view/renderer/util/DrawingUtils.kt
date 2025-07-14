package com.hereliesaz.cuedetat.view.renderer.util

import android.graphics.Matrix
import android.graphics.PointF
import com.hereliesaz.cuedetat.view.state.OverlayState
import kotlin.math.abs
import kotlin.math.sin

object DrawingUtils {

    data class PerspectiveRadiusInfo(val radius: Float, val lift: Float)

    fun getPerspectiveRadiusAndLift(
        logicalCenter: PointF,
        logicalRadius: Float,
        state: OverlayState,
        matrix: Matrix? = null // Allow overriding the matrix
    ): PerspectiveRadiusInfo {
        val usedMatrix = matrix ?: state.pitchMatrix
        if (!state.hasInverseMatrix && matrix == null) return PerspectiveRadiusInfo(logicalRadius, 0f)

        // The righteous path: Use the pre-calculated flat matrix from the state
        // to get an average scaled radius that is robust against perspective distortion.
        val radiusOnScreen = state.flatMatrix.mapRadius(logicalRadius)

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