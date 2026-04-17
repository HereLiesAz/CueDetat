package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import com.hereliesaz.cuedetat.view.model.Table
import kotlin.math.*

object SpinPhysicsCalculator {
    private const val K_SQUIRT = 0.04f   // Side-spin squirt factor
    private const val K_THROW = 0.15f    // Rail throw factor
    private const val K_CURVE = 0.015f   // Follow/Draw curve induction rate
    private const val K_DECAY = 0.005f   // Spin decay rate per logical unit
    private const val PATH_LENGTH = 2000f 

    private const val STEP_SIZE = 4f
    private const val PATH_SAMPLING_RATE = 4
    private const val MAX_STEPS = (PATH_LENGTH / STEP_SIZE).toInt()

    fun calculatePath(
        spinOffset: Vector2, // x = Lateral English (-1..1), y = Follow/Draw (-1..1)
        cueBallPos: Vector2,
        targetBallPos: Vector2,
        shotAngle: Float,
        table: Table,
        velocity: Float = 1.0f,
        maxBounces: Int = 2
    ): List<Vector2> {
        val dx = targetBallPos.x - cueBallPos.x
        val dy = targetBallPos.y - cueBallPos.y
        val distToTarget = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        if (distToTarget < 0.001f) return listOf(cueBallPos)

        // 1. Initial Impact Geometry
        val impactAngle = atan2(dy, dx)
        val cross = sin(shotAngle - impactAngle)
        val tangentSide = if (cross >= 0f) 1f else -1f
        val tangentAngle = impactAngle + tangentSide * (PI / 2).toFloat()
        
        // 2. Initial State
        // sideSpin (x) primarily affects rail reflections.
        // verticalSpin (y) causes the post-collision curve.
        var currentAngle = tangentAngle 
        var sideSpin = spinOffset.x
        var verticalSpin = spinOffset.y
        
        val points = mutableListOf(cueBallPos)
        var currentPos = cueBallPos
        var totalDistance = 0f
        val pocketThreshold = LOGICAL_BALL_RADIUS * 1.5f

        repeat(maxBounces + 1) {
            if (!table.isVisible) {
                val endX = currentPos.x + cos(currentAngle) * PATH_LENGTH
                val endY = currentPos.y + sin(currentAngle) * PATH_LENGTH
                points.add(Vector2(endX, endY))
                return points
            }

            var hitRail = false
            for (step in 1..MAX_STEPS) {
                // 3. CURVE INDUCTION (Follow/Draw)
                // The vertical spin induces a force that pulls the velocity vector toward the natural angle.
                // For follow (y > 0), target direction is shotAngle.
                // For draw (y < 0), target direction is shotAngle + PI.
                val naturalAngle = if (verticalSpin >= 0) shotAngle else (shotAngle + PI.toFloat())
                
                // Curve speed depends on spin magnitude and distance traveled.
                val spinEff = verticalSpin * exp((-K_DECAY * totalDistance).toDouble()).toFloat()
                
                // Gradually rotate currentAngle toward naturalAngle
                val angleDiff = normalizeAngle(naturalAngle - currentAngle)
                val rotateAmount = angleDiff * K_CURVE * abs(spinEff)
                currentAngle += rotateAmount

                val nextX = currentPos.x + cos(currentAngle) * STEP_SIZE
                val nextY = currentPos.y + sin(currentAngle) * STEP_SIZE
                val nextPos = Vector2(nextX, nextY)
                totalDistance += STEP_SIZE

                // 4. Pocket detection
                if (checkPocket(currentPos, nextPos, table, pocketThreshold)) {
                    points.add(nextPos)
                    return points
                }

                // 5. Rail detection
                val railHit = table.findRailIntersectionAndNormal(currentPos.toPointF(), nextPos.toPointF())
                if (railHit != null) {
                    val intersection = railHit.first.toVector2()
                    val normal = railHit.second.toVector2()
                    points.add(intersection)

                    // REFLECTION WITH ENGLISH
                    val dot = cos(currentAngle) * normal.x + sin(currentAngle) * normal.y
                    val reflectedX = cos(currentAngle) - 2f * dot * normal.x
                    val reflectedY = sin(currentAngle) - 2f * dot * normal.y
                    val reflectedAngle = atan2(reflectedY, reflectedX)

                    val normalAngle = atan2(normal.y, normal.x)
                    var incidentAngle = abs(currentAngle - (normalAngle + PI.toFloat()))
                    if (incidentAngle > PI) incidentAngle = (2 * PI - incidentAngle).toFloat()

                    val sideSpinEff = sideSpin * exp((-K_DECAY * totalDistance).toDouble()).toFloat()
                    val throwAmount = K_THROW * sideSpinEff * cos(incidentAngle)
                    
                    currentAngle = reflectedAngle + throwAmount
                    currentPos = intersection
                    hitRail = true
                    break
                }

                if (step % PATH_SAMPLING_RATE == 0) {
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

    private fun checkPocket(start: Vector2, end: Vector2, table: Table, threshold: Float): Boolean {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val magSq = dx * dx + dy * dy
        if (magSq < 0.001f) return false
        for (pocket in table.pockets) {
            val t = ((pocket.x - start.x) * dx + (pocket.y - start.y) * dy) / magSq
            val ct = t.coerceIn(0f, 1f)
            val dist = hypot((start.x + ct * dx - pocket.x).toDouble(), (start.y + ct * dy - pocket.y).toDouble()).toFloat()
            if (dist < threshold) return true
        }
        return false
    }

    private fun normalizeAngle(a: Float): Float {
        var ang = a
        while (ang <= -PI) ang += (2 * PI).toFloat()
        while (ang > PI) ang -= (2 * PI).toFloat()
        return ang
    }
}
