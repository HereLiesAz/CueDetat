// app/src/main/java/com/hereliesaz/cuedetat/view/model/Perspective.kt
package com.hereliesaz.cuedetat.view.model

import android.graphics.Camera
import android.graphics.Matrix
import android.graphics.PointF
import com.hereliesaz.cuedetat.data.FullOrientation
import android.util.Log

object Perspective {

    fun createPitchMatrix(
        currentOrientation: FullOrientation,
        anchorOrientation: FullOrientation?,
        isSpatiallyLocked: Boolean,
        viewWidth: Int,
        viewHeight: Int,
        camera: Camera,
        lift: Float = 0f
    ): Matrix {
        val classTag = "Perspective"
        Log.d(classTag, "CALLED - Locked=$isSpatiallyLocked, " +
                "Cur(P:${currentOrientation.pitch.f1()},R:${currentOrientation.roll.f1()},Y:${currentOrientation.yaw.f1()}), " +
                "Anc(P:${anchorOrientation?.pitch?.f1()},R:${anchorOrientation?.roll?.f1()},Y:${anchorOrientation?.yaw?.f1()})")

        val matrix = Matrix()
        camera.save()
        camera.setLocation(0f, 0f, -32f) // Reset position each time

        if (lift != 0f) {
            camera.translate(0f, lift, 0f)
        }

        val finalPitchForCamera: Float
        val finalRollForCamera: Float
        // For finalYawForCamera, we need to be careful. Android's yaw is often 0 at North.
        // If anchorYaw is -170 and currentYaw is +170 (crossed North), delta is 340, 2*anchor - current is very wrong.
        // We need to handle yaw wraparound.
        var finalYawForCamera: Float

        if (isSpatiallyLocked && anchorOrientation != null) {
            // LOCKED MODE: Stabilize the view based on anchor.
            // FinalCameraAngle = AnchorAngle - (CurrentAngle - AnchorAngle)
            // FinalCameraAngle = 2 * AnchorAngle - CurrentAngle

            finalPitchForCamera = 2 * anchorOrientation.pitch - currentOrientation.pitch
            finalRollForCamera = 2 * anchorOrientation.roll - currentOrientation.roll

            // Handle Yaw wraparound (e.g., from -179 to +179 is a small change, not 358 degrees)
            var deltaYaw = currentOrientation.yaw - anchorOrientation.yaw
            if (deltaYaw > 180) {
                deltaYaw -= 360
            } else if (deltaYaw < -180) {
                deltaYaw += 360
            }
            finalYawForCamera = anchorOrientation.yaw - deltaYaw // Equivalent to 2*anchor - current after wraparound adjustment

            Log.d(classTag, "LOCKED: Anchor(P:${anchorOrientation.pitch.f1()},R:${anchorOrientation.roll.f1()},Y:${anchorOrientation.yaw.f1()})")
            Log.d(classTag, "LOCKED: Current(P:${currentOrientation.pitch.f1()},R:${currentOrientation.roll.f1()},Y:${currentOrientation.yaw.f1()})")
            Log.d(classTag, "LOCKED: DeltaYaw(raw):${(currentOrientation.yaw - anchorOrientation.yaw).f1()}, DeltaYaw(wrapped):${deltaYaw.f1()}")
            Log.d(classTag, "LOCKED: FinalCamAngles(P:${finalPitchForCamera.f1()},R:${finalRollForCamera.f1()},Y:${finalYawForCamera.f1()})")

        } else {
            // UNLOCKED MODE: View is primarily affected by pitch. Roll and Yaw are ignored for the main plane.
            finalPitchForCamera = currentOrientation.pitch
            finalRollForCamera = 0f
            finalYawForCamera = 0f // No yaw effect from phone in unlocked mode on the plane
        }

        // Apply rotations to the camera.
        // Order Z, X, Y (Roll, Pitch, Yaw applied to camera's local axes)
        camera.rotateZ(finalRollForCamera)
        camera.rotateX(finalPitchForCamera)
        camera.rotateY(-finalYawForCamera) // Sign of Yaw application is critical.
        // If finalYawForCamera is world-space yaw scene should have,
        // -finalYawForCamera for camera.rotateY might be correct.

        camera.getMatrix(matrix)
        camera.restore()

        val pivotX = viewWidth / 2f
        val pivotY = viewHeight / 2f
        matrix.preTranslate(-pivotX, -pivotY)
        matrix.postTranslate(pivotX, pivotY)

        return matrix
    }

    fun screenToLogical(screenPoint: PointF, inverseMatrix: Matrix): PointF {
        val logicalCoords = floatArrayOf(screenPoint.x, screenPoint.y)
        inverseMatrix.mapPoints(logicalCoords)
        return PointF(logicalCoords[0], logicalCoords[1])
    }
}

// Helper for logging
private fun Float?.f1(): String = this?.let { "%.1f".format(it) } ?: "n"
private fun Float.f1(): String = "%.1f".format(this)