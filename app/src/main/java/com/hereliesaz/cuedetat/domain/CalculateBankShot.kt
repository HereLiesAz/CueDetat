// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/CalculateBankShot.kt

package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import javax.inject.Inject
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
        // Validation: Ensure banking mode is active and we have an aim target.
        if (!state.isBankingMode || state.bankingAimTarget == null) {
            return BankShotResult(emptyList(), null)
        }

        // Determine the starting point (cue ball position). 
        // Use CV-detected ball if available, otherwise fallback to the logical shot anchor.
        val startPoint = state.onPlaneBall?.center ?: state.shotLineAnchor ?: return BankShotResult(emptyList(), null)

        // Initialize path with the starting point.
        val path = mutableListOf(startPoint)
        var currentPoint = startPoint

        // Calculate the initial direction vector towards the aim target.
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
                val (pocketedOnSegment, pocketIntersectionPoint) = findPocketAim(
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
                // Pure mirror reflection. Spin is deliberately NOT passed: Table.reflect's spin term
                // (0.15 rad per unit spin) has no provenance and rotates the same way on every rail
                // regardless of the side the ball arrives from, so it bent banks the wrong way on
                // half the rails. A physical cushion/english model belongs in core/physics.
                direction = state.table.reflect(direction, normal)

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
     * Normalizes a vector to have a magnitude of 1.
     */
    private fun normalize(p: PointF): PointF {
        val mag = kotlin.math.hypot(p.x.toDouble(), p.y.toDouble()).toFloat()
        return if (mag > 0.001f) PointF(p.x / mag, p.y / mag) else PointF(0f, 0f)
    }
}

/** Effective pocket capture radius, as a multiple of the ball radius. */
private const val POCKET_CAPTURE_RADIUS_FACTOR = 1.8f

/**
 * Checks if the segment [start]→[end] passes through a pocket's capture circle (the single shared
 * pocket-aim test for bank legs and direct/banked aim lines). Bounded to the segment: a pocket
 * beyond [end] does not count, so callers wanting a ray pass an already-extended [end].
 *
 * @return the index of the first pocket hit along the segment (or null) and its entry point.
 */
internal fun findPocketAim(
    start: PointF,
    end: PointF,
    state: CueDetatState,
): Pair<Int?, PointF?> {
    val dirX = end.x - start.x
    val dirY = end.y - start.y
    val mag = sqrt(dirX * dirX + dirY * dirY)
    if (mag < 0.001f) return Pair(null, null)

    val pocketRadius = state.protractorUnit.radius * POCKET_CAPTURE_RADIUS_FACTOR

    var closestIntersection: PointF? = null
    var closestPocketIndex: Int? = null
    var minDistanceSq = Float.MAX_VALUE

    state.table.pockets.forEachIndexed { index, pocket ->
        val intersection = segmentCircleEntry(start, end, pocket, pocketRadius)
        if (intersection != null) {
            // The pocket must be in FRONT of the start point, not behind.
            val dot = (pocket.x - start.x) * (dirX / mag) + (pocket.y - start.y) * (dirY / mag)
            if (dot > 0) {
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
 * Entry point of the segment [p1]→[p2] into a circle, or null if the segment does not reach it
 * (entry parameter t outside [0, 1]). A segment starting inside the circle returns [p1].
 */
private fun segmentCircleEntry(p1: PointF, p2: PointF, circleCenter: PointF, radius: Float): PointF? {
    val dx = p2.x - p1.x
    val dy = p2.y - p1.y
    val a = dx * dx + dy * dy
    if (a < 0.0001f) return null
    val b = 2 * (dx * (p1.x - circleCenter.x) + dy * (p1.y - circleCenter.y))
    val c = (p1.x - circleCenter.x).pow(2) + (p1.y - circleCenter.y).pow(2) - radius * radius
    val delta = b * b - 4 * a * c
    if (delta < 0) return null
    val sq = sqrt(delta)
    val tEnter = (-b - sq) / (2 * a)
    val tExit = (-b + sq) / (2 * a)
    if (tExit < 0f || tEnter > 1f) return null
    val t = tEnter.coerceAtLeast(0f)
    return PointF(p1.x + t * dx, p1.y + t * dy)
}
