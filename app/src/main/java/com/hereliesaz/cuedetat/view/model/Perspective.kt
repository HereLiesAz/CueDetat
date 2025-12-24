package com.hereliesaz.cuedetat.view.model

import android.graphics.Matrix
import android.graphics.PointF

object Perspective {
    fun screenToLogical(screenPoint: PointF, inverseMatrix: Matrix): PointF {
        val points = floatArrayOf(screenPoint.x, screenPoint.y)
        inverseMatrix.mapPoints(points)
        return PointF(points[0], points[1])
    }
}
