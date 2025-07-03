package com.hereliesaz.cuedetatlite.view.renderer

import android.graphics.Canvas
import android.graphics.Paint
import com.hereliesaz.cuedetatlite.view.PaintCache
import com.hereliesaz.cuedetatlite.view.state.OverlayState

class RailRenderer(private val paints: PaintCache) {

    private val railVisualOffsetFromEdgeFactor = 0.75f
    private val railVisualThicknessFactor = 0.5f
    private val diamondSizeFactor = 0.25f

    fun draw(canvas: Canvas, state: OverlayState) {
        val tableModel = state.screenState.tableModel ?: return
        val referenceRadius = state.screenState.actualCueBall?.radius ?: state.screenState.protractorUnit.targetBall.radius
        if (referenceRadius <= 0) return

        val railLinePaint = Paint(paints.tableOutlinePaint).apply {
            strokeWidth = referenceRadius * railVisualThicknessFactor
        }

        val diamondRadius = referenceRadius * diamondSizeFactor
        val diamondPaint = Paint(paints.tableOutlinePaint).apply {
            strokeWidth = (referenceRadius * diamondSizeFactor) / 2f
        }

        val railOffsetAmount = referenceRadius * railVisualOffsetFromEdgeFactor
        val tableBounds = tableModel.bounds

        val railTopCenterY = tableBounds.top - railOffsetAmount
        val railBottomCenterY = tableBounds.bottom + railOffsetAmount
        val railLeftCenterX = tableBounds.left - railOffsetAmount
        val railRightCenterX = tableBounds.right + railOffsetAmount

        val railEndExtension = railOffsetAmount * 1.5f

        // Draw rail lines
        canvas.drawLine(tableBounds.left - railEndExtension, railTopCenterY, tableBounds.right + railEndExtension, railTopCenterY, railLinePaint)
        canvas.drawLine(tableBounds.left - railEndExtension, railBottomCenterY, tableBounds.right + railEndExtension, railBottomCenterY, railLinePaint)
        canvas.drawLine(railLeftCenterX, tableBounds.top - railEndExtension, railLeftCenterX, tableBounds.bottom + railEndExtension, railLinePaint)
        canvas.drawLine(railRightCenterX, tableBounds.top - railEndExtension, railRightCenterX, tableBounds.bottom + railEndExtension, railLinePaint)

        // Draw Diamonds on rails
        for (i in 1..3) {
            val xPos = tableBounds.left + (tableBounds.width() * (i / 4.0f))
            canvas.drawCircle(xPos, railTopCenterY, diamondRadius, diamondPaint)
            canvas.drawCircle(xPos, railBottomCenterY, diamondRadius, diamondPaint)
        }
        canvas.drawCircle(railLeftCenterX, tableBounds.centerY(), diamondRadius, diamondPaint)
        canvas.drawCircle(railRightCenterX, tableBounds.centerY(), diamondRadius, diamondPaint)
    }
}
