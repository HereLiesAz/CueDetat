// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/CalculateBankShot.kt
package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import com.hereliesaz.cuedetat.view.model.BankShotResult
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import kotlin.math.pow
import kotlin.math.sqrt

class CalculateBankShot @Inject constructor(
    private val reducerUtils: ReducerUtils
) {
    private val lineExtensionFactor = 5000f

    operator fun invoke(state: OverlayState): BankShotResult {
        if (state.onPlaneBall == null || !state.table.geometry.isValid) {
            return BankShotResult()
        }

        val startPoint = state.onPlaneBall.center
        val aimPoint = state.bankingAimTarget
        val path = mutableListOf(startPoint)
        var currentPoint = startPoint
        var currentDir = PointF(aimPoint.x - startPoint.x, aimPoint.y - startPoint.y)

        for (i in 0 until 4) {
            val extendedEnd = PointF(currentPoint.x + currentDir.x * lineExtensionFactor, currentPoint.y + currentDir.y * lineExtensionFactor)
            val intersectionResult = state.table.findRailIntersectionAndNormal(currentPoint, extendedEnd)

            if (intersectionResult != null) {
                val (intersectionPoint, railNormal) = intersectionResult
                path.add(intersectionPoint)
                val (pocketedIndex, pocketedIntersection) = checkPocketAim(intersectionPoint, state)
                if (pocketedIndex != null && pocketedIntersection != null) {
                    path[path.lastIndex] = pocketedIntersection
                    return BankShotResult(path, pocketedIndex)
                }
                currentPoint = intersectionPoint
                currentDir = reducerUtils.reflect(currentDir, railNormal)
            } else {
                break
            }
        }

        return BankShotResult(path)
    }

    private fun checkPocketAim(point: PointF, state: OverlayState): Pair<Int?, PointF?> {
        val pockets = state.table.getLogicalPockets(state.protractorUnit.radius)
        val pocketRadius = state.protractorUnit.radius * 1.8f

        pockets.forEachIndexed { index, pocketCenter ->
            val dist = sqrt((point.x - pocketCenter.x).pow(2) + (point.y - pocketCenter.y).pow(2))
            if (dist < pocketRadius) {
                return Pair(index, point)
            }
        }
        return Pair(null, null)
    }
}