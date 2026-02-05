// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/model/Perspective.kt

package com.hereliesaz.cuedetat.view.model

import android.graphics.Camera
import android.graphics.Matrix
import android.graphics.PointF
import com.hereliesaz.cuedetat.data.FullOrientation
import kotlin.math.abs
import kotlin.math.pow

/**
 * A singleton utility object dedicated to calculating and managing 3D perspective transformations.
 * This object is the mathematical heart of the application's "Ghost Table" rendering,
 * responsible for mapping the 2D logical billiards table into a 3D perspective that matches
 * the device's physical orientation in the real world.
 */
object Perspective {

    /**
     * Creates the base 3D perspective matrix based on the device's sensor pitch.
     *
     * This function generates a Matrix that transforms a flat 2D plane (the logical table)
     * into a 3D tilted plane. It handles the mapping from "Physical Pitch" (what the sensors report)
     * to "Visual Pitch" (what looks good on screen), including a smoothing curve to prevent
     * jitter at high angles.
     *
     * @param currentOrientation The raw sensor data containing the device's current pitch, roll, and azimuth.
     * @param camera A reusable Android [Camera] object (from `android.graphics`) used to compute 3D transformations.
     *               Note: This is NOT the hardware camera, but a legacy 3D matrix helper class.
     * @param lift An optional vertical translation (Z-axis offset) for rendering elements like
     *             rails that sit physically higher than the table surface. Default is 0f (table surface).
     * @param applyPitch A flag to enable or disable the perspective tilt. If false, returns a flat top-down view.
     * @return A [Matrix] representing the 3D transformation to be applied to the Canvas.
     */
    fun createPerspectiveMatrix(
        currentOrientation: FullOrientation,
        camera: Camera,
        lift: Float = 0f,
        applyPitch: Boolean = true,
    ): Matrix {
        // Instantiate a new Matrix to hold the resulting transformation.
        val matrix = Matrix()

        // Save the current state of the Camera object to the stack.
        // This is crucial because the Camera object is often reused across multiple calls to avoid allocation,
        // and we must ensure we don't pollute its state for subsequent operations.
        camera.save()

        // Check if the caller has requested the pitch transformation to be applied.
        // This is typically true for the main overlay but might be false for debugging or specific UI modes.
        if (applyPitch) {
            // Extract the raw pitch angle (in degrees) from the sensor data.
            // Positive pitch usually indicates the device is tilting forward/down.
            val physicalPitch = currentOrientation.pitch

            // Define the maximum physical tilt angle we expect the user to hold the phone at.
            // Beyond 75 degrees, holding the phone becomes awkward, so we treat this as the input cap.
            val physicalMaxPitch = 75f

            // Define the maximum visual tilt angle we want to render.
            // We aim for 87 degrees (nearly 90, which is perfectly flat) to create a dramatic depth effect.
            // We avoid 90 because math breaks at 90 (division by zero, infinite planes).
            val virtualMaxPitch = 87f

            // Define the "Smoothing Zone". Up to 60 degrees, the mapping is linear.
            // After 60 degrees, we apply an easing curve to "soft land" the perspective at the max.
            val smoothingZoneStart = 60f

            // Calculate the linear scale factor to map the physical range to the virtual range.
            // This determines how "sensitive" the visual tilt is to physical movement.
            val overallScaleFactor = virtualMaxPitch / physicalMaxPitch

            // We work with the absolute magnitude to handle both positive (forward) and negative (backward) tilt symmetrically.
            val physicalPitchMagnitude = abs(physicalPitch)

            // Determine the visual pitch magnitude based on the input range.
            val visualPitchMagnitude = when {
                // CASE 1: Linear Range.
                // If the pitch is less than the smoothing threshold (60 degrees), map it linearly.
                physicalPitchMagnitude < smoothingZoneStart -> {
                    // Simple multiplication: Physical Input * Scale Factor = Visual Output.
                    physicalPitchMagnitude * overallScaleFactor
                }

                // CASE 2: Smoothing Zone.
                // If the pitch is between 60 and 75 degrees, apply a cubic ease-out curve.
                // This prevents the table from appearing to "snap" or jitter as the user approaches the limit.
                physicalPitchMagnitude in smoothingZoneStart..physicalMaxPitch -> {
                    // Calculate the starting point of the virtual smoothing zone.
                    val virtualSmoothingStart = smoothingZoneStart * overallScaleFactor

                    // Calculate the size of the input range (e.g., 75 - 60 = 15 degrees).
                    val physicalRange = physicalMaxPitch - smoothingZoneStart

                    // Calculate the size of the output range (e.g., 87 - virtualSmoothingStart).
                    val virtualRangeToEase = virtualMaxPitch - virtualSmoothingStart

                    // Normalize the current input to a 0.0 - 1.0 progress value within the zone.
                    val progress = (physicalPitchMagnitude - smoothingZoneStart) / physicalRange

                    // Apply the Ease-Out Cubic formula: 1 - (1 - x)^3.
                    // This creates a curve that starts fast and slows down as it approaches 1.0.
                    val easedProgress = 1f - (1f - progress).pow(3)

                    // Interpolate the final value based on the eased progress.
                    virtualSmoothingStart + (easedProgress * virtualRangeToEase)
                }

                // CASE 3: Cap.
                // If the pitch exceeds the physical max, clamp the visual output to the virtual max.
                else -> {
                    virtualMaxPitch
                }
            }

            // Restore the original sign (direction) of the pitch.
            // If the input was negative, the output must be negative.
            val visualPitch = if (physicalPitch < 0) -visualPitchMagnitude else visualPitchMagnitude

            // Apply the calculated rotation around the X-axis of the Camera.
            // Rotating around X creates the "tilt" effect (top of screen moves away, bottom moves closer).
            // We clamp the final value to -90..90 just to be safe from invalid inputs.
            camera.rotateX(visualPitch.coerceIn(-90f, 90f))
        }

        // Translate the camera in 3D space.
        // 1. x=0f: No horizontal shift.
        // 2. y=lift: Apply the "lift" argument. Positive values move the object down (or camera up),
        //    simulating height (used for rail tops).
        // 3. z=-32f: Move the camera back along the Z-axis.
        //    The value -32f is a "magic number" standard in Android's Camera class that roughly approximates
        //    a standard viewing distance. Without this, the perspective distortion would be too extreme or inverted.
        camera.translate(0f, lift, -32f)

        // Compute the matrix for the current Camera state and store it in our `matrix` object.
        camera.getMatrix(matrix)

        // Restore the Camera state to what it was before `camera.save()`.
        // This is effectively "popping" the stack.
        camera.restore()

        // Return the fully computed transformation matrix.
        return matrix
    }


    /**
     * Converts a 2D point from the on-screen coordinate space back to the logical 2D plane.
     * This is the mathematical inverse of the rendering pipeline, used for handling touch events.
     *
     * Example: User taps the screen at (500px, 300px). We need to know what point on the virtual table
     * that corresponds to (e.g., 10 inches left, 5 inches up).
     *
     * @param screenPoint The point in screen pixel coordinates (e.g., from [MotionEvent.getX]).
     * @param inverseMatrix The inverted final projection matrix.
     *                      This matrix must be pre-calculated by inverting the render matrix.
     * @return The corresponding [PointF] in the logical coordinate space (inches from center).
     */
    fun screenToLogical(screenPoint: PointF, inverseMatrix: Matrix): PointF {
        // Create a float array to hold the coordinate pair.
        // Matrix.mapPoints expects an array, not individual floats.
        val logicalCoords = floatArrayOf(screenPoint.x, screenPoint.y)

        // Apply the inverse matrix to the point.
        // This modifies the array in-place.
        inverseMatrix.mapPoints(logicalCoords)

        // Construct and return a new PointF from the transformed coordinates.
        return PointF(logicalCoords[0], logicalCoords[1])
    }
}
