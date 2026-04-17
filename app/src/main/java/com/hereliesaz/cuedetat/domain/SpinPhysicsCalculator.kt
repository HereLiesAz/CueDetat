package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import com.hereliesaz.cuedetat.view.model.Table
import kotlin.math.*

object SpinPhysicsCalculator {
    private const val K1 = 0.04f // squirt coefficient
    private const val K2 = 0.15f // rail throw coefficient
    private const val K3 = 0.008f // spin decay rate per logical unit
    private const val PATH_LENGTH = 2000f // must reach any rail from anywhere on the table

    private const val STEP_SIZE = 5f
    private const val MAX_STEPS = (PATH_LENGTH / STEP_SIZE).toInt()

    fun calculatePath(
        spinOffset: PointF, // normalized -1..1; x = lateral English
        cueBallPos: PointF, // world coords — ghost cue ball position (where contact occurs)
        targetBallPos: PointF, // world coords — object ball center
        shotAngle: Float, // radians — direction cue was traveling before contact
        table: Table,
        maxBounces: Int = 2
    ): List<PointF> {
        val dx = targetBallPos.x - cueBallPos.x
        val dy = targetBallPos.y - cueBallPos.y
        val mag = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        if (mag < 0.001f) return listOf(cueBallPos)

        val impactAngle = atan2(dy, dx)
        val cross = sin(shotAngle - impactAngle)
        val tangentSide = if (cross >= 0f) 1f else -1f
        val tangentAngle = impactAngle + tangentSide * (PI / 2).toFloat()

        var cutAngle = abs(shotAngle - tangentAngle)
        if (cutAngle > PI) cutAngle = (2 * PI - cutAngle).toFloat()

        val squirt = K1 * spinOffset.x * sin(cutAngle)
        var currentAngle = tangentAngle + squirt
        var omega = spinOffset.x

        val points = mutableListOf(cueBallPos)
        var currentPos = cueBallPos
        var totalDistance = 0f

        val pocketThreshold = LOGICAL_BALL_RADIUS * 1.5f

        repeat(maxBounces + 1) {
            if (!table.isVisible) {
                val endX = currentPos.x + cos(currentAngle) * PATH_LENGTH
                val endY = currentPos.y + sin(currentAngle) * PATH_LENGTH
                val endPoint = PointF().apply { x = endX; y = endY }
                points.add(endPoint)
                return points
            }

            var hitRail = false
            
            for (step in 1..MAX_STEPS) {
                val swerveAmount = 0.001f * omega * exp((-K3 * totalDistance).toDouble()).toFloat()
                currentAngle += swerveAmount
                
                val nextX = currentPos.x + cos(currentAngle) * STEP_SIZE
                val nextY = currentPos.y + sin(currentAngle) * STEP_SIZE
                val nextPos = PointF().apply { x = nextX; y = nextY }
                
                totalDistance += STEP_SIZE
                
                for (pocket in table.pockets) {
                    val pdx = nextX - pocket.x
                    val pdy = nextY - pocket.y
                    if (hypot(pdx.toDouble(), pdy.toDouble()).toFloat() < pocketThreshold) {
                        points.add(nextPos)
                        return points
                    }
                }
                
                val railHit = table.findRailIntersectionAndNormal(currentPos, nextPos)
                if (railHit != null) {
                    val intersection = railHit.first
                    val normal = railHit.second
                    points.add(intersection)
                    
                    val omegaAtRail = abs(omega) * exp((-K3 * totalDistance).toDouble()).toFloat()
                    val dot = cos(currentAngle) * normal.x + sin(currentAngle) * normal.y
                    val reflectedX = cos(currentAngle) - 2f * dot * normal.x
                    val reflectedY = sin(currentAngle) - 2f * dot * normal.y
                    val reflectedAngle = atan2(reflectedY, reflectedX)

                    val normalAngle = atan2(normal.y, normal.x)
                    var incidentAngle = abs(currentAngle - (normalAngle + PI.toFloat()))
                    if (incidentAngle > PI) incidentAngle = (2 * PI - incidentAngle).toFloat()

                    val throwAmount = K2 * omegaAtRail * cos(incidentAngle) * sign(spinOffset.x)
                    currentAngle = reflectedAngle + throwAmount
                    omega = omegaAtRail * sign(spinOffset.x)
                    currentPos = intersection
                    hitRail = true
                    break
                }
                
                if (step == MAX_STEPS || step % 5 == 0) {
                    points.add(nextPos)
                }
                
                currentPos = nextPos
            }
            
            if (!hitRail) {
                points.add(currentPos)
                return points
            }
        }
        return points
    }
}
