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
 *
 * These functions bridge the gap between abstract geometric concepts (Logical Space, e.g. Inches)
 * and concrete pixel coordinates (Screen Space), handling 3D perspective projection simplifications.
 */
object DrawingUtils {

    /**
     * A data class to hold the calculated visual properties of a ball in 3D perspective space.
     * @property radius The apparent radius of the ball as it appears on the screen (in pixels).
     * @property lift The vertical Y-offset (in pixels) to apply to the ball to make it sit "on" the table surface properly.
     */
    data class PerspectiveRadiusInfo(val radius: Float, val lift: Float)

    /**
     * Calculates how large a ball should look on screen and how much it should be "lifted"
     * to appear 3D, based on its position on the table and the camera angle.
     *
     * THE PHYSICS OF LIFT:
     * In a 2D top-down view, a ball is just a circle drawn at (x,y).
     * In a 3D perspective view, a ball is a sphere sitting ON TOP of the table surface (Z-axis).
     * If we just drew a circle at the projected table coordinates (x,y), the ball would look like a flat sticker.
     * To simulate 3D volume without a true 3D engine, we:
     * 1. Calculate the perspective-distorted radius (balls further away look smaller).
     * 2. Shift the drawing center "up" (negative Y in screen space) proportional to the camera tilt (pitch).
     *    - At 0 degrees pitch (top-down), lift is 0.
     *    - At 90 degrees pitch (flat view), lift is equal to the radius (ball sits on the horizon line).
     *
     * @param logicalCenter The center of the ball in Logical Inches (0,0 is table center).
     * @param logicalRadius The radius of the ball in Logical Inches (standard pool ball radius).
     * @param state The current application state (provides pitch angle).
     * @param matrix The transformation matrix to use (usually `state.sizeCalculationMatrix` or `pitchMatrix`).
     * @return A [PerspectiveRadiusInfo] containing the visual radius and vertical offset.
     */
    fun getPerspectiveRadiusAndLift(
        logicalCenter: PointF,
        logicalRadius: Float,
        state: CueDetatState,
        matrix: Matrix
    ): PerspectiveRadiusInfo {

        // Step 1: Map the center point from Logical Space to Screen Space.
        val centerPoint = floatArrayOf(logicalCenter.x, logicalCenter.y)
        matrix.mapPoints(centerPoint)

        // Step 2: Map a point on the "edge" of the ball.
        // We use the point at (center.x + radius, center.y).
        // This generally works well because the horizontal axis is less distorted by pitch than the vertical.
        val edgePoint = floatArrayOf(logicalCenter.x + logicalRadius, logicalCenter.y)
        matrix.mapPoints(edgePoint)

        // Step 3: Calculate the Euclidean distance between the mapped center and mapped edge.
        // This gives us the apparent radius in screen pixels.
        val radiusOnScreen = hypot(
            (centerPoint[0] - edgePoint[0]).toDouble(),
            (centerPoint[1] - edgePoint[1]).toDouble()
        ).toFloat()

        // Step 4: Calculate "Lift".
        // Lift simulates the ball standing up from the surface.
        // Lift = Radius * sin(Pitch).
        // If pitch is 0 (looking down), sin(0) = 0 -> No lift.
        // If pitch is 90 (looking flat), sin(90) = 1 -> Lift = Radius.
        val lift = radiusOnScreen * abs(sin(Math.toRadians(state.pitchAngle.toDouble()))).toFloat()

        // Return the calculated properties.
        return PerspectiveRadiusInfo(radiusOnScreen, lift)
    }

    /**
     * Helper to map a single point using an Android Matrix.
     *
     * Reduces boilerplate array allocation/mapping.
     *
     * @param p The input PointF in source coordinates.
     * @param m The Matrix to apply.
     * @return A new PointF in destination coordinates.
     */
    fun mapPoint(p: PointF, m: Matrix): PointF {
        val arr = floatArrayOf(p.x, p.y)
        m.mapPoints(arr)
        return PointF(arr[0], arr[1])
    }
}
