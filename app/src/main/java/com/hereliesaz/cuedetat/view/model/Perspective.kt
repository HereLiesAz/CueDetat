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
        isSpatiallyLocked: Boolean,
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
            // LOCKED MODE:
            // The camera's final orientation should be the orientation it had AT THE MOMENT OF LOCK,
            // further adjusted by how much the phone has moved SINCE THE MOMENT OF LOCK.

            // 1. What was the phone's orientation when "Lock" was pressed? This is anchorOrientation.
            //    The scene should *initially* appear as it did at anchorOrientation.pitch, .roll, .yaw.

            // 2. How much has the phone moved since locking? This is (currentOrientation - anchorOrientation).
            //    Let this be deltaOrientation.

            // 3. The final effective orientation for the camera to apply is:
            //    EffectivePitch = anchorOrientation.pitch + (currentOrientation.pitch - anchorOrientation.pitch) = currentOrientation.pitch
            //    EffectiveRoll = anchorOrientation.roll + (currentOrientation.roll - anchorOrientation.roll) = currentOrientation.roll
            //    EffectiveYaw = anchorOrientation.yaw + (currentOrientation.yaw - anchorOrientation.yaw) = currentOrientation.yaw

            // This mathematical simplification means that if we just apply currentOrientation directly,
            // the scene *will* reflect the phone's current absolute orientation.
            // This *is* what we want for the scene to follow the phone once locked.

            // The "reset to some default view" problem implies that either:
            //    a) `anchorOrientation` is not being captured correctly when lock is pressed (StateReducer issue).
            //    b) Or, `currentOrientation` is somehow incorrect/defaulted when lock is active.
            //    c) Or, the application of these rotations to the `android.graphics.Camera` is not achieving the desired visual.

            // Let's ensure the camera rotations directly use the currentOrientation values when locked.
            // The "lock" then is purely about disabling user input for element manipulation.
            // The view itself should *always* track the current phone orientation.
            // If it "resets", it means currentOrientation itself is perceived as "reset" by this function when locked.

            // Apply rotations based on the phone's CURRENT full orientation.
            // Order: Z (Roll), X (Pitch), Y (Yaw) for the camera object.
            // Remember currentOrientation.pitch is already -sensorPitch.
            // A positive camera.rotateX makes the scene pitch "down" (camera looking more up).
            camera.rotateZ(currentOrientation.roll)
            camera.rotateX(currentOrientation.pitch)
            // For Yaw: Android's getOrientation often gives yaw where 0 is North.
            // camera.rotateY rotates around the camera's Y (vertical) axis.
            // If phone turns right (yaw increases), scene should appear to pan left.
            // A positive rotateY makes the scene shift left. So, if yaw increases (turn right), we want positive rotateY.
            // This means the sign of yaw might need adjustment based on its definition.
            // If currentOrientation.yaw directly reflects sensor yaw (e.g. increases clockwise):
            camera.rotateY(-currentOrientation.yaw) // Try with negative first.

            // Log.d("Perspective", "LOCKED: Using Current(P:${currentOrientation.pitch}, R:${currentOrientation.roll}, Y:${currentOrientation.yaw})")

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