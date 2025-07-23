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
     * [cite: 1283]
     *
     * @param currentOrientation The raw sensor data for the device's
     *    orientation. [cite: 1284]
     * @param camera A reusable Camera object for 3D transformations.
     *    [cite: 1285]
     * @param lift An optional vertical translation for rendering elements like
     *    rails above the table surface. [cite: 1286]
     * @param applyPitch A flag to enable or disable the perspective tilt.
     *    [cite: 1287]
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

        // The camera is at a fixed Z distance. [cite: 1288]
        camera.setLocation(0f, 0f, -32f)

        if (applyPitch) {
            val physicalPitch = currentOrientation.pitch
            val physicalMaxPitch = 75f
            val virtualMaxPitch = 87f // Adjusted from 90f [cite: 1642]
            val smoothingZoneStart = 60f

            val overallScaleFactor = virtualMaxPitch / physicalMaxPitch

            val visualPitch = when {
                physicalPitch < smoothingZoneStart -> {
                    // Linear mapping for the initial tilt range. [cite: 1290]
                    physicalPitch * overallScaleFactor
                }

                physicalPitch in smoothingZoneStart..physicalMaxPitch -> {
                    // In the smoothing zone, apply an ease-out curve. [cite: 1291]
                    val virtualSmoothingStart = smoothingZoneStart * overallScaleFactor
                    val physicalRange = physicalMaxPitch - smoothingZoneStart
                    val virtualRangeToEase = virtualMaxPitch - virtualSmoothingStart

                    val progress = (physicalPitch - smoothingZoneStart) / physicalRange
                    val easedProgress =
                        1f - (1f - progress).pow(3) // Cubic ease-out for stronger effect [cite: 1292]

                    virtualSmoothingStart + (easedProgress * virtualRangeToEase)
                }

                else -> {
                    // Past the max, lock it. [cite: 1293, 1294]
                    virtualMaxPitch
                }
            }

            camera.rotateX(visualPitch.coerceIn(0f, 90f))
        }

        // CORRECTED: Translate must happen AFTER rotation to function as a "lift".
        if (lift != 0f) {
            camera.translate(0f, lift, 0f)
        }

        camera.getMatrix(matrix)
        camera.restore()
        return matrix
    }


    /**
     * Converts a 2D point from the on-screen coordinate space back to the
     * logical 2D plane by applying an inverted transformation matrix.
     * [cite: 1295, 1296]
     *
     * @param screenPoint The point in screen coordinates (e.g., from a touch
     *    event). [cite: 1297]
     * @param inverseMatrix The inverted final projection matrix.
     * @return The corresponding PointF in the logical coordinate space.
     *    [cite: 1298]
     */
    fun screenToLogical(screenPoint: PointF, inverseMatrix: Matrix): PointF {
        val logicalCoords = floatArrayOf(screenPoint.x, screenPoint.y)
        inverseMatrix.mapPoints(logicalCoords)
        return PointF(logicalCoords[0], logicalCoords[1])
    }
}
