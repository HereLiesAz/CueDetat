// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/model/Table.kt

package com.hereliesaz.cuedetat.view.model

import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.view.state.TableSize
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The single source of truth for the pool table's state and geometry.
 *
 * This data class encapsulates all properties of the table and is the sole authority
 * for calculations related to its boundaries, pockets, and rails. Its center is
 * architecturally fixed to the logical plane's origin (0,0).
 *
 * It stores its geometry in an un-rotated, "portrait" state. All rotation is handled
 * by the rendering pipeline via matrix transformations.
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
    val unrotatedCorners: List<PointF>
    val unrotatedPockets: List<PointF>
    private val unrotatedNormals: List<PointF>

    init {
        val ballRealDiameter = 2.25f
        val ballLogicalDiameter = LOGICAL_BALL_RADIUS * 2
        val scale = ballLogicalDiameter / ballRealDiameter
        logicalWidth = size.shortSideInches * scale
        logicalHeight = size.longSideInches * scale

        val halfW = logicalWidth / 2f
        val halfH = logicalHeight / 2f

        unrotatedCorners = listOf(
            PointF(-halfW, -halfH), // Top-Left
            PointF(halfW, -halfH),  // Top-Right
            PointF(halfW, halfH),   // Bottom-Right
            PointF(-halfW, halfH)   // Bottom-Left
        )

        val sidePocketOffset = LOGICAL_BALL_RADIUS * 0.5f
        unrotatedPockets = listOf(
            PointF(-halfW, -halfH), PointF(halfW, -halfH),
            PointF(-halfW, halfH), PointF(halfW, halfH),
            PointF(-halfW - sidePocketOffset, 0f), PointF(halfW + sidePocketOffset, 0f)
        )

        unrotatedNormals = listOf(
            PointF(0f, -1f), // Top
            PointF(1f, 0f),  // Right
            PointF(0f, 1f),  // Bottom
            PointF(-1f, 0f)  // Left
        )
    }

    internal fun getDeRotatedPoint(point: PointF): PointF {
        val angleRad = Math.toRadians(-rotationDegrees.toDouble())
        val cosA = cos(angleRad).toFloat()
        val sinA = sin(angleRad).toFloat()
        return PointF(
            point.x * cosA - point.y * sinA,
            point.x * sinA + point.y * cosA
        )
    }

    internal fun getRotatedPoint(point: PointF): PointF {
        val angleRad = Math.toRadians(rotationDegrees.toDouble())
        val cosA = cos(angleRad).toFloat()
        val sinA = sin(angleRad).toFloat()
        return PointF(
            point.x * cosA - point.y * sinA,
            point.x * sinA + point.y * cosA
        )
    }


    fun isPointInside(point: PointF): Boolean {
        if (unrotatedCorners.isEmpty()) return false
        val deRotatedPoint = getDeRotatedPoint(point)

        var isInside = false
        var j = unrotatedCorners.size - 1
        for (i in unrotatedCorners.indices) {
            val pi = unrotatedCorners[i]
            val pj = unrotatedCorners[j]
            if ((pi.y > deRotatedPoint.y) != (pj.y > deRotatedPoint.y) &&
                (deRotatedPoint.x < (pj.x - pi.x) * (deRotatedPoint.y - pi.y) / (pj.y - pi.y) + pi.x)
            ) {
                isInside = !isInside
            }
            j = i
        }
        return isInside
    }

    fun findRailIntersectionAndNormal(startPoint: PointF, endPoint: PointF): Pair<PointF, PointF>? {
        if (unrotatedCorners.size < 4) return null

        val deRotatedStart = getDeRotatedPoint(startPoint)
        val deRotatedEnd = getDeRotatedPoint(endPoint)

        val rails = listOf(
            unrotatedCorners[0] to unrotatedCorners[1], // Top
            unrotatedCorners[1] to unrotatedCorners[2], // Right
            unrotatedCorners[2] to unrotatedCorners[3], // Bottom
            unrotatedCorners[3] to unrotatedCorners[0]  // Left
        )

        var closestIntersection: PointF? = null
        var intersectionNormal: PointF? = null
        var minDistanceSq = Float.MAX_VALUE

        rails.forEachIndexed { index, rail ->
            val intersection = getLineSegmentIntersection(deRotatedStart, deRotatedEnd, rail.first, rail.second)
            if (intersection != null) {
                val distSq = (intersection.x - deRotatedStart.x).pow(2) + (intersection.y - deRotatedStart.y).pow(2)
                if (distSq < minDistanceSq && distSq > 0.001f) {
                    minDistanceSq = distSq
                    closestIntersection = intersection
                    intersectionNormal = unrotatedNormals[index]
                }
            }
        }

        return if (closestIntersection != null && intersectionNormal != null) {
            // Re-rotate the intersection point and normal back to the world's rotation
            val rotatedIntersection = getRotatedPoint(closestIntersection!!)
            val rotatedNormal = getRotatedPoint(intersectionNormal!!)

            Pair(rotatedIntersection, rotatedNormal)
        } else {
            null
        }
    }


    private fun getLineSegmentIntersection(p1: PointF, p2: PointF, p3: PointF, p4: PointF): PointF? {
        val d = (p1.x - p2.x) * (p3.y - p4.y) - (p1.y - p2.y) * (p3.x - p4.x)
        if (abs(d) < 0.0001f) return null

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