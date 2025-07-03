package com.hereliesaz.cuedetatlite.view.model

import android.graphics.PointF
import android.graphics.RectF
import kotlin.math.pow
import kotlin.math.sqrt

data class TableModel(
    val bounds: RectF,
    val pockets: List<PointF>
) {
    private enum class Rail { TOP, BOTTOM, LEFT, RIGHT, NONE }
    private data class IntersectionResult(val point: PointF?, val railHit: Rail, val distanceSq: Float)

    companion object {
        private const val TABLE_TO_BALL_RATIO_LONG = 88f
        private const val TABLE_TO_BALL_RATIO_SHORT = 44f
        private const val POCKET_TO_BALL_RATIO = 1.8f

        fun create(viewWidth: Float, viewHeight: Float, ballRadius: Float = 1f): TableModel {
            val tableHeight = TABLE_TO_BALL_RATIO_SHORT * ballRadius
            val tableWidth = TABLE_TO_BALL_RATIO_LONG * ballRadius
            val centerX = viewWidth / 2f
            val centerY = viewHeight / 2f

            val bounds = RectF(
                centerX - tableWidth / 2,
                centerY - tableHeight / 2,
                centerX + tableWidth / 2,
                centerY + tableHeight / 2
            )

            val pocketRadius = ballRadius * POCKET_TO_BALL_RATIO
            val pockets = listOf(
                PointF(bounds.left, bounds.top),
                PointF(bounds.right, bounds.top),
                PointF(bounds.left, bounds.bottom),
                PointF(bounds.right, bounds.bottom),
                PointF(centerX, bounds.top),
                PointF(centerX, bounds.bottom)
            )
            return TableModel(bounds, pockets)
        }
    }

    fun calculateBankingPath(startPoint: PointF, aimTarget: PointF): List<PointF> {
        val path = mutableListOf<PointF>()
        path.add(startPoint)

        var currentPoint = startPoint
        var currentAimVector = PointF(aimTarget.x - startPoint.x, aimTarget.y - startPoint.y)
        var lastHitRail = Rail.NONE

        for (i in 0..2) { // Calculate up to 3 banks
            val farOffTarget = PointF(currentPoint.x + currentAimVector.x * 1000, currentPoint.y + currentAimVector.y * 1000)
            val hitResult = findClosestRailIntersection(currentPoint, farOffTarget, lastHitRail)

            if (hitResult.point != null) {
                path.add(hitResult.point)
                currentPoint = hitResult.point
                currentAimVector = reflectVector(currentAimVector, hitResult.railHit)
                lastHitRail = hitResult.railHit

                if (i == 2) { // Add a final segment for the 3rd bank
                    path.add(PointF(currentPoint.x + currentAimVector.x * 1000, currentPoint.y + currentAimVector.y * 1000))
                }
            } else {
                path.add(farOffTarget)
                break // No more intersections found
            }
        }
        return path
    }

    private fun findClosestRailIntersection(start: PointF, endRayTarget: PointF, ignoreRail: Rail): IntersectionResult {
        var closestIntersection: PointF? = null
        var railHit = Rail.NONE
        var minDistanceSq = Float.MAX_VALUE

        val candidates = mutableListOf<IntersectionResult>()
        if (ignoreRail != Rail.TOP) getLineSegmentRayIntersection(start, endRayTarget, PointF(bounds.left, bounds.top), PointF(bounds.right, bounds.top))?.let { candidates.add(IntersectionResult(it, Rail.TOP, distanceSq(start, it))) }
        if (ignoreRail != Rail.BOTTOM) getLineSegmentRayIntersection(start, endRayTarget, PointF(bounds.left, bounds.bottom), PointF(bounds.right, bounds.bottom))?.let { candidates.add(IntersectionResult(it, Rail.BOTTOM, distanceSq(start, it))) }
        if (ignoreRail != Rail.LEFT) getLineSegmentRayIntersection(start, endRayTarget, PointF(bounds.left, bounds.top), PointF(bounds.left, bounds.bottom))?.let { candidates.add(IntersectionResult(it, Rail.LEFT, distanceSq(start, it))) }
        if (ignoreRail != Rail.RIGHT) getLineSegmentRayIntersection(start, endRayTarget, PointF(bounds.right, bounds.top), PointF(bounds.right, bounds.bottom))?.let { candidates.add(IntersectionResult(it, Rail.RIGHT, distanceSq(start, it))) }

        for (candidate in candidates) {
            if (candidate.point != null) {
                val dotProduct = (candidate.point.x - start.x) * (endRayTarget.x - start.x) + (candidate.point.y - start.y) * (endRayTarget.y - start.y)
                if (dotProduct >= -0.001f && candidate.distanceSq < minDistanceSq) {
                    minDistanceSq = candidate.distanceSq
                    closestIntersection = candidate.point
                    railHit = candidate.railHit
                }
            }
        }
        return IntersectionResult(closestIntersection, railHit, minDistanceSq)
    }

    private fun getLineSegmentRayIntersection(rayOrigin: PointF, rayTarget: PointF, segP1: PointF, segP2: PointF): PointF? {
        val rDx = rayTarget.x - rayOrigin.x; val rDy = rayTarget.y - rayOrigin.y
        val sDx = segP2.x - segP1.x; val sDy = segP2.y - segP1.y
        val rMagSq = rDx * rDx + rDy * rDy; val sMagSq = sDx * sDx + sDy * sDy
        if (rMagSq < 0.0001f || sMagSq < 0.0001f) return null
        val denominator = rDx * sDy - rDy * sDx
        if (kotlin.math.abs(denominator) < 0.0001f) return null
        val t = ((segP1.x - rayOrigin.x) * sDy - (segP1.y - rayOrigin.y) * sDx) / denominator
        val u = ((segP1.x - rayOrigin.x) * rDy - (segP1.y - rayOrigin.y) * rDx) / denominator
        if (t >= 0 && u >= 0 && u <= 1) return PointF(rayOrigin.x + t * rDx, rayOrigin.y + t * rDy)
        return null
    }

    private fun distanceSq(p1: PointF, p2: PointF): Float {
        val dx = p1.x - p2.x; val dy = p1.y - p2.y; return dx * dx + dy * dy
    }

    private fun reflectVector(incident: PointF, rail: Rail): PointF {
        return when (rail) {
            Rail.TOP, Rail.BOTTOM -> PointF(incident.x, -incident.y)
            Rail.LEFT, Rail.RIGHT -> PointF(-incident.x, incident.y)
            Rail.NONE -> incident
        }
    }
}
