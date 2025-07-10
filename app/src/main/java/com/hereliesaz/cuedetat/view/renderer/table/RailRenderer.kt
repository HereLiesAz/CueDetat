package com.hereliesaz.cuedetat.view.renderer.table

import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.PointF
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.state.OverlayState

class RailRenderer {
    private val diamondSizeFactor = 0.25f

    companion object {
        fun getDiamondPositions(state: OverlayState): List<PointF> {
            val referenceRadius = state.onPlaneBall?.radius ?: state.protractorUnit.radius
            if (referenceRadius <= 0) return emptyList()

            val tableToBallRatioLong = state.tableSize.getTableToBallRatioLong()
            val tableToBallRatioShort = tableToBallRatioLong / state.tableSize.aspectRatio
            val tablePlayingSurfaceWidth = tableToBallRatioLong * referenceRadius
            val tablePlayingSurfaceHeight = tableToBallRatioShort * referenceRadius

            val canvasCenterX = state.viewWidth / 2f
            val canvasCenterY = state.viewHeight / 2f
            val left = canvasCenterX - tablePlayingSurfaceWidth / 2
            val top = canvasCenterY - tablePlayingSurfaceHeight / 2
            val right = canvasCenterX + tablePlayingSurfaceWidth / 2
            val bottom = canvasCenterY + tablePlayingSurfaceHeight / 2

            val diamonds = mutableListOf<PointF>()
            // Long rails
            for (i in 1..3) {
                val xOffset = tablePlayingSurfaceWidth * (i / 8.0f)
                diamonds.add(PointF(left + xOffset, top))
                diamonds.add(PointF(right - xOffset, top))
                diamonds.add(PointF(left + xOffset, bottom))
                diamonds.add(PointF(right - xOffset, bottom))
            }
            // Short rails
            for (i in 1..1) {
                val yOffset = tablePlayingSurfaceHeight * (i / 2.0f)
                diamonds.add(PointF(left, top + yOffset))
                diamonds.add(PointF(right, top + yOffset))
                diamonds.add(PointF(left, bottom - yOffset))
                diamonds.add(PointF(right, bottom - yOffset))
            }
            return diamonds
        }
    }

    fun draw(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        val referenceRadius = state.onPlaneBall?.radius ?: state.protractorUnit.radius
        if (referenceRadius <= 0) return

        val tableToBallRatioLong = state.tableSize.getTableToBallRatioLong()
        val tableToBallRatioShort = tableToBallRatioLong / state.tableSize.aspectRatio

        val tablePlayingSurfaceWidth = tableToBallRatioLong * referenceRadius
        val tablePlayingSurfaceHeight = tableToBallRatioShort * referenceRadius

        val tableCenterX = state.viewWidth / 2f
        val tableCenterY = state.viewHeight / 2f

        val innerLeft = tableCenterX - tablePlayingSurfaceWidth / 2
        val innerTop = tableCenterY - tablePlayingSurfaceHeight / 2
        val innerRight = tableCenterX + tablePlayingSurfaceWidth / 2
        val innerBottom = tableCenterY + tablePlayingSurfaceHeight / 2

        val railLinePaint = paints.tableOutlinePaint
        val railLineGlowPaint = Paint(paints.lineGlowPaint).apply { strokeWidth = railLinePaint.strokeWidth + 8f }
        val diamondPaint = Paint(paints.tableOutlinePaint).apply { style = Paint.Style.FILL_AND_STROKE }

        val pocketRadius = referenceRadius * 1.8f

        // --- Draw Rail Segments ---
        val railSegments = listOf(
            // Top rail
            PointF(innerLeft + pocketRadius, innerTop) to PointF(tableCenterX - pocketRadius, innerTop),
            PointF(tableCenterX + pocketRadius, innerTop) to PointF(innerRight - pocketRadius, innerTop),
            // Bottom rail
            PointF(innerLeft + pocketRadius, innerBottom) to PointF(tableCenterX - pocketRadius, innerBottom),
            PointF(tableCenterX + pocketRadius, innerBottom) to PointF(innerRight - pocketRadius, innerBottom),
            // Side rails
            PointF(innerLeft, innerTop + pocketRadius) to PointF(innerLeft, innerBottom - pocketRadius),
            PointF(innerRight, innerTop + pocketRadius) to PointF(innerRight, innerBottom - pocketRadius)
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
            canvas.drawCircle(tableCenterX - xOffset, innerTop, diamondRadius, diamondPaint)
            canvas.drawCircle(tableCenterX + xOffset, innerTop, diamondRadius, diamondPaint)
            canvas.drawCircle(tableCenterX - xOffset, innerBottom, diamondRadius, diamondPaint)
            canvas.drawCircle(tableCenterX + xOffset, innerBottom, diamondRadius, diamondPaint)
        }
        // Short rails (3 diamonds per rail)
        val shortRailYOffsets = listOf(-halfHeight / 2, 0f, halfHeight / 2)
        for (yOffset in shortRailYOffsets) {
            canvas.drawCircle(innerLeft, tableCenterY + yOffset, diamondRadius, diamondPaint)
            canvas.drawCircle(innerRight, tableCenterY + yOffset, diamondRadius, diamondPaint)
        }
    }
}