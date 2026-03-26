package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import com.hereliesaz.cuedetat.view.model.Table
import kotlin.math.*

object SpinPhysicsCalculator {

    private const val K1 = 0.04f        // squirt coefficient
    private const val K2 = 0.15f        // rail throw coefficient
    private const val K3 = 0.008f       // spin decay rate per logical unit
    private const val PATH_LENGTH = 5000f

    fun calculatePath(
        spinOffset: PointF,      // normalized -1..1; x = lateral English
        cueBallPos: PointF,      // world coords — ghost cue ball position (where contact occurs)
        targetBallPos: PointF,   // world coords — object ball center
        shotAngle: Float,        // radians — direction cue was traveling before contact
        table: Table,
        maxBounces: Int = 2
    ): List<PointF> {
        val dx = targetBallPos.x - cueBallPos.x
        val dy = targetBallPos.y - cueBallPos.y
        val mag = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        if (mag < 0.001f) return listOf(cueBallPos)

        val impactAngle = atan2(dy, dx)

        // Which perpendicular does the cue ball take? Determined by which side the cue came from.
        // cross = sin(shotAngle - impactAngle): positive → ball deflects CCW (left of impact line)
        val cross = sin(shotAngle - impactAngle)
        val tangentSide = if (cross >= 0f) 1f else -1f
        val tangentAngle = impactAngle + tangentSide * (PI / 2).toFloat()

        // Cut angle: angle between shot direction and tangent line
        var cutAngle = abs(shotAngle - tangentAngle)
        if (cutAngle > PI) cutAngle = (2 * PI - cutAngle).toFloat()

        // Squirt: lateral spin deflects the cue ball slightly off the pure tangent
        val squirt = K1 * spinOffset.x * sin(cutAngle)
        var currentAngle = tangentAngle + squirt

        var omega = abs(spinOffset.x)   // initial spin magnitude (0..1)
        val points = mutableListOf(cueBallPos)
        var currentPos = cueBallPos
        var totalDistance = 0f

        // One initial segment + up to maxBounces reflections
        repeat(maxBounces + 1) {
            val endX = currentPos.x + cos(currentAngle) * PATH_LENGTH
            val endY = currentPos.y + sin(currentAngle) * PATH_LENGTH
            val endPoint = PointF(endX, endY)

            if (!table.isVisible) {
                points.add(endPoint)
                return points
            }

            val railHit = table.findRailIntersectionAndNormal(currentPos, endPoint)
            if (railHit == null) {
                points.add(endPoint)
                return points
            }

            val (intersection, normal) = railHit
            val segDist = hypot(
                (intersection.x - currentPos.x).toDouble(),
                (intersection.y - currentPos.y).toDouble()
            ).toFloat()
            totalDistance += segDist
            points.add(intersection)

            // Spin decay along this segment's distance
            val omegaAtRail = omega * exp((-K3 * totalDistance).toDouble()).toFloat()

            // Geometric reflection
            val dot = cos(currentAngle) * normal.x + sin(currentAngle) * normal.y
            val reflectedX = cos(currentAngle) - 2f * dot * normal.x
            val reflectedY = sin(currentAngle) - 2f * dot * normal.y
            val reflectedAngle = atan2(reflectedY, reflectedX)

            // Incident angle for throw formula
            val normalAngle = atan2(normal.y, normal.x)
            var incidentAngle = abs(currentAngle - (normalAngle + PI.toFloat()))
            if (incidentAngle > PI) incidentAngle = (2 * PI - incidentAngle).toFloat()

            // Rail throw: running English widens angle; reverse English narrows
            val throwAmount = K2 * omegaAtRail * cos(incidentAngle) * sign(spinOffset.x)
            currentAngle = reflectedAngle + throwAmount

            omega = omegaAtRail
            currentPos = intersection
        }

        return points
    }
}
