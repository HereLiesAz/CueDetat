package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sqrt

data class BankShotResult(
    val path: List<PointF>,
    val pocketedPocketIndex: Int?
)

@Singleton
class CalculateBankShot @Inject constructor() {

    private val maxBounces = 4 // Up to 4 rails
    private val spinThrowConstant = 0.3f // How much spin affects the rebound angle

    operator fun invoke(state: OverlayState): BankShotResult {
        if (!state.isBankingMode || state.onPlaneBall == null || state.bankingAimTarget == null) {
            return BankShotResult(emptyList(), null)
        }

        val canvasCenterX = state.viewWidth / 2f
        val canvasCenterY = state.viewHeight / 2f

        val ballCenter = state.onPlaneBall.center
        val aimTarget = state.bankingAimTarget
        val calcBallCenter = PointF(ballCenter.x - canvasCenterX, ballCenter.y - canvasCenterY)
        val calcAimTarget = PointF(aimTarget.x - canvasCenterX, aimTarget.y - canvasCenterY)

        val referenceRadius = state.protractorUnit.radius
        // Use the table size from the state to determine proportions
        val tableToBallRatioLong = state.tableSize.getTableToBallRatioLong()
        val tableToBallRatioShort = tableToBallRatioLong / state.tableSize.aspectRatio

        val logicalTableWidth = tableToBallRatioLong * referenceRadius
        val logicalTableHeight = tableToBallRatioShort * referenceRadius

        val halfW = logicalTableWidth / 2f
        val halfH = logicalTableHeight / 2f

        val pocketRadius = referenceRadius * 1.8f
        val pockets = listOf(
            PointF(-halfW, -halfH), PointF(halfW, -halfH), // Top corners
            PointF(-halfW, halfH), PointF(halfW, halfH),   // Bottom corners
            PointF(0f, -halfH), PointF(0f, halfH)          // Side pockets
        )

        var currentPos = calcBallCenter
        var direction = PointF(calcAimTarget.x - currentPos.x, calcAimTarget.y - currentPos.y).normalized()

        val pathInCalcSpace = mutableListOf(currentPos)
        var pocketedIndex: Int? = null

        val spinOffset = state.selectedSpinOffset ?: state.lingeringSpinOffset

        for (i in 0 until maxBounces) {
            var t = Float.MAX_VALUE
            var wallNormal: PointF? = null

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

            val pocketResult = checkPocketCollision(currentPos, nextPos, pockets, pocketRadius)
            if (pocketResult.first) {
                pathInCalcSpace.add(pocketResult.second)
                pocketedIndex = pocketResult.third
                break
            }

            pathInCalcSpace.add(nextPos)
            currentPos = nextPos
            direction = reflectWithSpin(direction, wallNormal, spinOffset)
        }

        val finalPath = pathInCalcSpace.map {
            PointF(it.x + canvasCenterX, it.y + canvasCenterY)
        }

        return BankShotResult(finalPath, pocketedIndex)
    }

    private fun reflectWithSpin(v: PointF, n: PointF, spinOffset: PointF?): PointF {
        val standardReflection = reflect(v, n)
        if (spinOffset == null) {
            return standardReflection
        }

        val spinControlRadius = 60f * 2
        val spinControlCenter = PointF(spinControlRadius, spinControlRadius)
        val relativeSpin = PointF(spinOffset.x - spinControlCenter.x, spinOffset.y - spinControlCenter.y)

        // Project spin onto the vector perpendicular to the direction of travel
        // This isolates the "sidespin" component.
        val vNormalized = v.normalized()
        val spinPerp = PointF(-vNormalized.y, vNormalized.x) // Perpendicular to velocity
        val sideSpinFactor = (relativeSpin.dot(spinPerp)) / spinControlRadius

        // The "throw" vector is parallel to the rail (perpendicular to the normal)
        val throwVector = PointF(-n.y, n.x)

        // Apply the throw, scaled by the sidespin factor and a constant
        val finalVector = PointF(
            standardReflection.x + throwVector.x * sideSpinFactor * spinThrowConstant,
            standardReflection.y + throwVector.y * sideSpinFactor * spinThrowConstant
        )

        return finalVector.normalized()
    }

    private fun reflect(v: PointF, n: PointF): PointF {
        val dot = v.x * n.x + v.y * n.y
        return PointF(v.x - 2 * dot * n.x, v.y - 2 * dot * n.y)
    }

    private fun PointF.normalized(): PointF {
        val mag = hypot(x.toDouble(), y.toDouble()).toFloat()
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