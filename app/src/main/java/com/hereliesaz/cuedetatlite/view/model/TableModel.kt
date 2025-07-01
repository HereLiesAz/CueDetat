// hereliesaz/cuedetat/CueDetat-CueDetatLite/app/src/main/java/com/hereliesaz/cuedetatlite/view/model/TableModel.kt
package com.hereliesaz.cuedetatlite.view.model

import android.graphics.PointF
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

data class TableModel(
    val surface: RectF,
    val pockets: List<Pocket>,
    private val diamonds: List<PointF> // All diamonds in clockwise order
) {
    data class Pocket(val center: PointF, val radius: Float)

    companion object {
        fun create(width: Float, height: Float): TableModel {
            val tableWidth = width * 0.8f
            val tableHeight = tableWidth / 2
            val left = (width - tableWidth) / 2
            val top = (height - tableHeight) / 2
            val right = left + tableWidth
            val bottom = top + tableHeight

            val surface = RectF(left, top, right, bottom)

            val cornerPocketRadius = tableWidth / 22f
            val sidePocketRadius = tableWidth / 25f

            val pockets = listOf(
                Pocket(PointF(left, top), cornerPocketRadius), // 0: TL
                Pocket(PointF(right, top), cornerPocketRadius), // 1: TR
                Pocket(PointF(left, bottom), cornerPocketRadius), // 2: BL
                Pocket(PointF(right, bottom), cornerPocketRadius), // 3: BR
                Pocket(PointF(left + tableWidth / 2, top), sidePocketRadius), // 4: TM
                Pocket(PointF(left + tableWidth / 2, bottom), sidePocketRadius)  // 5: BM
            )

            // Diamonds are defined in clockwise order starting from top-left pocket.
            // Side pockets are included in the diamond count.
            val diamonds = listOf(
                pockets[0].center, // 0 - TL Pocket
                PointF(left + tableWidth / 4, top), // 1
                pockets[4].center, // 2 - TM Pocket
                PointF(left + 3 * tableWidth / 4, top), // 3
                pockets[1].center, // 4 - TR Pocket
                PointF(right, top + tableHeight / 2), // 5
                pockets[3].center, // 6 - BR Pocket
                PointF(right - tableWidth / 4, bottom), // 7
                pockets[5].center, // 8 - BM Pocket
                PointF(left + tableWidth / 4, bottom), // 9
                pockets[2].center, // 10 - BL Pocket
                PointF(left, top + tableHeight / 2) // 11
            )

            return TableModel(surface, pockets, diamonds)
        }
    }

    fun getDiamonds(): List<PointF> = diamonds

    fun calculateBankingPath(startPoint: PointF, aimPoint: PointF): List<PointF> {
        val path = mutableListOf<PointF>()
        var currentPoint = startPoint
        path.add(currentPoint)

        var dx = aimPoint.x - currentPoint.x
        var dy = aimPoint.y - currentPoint.y
        val mag = sqrt(dx.pow(2) + dy.pow(2))
        if (mag == 0f) return path
        dx /= mag
        dy /= mag

        for (i in 0..5) { // Max 6 banks
            val railHit = findNextRailHit(currentPoint, dx, dy)
            val pocketHit = findNextPocketHit(currentPoint, dx, dy)

            if (pocketHit != null && (railHit == null || pocketHit.first < railHit.first)) {
                path.add(pocketHit.second)
                return path // End path in pocket
            }

            if (railHit != null) {
                path.add(railHit.second)
                currentPoint = railHit.second
                when (railHit.third) {
                    0, 2 -> dy = -dy // Top/Bottom
                    1, 3 -> dx = -dx // Left/Right
                }
            } else {
                return path
            }
        }
        return path
    }

    fun getDiamondValue(point: PointF): Float? {
        val tolerance = 5f
        val railIndex = when {
            abs(point.y - surface.top) < tolerance -> 0    // Top
            abs(point.x - surface.right) < tolerance -> 1 // Right
            abs(point.y - surface.bottom) < tolerance -> 2 // Bottom
            abs(point.x - surface.left) < tolerance -> 3  // Left
            else -> return null
        }

        val (railDiamonds, baseIndex, reversed) = when(railIndex) {
            0 -> Triple(diamonds.slice(0..4), 0, false)
            1 -> Triple(diamonds.slice(4..6), 4, false)
            2 -> Triple(diamonds.slice(6..10), 6, true)
            3 -> Triple(listOf(diamonds[10], diamonds[11], diamonds[0]), 10, false)
            else -> return null
        }

        val isHorizontal = railIndex == 0 || railIndex == 2
        for (i in 0 until railDiamonds.size - 1) {
            val d1 = railDiamonds[i]
            val d2 = railDiamonds[i+1]
            val p = if (isHorizontal) point.x else point.y
            val start = if (isHorizontal) d1.x else d1.y
            val end = if (isHorizontal) d2.x else d2.y

            if ((p in start..end) || (p in end..start)) {
                val totalDist = abs(start - end)
                if (totalDist < 1e-6) continue
                val partialDist = abs(p - start)
                val fraction = partialDist / totalDist
                return (if(reversed) baseIndex + (railDiamonds.size - 2 - i) else baseIndex + i) + fraction
            }
        }
        return null
    }


    private fun findNextRailHit(p: PointF, dx: Float, dy: Float): Triple<Float, PointF, Int>? {
        var t = Float.MAX_VALUE
        var railIndex = -1

        // Top rail (index 0)
        if (dy < 0) { val tRail = (surface.top - p.y) / dy; if (tRail > 1e-6 && tRail < t) { t = tRail; railIndex = 0 } }
        // Right rail (index 1)
        if (dx > 0) { val tRail = (surface.right - p.x) / dx; if (tRail > 1e-6 && tRail < t) { t = tRail; railIndex = 1 } }
        // Bottom rail (index 2)
        if (dy > 0) { val tRail = (surface.bottom - p.y) / dy; if (tRail > 1e-6 && tRail < t) { t = tRail; railIndex = 2 } }
        // Left rail (index 3)
        if (dx < 0) { val tRail = (surface.left - p.x) / dx; if (tRail > 1e-6 && tRail < t) { t = tRail; railIndex = 3 } }

        return if (railIndex != -1) Triple(t, PointF(p.x + t * dx, p.y + t * dy), railIndex) else null
    }

    private fun findNextPocketHit(p: PointF, dx: Float, dy: Float): Pair<Float, PointF>? {
        var closestHit: Pair<Float, PointF>? = null
        for (pocket in pockets) {
            val oc = PointF(p.x - pocket.center.x, p.y - pocket.center.y)
            val b = 2 * (oc.x * dx + oc.y * dy)
            val c = oc.x * oc.x + oc.y * oc.y - pocket.radius * pocket.radius
            val discriminant = b * b - 4 * c
            if (discriminant >= 0) {
                val t = (-b - sqrt(discriminant)) / 2
                if (t > 1e-6) {
                    if (closestHit == null || t < closestHit.first) {
                        closestHit = Pair(t, PointF(p.x + t * dx, p.y + t * dy))
                    }
                }
            }
        }
        return closestHit
    }
}