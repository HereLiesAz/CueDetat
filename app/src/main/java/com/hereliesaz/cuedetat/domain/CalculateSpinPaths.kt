// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/CalculateSpinPaths.kt

package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.hereliesaz.cuedetat.ui.theme.RebelYellow
import com.hereliesaz.cuedetat.ui.theme.WarningRed
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
        val spinOffset = state.lingeringSpinOffset ?: state.selectedSpinOffset ?: return emptyMap()

        val startPoint = state.protractorUnit.ghostCueBallCenter
        val targetPoint = state.protractorUnit.center
        val tangentDirection = state.tangentDirection

        val dxToTarget = targetPoint.x - startPoint.x
        val dyToTarget = targetPoint.y - startPoint.y
        val magToTarget = hypot(dxToTarget.toDouble(), dyToTarget.toDouble()).toFloat()
        if (magToTarget < 0.001f) return emptyMap()

        val tangentDx = (-dyToTarget / magToTarget) * tangentDirection
        val tangentDy = (dxToTarget / magToTarget) * tangentDirection

        val spinControlRadius = 60f * 2 // From SpinControl's new size of 120dp
        val spinMagnitude = hypot((spinOffset.x - spinControlRadius).toDouble(), (spinOffset.y - spinControlRadius).toDouble()).toFloat() / spinControlRadius
        val spinAngle = atan2(spinOffset.y - spinControlRadius, spinOffset.x - spinControlRadius)

        val maxCurveOffset = state.protractorUnit.radius * 2.5f
        val curveAmount = spinMagnitude * maxCurveOffset

        val pathColor = getColorFromSpin(spinOffset, spinControlRadius)

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

        val path = generateBezierCurve(startPoint, controlPoint1, controlPoint2, endPoint)

        return mapOf(pathColor to path)
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

    private fun getColorFromSpin(offset: PointF, radius: Float): Color {
        val distance = hypot((offset.x - radius).toDouble(), (offset.y - radius).toDouble()).toFloat()
        val normalizedDistance = (distance / radius).coerceIn(0f, 1f)

        val colorStops = listOf(
            0.0f to Color.White,
            0.17f to RebelYellow,
            0.34f to Color.Red,
            0.51f to Color(0.5f, 0f, 1f), // Violet
            0.68f to Color.Blue,
            0.85f to Color.Green,
            1.0f to Color.Green.copy(alpha = 0.7f)
        )

        if (normalizedDistance <= 0f) return colorStops.first().second
        if (normalizedDistance >= 1f) return colorStops.last().second

        for (i in 0 until colorStops.size - 1) {
            val (stop1, color1) = colorStops[i]
            val (stop2, color2) = colorStops[i + 1]
            if (normalizedDistance in stop1..stop2) {
                val fraction = (normalizedDistance - stop1) / (stop2 - stop1)
                return lerp(color1, color2, fraction)
            }
        }
        return colorStops.last().second
    }
}