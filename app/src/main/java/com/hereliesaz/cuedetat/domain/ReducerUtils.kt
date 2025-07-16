// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/ReducerUtils.kt

package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import android.graphics.Rect
import com.hereliesaz.cuedetat.ui.ZoomMapping
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.pow
import kotlin.math.sqrt

@Singleton
class ReducerUtils @Inject constructor() {

    fun getDefaultTargetBallPosition(): PointF = PointF(0f, 0f)

    fun getDefaultCueBallPosition(state: OverlayState): PointF {
        val logicalRadius = getCurrentLogicalRadius(state.viewWidth, state.viewHeight, 0f)
        val scale = (logicalRadius * 2) / 2.25f
        val tablePlayingSurfaceHeight = state.tableSize.shortSideInches * scale
        val headSpotY = tablePlayingSurfaceHeight / 4f // Relative to table center
        return PointF(0f, headSpotY)
    }

    fun getCurrentLogicalRadius(stateWidth: Int, stateHeight: Int, zoomSliderPos: Float): Float {
        if (stateWidth == 0 || stateHeight == 0) return 1f
        val zoomFactor = ZoomMapping.sliderToZoom(zoomSliderPos)
        return (min(stateWidth, stateHeight) * 0.30f / 2f) * zoomFactor
    }

    fun getLogicalTableCorners(state: OverlayState): List<PointF> {
        val referenceRadius = state.onPlaneBall?.radius ?: state.protractorUnit.radius
        if (referenceRadius <= 0f) return emptyList()

        val ballRealDiameter = 2.25f
        val ballLogicalDiameter = referenceRadius * 2
        val scale = ballLogicalDiameter / ballRealDiameter

        val logicalWidth = state.tableSize.longSideInches * scale
        val logicalHeight = state.tableSize.shortSideInches * scale

        val halfW = logicalWidth / 2f
        val halfH = logicalHeight / 2f

        val corners = listOf(
            PointF(-halfW, -halfH), // Top-Left
            PointF(halfW, -halfH),  // Top-Right
            PointF(halfW, halfH),   // Bottom-Right
            PointF(-halfW, halfH)   // Bottom-Left
        )

        val angleRad = Math.toRadians(state.tableRotationDegrees.toDouble())
        val cosA = cos(angleRad).toFloat()
        val sinA = sin(angleRad).toFloat()

        return corners.map { p ->
            PointF(
                p.x * cosA - p.y * sinA,
                p.x * sinA + p.y * cosA
            )
        }
    }

    fun isPointInsidePolygon(point: PointF, polygon: List<PointF>): Boolean {
        if (polygon.isEmpty()) return false
        var isInside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val pi = polygon[i]
            val pj = polygon[j]
            if ((pi.y > point.y) != (pj.y > point.y) &&
                (point.x < (pj.x - pi.x) * (point.y - pi.y) / (pj.y - pi.y) + pi.x)
            ) {
                isInside = !isInside
            }
            j = i
        }
        return isInside
    }

    fun snapViolatingBalls(state: OverlayState): OverlayState {
        if (!state.showTable) return state

        val corners = getLogicalTableCorners(state)
        if (corners.isEmpty()) return state

        var updatedState = state

        if (!isPointInsidePolygon(state.protractorUnit.center, corners)) {
            updatedState = updatedState.copy(
                protractorUnit = state.protractorUnit.copy(center = getDefaultTargetBallPosition())
            )
        }

        updatedState.onPlaneBall?.let { ball ->
            if (!isPointInsidePolygon(ball.center, corners)) {
                updatedState = updatedState.copy(
                    onPlaneBall = ball.copy(center = getDefaultCueBallPosition(updatedState))
                )
            }
        }

        val confinedObstacles = updatedState.obstacleBalls.filter {
            isPointInsidePolygon(it.center, corners)
        }
        if (confinedObstacles.size != updatedState.obstacleBalls.size) {
            updatedState = updatedState.copy(obstacleBalls = confinedObstacles)
        }

        return updatedState
    }

    fun findRailIntersectionAndNormal(startPoint: PointF, endPoint: PointF, state: OverlayState): Pair<PointF, PointF>? {
        val corners = getLogicalTableCorners(state)
        if (corners.size < 4) return null

        val rails = listOf(
            corners[0] to corners[1], // Top
            corners[1] to corners[2], // Right
            corners[2] to corners[3], // Bottom
            corners[3] to corners[0]  // Left
        )

        val unrotatedNormals = listOf(
            PointF(0f, -1f), // Top
            PointF(1f, 0f),  // Right
            PointF(0f, 1f),   // Bottom
            PointF(-1f, 0f)  // Left
        )

        val angleRad = Math.toRadians(state.tableRotationDegrees.toDouble())
        val cosA = cos(angleRad).toFloat()
        val sinA = sin(angleRad).toFloat()

        val normals = unrotatedNormals.map { n ->
            PointF(n.x * cosA - n.y * sinA, n.x * sinA + n.y * cosA)
        }

        var closestIntersection: PointF? = null
        var intersectionNormal: PointF? = null
        var minDistanceSq = Float.MAX_VALUE

        rails.forEachIndexed { index, rail ->
            val intersection = getLineSegmentIntersection(startPoint, endPoint, rail.first, rail.second)
            if (intersection != null) {
                val distSq = (intersection.x - startPoint.x).pow(2) + (intersection.y - startPoint.y).pow(2)
                if (distSq < minDistanceSq && distSq > 0.001f) {
                    minDistanceSq = distSq
                    closestIntersection = intersection
                    intersectionNormal = normals[index]
                }
            }
        }

        return if (closestIntersection != null && intersectionNormal != null) {
            Pair(closestIntersection!!, intersectionNormal!!)
        } else {
            null
        }
    }

    private fun getLineSegmentIntersection(p1: PointF, p2: PointF, p3: PointF, p4: PointF): PointF? {
        val d = (p1.x - p2.x) * (p3.y - p4.y) - (p1.y - p2.y) * (p3.x - p4.x)
        if (d == 0f) return null

        val t = ((p1.x - p3.x) * (p3.y - p4.y) - (p1.y - p3.y) * (p3.x - p4.x)) / d
        val u = -((p1.x - p2.x) * (p1.y - p3.y) - (p1.y - p2.y) * (p1.x - p3.x)) / d

        return if (t > 0.001f && u >= 0f && u <= 1f) {
            PointF(p1.x + t * (p2.x - p1.x), p1.y + t * (p2.y - p1.y))
        } else {
            null
        }
    }

    fun reflect(v: PointF, n: PointF): PointF {
        val dot = v.x * n.x + v.y * n.y
        return PointF(v.x - 2 * dot * n.x, v.y - 2 * dot * n.y)
    }
}