package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.sqrt

data class BankShotResult(
    val path: List<PointF>,
    val pocketedPocketIndex: Int?
)

@Singleton
class CalculateBankShot @Inject constructor() {

    private val tableToBallRatioLong = 88f
    private val tableToBallRatioShort = 44f
    private val maxBounces = 4 // Up to 4 rails

    operator fun invoke(state: OverlayState): BankShotResult {
        if (!state.isBankingMode || state.onPlaneBall == null || state.bankingAimTarget == null) {
            return BankShotResult(emptyList(), null)
        }

        val referenceRadius = state.protractorUnit.radius
        val logicalTableWidth = tableToBallRatioLong * referenceRadius
        val logicalTableHeight = tableToBallRatioShort * referenceRadius

        val halfW = logicalTableWidth / 2f
        val halfH = logicalTableHeight / 2f

        // Pockets are defined in the same logical space
        val pocketRadius = referenceRadius * 1.8f
        val pockets = listOf(
            PointF(-halfW, -halfH), PointF(halfW, -halfH), // Top corners
            PointF(-halfW, halfH), PointF(halfW, halfH),   // Bottom corners
            PointF(0f, -halfH), PointF(0f, halfH)          // Side pockets
        )

        var currentPos = state.onPlaneBall.center
        val targetPos = state.bankingAimTarget
        var direction = PointF(targetPos.x - currentPos.x, targetPos.y - currentPos.y).normalized()

        val path = mutableListOf(currentPos)
        var pocketedIndex: Int? = null

        for (i in 0 until maxBounces) {
            var t = Float.MAX_VALUE
            var wallNormal: PointF? = null

            // Time to hit vertical walls
            if (direction.x != 0f) {
                val tLeft = (-halfW - currentPos.x) / direction.x
                val tRight = (halfW - currentPos.x) / direction.x
                if (tLeft > 0.001f && tLeft < t) {
                    t = tLeft
                    wallNormal = PointF(1f, 0f)
                }
                if (tRight > 0.001f && tRight < t) {
                    t = tRight
                    wallNormal = PointF(-1f, 0f)
                }
            }
            // Time to hit horizontal walls
            if (direction.y != 0f) {
                val tTop = (-halfH - currentPos.y) / direction.y
                val tBottom = (halfH - currentPos.y) / direction.y
                if (tTop > 0.001f && tTop < t) {
                    t = tTop
                    wallNormal = PointF(0f, 1f)
                }
                if (tBottom > 0.001f && tBottom < t) {
                    t = tBottom
                    wallNormal = PointF(0f, -1f)
                }
            }

            if (t == Float.MAX_VALUE || wallNormal == null) break

            val nextPos = PointF(currentPos.x + direction.x * t, currentPos.y + direction.y * t)

            // Check for pocket collision along this segment
            val pocketResult = checkPocketCollision(currentPos, nextPos, pockets, pocketRadius)
            if (pocketResult.first) {
                path.add(pocketResult.second) // Add the collision point on the pocket edge
                pocketedIndex = pocketResult.third
                break
            }

            path.add(nextPos)
            currentPos = nextPos
            direction = reflect(direction, wallNormal)
        }

        return BankShotResult(path, pocketedIndex)
    }

    private fun reflect(v: PointF, n: PointF): PointF {
        val dot = v.x * n.x + v.y * n.y
        return PointF(v.x - 2 * dot * n.x, v.y - 2 * dot * n.y)
    }

    private fun PointF.normalized(): PointF {
        val mag = sqrt(x * x + y * y)
        return if (mag != 0f) PointF(x / mag, y / mag) else PointF(0f, 0f)
    }

    private fun checkPocketCollision(start: PointF, end: PointF, pockets: List<PointF>, radius: Float): Triple<Boolean, PointF, Int?> {
        pockets.forEachIndexed { index, pocketCenter ->
            val d = PointF(end.x - start.x, end.y - start.y)
            val f = PointF(start.x - pocketCenter.x, start.y - pocketCenter.y)

            val a = d.dot(d)
            val b = 2 * f.dot(d)
            val c = f.dot(f) - radius * radius

            var discriminant = b * b - 4 * a * c
            if (discriminant >= 0) {
                discriminant = sqrt(discriminant)
                val t1 = (-b - discriminant) / (2 * a)
                val t2 = (-b + discriminant) / (2 * a)

                if (t1 in 0.0..1.0 || t2 in 0.0..1.0) {
                    // Find the closest valid intersection time
                    val t = if (t1 in 0.0..1.0 && t2 in 0.0..1.0) {
                        minOf(t1, t2)
                    } else if (t1 in 0.0..1.0) {
                        t1
                    } else {
                        t2
                    }
                    return Triple(true, PointF(start.x + t * d.x, start.y + t * d.y), index)
                }
            }
        }
        return Triple(false, PointF(0f, 0f), null)
    }

    private fun PointF.dot(other: PointF): Float = this.x * other.x + this.y * other.y
}