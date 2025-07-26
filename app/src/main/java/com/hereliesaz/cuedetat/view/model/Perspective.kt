// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/model/Perspective.kt
package com.hereliesaz.cuedetat.view.model

import android.graphics.Camera
import android.graphics.Matrix
import android.graphics.PointF
import com.hereliesaz.cuedetat.data.FullOrientation
import kotlin.math.abs
import kotlin.math.pow

object Perspective {

    /**
     * Creates the base 3D perspective matrix based on sensor pitch.
     *
     * @param currentOrientation The raw sensor data for the device's
     *    orientation.
     * @param worldRotationDegrees The desired 3D rotation of the world around
     *    the Y-axis.
     * @param camera A reusable Camera object for 3D transformations.
     * @param lift An optional vertical translation for rendering elements like
     *    rails above the table surface.
     * @param applyPitch A flag to enable or disable the perspective tilt.
     * @return A Matrix containing the calculated 3D transformation.
     */
    fun createPerspectiveMatrix(
        currentOrientation: FullOrientation,
        worldRotationDegrees: Float,
        camera: Camera,
        lift: Float = 0f,
        applyPitch: Boolean = true,
    ): Matrix {
        val matrix = Matrix()
        camera.save()

        // The camera is at a fixed Z distance.
        camera.setLocation(0f, 0f, -32f)

        // 1. Apply world rotation around the Y-axis (spin) before other transformations.
        camera.rotateY(worldRotationDegrees)

        if (applyPitch) {
            val physicalPitch = currentOrientation.pitch
            val physicalMaxPitch = 75f
            val virtualMaxPitch = 87f // Adjusted from 90f
            val smoothingZoneStart = 60f

            val overallScaleFactor = virtualMaxPitch / physicalMaxPitch

            val physicalPitchMagnitude = abs(physicalPitch)

            val visualPitchMagnitude = when {
                physicalPitchMagnitude < smoothingZoneStart -> {
                    // Linear mapping for the initial tilt range.
                    physicalPitchMagnitude * overallScaleFactor
                }

                physicalPitchMagnitude in smoothingZoneStart..physicalMaxPitch -> {
                    // In the smoothing zone, apply an ease-out curve.
                    val virtualSmoothingStart = smoothingZoneStart * overallScaleFactor
                    val physicalRange = physicalMaxPitch - smoothingZoneStart
                    val virtualRangeToEase = virtualMaxPitch - virtualSmoothingStart

                    val progress = (physicalPitchMagnitude - smoothingZoneStart) / physicalRange
                    val easedProgress =
                        1f - (1f - progress).pow(3) // Cubic ease-out for stronger effect

                    virtualSmoothingStart + (easedProgress * virtualRangeToEase)
                }

                else -> {
                    // Past the max, lock it.
                    virtualMaxPitch
                }
            }

            // Re-apply the original sign to the calculated magnitude
            val visualPitch = if (physicalPitch < 0) -visualPitchMagnitude else visualPitchMagnitude

            // 2. Apply pitch (forward-back tilt).
            camera.rotateX(visualPitch.coerceIn(-90f, 90f))
        }

        // 3. Translate must happen AFTER rotation to function as a "lift".
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