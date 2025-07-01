// app/src/main/java/com/hereliesaz/cuedetatlite/view/state

package com.hereliesaz.cuedetatlite.view.state

import android.graphics.Matrix
import android.graphics.PointF

data class ScreenState(val width: Int, val height: Int) {

    fun screenToLogical(x: Float, y: Float, pitchMatrix: Matrix): PointF {
        val invertedMatrix = Matrix()
        pitchMatrix.invert(invertedMatrix)
        val point = floatArrayOf(x, y)
        invertedMatrix.mapPoints(point)
        return PointF(point[0], point[1])
    }

    fun logicalToScreen(logicalPoint: PointF, pitchMatrix: Matrix): PointF {
        val point = floatArrayOf(logicalPoint.x, logicalPoint.y)
        pitchMatrix.mapPoints(point)
        return PointF(point[0], point[1])
    }
}