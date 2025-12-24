package com.hereliesaz.cuedetat.view.renderer.util

import android.graphics.Matrix
import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.CueDetatState
import kotlin.math.abs
import kotlin.math.sin

data class RadiusInfo(val radius: Float, val lift: Float)

object DrawingUtils {
    fun mapPoint(point: PointF, matrix: Matrix): PointF {
        val points = floatArrayOf(point.x, point.y)
        matrix.mapPoints(points)
        return PointF(points[0], points[1])
    }

    fun getPerspectiveRadiusAndLift(
        center: PointF,
        radius: Float,
        state: CueDetatState,
        matrix: Matrix
    ): RadiusInfo {
        // Map center and a point on the edge to determine visual radius
        val mappedCenter = mapPoint(center, matrix)
        // We use (center.x + radius) to measure horizontal width.
        // Note: Perspective might distort circles into ellipses, but we assume circle for simple rendering.
        val mappedEdge = mapPoint(PointF(center.x + radius, center.y), matrix)
        val visualRadius = abs(mappedEdge.x - mappedCenter.x)

        // Calculate lift based on pitch
        // "Doctrine of Rail Lift: The railLiftAmount calculation... must be proportional to the sine of the pitch angle (lift * abs(sin(pitch)))."
        val pitch = state.currentOrientation.pitch
        val lift = radius * abs(sin(Math.toRadians(pitch.toDouble()))).toFloat()

        return RadiusInfo(visualRadius, lift)
    }
}
