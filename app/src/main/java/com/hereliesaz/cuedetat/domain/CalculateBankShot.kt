// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/CalculateBankShot.kt

package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Data class representing the result of a bank shot calculation.
 *
 * @property path A list of points defining the polyline path of the ball (start -> rail -> pocket/end).
 * @property pocketedPocketIndex The index of the pocket if the shot is successful, or null if it misses.
 */
data class BankShotResult(val path: List<PointF>, val pocketedPocketIndex: Int?)

/**
 * Use case responsible for calculating the trajectory of bank shots (shots reflecting off rails).
 *
 * It uses raycasting to determine intersections with table rails and calculates reflections
 * based on the angle of incidence. It simulates up to 4 banks.
 */
class CalculateBankShot @Inject constructor() {

    /**
     * Calculates the bank shot path based on the current state.
     *
     * @param state The current application state containing ball positions and table geometry.
     * @return A [BankShotResult] containing the path and success status.
     */
    operator fun invoke(state: CueDetatState): BankShotResult {
        // Validation: Ensure banking mode is active and necessary objects (cue ball, aim target) exist.
        if (!state.isBankingMode || state.onPlaneBall == null || state.bankingAimTarget == null) {
            return BankShotResult(emptyList(), null)
        }

        // Initialize path with the starting point (cue ball position).
        val path = mutableListOf(state.onPlaneBall.center)
        var currentPoint = state.onPlaneBall.center

        // Calculate the initial direction vector from the cue ball towards the aim target.
        var direction = normalize(PointF(
            state.bankingAimTarget.x - currentPoint.x,
            state.bankingAimTarget.y - currentPoint.y
        ))
        var pocketedIndex: Int? = null

        // Loop to simulate multiple bounces (banks).
        // Limit to 4 banks (5 path segments) to prevent infinite loops and stick to realistic shots.
        for (i in 0..4) {
            // Project a "long enough" ray in the current direction to guarantee it hits a rail.
            val endOfLine = PointF(currentPoint.x + direction.x * 5000f, currentPoint.y + direction.y * 5000f)

            // Find where this ray intersects with the table boundaries (rails).
            // This function returns the intersection point and the normal vector of the rail hit.
            val intersectionResult = state.table.findRailIntersectionAndNormal(currentPoint, endOfLine)

            if (intersectionResult != null) {
                val (intersection, normal) = intersectionResult

                // Check if the ball goes into a pocket on its way to the rail or exactly at the rail/corner.
                val (pocketedOnSegment, pocketIntersectionPoint) = checkPocketAim(
                    currentPoint,
                    intersection,
                    state
                )

                // If a pocket was hit:
                if (pocketedOnSegment != null && pocketIntersectionPoint != null) {
                    path.add(pocketIntersectionPoint) // Terminate the path AT the pocket center.
                    pocketedIndex = pocketedOnSegment // Store which pocket was hit.
                    break // Stop simulation (ball is potted).
                }

                // If no pocket was hit, the ball hits the rail.
                path.add(intersection)

                // Reflect the direction vector off the rail normal.
                // Pass spin offset if available to adjust reflection angle (english).
                direction = state.table.reflect(direction, normal, state.selectedSpinOffset?.x ?: 0f)

                // Update current point for the next iteration (bounce).
                currentPoint = intersection
            } else {
                // If no rail intersection found (unlikely given large projection), just add the end point.
                path.add(endOfLine)
                break
            }
        }
        // Return the full calculated path.
        return BankShotResult(path, pocketedIndex)
    }

    /**
     * Checks if a linear segment intersects with any of the table pockets.
     *
     * @param start The start point of the segment.
     * @param end The end point of the segment.
     * @param state The current state.
     * @return A Pair containing the index of the hit pocket (or null) and the intersection point.
     */
    private fun checkPocketAim(
        start: PointF,
        end: PointF,
        state: CueDetatState
    ): Pair<Int?, PointF?> {
        val dirX = end.x - start.x
        val dirY = end.y - start.y
        val mag = sqrt(dirX * dirX + dirY * dirY)
        if (mag < 0.001f) return Pair(null, null)

        // Extend the checking ray slightly beyond the segment end to ensure we catch pockets near corners.
        val extendedEnd = PointF(start.x + dirX / mag * 5000f, start.y + dirY / mag * 5000f)

        val pockets = state.table.pockets
        // Define effective pocket radius for hit detection (slightly larger than physical hole).
        val pocketRadius = state.protractorUnit.radius * 1.8f

        var closestIntersection: PointF? = null
        var closestPocketIndex: Int? = null
        var minDistanceSq = Float.MAX_VALUE

        // Check intersection against every pocket on the table.
        pockets.forEachIndexed { index, pocket ->
            // Use math to find intersection between line segment and circle.
            val intersection = getLineCircleIntersection(start, extendedEnd, pocket, pocketRadius)
            if (intersection != null) {
                // Verify direction: Dot product ensures the pocket is in FRONT of the start point, not behind.
                val vecToPocketX = pocket.x - start.x
                val vecToPocketY = pocket.y - start.y
                val dotProduct = vecToPocketX * (dirX / mag) + vecToPocketY * (dirY / mag)
                if (dotProduct > 0) {
                    // Check if this pocket is closer than any previously found pocket (find the FIRST hit).
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

    /**
     * Calculates the perpendicular distance from a point to a line segment.
     * (Currently unused but kept for utility).
     */
    private fun linePointDistance(p1: PointF, p2: PointF, p: PointF): Float {
        val num = abs((p2.x - p1.x) * (p1.y - p.y) - (p1.x - p.x) * (p2.y - p1.y))
        val den = sqrt((p2.x - p1.x).pow(2) + (p2.y - p1.y).pow(2))
        return if (den == 0f) 0f else num / den
    }

    /**
     * Calculates the intersection point(s) between a line defined by two points and a circle.
     * Returns the closest intersection point along the line direction, or null if no intersection.
     *
     * Solves the quadratic equation derived from substituting the line equation into the circle equation.
     */
    private fun getLineCircleIntersection(p1: PointF, p2: PointF, circleCenter: PointF, radius: Float): PointF? {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        // Coefficients for quadratic equation: at^2 + bt + c = 0
        val a = dx * dx + dy * dy
        if (a < 0.0001f) return null // Line length is zero.

        val b = 2 * (dx * (p1.x - circleCenter.x) + dy * (p1.y - circleCenter.y))
        val c = (p1.x - circleCenter.x).pow(2) + (p1.y - circleCenter.y).pow(2) - radius * radius

        val delta = b * b - 4 * a * c
        if (delta < 0) return null // No real roots -> No intersection.

        // Find t for the first intersection point (using -sqrt(delta)).
        val t = (-b - sqrt(delta)) / (2 * a)

        // Calculate coordinates using parameter t.
        return PointF(p1.x + t * dx, p1.y + t * dy)
    }

    /**
     * Normalizes a vector to have a magnitude of 1.
     */
    private fun normalize(p: PointF): PointF {
        val mag = kotlin.math.hypot(p.x.toDouble(), p.y.toDouble()).toFloat()
        return if (mag > 0.001f) PointF(p.x / mag, p.y / mag) else PointF(0f, 0f)
    }
}
