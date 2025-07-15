// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/table/RailRenderer.kt

package com.hereliesaz.cuedetat.view.renderer.table

import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.PointF
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.config.table.Diamonds
import com.hereliesaz.cuedetat.view.config.table.Rail
import com.hereliesaz.cuedetat.view.state.OverlayState

class RailRenderer {
    private val railVisualOffsetFromEdgeFactor = 0.75f
    private val diamondSizeFactor = 0.25f
    private val railConfig = Rail()
    private val diamondConfig = Diamonds()

    fun draw(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        val referenceRadius = state.onPlaneBall?.radius ?: state.protractorUnit.radius
        if (referenceRadius <= 0) return

        val ballRealDiameter = 2.25f
        val ballLogicalDiameter = referenceRadius * 2
        val scale = ballLogicalDiameter / ballRealDiameter
        val tablePlayingSurfaceWidth = state.tableSize.longSideInches * scale
        val tablePlayingSurfaceHeight = state.tableSize.shortSideInches * scale

        // Corrected: Use logical origin (0,0) instead of screen center
        val tableCenterX = 0f
        val tableCenterY = 0f

        val innerLeft = tableCenterX - tablePlayingSurfaceWidth / 2
        val innerTop = tableCenterY - tablePlayingSurfaceHeight / 2
        val innerRight = tableCenterX + tablePlayingSurfaceWidth / 2
        val innerBottom = tableCenterY + tablePlayingSurfaceHeight / 2

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

        val railOffsetAmount = referenceRadius * railVisualOffsetFromEdgeFactor
        val railTopCenterY = innerTop - railOffsetAmount
        val railBottomCenterY = innerBottom + railOffsetAmount
        val railLeftCenterX = innerLeft - railOffsetAmount
        val railRightCenterX = innerRight + railOffsetAmount

        val pocketRadius = referenceRadius * 1.8f
        // Reduce the gap for the side pockets to make them appear shallower.
        val railGapRadius = pocketRadius * 0.75f

        // --- Draw Rail Segments ---
        val railSegments = listOf(
            // Top rail
            PointF(innerLeft + pocketRadius, railTopCenterY) to PointF(tableCenterX - railGapRadius, railTopCenterY),
            PointF(tableCenterX + railGapRadius, railTopCenterY) to PointF(innerRight - pocketRadius, railTopCenterY),
            // Bottom rail
            PointF(innerLeft + pocketRadius, railBottomCenterY) to PointF(tableCenterX - railGapRadius, railBottomCenterY),
            PointF(tableCenterX + railGapRadius, railBottomCenterY) to PointF(innerRight - pocketRadius, railBottomCenterY),
            // Side rails
            PointF(railLeftCenterX, innerTop + pocketRadius) to PointF(railLeftCenterX, innerBottom - pocketRadius),
            PointF(railRightCenterX, innerTop + pocketRadius) to PointF(railRightCenterX, innerBottom - pocketRadius)
        )

        railSegments.forEach { (start, end) ->
            canvas.drawLine(start.x, start.y, end.x, end.y, railLineGlowPaint)
            canvas.drawLine(start.x, start.y, end.x, end.y, railLinePaint)
        }

        // Draw Diamonds on the rails
        val diamondRadius = referenceRadius * diamondSizeFactor
        val halfWidth = tablePlayingSurfaceWidth / 2f
        val halfHeight = tablePlayingSurfaceHeight / 2f

        // Long rails (3 diamonds between corner and side pockets)
        for (i in 1..3) {
            val xOffset = halfWidth * (i / 4.0f)
            canvas.drawCircle(tableCenterX - xOffset, railTopCenterY, diamondRadius, diamondPaint)
            canvas.drawCircle(tableCenterX + xOffset, railTopCenterY, diamondRadius, diamondPaint)
            canvas.drawCircle(tableCenterX - xOffset, railBottomCenterY, diamondRadius, diamondPaint)
            canvas.drawCircle(tableCenterX + xOffset, railBottomCenterY, diamondRadius, diamondPaint)
        }
        // Short rails (3 diamonds per rail)
        val shortRailYOffsets = listOf(-halfHeight / 2, 0f, halfHeight / 2)
        for (yOffset in shortRailYOffsets) {
            canvas.drawCircle(railLeftCenterX, tableCenterY + yOffset, diamondRadius, diamondPaint)
            canvas.drawCircle(railRightCenterX, tableCenterY + yOffset, diamondRadius, diamondPaint)
        }
    }
}