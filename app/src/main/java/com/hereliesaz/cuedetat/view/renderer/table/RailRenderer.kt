package com.hereliesaz.cuedetat.view.renderer.table

import android.graphics.Paint
import android.graphics.Canvas
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.state.OverlayState

class RailRenderer {
    private val railVisualOffsetFromEdgeFactor = 0.75f
    private val railVisualThicknessFactor = 0.5f
    private val diamondSizeFactor = 0.25f

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
        val diamondPaint = Paint(paints.tableOutlinePaint).apply { strokeWidth = (referenceRadius * diamondSizeFactor) / 2f }

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

        // Draw Diamonds
        val diamondRadius = referenceRadius * diamondSizeFactor
        val halfWidth = tablePlayingSurfaceWidth / 2f
        val halfHeight = tablePlayingSurfaceHeight / 2f

        // Long rails (3 diamonds between corner and side pockets)
        for (i in 1..3) {
            val xOffset = halfWidth * (i / 4.0f)
            canvas.drawCircle(tableCenterX - xOffset, railTopCenterY, diamondRadius, diamondPaint) // Top-left section
            canvas.drawCircle(tableCenterX + xOffset, railTopCenterY, diamondRadius, diamondPaint) // Top-right section
            canvas.drawCircle(tableCenterX - xOffset, railBottomCenterY, diamondRadius, diamondPaint) // Bottom-left section
            canvas.drawCircle(tableCenterX + xOffset, railBottomCenterY, diamondRadius, diamondPaint) // Bottom-right section
        }

        // Short rails (3 diamonds between corner pockets)
        for (i in 1..3) {
            val yOffset = halfHeight * (i / 4.0f)
            canvas.drawCircle(railLeftCenterX, tableCenterY - yOffset, diamondRadius, diamondPaint) // Left rail
            canvas.drawCircle(railRightCenterX, tableCenterY - yOffset, diamondRadius, diamondPaint) // Right rail
            canvas.drawCircle(railLeftCenterX, tableCenterY + yOffset, diamondRadius, diamondPaint) // Left rail
            canvas.drawCircle(railRightCenterX, tableCenterY + yOffset, diamondRadius, diamondPaint) // Right rail
        }
    }
}