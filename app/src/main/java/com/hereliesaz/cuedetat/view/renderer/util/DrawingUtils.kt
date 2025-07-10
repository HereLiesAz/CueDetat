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
        state: OverlayState
    ): PerspectiveRadiusInfo {
        if (!state.hasInverseMatrix) return PerspectiveRadiusInfo(logicalRadius, 0f)

        // The righteous path: Use matrix.mapRadius to get an average scaled radius,
        // which is robust against rotation and perspective distortion. This is the one true way.
        val radiusOnScreen = state.pitchMatrix.mapRadius(logicalRadius)

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