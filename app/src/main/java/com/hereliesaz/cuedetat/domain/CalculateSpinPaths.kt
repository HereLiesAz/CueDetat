// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/CalculateSpinPaths.kt

package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.view.renderer.util.SpinColorUtils
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Use case responsible for calculating the predicted path of the cue ball when spin (english) is applied.
 *
 * It generates a curved path (using Bezier curves) representing the swerve or deflection
 * caused by spin, and calculates reflections off rails if applicable.
 */
@Singleton
class CalculateSpinPaths @Inject constructor() {
    // Multiplier to determine how far to project the spin path.
    private val maxPathLengthFactor = 20f

    /**
     * Calculates the spin path based on the current state.
     *
     * @param state The current application state.
     * @return A map where the key is the path color (indicating spin severity) and the value is the list of points.
     */
    operator fun invoke(state: CueDetatState): Map<Color, List<PointF>> {
        // Determine the active spin offset (either currently dragging or lingering).
        val interactiveSpinOffset = state.lingeringSpinOffset ?: state.selectedSpinOffset ?: return emptyMap()

        // Calculate path for this specific offset.
        return calculateSinglePath(state, interactiveSpinOffset)
    }

    /**
     * Computes the path for a single spin configuration.
     */
    private fun calculateSinglePath(
        state: CueDetatState,
        spinOffset: PointF
    ): Map<Color, List<PointF>> {
        // The radius of the spin control UI, used for normalization.
        val spinControlRadius = 60f * 2

        // Normalize the spin vector magnitude (0.0 to 1.0).
        val spinMagnitude = hypot((spinOffset.x - spinControlRadius).toDouble(), (spinOffset.y - spinControlRadius).toDouble()).toFloat() / spinControlRadius
        // Calculate the angle of the spin vector.
        val angleDegrees = Math.toDegrees(atan2(spinOffset.y - spinControlRadius, spinOffset.x - spinControlRadius).toDouble()).toFloat()

        // Generate the curved path points based on physics approximation.
        val path = calculatePathForSpin(state, angleDegrees, spinMagnitude)

        // Determine the color of the path (e.g., green for safe, red for extreme spin).
        val color = SpinColorUtils.getColorFromAngleAndDistance(angleDegrees, spinMagnitude)

        // If the virtual table is active, clip/reflect the path against rails.
        val finalPath = if (state.table.isVisible) {
            bankCurve(path, state)
        } else {
            path
        }

        return mapOf(color to finalPath)
    }

    /**
     * Generates a Bezier curve representing the ball's trajectory under spin.
     *
     * @param state Current state.
     * @param angleDegrees Angle of the spin applied.
     * @param magnitude Intensity of the spin (0-1).
     */
    private fun calculatePathForSpin(
        state: CueDetatState,
        angleDegrees: Float,
        magnitude: Float
    ): List<PointF> {
        // Start from the ghost ball position (impact point).
        val startPoint = state.protractorUnit.ghostCueBallCenter
        // Target is the object ball center.
        val targetPoint = state.protractorUnit.center
        // Tangent direction determines the throw line.
        val tangentDirection = state.tangentDirection

        // Calculate vector from ghost ball to target ball.
        val dxToTarget = targetPoint.x - startPoint.x
        val dyToTarget = targetPoint.y - startPoint.y
        val magToTarget = hypot(dxToTarget.toDouble(), dyToTarget.toDouble()).toFloat()
        if (magToTarget < 0.001f) return emptyList()

        // Calculate the tangent vector (perpendicular to impact line).
        val tangentDx = (-dyToTarget / magToTarget) * tangentDirection
        val tangentDy = (dxToTarget / magToTarget) * tangentDirection

        // Convert spin angle to radians.
        val spinAngle = Math.toRadians(angleDegrees.toDouble()).toFloat()

        // Determine how much the ball curves based on spin intensity.
        val maxCurveOffset = state.protractorUnit.radius * 2.5f
        val curveAmount = magnitude * magnitude * maxCurveOffset

        // Define Bezier control points to create the swerve effect.
        // Control Point 1: Moves straight along the tangent line initially.
        val controlPoint1 = PointF(
            startPoint.x + tangentDx * (maxPathLengthFactor * state.protractorUnit.radius * 0.33f),
            startPoint.y + tangentDy * (maxPathLengthFactor * state.protractorUnit.radius * 0.33f)
        )

        // End Point: Where the path eventually leads (deviated by curveAmount).
        val endPoint = PointF(
            startPoint.x + tangentDx * (maxPathLengthFactor * state.protractorUnit.radius) + (curveAmount * cos(spinAngle)),
            startPoint.y + tangentDy * (maxPathLengthFactor * state.protractorUnit.radius) + (curveAmount * sin(spinAngle))
        )

        // Control Point 2: Adjusts the approach to the end point.
        val controlPoint2 = PointF(
            endPoint.x - tangentDx * (maxPathLengthFactor * state.protractorUnit.radius * 0.33f),
            endPoint.y - tangentDy * (maxPathLengthFactor * state.protractorUnit.radius * 0.33f)
        )

        // Generate the actual list of points for the curve.
        return generateBezierCurve(startPoint, controlPoint1, controlPoint2, endPoint)
    }

    /**
     * Truncates and reflects a curve path if it hits a table rail.
     */
    private fun bankCurve(path: List<PointF>, state: CueDetatState): List<PointF> {
        if (path.size < 2) return path

        // Iterate through path segments.
        for (i in 0 until path.size - 1) {
            val p1 = path[i]
            val p2 = path[i + 1]

            // Check if this segment crosses a rail.
            val intersectionResult = state.table.findRailIntersectionAndNormal(p1, p2)

            if (intersectionResult != null) {
                val (intersectionPoint, railNormal) = intersectionResult

                // Keep the path up to the intersection.
                val truncatedPath = path.subList(0, i + 1).toMutableList()
                truncatedPath.add(intersectionPoint)

                // Calculate reflection vector for the remaining momentum.
                val incidentVector = PointF(p2.x - p1.x, p2.y - p1.y)
                val reflectedVector = state.table.reflect(incidentVector, railNormal)

                // Add a straight line extension representing the rebound.
                val extendedEndPoint = PointF(
                    intersectionPoint.x + reflectedVector.x * 5000f,
                    intersectionPoint.y + reflectedVector.y * 5000f
                )
                truncatedPath.add(extendedEndPoint)

                return truncatedPath
            }
        }
        return path
    }


    /**
     * Generates points for a Cubic Bezier curve.
     *
     * @param p0 Start point.
     * @param p1 Control point 1.
     * @param p2 Control point 2.
     * @param p3 End point.
     * @param numPoints Number of segments to generate.
     */
    private fun generateBezierCurve(p0: PointF, p1: PointF, p2: PointF, p3: PointF, numPoints: Int = 30): List<PointF> {
        val curve = mutableListOf<PointF>()
        for (i in 0..numPoints) {
            val t = i.toFloat() / numPoints
            val u = 1 - t
            val tt = t * t
            val uu = u * u
            val uuu = uu * u
            val ttt = tt * t

            // Cubic Bezier formula: B(t) = (1-t)^3*P0 + 3(1-t)^2*t*P1 + 3(1-t)*t^2*P2 + t^3*P3
            val x = uuu * p0.x + 3 * uu * t * p1.x + 3 * u * tt * p2.x + ttt * p3.x
            val y = uuu * p0.y + 3 * uu * t * p1.y + 3 * u * tt * p2.y + ttt * p3.y
            curve.add(PointF(x, y))
        }
        return curve
    }
}
