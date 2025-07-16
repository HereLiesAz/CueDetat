// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/table/RailRenderer.kt

package com.hereliesaz.cuedetat.view.renderer.table

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.config.table.Diamonds
import com.hereliesaz.cuedetat.view.config.table.Rail
import com.hereliesaz.cuedetat.view.renderer.text.LineTextRenderer
import com.hereliesaz.cuedetat.view.state.OverlayState

class RailRenderer {
    private val railVisualOffsetFromEdgeFactor = 0.75f
    private val diamondSizeFactor = 0.25f
    private val railConfig = Rail()
    private val diamondConfig = Diamonds()
    private val textRenderer = LineTextRenderer()

    fun draw(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        if (!state.table.isVisible || state.table.corners.size < 4) return

        val railLinePaint = Paint(paints.tableOutlinePaint).apply {
            color = railConfig.strokeColor.toArgb()
            strokeWidth = railConfig.strokeWidth
        }
        val railLineGlowPaint = Paint(paints.lineGlowPaint).apply {
            strokeWidth = railConfig.glowWidth
            color = railConfig.glowColor.toArgb()
        }
        val diamondPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = diamondConfig.fillColor.toArgb()
            alpha = (diamondConfig.opacity * 255).toInt()
        }

        val railOffsetAmount = LOGICAL_BALL_RADIUS * railVisualOffsetFromEdgeFactor
        val pocketRadius = LOGICAL_BALL_RADIUS * 1.8f
        val railGapRadius = pocketRadius * 0.75f

        val corners = state.table.corners
        val normals = listOf(
            normalize(PointF(corners[1].y - corners[0].y, corners[0].x - corners[1].x)), // Top
            normalize(PointF(corners[2].y - corners[1].y, corners[1].x - corners[2].x)), // Right
            normalize(PointF(corners[3].y - corners[2].y, corners[2].x - corners[3].x)), // Bottom
            normalize(PointF(corners[0].y - corners[3].y, corners[3].x - corners[0].x))  // Left
        )

        val offsetCorners = corners.mapIndexed { index, corner ->
            val normal1 = normals[(index + 3) % 4] // Previous rail normal
            val normal2 = normals[index]           // Current rail normal
            PointF(
                corner.x + (normal1.x + normal2.x) * railOffsetAmount,
                corner.y + (normal1.y + normal2.y) * railOffsetAmount
            )
        }

        // --- Draw Rail Segments ---
        val railSegments = listOf(
            // Top rail segments
            getPointAlongLine(offsetCorners[0], offsetCorners[1], pocketRadius) to getPointAlongLine(offsetCorners[1], offsetCorners[0], pocketRadius),
            // Bottom rail segments
            getPointAlongLine(offsetCorners[3], offsetCorners[2], pocketRadius) to getPointAlongLine(offsetCorners[2], offsetCorners[3], pocketRadius),
            // Side rail segments
            getPointAlongLine(offsetCorners[0], offsetCorners[3], pocketRadius) to getPointAlongLine(offsetCorners[3], offsetCorners[0], pocketRadius),
            getPointAlongLine(offsetCorners[1], offsetCorners[2], pocketRadius) to getPointAlongLine(offsetCorners[2], offsetCorners[1], pocketRadius)
        )

        railSegments.forEach { (start, end) ->
            canvas.drawLine(start.x, start.y, end.x, end.y, railLineGlowPaint)
            canvas.drawLine(start.x, start.y, end.x, end.y, railLinePaint)
        }

        // --- Draw Diamonds ---
        val diamondRadius = LOGICAL_BALL_RADIUS * diamondSizeFactor

        // Long rails (top and bottom) - 4 diamonds each side of center
        for(i in 1..4) {
            val fraction = i / 5.0f
            val top1 = interpolate(offsetCorners[0], offsetCorners[1], fraction)
            val bottom1 = interpolate(offsetCorners[3], offsetCorners[2], fraction)
            canvas.drawCircle(top1.x, top1.y, diamondRadius, diamondPaint)
            canvas.drawCircle(bottom1.x, bottom1.y, diamondRadius, diamondPaint)
        }

        // Short rails (left and right) - 3 diamonds each
        for(i in 1..3) {
            val fraction = i / 4.0f
            val left = interpolate(offsetCorners[0], offsetCorners[3], fraction)
            val right = interpolate(offsetCorners[1], offsetCorners[2], fraction)
            canvas.drawCircle(left.x, left.y, diamondRadius, diamondPaint)
            canvas.drawCircle(right.x, right.y, diamondRadius, diamondPaint)
        }
    }

    fun drawRailLabels(canvas: Canvas, state: OverlayState, paints: PaintCache, typeface: Typeface?) {
        val textPaint = paints.textPaint.apply {
            this.typeface = typeface
            this.textSize = 60f // Make labels much larger
        }

        // Diamond label for banked Aiming Line
        if (state.aimingLineBankPath.size > 1) {
            val bankPoint = state.aimingLineBankPath[1]
            textRenderer.drawDiamondLabel(canvas, bankPoint, state, textPaint)
        }
        // Diamond label for banked Tangent Line
        if (state.tangentLineBankPath.size > 1) {
            val tangentBankPoint = state.tangentLineBankPath[1]
            textRenderer.drawDiamondLabel(canvas, tangentBankPoint, state, textPaint)
        }
        // Diamond label for Shot Guide Line
        state.shotGuideImpactPoint?.let { impactPoint ->
            textRenderer.drawDiamondLabel(canvas, impactPoint, state, textPaint)
        }
    }


    private fun normalize(p: PointF): PointF {
        val mag = kotlin.math.hypot(p.x, p.y)
        return if (mag > 0) PointF(p.x / mag, p.y / mag) else PointF(0f, 0f)
    }

    private fun getPointAlongLine(start: PointF, end: PointF, distance: Float): PointF {
        val vector = PointF(end.x - start.x, end.y - start.y)
        val mag = kotlin.math.hypot(vector.x, vector.y)
        val unitVector = if (mag > 0) PointF(vector.x / mag, vector.y / mag) else PointF(0f, 0f)
        return PointF(start.x + unitVector.x * distance, start.y + unitVector.y * distance)
    }

    private fun interpolate(p1: PointF, p2: PointF, fraction: Float): PointF {
        return PointF(p1.x + (p2.x - p1.x) * fraction, p1.y + (p2.y - p1.y) * fraction)
    }
}