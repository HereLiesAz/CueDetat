package com.hereliesaz.cuedetat.view.model

import android.graphics.PointF
import androidx.annotation.Keep
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.view.state.TableSize
import kotlin.math.pow

@Keep
data class Table(
    val size: TableSize,
    val isVisible: Boolean,
) {
    val logicalWidth: Float
    val logicalHeight: Float
    val corners: List<PointF>
    val pockets: List<PointF>
    val normals: List<PointF>

    init {
        val ballRealDiameter = 2.25f
        val ballLogicalDiameter = LOGICAL_BALL_RADIUS * 2
        val scale = ballLogicalDiameter / ballRealDiameter
        logicalWidth = size.shortSideInches * scale
        logicalHeight = size.longSideInches * scale

        val halfW = logicalWidth / 2f
        val halfH = logicalHeight / 2f

        corners = listOf(
            PointF().apply { x = -halfW; y = -halfH },
            PointF().apply { x = halfW; y = -halfH },
            PointF().apply { x = halfW; y = halfH },
            PointF().apply { x = -halfW; y = halfH }
        )

        val sidePocketOffset = LOGICAL_BALL_RADIUS * 0.5f

        pockets = listOf(
            PointF().apply { x = -halfW; y = -halfH },
            PointF().apply { x = halfW; y = -halfH },
            PointF().apply { x = -halfW; y = halfH },
            PointF().apply { x = halfW; y = halfH },
            PointF().apply { x = -halfW - sidePocketOffset; y = 0f },
            PointF().apply { x = halfW + sidePocketOffset; y = 0f }
        )

        normals = listOf(
            PointF().apply { x = 0f; y = 1f },
            PointF().apply { x = -1f; y = 0f },
            PointF().apply { x = 0f; y = -1f },
            PointF().apply { x = 1f; y = 0f }
        )
    }

    fun isPointInside(point: PointF): Boolean {
        val halfW = logicalWidth / 2f
        val halfH = logicalHeight / 2f
        return point.x >= -halfW && point.x <= halfW &&
                point.y >= -halfH && point.y <= halfH
    }

    fun findRailIntersectionAndNormal(startPoint: PointF, endPoint: PointF): Pair<PointF, PointF>? {
        val rails = listOf(
            corners[0] to corners[1],
            corners[1] to corners[2],
            corners[2] to corners[3],
            corners[3] to corners[0]
        )

        var closestIntersection: PointF? = null
        var intersectionNormal: PointF? = null
        var minDistanceSq = Float.MAX_VALUE

        rails.forEachIndexed { index, rail ->
            val intersection = getLineSegmentIntersection(startPoint, endPoint, rail.first, rail.second)
            if (intersection != null) {
                val distSq = (intersection.x - startPoint.x).pow(2) + (intersection.y - startPoint.y).pow(2)
                if (distSq < minDistanceSq) {
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

        return if (t in 0.001f..1.0f && u in 0f..1f) {
            PointF().apply {
                x = p1.x + t * (p2.x - p1.x)
                y = p1.y + t * (p2.y - p1.y)
            }
        } else {
            null
        }
    }

    /**
     * Reflects a vector with an optional spin value.
     * Default spin=0f maintains behavior for legacy calls.
     */
    fun reflect(v: PointF, n: PointF, spin: Float = 0f): PointF {
        val dot = v.x * n.x + v.y * n.y
        val reflectedVx = v.x - 2 * dot * n.x
        val reflectedVy = v.y - 2 * dot * n.y

        val angleAdjustment = spin * 0.15f
        val newAngle = kotlin.math.atan2(reflectedVy, reflectedVx) + angleAdjustment
        val mag = kotlin.math.hypot(reflectedVx.toDouble(), reflectedVy.toDouble()).toFloat()

        return PointF().apply {
            x = (mag * kotlin.math.cos(newAngle)).toFloat()
            y = (mag * kotlin.math.sin(newAngle)).toFloat()
        }
    }
}