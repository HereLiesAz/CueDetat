package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import com.hereliesaz.cuedetat.view.model.Table
import kotlin.math.*

object MassePhysicsSimulator {

    private const val STEPS = 100
    private const val MU_ROLL = 0.02f
    private const val EPSILON = 0.5f
    private const val ELASTIC = 0.65f
    private const val V0 = 60f
    private const val R = LOGICAL_BALL_RADIUS

    fun simulate(
        contactOffset: PointF,
        elevationDeg: Float,
        shotAngle: Float,
        table: Table,
        mu: Float = 1.5f,
        startPos: PointF = PointF()
    ): MasseResult {
        val alpha = Math.toRadians(elevationDeg.toDouble()).toFloat()

        var vx = V0 * cos(alpha)
        var vy = 0f
        var omegaX = -(5f / 2f) * V0 * sin(alpha) * contactOffset.x / R
        var omegaY = -(5f / 2f) * V0 * sin(alpha) * contactOffset.y / R

        val rotAngle = shotAngle + (PI / 2).toFloat()
        val cosR = cos(rotAngle)
        val sinR = sin(rotAngle)

        var posX = startPos.x * cosR + startPos.y * sinR
        var posY = -startPos.x * sinR + startPos.y * cosR

        val points = mutableListOf(PointF(posX, posY))

        val pocketThreshold = R * 1.3f
        val pocketThresholdSq = pocketThreshold * pocketThreshold
        var pocketIndex: Int? = null
        val impactPoints = mutableListOf<PointF>()

        for (step in 1..STEPS) {
            val slipX = vx - R * omegaY
            val slipY = vy + R * omegaX

            val slipSpeed = sqrt(slipX * slipX + slipY * slipY)

            if (slipSpeed > EPSILON) {
                val fx = -mu * slipX / slipSpeed
                val fy = -mu * slipY / slipSpeed

                vx += fx
                vy += fy

                omegaX += (5f / 2f) * fy / R
                omegaY -= (5f / 2f) * fx / R
            } else {
                vx *= (1f - MU_ROLL)
                vy *= (1f - MU_ROLL)
            }

            val nextX = posX + vx
            val nextY = posY + vy

            val worldCurX = posX * cosR - posY * sinR
            val worldCurY = posX * sinR + posY * cosR
            val worldNextX = nextX * cosR - nextY * sinR
            val worldNextY = nextX * sinR + nextY * cosR

            if (table.isVisible) {
                // Segment-to-pocket distance check to prevent tunneling
                for (idx in table.pockets.indices) {
                    val p = table.pockets[idx]
                    val sdx = worldNextX - worldCurX
                    val sdy = worldNextY - worldCurY
                    val slenSq = sdx * sdx + sdy * sdy
                    
                    val distSq = if (slenSq < 0.001f) {
                        (worldNextX - p.x).pow(2) + (worldNextY - p.y).pow(2)
                    } else {
                        var t = ((p.x - worldCurX) * sdx + (p.y - worldCurY) * sdy) / slenSq
                        t = t.coerceIn(0f, 1f)
                        val projX = worldCurX + t * sdx
                        val projY = worldCurY + t * sdy
                        (projX - p.x).pow(2) + (projY - p.y).pow(2)
                    }
                    
                    if (distSq < pocketThresholdSq) {
                        pocketIndex = idx
                        break
                    }
                }
            }
            if (pocketIndex != null) break

            if (table.isVisible) {
                val railHit = table.findRailIntersectionAndNormal(
                    PointF(worldCurX, worldCurY),
                    PointF(worldNextX, worldNextY)
                )
                if (railHit != null) {
                    val (worldIntersect, worldNormal) = railHit

                    val localIx = worldIntersect.x * cosR + worldIntersect.y * sinR
                    val localIy = -worldIntersect.x * sinR + worldIntersect.y * cosR
                    points.add(PointF(localIx, localIy))

                    val ghostWorldX = worldIntersect.x + worldNormal.x * R
                    val ghostWorldY = worldIntersect.y + worldNormal.y * R
                    val ghostLocalIx = ghostWorldX * cosR + ghostWorldY * sinR
                    val ghostLocalIy = -ghostWorldX * sinR + ghostWorldY * cosR
                    impactPoints.add(PointF(ghostLocalIx, ghostLocalIy))

                    val localNx = worldNormal.x * cosR + worldNormal.y * sinR
                    val localNy = -worldNormal.x * sinR + worldNormal.y * cosR

                    val dot = vx * localNx + vy * localNy
                    vx = (vx - 2f * dot * localNx) * ELASTIC
                    vy = (vy - 2f * dot * localNy) * ELASTIC

                    val rfx = -mu * (-dot * localNx).let { if (abs(it) > EPSILON) it / abs(it) else 0f }
                    val rfy = -mu * (-dot * localNy).let { if (abs(it) > EPSILON) it / abs(it) else 0f }
                    omegaX += (5f / 2f) * rfy / R
                    omegaY -= (5f / 2f) * rfx / R

                    posX = localIx
                    posY = localIy
                    continue
                }
            }

            posX = nextX
            posY = nextY
            points.add(PointF(posX, posY))

            if (vx * vx + vy * vy < 0.0025f) break // approx 0.05f speed squared
        }

        val worldPoints = points.map { p ->
            PointF(p.x * cosR - p.y * sinR, p.x * sinR + p.y * cosR)
        }
        val worldImpacts = impactPoints.map { p ->
            PointF(p.x * cosR - p.y * sinR, p.x * sinR + p.y * cosR)
        }

        return MasseResult(
            points = worldPoints,
            pocketIndex = pocketIndex,
            impactPoints = worldImpacts,
            isAirborne = false,
            peakHeight = 0f
        )
    }
}
