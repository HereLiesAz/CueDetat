// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/model/Table.kt

package com.hereliesaz.cuedetat.view.model

import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.view.state.TableSize
import kotlin.math.pow

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
            PointF(-halfW, -halfH), // Top-Left
            PointF(halfW, -halfH),  // Top-Right
            PointF(halfW, halfH),   // Bottom-Right
            PointF(-halfW, halfH)   // Bottom-Left
        )

        val sidePocketOffset = LOGICAL_BALL_RADIUS * 0.5f

        pockets = listOf(
            PointF(-halfW, -halfH), PointF(halfW, -halfH),
            PointF(-halfW, halfH), PointF(halfW, halfH),
            PointF(-halfW - sidePocketOffset, 0f), PointF(halfW + sidePocketOffset, 0f)
        )

        normals = listOf(
            PointF(0f, -1f), // Top
            PointF(1f, 0f),  // Right
            PointF(0f, 1f),   // Bottom
            PointF(-1f, 0f)  // Left
        )
    }

    fun isPointInside(point: PointF): Boolean {
        if (corners.isEmpty()) return false

        val halfW = logicalWidth / 2f
        val halfH = logicalHeight / 2f

        return point.x >= -halfW && point.x <= halfW &&
                point.y >= -halfH && point.y <= halfH
    }

    fun findRailIntersectionAndNormal(startPoint: PointF, endPoint: PointF): Pair<PointF, PointF>? {
        if (corners.size < 4) return null

        val rails = listOf(
            corners[0] to corners[1], // Top
            corners[1] to corners[2], // Right
            corners[2] to corners[3], // Bottom
            corners[3] to corners[0]  // Left
        )

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

    fun reflect(v: PointF, n: PointF, spin: Float): PointF {
        val dot = v.x * n.x + v.y * n.y
        val reflectedVx = v.x - 2 * dot * n.x
        val reflectedVy = v.y - 2 * dot * n.y

        // Adjust the reflection angle based on the spin
        val angleAdjustment = spin * 0.1f // This is a magic number, I will need to tune it
        val newAngle = kotlin.math.atan2(reflectedVy, reflectedVx) + angleAdjustment
        val mag = kotlin.math.hypot(reflectedVx.toDouble(), reflectedVy.toDouble()).toFloat()
        return PointF((mag * kotlin.math.cos(newAngle)).toFloat(), (mag * kotlin.math.sin(newAngle)).toFloat())
    }
}