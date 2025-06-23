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
        anchorOrientation: FullOrientation?, // If null, we are in UNLOCKED mode
        isSpatiallyLocked: Boolean,        // Explicit flag for clarity
        viewWidth: Int,
        viewHeight: Int,
        camera: Camera,
        lift: Float = 0f
    ): Matrix {
        val matrix = Matrix()
        camera.save()
        camera.setLocation(0f, 0f, -32f)

        if (lift != 0f) {
            camera.translate(0f, lift, 0f)
        }

        if (isSpatiallyLocked && anchorOrientation != null) {
            // LOCKED MODE: Apply rotations based on the DELTA from the anchor orientation.
            // This makes the scene appear to move in full 3D with the phone, relative to where it was locked.

            // Calculate deltas (current - anchor)
            // Pitch: currentOrientation.pitch is already -sensorPitch.
            // If phone pitches further down (sensor pitch increases, currentOrientation.pitch decreases e.g. -30 to -40)
            // deltaPitch = -40 - (-30) = -10. This means camera should rotate by -10 around X.
            val deltaPitch = currentOrientation.pitch - anchorOrientation.pitch

            // Roll: If phone rolls right (sensor roll increases), currentOrientation.roll increases.
            // deltaRoll = current.roll - anchor.roll. Camera Z-axis rotation.
            val deltaRoll = currentOrientation.roll - anchorOrientation.roll

            // Yaw: If phone yaws right (sensor yaw increases), currentOrientation.yaw increases.
            // deltaYaw = current.yaw - anchor.yaw. Camera Y-axis rotation.
            val deltaYaw = currentOrientation.yaw - anchorOrientation.yaw

            // Apply the BASE orientation of the anchor first.
            // Then, apply the DELTA rotations.
            // Order of applying rotations to the camera matters.
            // Typical order: Roll (Z), then Pitch (X), then Yaw (Y).
            // Base anchor rotations:
            camera.rotateZ(anchorOrientation.roll)
            camera.rotateX(anchorOrientation.pitch) // This is the original locked pitch
            camera.rotateY(-anchorOrientation.yaw)   // Apply anchored yaw (sign might need testing)

            // Now apply the deltas on top of the anchored orientation
            // These deltas represent the phone's movement *since* it was locked.
            camera.rotateZ(deltaRoll)
            camera.rotateX(deltaPitch)
            camera.rotateY(-deltaYaw) // Sign for deltaYaw also needs testing

            // Log.d("Perspective", "LOCKED: Anchor(P:${anchorOrientation.pitch}, R:${anchorOrientation.roll}, Y:${anchorOrientation.yaw}), Delta(P:$deltaPitch, R:$deltaRoll, Y:$deltaYaw)")

        } else {
            // UNLOCKED MODE: Only apply the current pitch (forward/backward tilt).
            // Roll and Yaw do not affect the perspective matrix in unlocked mode.
            camera.rotateX(currentOrientation.pitch)
            // Log.d("Perspective", "UNLOCKED: Pitch:${currentOrientation.pitch}")
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