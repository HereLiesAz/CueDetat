// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/util/DrawingUtils.kt

package com.hereliesaz.cuedetat.view.renderer.util

import android.graphics.Matrix
import android.graphics.Path
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
     * - At 0 degrees pitch (top-down), lift is 0.
     * - At 90 degrees pitch (flat view), lift is equal to the radius (ball sits on the horizon line).
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

    /**
     * Warps a perfectly calculated 2D screen point to match the physical lens's barrel distortion.
     * Uses the Brown-Conrady distortion model.
     * * @param screenX The ideal pinhole X coordinate (pixels).
     * @param screenY The ideal pinhole Y coordinate (pixels).
     * @param cameraMatrix DoubleArray of size 9 from OpenCV's camera matrix.
     * @param distCoeffs DoubleArray of size 5 from OpenCV's distortion coefficients.
     * @return The distorted PointF to draw on the Android Canvas.
     */
    fun applyBarrelDistortion(
        screenX: Float, screenY: Float,
        cameraMatrix: DoubleArray,
        distCoeffs: DoubleArray
    ): PointF {
        // Extract intrinsic parameters
        val fx = cameraMatrix[0]
        val cx = cameraMatrix[2]
        val fy = cameraMatrix[4]
        val cy = cameraMatrix[5]

        // Extract distortion coefficients (k = radial, p = tangential)
        val k1 = distCoeffs[0]
        val k2 = distCoeffs[1]
        val p1 = distCoeffs[2]
        val p2 = distCoeffs[3]
        val k3 = if (distCoeffs.size > 4) distCoeffs[4] else 0.0

        // Normalize coordinates (convert from pixels to camera sensor space)
        val x = (screenX - cx) / fx
        val y = (screenY - cy) / fy

        // Calculate radial distance squared
        val r2 = x * x + y * y

        // Calculate Radial distortion multiplier
        val radialDistortion = 1.0 + k1 * r2 + k2 * (r2 * r2) + k3 * (r2 * r2 * r2)

        // Calculate Tangential distortion
        val xTangential = 2.0 * p1 * x * y + p2 * (r2 + 2.0 * x * x)
        val yTangential = p1 * (r2 + 2.0 * y * y) + 2.0 * p2 * x * y

        // Apply distortions to the normalized coordinates
        val xDistorted = x * radialDistortion + xTangential
        val yDistorted = y * radialDistortion + yTangential

        // Denormalize back to pixel coordinates for the Canvas
        val finalX = (xDistorted * fx + cx).toFloat()
        val finalY = (yDistorted * fy + cy).toFloat()

        return PointF(finalX, finalY)
    }

    /**
     * Creates a screen-space Path for a logical line by segmenting it, projecting it,
     * and applying barrel distortion to each segment so it curves with the lens.
     */
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

            // Interpolate the logical point
            val currentLogicalX = startLogical.x + (endLogical.x - startLogical.x) * t
            val currentLogicalY = startLogical.y + (endLogical.y - startLogical.y) * t

            // Project to screen space
            val screenPt = mapPoint(PointF(currentLogicalX, currentLogicalY), pitchMatrix)

            // Apply distortion if calibration exists
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