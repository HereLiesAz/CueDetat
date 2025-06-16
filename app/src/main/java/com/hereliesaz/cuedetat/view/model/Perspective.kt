package com.hereliesaz.cuedetat.view.model

import android.graphics.Camera
import android.graphics.Matrix
import android.graphics.PointF

/**
 * A helper object for handling the 3D perspective transformations.
 */
object Perspective {

    /**
     * Creates the 3D transformation matrix that pivots around the center of the view.
     * An optional lift parameter can be provided to translate the object vertically
     * in the 3D space before projection.
     */
    fun createPitchMatrix(
        pitchAngle: Float,
        viewWidth: Int,
        viewHeight: Int,
        camera: Camera,
        lift: Float = 0f // A positive value lifts the object "up" from the plane
    ): Matrix {
        val matrix = Matrix()
        camera.save()
        camera.setLocation(0f, 0f, -32f) // Move camera back to reduce distortion

        // Apply lift in 3D space. Android's Camera Y-axis is inverted.
        if (lift != 0f) {
            camera.translate(0f, lift, 0f)
        }

        camera.rotateX(pitchAngle)
        camera.getMatrix(matrix)
        camera.restore()

        val pivotX = viewWidth / 2f
        val pivotY = viewHeight / 2f
        matrix.preTranslate(-pivotX, -pivotY)
        matrix.postTranslate(pivotX, pivotY)

        return matrix
    }

    /**
     * Projects a point from the screen space (user touch) to the logical 2D plane.
     */
    fun screenToLogical(screenPoint: PointF, inverseMatrix: Matrix): PointF {
        val logicalCoords = floatArrayOf(screenPoint.x, screenPoint.y)
        inverseMatrix.mapPoints(logicalCoords)
        return PointF(logicalCoords[0], logicalCoords[1])
    }
}