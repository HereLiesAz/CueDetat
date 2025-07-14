// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/CalculateBankShot.kt

package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import com.hereliesaz.cuedetat.view.renderer.table.TableRenderer
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

data class BankShotResult(val path: List<PointF>, val pocketedPocketIndex: Int?)

class CalculateBankShot @Inject constructor(private val reducerUtils: ReducerUtils) {

    operator fun invoke(state: OverlayState): BankShotResult {
        if (!state.isBankingMode || state.onPlaneBall == null || state.bankingAimTarget == null) {
            return BankShotResult(emptyList(), null)
        }

        val path = mutableListOf(state.onPlaneBall.center)
        var currentPoint = state.onPlaneBall.center
        var currentVector = PointF(
            state.bankingAimTarget.x - currentPoint.x,
            state.bankingAimTarget.y - currentPoint.y
        )
        var pocketedIndex: Int? = null

        for (i in 0..10) { // Limit to 10 banks to prevent infinite loops
            val intersectionResult = reducerUtils.findRailIntersectionAndNormal(currentPoint, state.bankingAimTarget, state)
            if (intersectionResult != null) {
                val (intersection, normal) = intersectionResult
                path.add(intersection)

                // Check for pocket on this segment
                val (pocketedOnSegment, _) = checkPocketAim(currentPoint, intersection, state)
                if (pocketedOnSegment != null) {
                    pocketedIndex = pocketedOnSegment
                    break
                }

                currentVector = reducerUtils.reflect(currentVector, normal)
                currentPoint = intersection
            } else {
                // No more intersections, path ends.
                path.add(state.bankingAimTarget)
                break
            }
        }
        return BankShotResult(path, pocketedIndex)
    }

    private fun checkPocketAim(start: PointF, end: PointF, state: OverlayState): Pair<Int?, PointF?> {
        val dirX = end.x - start.x
        val dirY = end.y - start.y
        val pockets = TableRenderer.getLogicalPockets(state)
        val pocketRadius = state.protractorUnit.radius * 1.8f

        for ((index, pocket) in pockets.withIndex()) {
            val dist = linePointDistance(start, end, pocket)
            if (dist < pocketRadius) {
                val vecToPocketX = pocket.x - start.x
                val vecToPocketY = pocket.y - start.y
                val dotProduct = vecToPocketX * dirX + vecToPocketY * dirY
                if (dotProduct >= 0) {
                    return Pair(index, pocket) // Simplified: return pocket center
                }
            }
        }
        return Pair(null, null)
    }


    private fun linePointDistance(p1: PointF, p2: PointF, p: PointF): Float {
        val num = abs((p2.x - p1.x) * (p1.y - p.y) - (p1.x - p.x) * (p2.y - p1.y))
        val den = sqrt((p2.x - p1.x).pow(2) + (p2.y - p1.y).pow(2))
        return if (den == 0f) 0f else num / den
    }
}