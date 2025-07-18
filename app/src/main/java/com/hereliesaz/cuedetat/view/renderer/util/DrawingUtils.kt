package com.hereliesaz.cuedetat.view.renderer.util

import android.graphics.Matrix
import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.view.model.Perspective
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
        matrix: Matrix
    ): PerspectiveRadiusInfo {

        // Project two logical points (center and an edge) to screen space.
        val centerPoint = floatArrayOf(logicalCenter.x, logicalCenter.y)
        val edgePoint = floatArrayOf(logicalCenter.x + logicalRadius, logicalCenter.y)

        matrix.mapPoints(centerPoint)
        matrix.mapPoints(edgePoint)

        // The distance between these projected points is the true, perspective-correct radius.
        val radiusOnScreen = hypot(
            (centerPoint[0] - edgePoint[0]).toDouble(),
            (centerPoint[1] - edgePoint[1]).toDouble()
        ).toFloat()

        // The lift remains a function of the on-screen radius and pitch angle.
        val lift = radiusOnScreen * abs(sin(Math.toRadians(state.pitchAngle.toDouble()))).toFloat()
        return PerspectiveRadiusInfo(radiusOnScreen, lift)
    }

    /**
     * Calculates the expected on-screen pixel radius of a logical ball at a given screen Y-coordinate.
     * This is crucial for providing accurate radius estimates to CV algorithms.
     */
    fun getExpectedRadiusAtScreenY(y: Float, state: OverlayState): Float {
        if (!state.hasInverseMatrix) return LOGICAL_BALL_RADIUS // Fallback

        // 1. Convert the screen Y-coordinate into a point on the logical plane.
        val screenPoint = PointF(state.viewWidth / 2f, y)
        val logicalPoint = Perspective.screenToLogical(screenPoint, state.inversePitchMatrix)

        // 2. Create a second logical point, offset by the logical ball radius.
        val logicalPointOffset = PointF(logicalPoint.x + LOGICAL_BALL_RADIUS, logicalPoint.y)

        // 3. Project both logical points back to the screen using the main perspective matrix.
        val screenPointProjected = mapPoint(logicalPoint, state.pitchMatrix)
        val screenPointOffsetProjected = mapPoint(logicalPointOffset, state.pitchMatrix)

        // 4. The distance between the two projected screen points is the expected radius.
        return hypot(
            (screenPointProjected.x - screenPointOffsetProjected.x).toDouble(),
            (screenPointProjected.y - screenPointOffsetProjected.y).toDouble()
        ).toFloat()
    }

    fun mapPoint(p: PointF, m: Matrix): PointF {
        val arr = floatArrayOf(p.x, p.y)
        m.mapPoints(arr)
        return PointF(arr[0], arr[1])
    }
}