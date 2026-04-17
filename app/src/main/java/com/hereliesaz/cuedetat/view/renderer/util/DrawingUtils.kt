// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/util/DrawingUtils.kt

package com.hereliesaz.cuedetat.view.renderer.util

import android.graphics.Matrix
import android.graphics.Path
import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.CueDetatState
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sin

object DrawingUtils {

    data class PerspectiveRadiusInfo(val radius: Float, val lift: Float)

    fun getPerspectiveRadiusAndLift(
        logicalCenter: PointF,
        logicalRadius: Float,
        state: CueDetatState,
        matrix: Matrix
    ): PerspectiveRadiusInfo {

        val centerPoint = floatArrayOf(logicalCenter.x, logicalCenter.y)
        matrix.mapPoints(centerPoint)

        val edgePoint = floatArrayOf(logicalCenter.x + logicalRadius, logicalCenter.y)
        matrix.mapPoints(edgePoint)

        val radiusOnScreen = hypot(
            (centerPoint[0] - edgePoint[0]).toDouble(),
            (centerPoint[1] - edgePoint[1]).toDouble()
        ).toFloat()

        val lift = radiusOnScreen * abs(sin(Math.toRadians(state.pitchAngle.toDouble()))).toFloat()

        return PerspectiveRadiusInfo(radiusOnScreen, lift)
    }

    fun mapPoint(p: PointF, m: Matrix): PointF {
        val arr = floatArrayOf(p.x, p.y)
        m.mapPoints(arr)
        return PointF(arr[0], arr[1])
    }

    fun applyBarrelDistortion(
        screenX: Float, screenY: Float,
        cameraMatrix: DoubleArray,
        distCoeffs: DoubleArray
    ): PointF {
        val fx = cameraMatrix[0]
        val cx = cameraMatrix[2]
        val fy = cameraMatrix[4]
        val cy = cameraMatrix[5]

        val k1 = distCoeffs[0]
        val k2 = distCoeffs[1]
        val p1 = distCoeffs[2]
        val p2 = distCoeffs[3]
        val k3 = if (distCoeffs.size > 4) distCoeffs[4] else 0.0

        val x = (screenX - cx) / fx
        val y = (screenY - cy) / fy

        val r2 = x * x + y * y

        val radialDistortion = 1.0 + k1 * r2 + k2 * (r2 * r2) + k3 * (r2 * r2 * r2)

        val xTangential = 2.0 * p1 * x * y + p2 * (r2 + 2.0 * x * x)
        val yTangential = p1 * (r2 + 2.0 * y * y) + 2.0 * p2 * x * y

        val xDistorted = x * radialDistortion + xTangential
        val yDistorted = y * radialDistortion + yTangential

        val finalX = (xDistorted * fx + cx).toFloat()
        val finalY = (yDistorted * fy + cy).toFloat()

        return PointF(finalX, finalY)
    }

    fun buildDistortedLinePath(
        startLogical: PointF,
        endLogical: PointF,
        pitchMatrix: Matrix,
        cameraMatrix: DoubleArray?,
        distCoeffs: DoubleArray?,
        segments: Int = 20
    ): Path {
        val path = Path()

        for (i in 0..segments) {
            val t = i.toFloat() / segments

            val currentLogicalX = startLogical.x + (endLogical.x - startLogical.x) * t
            val currentLogicalY = startLogical.y + (endLogical.y - startLogical.y) * t

            val screenPt = mapPoint(PointF(currentLogicalX, currentLogicalY), pitchMatrix)

            val finalPt = if (cameraMatrix != null && distCoeffs != null && cameraMatrix.size == 9) {
                applyBarrelDistortion(screenPt.x, screenPt.y, cameraMatrix, distCoeffs)
            } else {
                screenPt
            }

            if (i == 0) {
                path.moveTo(finalPt.x, finalPt.y)
            } else {
                path.lineTo(finalPt.x, finalPt.y)
            }
        }
        return path
    }
}