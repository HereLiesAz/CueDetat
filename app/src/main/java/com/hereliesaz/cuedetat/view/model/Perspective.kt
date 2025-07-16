// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/model/Perspective.kt
package com.hereliesaz.cuedetat.view.model

import android.graphics.Camera
import android.graphics.Matrix
import android.graphics.PointF
import com.hereliesaz.cuedetat.data.FullOrientation

object Perspective {

    /**
     * Creates the base 3D perspective matrix based only on sensor pitch.
     * Table rotation and zoom are applied separately as 2D transformations in the UseCase.
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

        // Apply 3D tilt based on device pitch around the X-axis.
        if (applyPitch) {
            camera.rotateX(currentOrientation.pitch)
        }

        camera.getMatrix(matrix)
        camera.restore()
        return matrix
    }


    fun screenToLogical(screenPoint: PointF, inverseMatrix: Matrix): PointF {
        val logicalCoords = floatArrayOf(screenPoint.x, screenPoint.y)
        inverseMatrix.mapPoints(logicalCoords)
        return PointF(logicalCoords[0], logicalCoords[1])
    }
}