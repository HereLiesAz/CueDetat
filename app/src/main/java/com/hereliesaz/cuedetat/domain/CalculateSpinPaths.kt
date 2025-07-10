// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/CalculateSpinPaths.kt

package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.view.renderer.util.SpinColorUtils
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Singleton
class CalculateSpinPaths @Inject constructor() {
    private val maxPathLengthFactor = 20f

    operator fun invoke(state: OverlayState): Map<Color, List<PointF>> {
        val interactiveSpinOffset = state.lingeringSpinOffset ?: state.selectedSpinOffset ?: return emptyMap()

        return calculateSinglePath(state, interactiveSpinOffset)
    }

    private fun calculateSinglePath(state: OverlayState, spinOffset: PointF): Map<Color, List<PointF>> {
        val spinControlRadius = 60f * 2
        val spinMagnitude = hypot((spinOffset.x - spinControlRadius).toDouble(), (spinOffset.y - spinControlRadius).toDouble()).toFloat() / spinControlRadius
        val angleDegrees = Math.toDegrees(atan2(spinOffset.y - spinControlRadius, spinOffset.x - spinControlRadius).toDouble()).toFloat()

        val path = calculatePathForSpin(state, angleDegrees, spinMagnitude)
        val color = SpinColorUtils.getColorFromAngleAndDistance(angleDegrees, spinMagnitude)
        return mapOf(color to path)
    }

    private fun calculatePathForSpin(state: OverlayState, angleDegrees: Float, magnitude: Float): List<PointF> {
        val startPoint = state.protractorUnit.ghostCueBallCenter
        val targetPoint = state.protractorUnit.center
        val tangentDirection = state.tangentDirection

        val dxToTarget = targetPoint.x - startPoint.x
        val dyToTarget = targetPoint.y - startPoint.y
        val magToTarget = hypot(dxToTarget.toDouble(), dyToTarget.toDouble()).toFloat()
        if (magToTarget < 0.001f) return emptyList()

        val tangentDx = (-dyToTarget / magToTarget) * tangentDirection
        val tangentDy = (dxToTarget / magToTarget) * tangentDirection

        val spinAngle = Math.toRadians(angleDegrees.toDouble()).toFloat()
        val maxCurveOffset = state.protractorUnit.radius * 2.5f
        val curveAmount = magnitude * magnitude

        val controlPoint1 = PointF(
            startPoint.x + tangentDx * (maxPathLengthFactor * state.protractorUnit.radius * 0.33f),
            startPoint.y + tangentDy * (maxPathLengthFactor * state.protractorUnit.radius * 0.33f)
        )

        val endPoint = PointF(
            startPoint.x + tangentDx * (maxPathLengthFactor * state.protractorUnit.radius) + (curveAmount * cos(spinAngle)),
            startPoint.y + tangentDy * (maxPathLengthFactor * state.protractorUnit.radius) + (curveAmount * sin(spinAngle))
        )

        val controlPoint2 = PointF(
            endPoint.x - tangentDx * (maxPathLengthFactor * state.protractorUnit.radius * 0.33f),
            endPoint.y - tangentDy * (maxPathLengthFactor * state.protractorUnit.radius * 0.33f)
        )

        return generateBezierCurve(startPoint, controlPoint1, controlPoint2, endPoint)
    }

    private fun generateBezierCurve(p0: PointF, p1: PointF, p2: PointF, p3: PointF, numPoints: Int = 20): List<PointF> {
        val curve = mutableListOf<PointF>()
        for (i in 0..numPoints) {
            val t = i.toFloat() / numPoints
            val u = 1 - t
            val tt = t * t
            val uu = u * u
            val uuu = uu * u
            val ttt = tt * t

            val x = uuu * p0.x + 3 * uu * t * p1.x + 3 * u * tt * p2.x + ttt * p3.x
            val y = uuu * p0.y + 3 * uu * t * p1.y + 3 * u * tt * p2.y + ttt * p3.y
            curve.add(PointF(x, y))
        }
        return curve
    }
}