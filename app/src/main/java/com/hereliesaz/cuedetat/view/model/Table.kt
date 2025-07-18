// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/model/Table.kt

package com.hereliesaz.cuedetat.view.model

import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.view.state.TableSize
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * The single source of truth for the pool table's state and geometry.
 *
 * This data class encapsulates all properties of the table and is the sole authority
 * for calculations related to its boundaries, pockets, and rails. Its center is
 * architecturally fixed to the logical plane's origin (0,0).
 *
 * @property size The physical dimensions of the table (e.g., EIGHT_FT).
 * @property rotationDegrees The visual rotation of the table on the logical plane.
 * @property isVisible Whether the table is currently rendered.
 */
data class Table(
    val size: TableSize,
    val rotationDegrees: Float,
    val isVisible: Boolean,
) {
    val logicalWidth: Float
    val logicalHeight: Float
    val corners: List<PointF>
    val pockets: List<PointF>
    private val normals: List<PointF>

    init {
        val ballRealDiameter = 2.25f
        val ballLogicalDiameter = LOGICAL_BALL_RADIUS * 2
        val scale = ballLogicalDiameter / ballRealDiameter
        // Defines the table in a default portrait orientation
        logicalWidth = size.shortSideInches * scale
        logicalHeight = size.longSideInches * scale

        val halfW = logicalWidth / 2f
        val halfH = logicalHeight / 2f

        // Corners are now permanently stored in their un-rotated, logical orientation.
        // The transformation matrix is solely responsible for rotation.
        corners = listOf(
            PointF(-halfW, -halfH), // Top-Left
            PointF(halfW, -halfH),  // Top-Right
            PointF(halfW, halfH),   // Bottom-Right
            PointF(-halfW, halfH)   // Bottom-Left
        )

        // Pockets are also now permanently stored in their un-rotated, logical orientation.
        pockets = listOf(
            PointF(-halfW, -halfH), PointF(halfW, -halfH), // Top-Left, Top-Right
            PointF(-halfW, halfH), PointF(halfW, halfH),   // Bottom-Left, Bottom-Right
            PointF(-halfW, 0f), PointF(halfW, 0f)          // CORRECTED: Side pockets are on the rail.
        )

        // Normals are also now permanently stored in their un-rotated, logical orientation.
        normals = listOf(
            PointF(0f, -1f), // Top
            PointF(1f, 0f),  // Right
            PointF(0f, 1f),   // Bottom
            PointF(-1f, 0f)  // Left
        )
    }

    fun isPointInside(point: PointF): Boolean {
        if (corners.isEmpty()) return false

        // De-rotate the point to check against the un-rotated table bounds
        val deRotatedPoint = PointF()
        val angleRad = Math.toRadians(-rotationDegrees.toDouble())
        val cosA = kotlin.math.cos(angleRad).toFloat()
        val sinA = kotlin.math.sin(angleRad).toFloat()
        deRotatedPoint.x = point.x * cosA - point.y * sinA
        deRotatedPoint.y = point.x * sinA + point.y * cosA

        val halfW = logicalWidth / 2f
        val halfH = logicalHeight / 2f

        return deRotatedPoint.x >= -halfW && deRotatedPoint.x <= halfW &&
                deRotatedPoint.y >= -halfH && deRotatedPoint.y <= halfH
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
}