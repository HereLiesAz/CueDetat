// hereliesaz/cuedetat/CueDetat-CueDetatLite/app/src/main/java/com/hereliesaz/cuedetatlite/view/model/TableModel.kt
package com.hereliesaz.cuedetatlite.view.model

import android.graphics.PointF
import android.graphics.RectF
import kotlin.math.sqrt

data class TableModel(
    val bounds: RectF,
    val pockets: List<PointF>
) {
    private enum class Rail { TOP, BOTTOM, LEFT, RIGHT, NONE }
    private data class Intersection(val point: PointF, val rail: Rail)

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
        val path = mutableListOf(startPoint)
        var currentPoint = startPoint
        var direction = PointF(aimTarget.x - startPoint.x, aimTarget.y - startPoint.y)

        for (i in 0..3) {
            val intersection = findNextIntersection(currentPoint, direction)
            if (intersection != null) {
                path.add(intersection.point)
                currentPoint = intersection.point
                direction = when (intersection.rail) {
                    Rail.TOP, Rail.BOTTOM -> PointF(direction.x, -direction.y)
                    Rail.LEFT, Rail.RIGHT -> PointF(-direction.x, direction.y)
                    Rail.NONE -> break
                }
            } else {
                path.add(PointF(currentPoint.x + direction.x * 1000, currentPoint.y + direction.y * 1000))
                break
            }
        }
        return path
    }

    private fun findNextIntersection(origin: PointF, direction: PointF): Intersection? {
        var closestIntersection: Intersection? = null
        var minDistanceSq = Float.MAX_VALUE

        // Top Rail
        if (direction.y < 0) {
            val t = (bounds.top - origin.y) / direction.y
            if (t > 1e-6) {
                val x = origin.x + t * direction.x
                if (x >= bounds.left && x <= bounds.right) {
                    val point = PointF(x, bounds.top)
                    val distSq = distanceSq(origin, point)
                    if (distSq < minDistanceSq) {
                        minDistanceSq = distSq
                        closestIntersection = Intersection(point, Rail.TOP)
                    }
                }
            }
        }

        // Bottom Rail
        if (direction.y > 0) {
            val t = (bounds.bottom - origin.y) / direction.y
            if (t > 1e-6) {
                val x = origin.x + t * direction.x
                if (x >= bounds.left && x <= bounds.right) {
                    val point = PointF(x, bounds.bottom)
                    val distSq = distanceSq(origin, point)
                    if (distSq < minDistanceSq) {
                        minDistanceSq = distSq
                        closestIntersection = Intersection(point, Rail.BOTTOM)
                    }
                }
            }
        }

        // Left Rail
        if (direction.x < 0) {
            val t = (bounds.left - origin.x) / direction.x
            if (t > 1e-6) {
                val y = origin.y + t * direction.y
                if (y >= bounds.top && y <= bounds.bottom) {
                    val point = PointF(bounds.left, y)
                    val distSq = distanceSq(origin, point)
                    if (distSq < minDistanceSq) {
                        minDistanceSq = distSq
                        closestIntersection = Intersection(point, Rail.LEFT)
                    }
                }
            }
        }

        // Right Rail
        if (direction.x > 0) {
            val t = (bounds.right - origin.x) / direction.x
            if (t > 1e-6) {
                val y = origin.y + t * direction.y
                if (y >= bounds.top && y <= bounds.bottom) {
                    val point = PointF(bounds.right, y)
                    val distSq = distanceSq(origin, point)
                    if (distSq < minDistanceSq) {
                        minDistanceSq = distSq
                        closestIntersection = Intersection(point, Rail.RIGHT)
                    }
                }
            }
        }

        return closestIntersection
    }

    private fun distanceSq(p1: PointF, p2: PointF): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return dx * dx + dy * dy
    }
}