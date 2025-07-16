// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/CalculateSpinPaths.kt
package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.spinPathColors
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class CalculateSpinPaths @Inject constructor(private val reducerUtils: ReducerUtils) {

    private val lineExtensionFactor = 5000f

    operator fun invoke(state: OverlayState): Map<Color, List<PointF>> {
        if (state.selectedSpinOffset != null) {
            return getPathForSingleOffset(state.selectedSpinOffset, state)
        }
        return getDefaultSpinPaths(state)
    }

    private fun getPathForSingleOffset(offset: Offset, state: OverlayState): Map<Color, List<PointF>> {
        val angle = atan2(offset.y, offset.x)
        val color = spinPathColors.getOrElse( ( (Math.toDegrees(angle.toDouble()) + 360) % 360 / (360.0/spinPathColors.size) ).toInt() ) { Color.White }
        val path = calculateSpinPath(state, angle.toFloat())
        return mapOf(color to path)
    }

    private fun getDefaultSpinPaths(state: OverlayState): Map<Color, List<PointF>> {
        val paths = mutableMapOf<Color, List<PointF>>()
        spinPathColors.forEachIndexed { i, color ->
            val angle = (i.toFloat() / spinPathColors.size) * 2 * Math.PI
            val path = calculateSpinPath(state, angle.toFloat())
            paths[color] = path
        }
        return paths
    }

    private fun calculateSpinPath(state: OverlayState, spinAngleRad: Float): List<PointF> {
        val ghostBall = state.protractorUnit.ghostCueBallCenter
        val targetBall = state.protractorUnit.center

        val vectorToTarget = PointF(targetBall.x - ghostBall.x, targetBall.y - ghostBall.y)
        val tangentVector = PointF(-vectorToTarget.y, vectorToTarget.x)

        val combinedVector = PointF(
            vectorToTarget.x * cos(spinAngleRad) - tangentVector.x * sin(spinAngleRad),
            vectorToTarget.y * cos(spinAngleRad) + tangentVector.y * sin(spinAngleRad)
        )

        val path = mutableListOf(ghostBall)
        if (state.table.isVisible) {
            val bankPath = calculateSingleBank(ghostBall, combinedVector, state)
            path.addAll(bankPath.drop(1))
        } else {
            val extendedEnd = PointF(
                ghostBall.x + combinedVector.x * lineExtensionFactor,
                ghostBall.y + combinedVector.y * lineExtensionFactor
            )
            path.add(extendedEnd)
        }
        return path
    }

    private fun calculateSingleBank(start: PointF, direction: PointF, state: OverlayState): List<PointF> {
        val extendedEnd = PointF(start.x + direction.x * lineExtensionFactor, start.y + direction.y * lineExtensionFactor)
        val intersectionResult = state.table.findRailIntersectionAndNormal(start, extendedEnd) ?: return listOf(start, extendedEnd)
        val intersectionPoint = intersectionResult.first
        val railNormal = intersectionResult.second
        val reflectedDir = reducerUtils.reflect(direction, railNormal)
        val finalEndPoint = PointF(
            intersectionPoint.x + reflectedDir.x * lineExtensionFactor,
            intersectionPoint.y + reflectedDir.y * lineExtensionFactor
        )
        return listOf(start, intersectionPoint, finalEndPoint)
    }
}