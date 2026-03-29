package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import com.hereliesaz.cuedetat.view.model.Table
import kotlin.math.*

object SpinPhysicsCalculator {
    private const val K1 = 0.04f // squirt coefficient
    private const val K2 = 0.15f // rail throw coefficient
    private const val K3 = 0.008f // spin decay rate per logical unit
    private const val PATH_LENGTH = 2000f // must reach any rail from anywhere on the table

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
        var omega = abs(spinOffset.x)

        val points = mutableListOf(cueBallPos)
        var currentPos = cueBallPos
        var totalDistance = 0f

        val pocketThreshold = LOGICAL_BALL_RADIUS * 1.5f

        repeat(maxBounces + 1) {
            val endX = currentPos.x + cos(currentAngle) * PATH_LENGTH
            val endY = currentPos.y + sin(currentAngle) * PATH_LENGTH
            val endPoint = PointF().apply { x = endX; y = endY }

            if (!table.isVisible) {
                points.add(endPoint)
                return points
            }

            // --- Pocket detection: find the closest pocket entry along this segment ---
            val segDx = endX - currentPos.x
            val segDy = endY - currentPos.y
            val segLenSq = segDx * segDx + segDy * segDy
            var pocketEntry: PointF? = null
            var pocketT = Float.MAX_VALUE

            if (segLenSq > 0f) {
                for (pocket in table.pockets) {
                    val t = ((pocket.x - currentPos.x) * segDx + (pocket.y - currentPos.y) * segDy) / segLenSq
                    val ct = t.coerceIn(0f, 1f)
                    val cx = currentPos.x + ct * segDx
                    val cy = currentPos.y + ct * segDy
                    val dist = hypot((cx - pocket.x).toDouble(), (cy - pocket.y).toDouble()).toFloat()
                    if (dist < pocketThreshold && ct < pocketT) {
                        pocketT = ct
                        pocketEntry = PointF().apply { x = cx; y = cy }
                    }
                }
            }

            // --- Rail detection ---
            val railHit = table.findRailIntersectionAndNormal(currentPos, endPoint)
            val railT = if (railHit != null && segLenSq > 0f) {
                val rdx = railHit.first.x - currentPos.x
                val rdy = railHit.first.y - currentPos.y
                hypot(rdx.toDouble(), rdy.toDouble()).toFloat() / sqrt(segLenSq)
            } else Float.MAX_VALUE

            // Stop at whichever comes first: pocket or rail
            if (pocketEntry != null && pocketT <= railT) {
                points.add(pocketEntry)
                return points
            }

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

            val omegaAtRail = omega * exp((-K3 * totalDistance).toDouble()).toFloat()
            val dot = cos(currentAngle) * normal.x + sin(currentAngle) * normal.y
            val reflectedX = cos(currentAngle) - 2f * dot * normal.x
            val reflectedY = sin(currentAngle) - 2f * dot * normal.y
            val reflectedAngle = atan2(reflectedY, reflectedX)

            val normalAngle = atan2(normal.y, normal.x)
            var incidentAngle = abs(currentAngle - (normalAngle + PI.toFloat()))
            if (incidentAngle > PI) incidentAngle = (2 * PI - incidentAngle).toFloat()

            val throwAmount = K2 * omegaAtRail * cos(incidentAngle) * sign(spinOffset.x)
            currentAngle = reflectedAngle + throwAmount
            omega = omegaAtRail
            currentPos = intersection
        }
        return points
    }
}