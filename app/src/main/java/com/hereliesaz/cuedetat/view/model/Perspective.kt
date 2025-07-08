// app/src/main/java/com/hereliesaz/cuedetat/view/model/Perspective.kt
package com.hereliesaz.cuedetat.view.model

import android.graphics.Camera
import android.graphics.Matrix
import android.graphics.PointF
import com.hereliesaz.cuedetat.data.FullOrientation

object Perspective {

    fun createPitchMatrix(
        currentOrientation: FullOrientation,
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

        // Apply pitch, but ignore roll and yaw as mandated.
        val finalPitchForCamera = currentOrientation.pitch
        camera.rotateX(finalPitchForCamera)

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