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
        Log.d("PerspectiveCHK", "CALLED - Locked=$isSpatiallyLocked, " +
                "Current(P:${currentOrientation.pitch.f1()},R:${currentOrientation.roll.f1()},Y:${currentOrientation.yaw.f1()}), " +
                "Anchor(P:${anchorOrientation?.pitch?.f1()},R:${anchorOrientation?.roll?.f1()},Y:${anchorOrientation?.yaw?.f1()})")

        val matrix = Matrix()
        camera.save()
        camera.setLocation(0f, 0f, -32f)

        if (lift != 0f) {
            camera.translate(0f, lift, 0f)
        }

        if (isSpatiallyLocked && anchorOrientation != null) {
            // LOCKED MODE:
            // 1. Start with the orientation the camera had when things were locked (based on anchor's PITCH only, initially).
            // This establishes the baseline "forward/backward tilted" view.
            camera.rotateX(anchorOrientation.pitch)

            // 2. Calculate how much the phone has rolled and yawed *since locking*.
            val deltaRoll = currentOrientation.roll - anchorOrientation.roll
            val deltaYaw = currentOrientation.yaw - anchorOrientation.yaw
            // And how much it has pitched *additionally* since locking.
            val additionalPitch = currentOrientation.pitch - anchorOrientation.pitch

            // 3. Apply these deltas. These rotations are applied to the camera that is already
            //    pitched according to anchorOrientation.pitch.
            //    The order here is critical for how roll and yaw feel relative to the pitched view.
            //    Typically, you might apply yaw around the world's Y, then additional pitch, then roll.
            //    Or, for camera's local axes:

            // Option A: Apply deltas to camera's local axes after initial anchor pitch
            // camera.rotateY(-deltaYaw)   // Yaw around camera's Y (up)
            // camera.rotateX(additionalPitch) // Additional pitch around camera's X (right)
            // camera.rotateZ(deltaRoll)   // Roll around camera's Z (forward)

            // Option B: A more intuitive approach might be to think of world transformations
            // For now, let's try applying the deltas directly to the camera's current state.
            // The effect of camera.rotateX(anchorOrientation.pitch) has already set a tilted coordinate system for the camera.
            // Subsequent rotateZ and rotateY will be around the new Z and Y axes of this tilted camera.

            camera.rotateZ(deltaRoll)       // Roll the already pitched view
            camera.rotateY(-deltaYaw)       // Yaw the already pitched and rolled view
            camera.rotateX(additionalPitch) // Apply additional pitch last to avoid altering the roll/yaw plane too much

            Log.d("PerspectiveLOCK", "AnchorPitch:${anchorOrientation.pitch.f1()}, " +
                    "Delta(P:${additionalPitch.f1()}, R:${deltaRoll.f1()}, Y:${deltaYaw.f1()})")

        } else {
            // UNLOCKED MODE: Only apply the current pitch.
            camera.rotateX(currentOrientation.pitch)
            Log.d("PerspectiveUNLOCK", "Applied CurrentPitch:${currentOrientation.pitch.f1()}")
        }

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