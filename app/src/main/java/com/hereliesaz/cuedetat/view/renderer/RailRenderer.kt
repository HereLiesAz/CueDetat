package com.hereliesaz.cuedetat.view.renderer

import android.graphics.Canvas
import android.graphics.Paint
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.state.OverlayState

class RailRenderer {

    private val tableToBallRatioLong = 88f
    private val tableToBallRatioShort = 44f
    private val railVisualOffsetFromEdgeFactor =
        0.75f // How far "out" rail center is from playing surface edge, times ball radius
    private val railVisualThicknessFactor =
        0.5f    // How thick the rail line itself appears, times ball radius (strokeWidth)
    private val diamondSizeFactor = 0.25f

    fun draw(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        val referenceRadius = state.actualCueBall?.radius ?: state.protractorUnit.radius
        if (referenceRadius <= 0) return

        val tablePlayingSurfaceHeight = tableToBallRatioShort * referenceRadius
        val tablePlayingSurfaceWidth = tableToBallRatioLong * referenceRadius
        val tableCenterX = state.viewWidth / 2f
        val tableCenterY = state.viewHeight / 2f

        val innerLeft = tableCenterX - tablePlayingSurfaceWidth / 2
        val innerTop = tableCenterY - tablePlayingSurfaceHeight / 2
        val innerRight = tableCenterX + tablePlayingSurfaceWidth / 2
        val innerBottom = tableCenterY + tablePlayingSurfaceHeight / 2

        val railLinePaint = Paint(paints.tableOutlinePaint).apply {
            // Rails might be thicker than the table playing surface outline
            strokeWidth = referenceRadius * railVisualThicknessFactor
        }
        val diamondPaint = Paint(paints.tableOutlinePaint).apply {
            // Diamonds are small, use a thinner stroke or fill
            strokeWidth =
                (referenceRadius * diamondSizeFactor) / 2f // Make stroke relative to diamond size
            // style = Paint.Style.FILL_AND_STROKE // If you want filled diamonds
        }


        // Calculate Y positions for horizontal rails & X for vertical, offset from playing surface
        val railOffsetAmount = referenceRadius * railVisualOffsetFromEdgeFactor

        val railTopCenterY = innerTop - railOffsetAmount
        val railBottomCenterY = innerBottom + railOffsetAmount
        val railLeftCenterX = innerLeft - railOffsetAmount
        val railRightCenterX = innerRight + railOffsetAmount

        // Extend rail lines slightly past table corners for a mitered look (optional)
        val railEndExtension = railOffsetAmount * 1.5f

        // Draw rail lines (these are drawn on the canvas with railPitchMatrix for lift)
        // Top Rail
        canvas.drawLine(
            innerLeft - railEndExtension,
            railTopCenterY,
            innerRight + railEndExtension,
            railTopCenterY,
            railLinePaint
        )
        // Bottom Rail
        canvas.drawLine(
            innerLeft - railEndExtension,
            railBottomCenterY,
            innerRight + railEndExtension,
            railBottomCenterY,
            railLinePaint
        )
        // Left Rail
        canvas.drawLine(
            railLeftCenterX,
            innerTop - railEndExtension,
            railLeftCenterX,
            innerBottom + railEndExtension,
            railLinePaint
        )
        // Right Rail
        canvas.drawLine(
            railRightCenterX,
            innerTop - railEndExtension,
            railRightCenterX,
            innerBottom + railEndExtension,
            railLinePaint
        )

        // --- Draw Diamonds ---
        val diamondRadius = referenceRadius * diamondSizeFactor

        // Diamonds on the long rails (top and bottom)
        for (i in 1..3) {
            val xPos = innerLeft + (tablePlayingSurfaceWidth * (i / 4.0f)) // 1/4, 1/2, 3/4 points
            canvas.drawCircle(xPos, railTopCenterY, diamondRadius, diamondPaint)
            canvas.drawCircle(xPos, railBottomCenterY, diamondRadius, diamondPaint)
        }

        // Diamonds on the short rails (left and right) - typically one in the middle
        canvas.drawCircle(railLeftCenterX, tableCenterY, diamondRadius, diamondPaint)
        canvas.drawCircle(railRightCenterX, tableCenterY, diamondRadius, diamondPaint)
    }
}