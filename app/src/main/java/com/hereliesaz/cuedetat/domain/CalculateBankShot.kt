// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/CalculateBankShot.kt

package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import com.hereliesaz.cuedetat.view.model.Table
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

data class BankShotResult(val path: List<PointF>, val pocketedPocketIndex: Int?)

class CalculateBankShot @Inject constructor() {

    operator fun invoke(state: OverlayState): BankShotResult {
        if (!state.isBankingMode || state.onPlaneBall == null || state.bankingAimTarget == null) {
            return BankShotResult(emptyList(), null)
        }

        val path = mutableListOf(state.onPlaneBall.center)
        var currentPoint = state.onPlaneBall.center
        var direction = normalize(PointF(
            state.bankingAimTarget.x - currentPoint.x,
            state.bankingAimTarget.y - currentPoint.y
        ))
        var pocketedIndex: Int? = null

        for (i in 0..4) { // Limit to 4 banks (5 segments) as per doctrine.
            val endOfLine = PointF(currentPoint.x + direction.x * 5000f, currentPoint.y + direction.y * 5000f)
            val intersectionResult = state.table.findRailIntersectionAndNormal(currentPoint, endOfLine)

            if (intersectionResult != null) {
                val (intersection, normal) = intersectionResult

                val (pocketedOnSegment, pocketIntersectionPoint) = checkPocketAim(currentPoint, intersection, state.table, state.table.pockets)
                if (pocketedOnSegment != null && pocketIntersectionPoint != null) {
                    path.add(pocketIntersectionPoint) // Terminate the path AT the pocket
                    pocketedIndex = pocketedOnSegment
                    break
                }

                path.add(intersection)
                direction = state.table.reflect(direction, normal)
                currentPoint = intersection
            } else {
                path.add(endOfLine)
                break
            }
        }
        return BankShotResult(path, pocketedIndex)
    }

    fun getBankPathForLine(startPoint: PointF, endPoint: PointF, table: Table, pockets: List<PointF>): Pair<List<PointF>, Int?> {
        val extendedEnd = PointF(
            startPoint.x + (endPoint.x - startPoint.x) * 5000,
            startPoint.y + (endPoint.y - startPoint.y) * 5000
        )

        // First, check for a direct pocket.
        val (directPocketIndex, directIntersection) = checkPocketAim(startPoint, extendedEnd, table, pockets)
        if (directPocketIndex != null && directIntersection != null) {
            return Pair(listOf(startPoint, directIntersection), directPocketIndex)
        }

        // If no direct pocket, check for a one-rail bank.
        val intersectionResult = table.findRailIntersectionAndNormal(startPoint, extendedEnd)
        if (intersectionResult != null) {
            val (intersectionPoint, railNormal) = intersectionResult
            val incidentVector = PointF(extendedEnd.x - startPoint.x, extendedEnd.y - startPoint.y)
            val reflectedVector = table.reflect(incidentVector, railNormal)
            val reflectedEndPoint = PointF(
                intersectionPoint.x + reflectedVector.x * 5000,
                intersectionPoint.y + reflectedVector.y * 5000
            )

            // Check if the banked path goes into a pocket
            val (bankedPocketIndex, bankedIntersection) = checkPocketAim(intersectionPoint, reflectedEndPoint, table, pockets)
            if (bankedPocketIndex != null && bankedIntersection != null) {
                return Pair(listOf(startPoint, intersectionPoint, bankedIntersection), bankedPocketIndex)
            }
            return Pair(listOf(startPoint, intersectionPoint, reflectedEndPoint), null)
        }

        return Pair(listOf(startPoint, extendedEnd), null)
    }

    private fun checkPocketAim(start: PointF, end: PointF, table: Table, pockets: List<PointF>): Pair<Int?, PointF?> {
        val dirX = end.x - start.x
        val dirY = end.y - start.y
        val mag = sqrt(dirX * dirX + dirY * dirY)
        if (mag < 0.001f) return Pair(null, null)

        val extendedEnd = PointF(start.x + dirX / mag * 5000f, start.y + dirY / mag * 5000f)

        val pocketRadius = LOGICAL_BALL_RADIUS * 1.8f

        var closestIntersection: PointF? = null
        var closestPocketIndex: Int? = null
        var minDistanceSq = Float.MAX_VALUE

        pockets.forEachIndexed { index, pocket ->
            val intersection = getLineCircleIntersection(start, extendedEnd, pocket, pocketRadius)
            if (intersection != null) {
                val vecToPocketX = pocket.x - start.x
                val vecToPocketY = pocket.y - start.y
                val dotProduct = vecToPocketX * (dirX / mag) + vecToPocketY * (dirY / mag)
                if (dotProduct > 0) {
                    val distSq = (intersection.x - start.x).pow(2) + (intersection.y - start.y).pow(2)
                    if (distSq < minDistanceSq) {
                        minDistanceSq = distSq
                        closestIntersection = intersection
                        closestPocketIndex = index
                    }
                }
            }
        }
        return Pair(closestPocketIndex, closestIntersection)
    }

    private fun getLineCircleIntersection(p1: PointF, p2: PointF, circleCenter: PointF, radius: Float): PointF? {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        val a = dx * dx + dy * dy
        if (a < 0.0001f) return null
        val b = 2 * (dx * (p1.x - circleCenter.x) + dy * (p1.y - circleCenter.y))
        val c = (p1.x - circleCenter.x).pow(2) + (p1.y - circleCenter.y).pow(2) - radius * radius
        val delta = b * b - 4 * a * c
        if (delta < 0) return null
        // Find the t for the first intersection point along the line's direction.
        val t = (-b - sqrt(delta)) / (2 * a)
        return PointF(p1.x + t * dx, p1.y + t * dy)
    }

    private fun normalize(p: PointF): PointF {
        val mag = kotlin.math.hypot(p.x.toDouble(), p.y.toDouble()).toFloat()
        return if (mag > 0.001f) PointF(p.x / mag, p.y / mag) else PointF(0f, 0f)
    }
}