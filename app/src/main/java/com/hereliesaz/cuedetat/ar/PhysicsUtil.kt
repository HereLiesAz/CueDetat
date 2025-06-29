package com.hereliesaz.cuedetat.ar

import com.hereliesaz.cuedetat.ui.Rail
import dev.romainguy.kotlin.math.Float3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A utility object for handling complex shot physics and geometric calculations.
 */
object PhysicsUtil {

    /**
     * Calculates the points for a parabolic arc, representing a jump shot.
     * @param start The starting position of the arc.
     * @param end The ending position of the arc.
     * @param height The maximum height of the arc at its midpoint.
     * @param steps The number of line segments to use to approximate the curve.
     * @return A list of Float3 points representing the arc.
     */
    fun calculateJumpArc(start: Float3, end: Float3, height: Float, steps: Int = 20): List<Float3> {
        val points = mutableListOf<Float3>()
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            // Linear interpolation for X and Z coordinates
            val x = (1 - t) * start.x + t * end.x
            val z = (1 - t) * start.z + t * end.z
            // Parabolic interpolation for Y (height) using the formula 4h(t - t^2)
            // This creates a smooth arc that starts and ends at y=0 and peaks at height `h`.
            val y = start.y + 4 * height * (t - t * t)
            points.add(Float3(x, y, z))
        }
        return points
    }

    /**
     * Calculates the points for a curved path, representing a masse shot.
     * This uses a quadratic Bezier curve for a simple, visually effective approximation.
     * @param start The starting position of the curve.
     * @param end The ending position of the curve.
     * @param spinOffset A normalized Offset where x represents side spin (-1.0 to 1.0).
     * @param curveFactor A multiplier to control how much the curve bends.
     * @param steps The number of line segments to use.
     * @return A list of Float3 points representing the curve.
     */
    fun calculateMasseCurve(start: Float3, end: Float3, spinOffset: androidx.compose.ui.geometry.Offset, curveFactor: Float = 0.5f, steps: Int = 20): List<Float3> {
        val points = mutableListOf<Float3>()
        val shotVector = end - start
        // Get a vector perpendicular to the shot line on the XZ plane
        val perpendicular = Float3(-shotVector.z, 0f, shotVector.x).normalize()

        // The control point is the midpoint, pushed sideways by the spin amount.
        val controlPoint = (start + end) / 2f + perpendicular * spinOffset.x * curveFactor

        for (i in 0..steps) {
            val t = i.toFloat() / steps
            // Quadratic Bezier curve formula: (1-t)^2 * P0 + 2(1-t)t * P1 + t^2 * P2
            val p = (1 - t).let { it * it } * start + 2 * (1 - t) * t * controlPoint + t * t * end
            points.add(p)
        }
        return points
    }

    /**
     * Calculates the reflection point on a rail for bank and kick shots.
     * @return A Pair containing the aim point on the rail and the mirrored position of the target.
     */
    fun calculateBank(ballToHit: Float3, startBall: Float3, rail: Rail): Pair<Float3, Float3>? {
        val railPos = when(rail) {
            Rail.TOP -> ARConstants.TABLE_DEPTH / 2f
            Rail.BOTTOM -> -ARConstants.TABLE_DEPTH / 2f
            Rail.LEFT -> -ARConstants.TABLE_WIDTH / 2f
            Rail.RIGHT -> ARConstants.TABLE_WIDTH / 2f
        }
        val mirroredPos = when(rail) {
            Rail.TOP, Rail.BOTTOM -> Float3(ballToHit.x, ballToHit.y, railPos + (railPos - ballToHit.z))
            Rail.LEFT, Rail.RIGHT -> Float3(railPos + (railPos - ballToHit.x), ballToHit.y, ballToHit.z)
        }
        val aimVec = mirroredPos - startBall
        val t = when(rail) {
            Rail.TOP, Rail.BOTTOM -> if (aimVec.z == 0f) return null else (railPos - startBall.z) / aimVec.z
            Rail.LEFT, Rail.RIGHT -> if (aimVec.x == 0f) return null else (railPos - startBall.x) / aimVec.x
        }
        return Pair(startBall + t * aimVec, mirroredPos)
    }

    /**
     * Converts a point on a rail to its diamond system value.
     */
    fun getDiamondText(railPoint: Float3, rail: Rail): String {
        val halfWidth = ARConstants.TABLE_WIDTH / 2f
        val halfDepth = ARConstants.TABLE_DEPTH / 2f

        // The diamond system typically starts from a corner. We add the half-dimension
        // to convert from a centered coordinate system (-L/2 to +L/2) to a 0-L system.
        val value = when(rail) {
            Rail.TOP, Rail.BOTTOM -> (railPoint.x + halfWidth) / ARConstants.DIAMOND_SPACING_WIDTH
            Rail.LEFT, Rail.RIGHT -> (railPoint.z + halfDepth) / ARConstants.DIAMOND_SPACING_DEPTH
        }
        return "%.1f Diamonds".format(value)
    }
}