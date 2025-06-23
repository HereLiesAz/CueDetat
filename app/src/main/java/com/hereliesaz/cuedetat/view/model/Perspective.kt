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

        if (isSpatiallyLocked && anchorOrientation != null) {
            // LOCKED MODE:
            // Goal: Start view based on anchorPitch (no jump from unlocked).
            // Then, counteract phone's Pitch, Roll, Yaw movements relative to the anchor point.

            // 1. Base camera rotation: Set to the pitch observed at the moment of locking.
            //    This ensures the initial locked view matches the unlocked view's primary tilt.
            camera.rotateX(anchorOrientation.pitch)

            // 2. Calculate how much the phone has deviated from its anchored orientation
            //    for EACH axis (Roll, Yaw, and any *additional* Pitch beyond the anchor pitch).
            val deltaRoll = currentOrientation.roll - anchorOrientation.roll
            val deltaYaw = currentOrientation.yaw - anchorOrientation.yaw
            // This deltaPitch is the phone's pitch movement *relative to the pitch it had when locked*.
            val deltaPitch = currentOrientation.pitch - anchorOrientation.pitch

            // 3. Apply counter-rotations to the camera for these deltas.
            //    These rotations are applied to the camera *which is already pitched by anchorOrientation.pitch*.
            //    The order of these counter-rotations can matter. Z, Y, X is a common order for applying
            //    delta rotations to a camera's local axes to achieve world stabilization.
            //    - To counteract phone roll, camera rolls opposite: camera.rotateZ(-deltaRoll)
            //    - To counteract phone pitch delta, camera pitches opposite: camera.rotateX(-deltaPitch)
            //    - To counteract phone yaw, camera yaws opposite: camera.rotateY(deltaYaw)
            //      (Sign of deltaYaw for rotateY is tricky: if phone yaws right (sensor yaw increases, deltaYaw positive),
            //       scene should appear to move left. camera.rotateY(positive) makes scene move left. So +deltaYaw might be right)

            camera.rotateZ(-deltaRoll)    // Counteract phone's roll since anchor
            camera.rotateX(-deltaPitch)   // Counteract phone's additional pitch since anchor
            camera.rotateY(deltaYaw)      // Counteract phone's yaw since anchor (TEST THIS SIGN)


            Log.d(classTag, "LOCKED: BaseAnchorPitch:${anchorOrientation.pitch.f1()} | " +
                    "Delta(P:${deltaPitch.f1()},R:${deltaRoll.f1()},Y:${deltaYaw.f1()}) | " +
                    "AppliedCamCounter(P:${(-deltaPitch).f1()},R:${(-deltaRoll).f1()},Y:${(deltaYaw).f1()})")

        } else {
            // UNLOCKED MODE: Only apply the current pitch. Roll and Yaw are ignored.
            camera.rotateX(currentOrientation.pitch)
            // Log.d(classTag, "UNLOCKED: Applied CurrentPitch:${currentOrientation.pitch.f1()}")
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