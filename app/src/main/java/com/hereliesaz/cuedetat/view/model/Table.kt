// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/model/Table.kt
package com.hereliesaz.cuedetat.view.model

import android.graphics.PointF
import kotlin.math.*

data class Table(
    val size: TableSize = TableSize.EIGHT_FOOT,
    val rotationDegrees: Float = 90f,
    val isVisible: Boolean = false,
    val geometry: TableGeometry = TableGeometry()
) {
    fun withVisibility(newVisibility: Boolean): Table {
        return copy(isVisible = newVisibility)
    }

    fun withRotation(newRotation: Float): Table {
        return copy(rotationDegrees = newRotation)
    }

    fun withSize(newSize: TableSize): Table {
        return copy(size = newSize)
    }

    fun recalculateGeometry(referenceRadius: Float): Table {
        if (referenceRadius <= 0f) return copy(geometry = TableGeometry())

        val ballRealDiameter = 2.25f
        val ballLogicalDiameter = referenceRadius * 2
        val scale = ballLogicalDiameter / ballRealDiameter

        val logicalWidth = this.size.longSideInches * scale
        val logicalHeight = this.size.shortSideInches * scale

        val halfW = logicalWidth / 2f
        val halfH = logicalHeight / 2f

        val unrotatedCorners = listOf(
            PointF(-halfW, -halfH),
            PointF(halfW, -halfH),
            PointF(halfW, halfH),
            PointF(-halfW, halfH)
        )

        val angleRad = Math.toRadians(-this.rotationDegrees.toDouble())
        val cosA = cos(angleRad).toFloat()
        val sinA = sin(angleRad).toFloat()

        val rotatedCorners = unrotatedCorners.map { p ->
            PointF(
                p.x * cosA - p.y * sinA,
                p.x * sinA + p.y * cosA
            )
        }

        val newGeometry = TableGeometry(
            width = logicalWidth,
            height = logicalHeight,
            unrotatedCorners = unrotatedCorners,
            rotatedCorners = rotatedCorners
        )

        return copy(geometry = newGeometry)
    }

    // --- NEW AUTHORITATIVE GEOMETRY FUNCTIONS ---

    fun getLogicalPockets(referenceRadius: Float): List<PointF> {
        if (!geometry.isValid) return emptyList()
        val halfW = geometry.width / 2f
        val halfH = geometry.height / 2f
        val sidePocketOffset = referenceRadius * 0.5f

        return listOf(
            PointF(-halfW, -halfH), PointF(halfW, -halfH),
            PointF(-halfW, halfH), PointF(halfW, halfH),
            PointF(0f, -halfH - sidePocketOffset), PointF(0f, halfH + sidePocketOffset)
        )
    }

    fun isPointInside(point: PointF): Boolean {
        val polygon = geometry.rotatedCorners
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

    fun findRailIntersectionAndNormal(startPoint: PointF, endPoint: PointF): Pair<PointF, PointF>? {
        if (!geometry.isValid) return null

        val rails = listOf(
            geometry.rotatedCorners[0] to geometry.rotatedCorners[1],
            geometry.rotatedCorners[1] to geometry.rotatedCorners[2],
            geometry.rotatedCorners[2] to geometry.rotatedCorners[3],
            geometry.rotatedCorners[3] to geometry.rotatedCorners[0]
        )
        val unrotatedNormals = listOf(PointF(0f, -1f), PointF(1f, 0f), PointF(0f, 1f), PointF(-1f, 0f))
        val angleRad = Math.toRadians(-rotationDegrees.toDouble())
        val cosA = cos(angleRad).toFloat()
        val sinA = sin(angleRad).toFloat()
        val normals = unrotatedNormals.map { n -> PointF(n.x * cosA - n.y * sinA, n.x * sinA + n.y * cosA) }

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
}