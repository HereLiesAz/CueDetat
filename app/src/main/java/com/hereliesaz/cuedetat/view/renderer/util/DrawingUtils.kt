// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/util/DrawingUtils.kt

package com.hereliesaz.cuedetat.view.renderer.util

import android.graphics.Matrix
import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.CueDetatState
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sin

/**
 * A collection of static utility functions for geometric calculations involved in rendering.
 * These functions bridge the gap between abstract geometric concepts (Logical Space)
 * and concrete pixel coordinates (Screen Space).
 */
object DrawingUtils {

    /**
     * A data class to hold the calculated visual properties of a ball in 3D space.
     * @param radius The radius of the ball as it appears on the screen (in pixels).
     * @param lift The vertical offset (in pixels) to apply to the ball to make it sit "on" the table.
     */
    data class PerspectiveRadiusInfo(val radius: Float, val lift: Float)

    /**
     * Calculates how large a ball should look on screen and how much it should be "lifted"
     * to appear 3D, based on its position on the table.
     *
     * THE PHYSICS OF LIFT:
     * In a 2D top-down view, a ball is just a circle.
     * In a 3D perspective view, a ball is a sphere sitting ON TOP of the table surface.
     * If we just drew a circle at the table coordinates, the ball would look like a flat sticker.
     * To simulate 3D volume without a true 3D engine, we:
     * 1. Calculate the perspective-distorted radius.
     * 2. Shift the drawing center "up" (towards the top of the screen) proportional to the camera tilt (pitch).
     *    - At 0 degrees pitch (top-down), lift is 0.
     *    - At 90 degrees pitch (flat), lift is equal to the radius (ball sits on the horizon).
     *
     * @param logicalCenter The center of the ball in Logical Inches (0,0 is table center).
     * @param logicalRadius The radius of the ball in Logical Inches (standard pool ball radius).
     * @param state The current application state (for pitch angle).
     * @param matrix The transformation matrix to use (usually `state.sizeCalculationMatrix`).
     * @return A [PerspectiveRadiusInfo] containing the visual radius and vertical offset.
     */
    fun getPerspectiveRadiusAndLift(
        logicalCenter: PointF,
        logicalRadius: Float,
        state: CueDetatState,
        matrix: Matrix
    ): PerspectiveRadiusInfo {

        // Step 1: Map the center point.
        val centerPoint = floatArrayOf(logicalCenter.x, logicalCenter.y)
        matrix.mapPoints(centerPoint)

        // Step 2: Map a point on the "edge" of the ball.
        // We add the radius to the X-axis. This works because the X-axis is generally
        // less distorted by pitch than the Y-axis.
        val edgePoint = floatArrayOf(logicalCenter.x + logicalRadius, logicalCenter.y)
        matrix.mapPoints(edgePoint)

        // Step 3: Calculate the Euclidean distance between the mapped center and edge.
        // This gives us the radius in screen pixels.
        val radiusOnScreen = hypot(
            (centerPoint[0] - edgePoint[0]).toDouble(),
            (centerPoint[1] - edgePoint[1]).toDouble()
        ).toFloat()

        // Step 4: Calculate Lift.
        // Lift = Radius * sin(Pitch).
        // If pitch is 0, sin(0) = 0 -> No lift.
        // If pitch is 90, sin(90) = 1 -> Lift = Radius.
        val lift = radiusOnScreen * abs(sin(Math.toRadians(state.pitchAngle.toDouble()))).toFloat()

        return PerspectiveRadiusInfo(radiusOnScreen, lift)
    }

    /**
     * Helper to map a single point using a Matrix.
     * @param p The input point.
     * @param m The matrix to apply.
     * @return A new PointF with transformed coordinates.
     */
    fun mapPoint(p: PointF, m: Matrix): PointF {
        val arr = floatArrayOf(p.x, p.y)
        m.mapPoints(arr)
        return PointF(arr[0], arr[1])
    }
}
