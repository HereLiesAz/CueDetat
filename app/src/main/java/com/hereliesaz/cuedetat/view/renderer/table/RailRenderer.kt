package com.hereliesaz.cuedetat.view.renderer.table

import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.PointF
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.state.OverlayState

class RailRenderer {
    private val railVisualOffsetFromEdgeFactor = 0.75f
    private val railVisualThicknessFactor = 0.5f
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

        val railLinePaint = Paint(paints.tableOutlinePaint).apply {
            strokeWidth = referenceRadius * railVisualThicknessFactor
        }
        val railLineGlowPaint = Paint(paints.lineGlowPaint).apply { strokeWidth = railLinePaint.strokeWidth + 8f }
        val diamondPaint = Paint(paints.tableOutlinePaint).apply { style = Paint.Style.FILL_AND_STROKE }

        val railOffsetAmount = referenceRadius * railVisualOffsetFromEdgeFactor
        val railTopCenterY = innerTop - railOffsetAmount
        val railBottomCenterY = innerBottom + railOffsetAmount
        val railLeftCenterX = innerLeft - railOffsetAmount
        val railRightCenterX = innerRight + railOffsetAmount
        val railEndExtension = railOffsetAmount * 1.5f

        // Draw rail glows
        canvas.drawLine(innerLeft - railEndExtension, railTopCenterY, innerRight + railEndExtension, railTopCenterY, railLineGlowPaint)
        canvas.drawLine(innerLeft - railEndExtension, railBottomCenterY, innerRight + railEndExtension, railBottomCenterY, railLineGlowPaint)
        canvas.drawLine(railLeftCenterX, innerTop - railEndExtension, railLeftCenterX, innerBottom + railEndExtension, railLineGlowPaint)
        canvas.drawLine(railRightCenterX, innerTop - railEndExtension, railRightCenterX, innerBottom + railEndExtension, railLineGlowPaint)

        // Draw rail lines
        canvas.drawLine(innerLeft - railEndExtension, railTopCenterY, innerRight + railEndExtension, railTopCenterY, railLinePaint)
        canvas.drawLine(innerLeft - railEndExtension, railBottomCenterY, innerRight + railEndExtension, railBottomCenterY, railLinePaint)
        canvas.drawLine(railLeftCenterX, innerTop - railEndExtension, railLeftCenterX, innerBottom + railEndExtension, railLinePaint)
        canvas.drawLine(railRightCenterX, innerTop - railEndExtension, railRightCenterX, innerBottom + railEndExtension, railLinePaint)

        // Draw Diamond Grid
        val diamondGridPaint = paints.gridLinePaint
        val halfWidth = tablePlayingSurfaceWidth / 2f
        val halfHeight = tablePlayingSurfaceHeight / 2f

        // Vertical lines (connecting long rail diamonds)
        for (i in 1..3) {
            val xOffset = halfWidth * (i / 4.0f)
            canvas.drawLine(tableCenterX - xOffset, innerTop, tableCenterX - xOffset, innerBottom, diamondGridPaint)
            canvas.drawLine(tableCenterX + xOffset, innerTop, tableCenterX + xOffset, innerBottom, diamondGridPaint)
        }
        // Horizontal lines (connecting short rail diamonds)
        for (i in 1..1) {
            val yOffset = halfHeight * (i / 2.0f)
            canvas.drawLine(innerLeft, tableCenterY - yOffset, innerRight, tableCenterY - yOffset, diamondGridPaint)
            canvas.drawLine(innerLeft, tableCenterY + yOffset, innerRight, tableCenterY + yOffset, diamondGridPaint)
        }
        // Center lines
        canvas.drawLine(tableCenterX, innerTop, tableCenterX, innerBottom, diamondGridPaint)
        canvas.drawLine(innerLeft, tableCenterY, innerRight, tableCenterY, diamondGridPaint)


        // Draw Diamonds on the rails
        val diamondRadius = referenceRadius * diamondSizeFactor

        // Long rails (3 diamonds between corner and side pockets)
        for (i in 1..3) {
            val xOffset = halfWidth * (i / 4.0f)
            canvas.drawCircle(tableCenterX - xOffset, railTopCenterY, diamondRadius, diamondPaint)
            canvas.drawCircle(tableCenterX + xOffset, railTopCenterY, diamondRadius, diamondPaint)
            canvas.drawCircle(tableCenterX - xOffset, railBottomCenterY, diamondRadius, diamondPaint)
            canvas.drawCircle(tableCenterX + xOffset, railBottomCenterY, diamondRadius, diamondPaint)
        }
        // Short rails (3 diamonds between corner pockets)
        for (i in 1..3) {
            val yOffset = halfHeight * (i / 4.0f)
            canvas.drawCircle(railLeftCenterX, tableCenterY - yOffset, diamondRadius, diamondPaint)
            canvas.drawCircle(railRightCenterX, tableCenterY - yOffset, diamondRadius, diamondPaint)
            canvas.drawCircle(railLeftCenterX, tableCenterY + yOffset, diamondRadius, diamondPaint)
            canvas.drawCircle(railRightCenterX, tableCenterY + yOffset, diamondRadius, diamondPaint)
        }
    }
}