// app/src/main/java/com/hereliesaz/cuedetat/view/renderer/util/DrawingUtils.kt
package com.hereliesaz.cuedetat.view.renderer.util

import android.graphics.Matrix
import android.graphics.PointF
import com.hereliesaz.cuedetat.view.model.ILogicalBall
import com.hereliesaz.cuedetat.view.state.OverlayState
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object DrawingUtils {

    data class PerspectiveRadiusInfo(val radius: Float, val lift: Float)

    fun getPerspectiveRadiusAndLift(
        ball: ILogicalBall,
        state: OverlayState
    ): PerspectiveRadiusInfo {
        if (!state.hasInverseMatrix) return PerspectiveRadiusInfo(ball.radius, 0f)

        // Map the logicalPosition to screen coordinates
        val screenCenter = mapPoint(ball.logicalPosition, state.pitchMatrix)
        val logicalHorizontalEdge = PointF(ball.logicalPosition.x + ball.radius, ball.logicalPosition.y)
        val screenHorizontalEdge = mapPoint(logicalHorizontalEdge, state.pitchMatrix)
        val radiusOnScreen = distance(screenCenter, screenHorizontalEdge)
        val lift = radiusOnScreen * abs(sin(Math.toRadians(state.pitchAngle.toDouble()))).toFloat()
        return PerspectiveRadiusInfo(radiusOnScreen, lift)
    }

    fun mapPoint(p: PointF, m: Matrix): PointF {
        val arr = floatArrayOf(p.x, p.y)
        m.mapPoints(arr)
        return PointF(arr[0], arr[1]) // Corrected: Return arr[1] for Y coordinate
    }

    fun distance(p1: PointF, p2: PointF): Float =
        sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))
}