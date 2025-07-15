// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/ReducerUtils.kt

package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import android.graphics.Rect
import com.hereliesaz.cuedetat.ui.ZoomMapping
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class ReducerUtils @Inject constructor() {
    fun getCurrentLogicalRadius(stateWidth: Int, stateHeight: Int, zoomSliderPos: Float): Float {
        if (stateWidth == 0 || stateHeight == 0) return 1f
        val zoomFactor = ZoomMapping.sliderToZoom(zoomSliderPos)
        return (min(stateWidth, stateHeight) * 0.30f / 2f) * zoomFactor
    }

    fun getTableBoundaries(state: OverlayState): Rect {
        val referenceRadius = state.onPlaneBall?.radius ?: state.protractorUnit.radius
        val ballRealDiameter = 2.25f
        val ballLogicalDiameter = referenceRadius * 2
        val scale = ballLogicalDiameter / ballRealDiameter

        val logicalWidth = state.tableSize.longSideInches * scale
        val logicalHeight = state.tableSize.shortSideInches * scale

        val canvasCenterX = state.viewWidth / 2f
        val canvasCenterY = state.viewHeight / 2f
        val left = canvasCenterX - logicalWidth / 2
        val top = canvasCenterY - logicalHeight / 2
        val right = canvasCenterX + logicalWidth / 2
        val bottom = canvasCenterY + logicalHeight / 2

        return Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
    }

    private fun confinePoint(point: PointF, bounds: Rect): PointF {
        return PointF(
            point.x.coerceIn(bounds.left.toFloat(), bounds.right.toFloat()),
            point.y.coerceIn(bounds.top.toFloat(), bounds.bottom.toFloat())
        )
    }

    fun confineAllBallsToTable(state: OverlayState): OverlayState {
        if (!state.showTable) {
            return state
        }
        val bounds = getTableBoundaries(state)

        val confinedCueBall = state.onPlaneBall?.let {
            it.copy(center = confinePoint(it.center, bounds))
        }

        val confinedObstacles = state.obstacleBalls.map {
            it.copy(center = confinePoint(it.center, bounds))
        }

        val confinedTargetBall = state.protractorUnit.copy(
            center = confinePoint(state.protractorUnit.center, bounds)
        )

        return state.copy(
            onPlaneBall = confinedCueBall,
            obstacleBalls = confinedObstacles,
            protractorUnit = confinedTargetBall
        )
    }


    fun findRailIntersectionAndNormal(startPoint: PointF, endPoint: PointF, state: OverlayState): Pair<PointF, PointF>? {
        val bounds = getTableBoundaries(state)
        val left = bounds.left.toFloat()
        val top = bounds.top.toFloat()
        val right = bounds.right.toFloat()
        val bottom = bounds.bottom.toFloat()

        val dirX = endPoint.x - startPoint.x
        val dirY = endPoint.y - startPoint.y

        var t = Float.MAX_VALUE
        var normal: PointF? = null

        if (dirX != 0f) {
            val tLeft = (left - startPoint.x) / dirX
            if (tLeft > 0.001f && tLeft < t) {
                t = tLeft; normal = PointF(1f, 0f)
            }
            val tRight = (right - startPoint.x) / dirX
            if (tRight > 0.001f && tRight < t) {
                t = tRight; normal = PointF(-1f, 0f)
            }
        }
        if (dirY != 0f) {
            val tTop = (top - startPoint.y) / dirY
            if (tTop > 0.001f && tTop < t) {
                t = tTop; normal = PointF(0f, 1f)
            }
            val tBottom = (bottom - startPoint.y) / dirY
            if (tBottom > 0.001f && tBottom < t) {
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