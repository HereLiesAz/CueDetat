// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/table/RailRenderer.kt
package com.hereliesaz.cuedetat.view.renderer.table

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Typeface
import com.hereliesaz.cuedetat.view.config.table.Diamonds
import com.hereliesaz.cuedetat.view.config.table.Rail
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.sin

class RailRenderer @Inject constructor() {
    private val railVisualOffsetFromEdgeFactor = 0.75f
    private val diamondSizeFactor = 0.25f
    private val railConfig = Rail()
    private val diamondConfig = Diamonds()

    fun draw(canvas: Canvas, state: OverlayState, typeface: Typeface?) {
        val geometry = state.table.geometry
        if (!geometry.isValid) return

        val referenceRadius = state.onPlaneBall?.radius ?: state.protractorUnit.radius
        val tablePlayingSurfaceWidth = geometry.width
        val tablePlayingSurfaceHeight = geometry.height

        val railLinePaint = railConfig.getStrokePaint(state.luminanceAdjustment)
        val railLineGlowPaint = railConfig.getGlowPaint(state.glowStickValue)
        val diamondPaint = diamondConfig.getFillPaint(state.luminanceAdjustment)

        val angleRad = Math.toRadians(-state.table.rotationDegrees.toDouble())
        val cosA = cos(angleRad).toFloat()
        val sinA = sin(angleRad).toFloat()

        val railOffsetAmount = referenceRadius * railVisualOffsetFromEdgeFactor
        val diamondRadius = referenceRadius * diamondSizeFactor

        val longSidePoints = listOf(
            PointF(0f, -tablePlayingSurfaceHeight / 2 - railOffsetAmount)
        )

        for (i in 1..3) {
            val xOffset = tablePlayingSurfaceWidth / 4 * i - tablePlayingSurfaceWidth / 2
            val topRailDiamond = PointF(xOffset, -tablePlayingSurfaceHeight / 2 - railOffsetAmount)
            val bottomRailDiamond = PointF(xOffset, tablePlayingSurfaceHeight / 2 + railOffsetAmount)

            val rotatedTop = PointF(topRailDiamond.x * cosA - topRailDiamond.y * sinA, topRailDiamond.x * sinA + topRailDiamond.y * cosA)
            val rotatedBottom = PointF(bottomRailDiamond.x * cosA - bottomRailDiamond.y * sinA, bottomRailDiamond.x * sinA + bottomRailDiamond.y * cosA)

            canvas.drawCircle(rotatedTop.x, rotatedTop.y, diamondRadius, diamondPaint)
            canvas.drawCircle(rotatedBottom.x, rotatedBottom.y, diamondRadius, diamondPaint)
        }

        val shortSidePoints = listOf(
            PointF(-tablePlayingSurfaceWidth / 2 - railOffsetAmount, 0f),
            PointF(tablePlayingSurfaceWidth / 2 + railOffsetAmount, 0f)
        )
        for (yOffsetFactor in listOf(-0.5f, 0.5f)) {
            val yOffset = tablePlayingSurfaceHeight * yOffsetFactor
            val leftRailDiamond = PointF(-tablePlayingSurfaceWidth / 2 - railOffsetAmount, yOffset)
            val rightRailDiamond = PointF(tablePlayingSurfaceWidth / 2 + railOffsetAmount, yOffset)

            val rotatedLeft = PointF(leftRailDiamond.x * cosA - leftRailDiamond.y * sinA, leftRailDiamond.x * sinA + leftRailDiamond.y * cosA)
            val rotatedRight = PointF(rightRailDiamond.x * cosA - rightRailDiamond.y * sinA, rightRailDiamond.x * sinA + rightRailDiamond.y * cosA)

            canvas.drawCircle(rotatedLeft.x, rotatedLeft.y, diamondRadius, diamondPaint)
            canvas.drawCircle(rotatedRight.x, rotatedRight.y, diamondRadius, diamondPaint)
        }

        val innerCorners = geometry.unrotatedCorners
        val outerCorners = innerCorners.map {
            PointF(
                it.x + railOffsetAmount * if (it.x < 0) -1 else 1,
                it.y + railOffsetAmount * if (it.y < 0) -1 else 1
            )
        }

        val rotatedOuterCorners = outerCorners.map {
            PointF(it.x * cosA - it.y * sinA, it.x * sinA + it.y * cosA)
        }

        for (i in 0..3) {
            val start = rotatedOuterCorners[i]
            val end = rotatedOuterCorners[(i + 1) % 4]
            railLineGlowPaint?.let { canvas.drawLine(start.x, start.y, end.x, end.y, it) }
            canvas.drawLine(start.x, start.y, end.x, end.y, railLinePaint)
        }
    }
}