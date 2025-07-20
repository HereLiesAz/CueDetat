// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/model/Perspective.kt
package com.hereliesaz.cuedetat.view.model

import android.graphics.Camera
import android.graphics.Matrix
import android.graphics.PointF
import com.hereliesaz.cuedetat.data.FullOrientation
import kotlin.math.pow

object Perspective {

    /**
     * Creates the base 3D perspective matrix based on sensor pitch.
     *
     * @param currentOrientation The raw sensor data for the device's
     *    orientation.
     * @param camera A reusable Camera object for 3D transformations.
     * @param lift An optional vertical translation for rendering elements like
     *    rails above the table surface.
     * @param applyPitch A flag to enable or disable the perspective tilt.
     * @return A Matrix containing the calculated 3D transformation.
     */
    fun createPerspectiveMatrix(
        currentOrientation: FullOrientation,
        camera: Camera,
        lift: Float = 0f,
        applyPitch: Boolean = true
    ): Matrix {
        val matrix = Matrix()
        camera.save()

        // The camera is at a fixed Z distance.
        camera.setLocation(0f, 0f, -32f)

        if (lift != 0f) {
            camera.translate(0f, lift, 0f)
        }

        if (applyPitch) {
            val physicalPitch = currentOrientation.pitch
            val maxVisualPitch = 75f
            val smoothingZoneStart = 60f // Start smoothing 15 degrees before the max.

            val visualPitch = when {
                physicalPitch < smoothingZoneStart -> {
                    // Linear 1-to-1 mapping before the smoothing zone.
                    physicalPitch
                }

                physicalPitch in smoothingZoneStart..maxVisualPitch -> {
                    // In the smoothing zone, apply an ease-out curve to smoothly
                    // transition from the linear motion to the clamped limit.
                    val range = maxVisualPitch - smoothingZoneStart
                    val progress = (physicalPitch - smoothingZoneStart) / range
                    val easedProgress = 1f - (1f - progress).pow(2) // Quadratic ease-out
                    smoothingZoneStart + (easedProgress * range)
                }

                else -> {
                    // Past the max, lock it.
                    maxVisualPitch
                }
            }

            camera.rotateX(visualPitch.coerceIn(0f, 90f))
        }

        camera.getMatrix(matrix)
        camera.restore()
        return matrix
    }


    /**
     * Converts a 2D point from the on-screen coordinate space back to the
     * logical 2D plane by applying an inverted transformation matrix.
     *
     * @param screenPoint The point in screen coordinates (e.g., from a touch
     *    event).
     * @param inverseMatrix The inverted final projection matrix.
     * @return The corresponding PointF in the logical coordinate space.
     */
    fun screenToLogical(screenPoint: PointF, inverseMatrix: Matrix): PointF {
        val logicalCoords = floatArrayOf(screenPoint.x, screenPoint.y)
        inverseMatrix.mapPoints(logicalCoords)
        return PointF(logicalCoords[0], logicalCoords[1])
    }
}