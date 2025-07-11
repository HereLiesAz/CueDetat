package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import com.hereliesaz.cuedetat.ui.ZoomMapping
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.min

@Singleton
class ReducerUtils @Inject constructor() {
    fun getCurrentLogicalRadius(stateWidth: Int, stateHeight: Int, zoomSliderPos: Float): Float {
        if (stateWidth == 0 || stateHeight == 0) return 1f
        val zoomFactor = ZoomMapping.sliderToZoom(zoomSliderPos)
        return (min(stateWidth, stateHeight) * 0.30f / 2f) * zoomFactor
    }

    fun findRailIntersectionAndNormal(startPoint: PointF, endPoint: PointF, state: OverlayState): Pair<PointF, PointF>? {
        val referenceRadius = state.onPlaneBall?.radius ?: state.protractorUnit.radius
        if (referenceRadius <= 0) return null

        val tableToBallRatioLong = state.tableSize.getTableToBallRatioLong()
        val tableToBallRatioShort = tableToBallRatioLong / state.tableSize.aspectRatio
        val tableWidth = tableToBallRatioLong * referenceRadius
        val tableHeight = tableToBallRatioShort * referenceRadius

        val halfW = tableWidth / 2f
        val halfH = tableHeight / 2f
        val canvasCenterX = state.viewWidth / 2f
        val canvasCenterY = state.viewHeight / 2f

        val left = canvasCenterX - halfW
        val top = canvasCenterY - halfH
        val right = canvasCenterX + halfW
        val bottom = canvasCenterY + halfH

        val dirX = endPoint.x - startPoint.x
        val dirY = endPoint.y - startPoint.y

        var t = Float.MAX_VALUE
        var normal: PointF? = null

        // Check against each of the four rails
        if (dirX != 0f) {
            val tLeft = (left - startPoint.x) / dirX
            if (tLeft in 0.0..1.0 && tLeft < t) {
                t = tLeft; normal = PointF(1f, 0f)
            }
            val tRight = (right - startPoint.x) / dirX
            if (tRight in 0.0..1.0 && tRight < t) {
                t = tRight; normal = PointF(-1f, 0f)
            }
        }
        if (dirY != 0f) {
            val tTop = (top - startPoint.y) / dirY
            if (tTop in 0.0..1.0 && tTop < t) {
                t = tTop; normal = PointF(0f, 1f)
            }
            val tBottom = (bottom - startPoint.y) / dirY
            if (tBottom in 0.0..1.0 && tBottom < t) {
                t = tBottom; normal = PointF(0f, -1f)
            }
        }

        return if (t != Float.MAX_VALUE && normal != null) {
            Pair(PointF(startPoint.x + t * dirX, startPoint.y + t * dirY), normal)
        } else {
            null
        }
    }

    fun reflect(v: PointF, n: PointF): PointF {
        val dot = v.x * n.x + v.y * n.y
        return PointF(v.x - 2 * dot * n.x, v.y - 2 * dot * n.y)
    }
}