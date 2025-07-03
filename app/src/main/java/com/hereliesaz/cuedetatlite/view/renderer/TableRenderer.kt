package com.hereliesaz.cuedetatlite.view.renderer

import android.graphics.Canvas
import android.graphics.Paint
import com.hereliesaz.cuedetatlite.view.PaintCache
import com.hereliesaz.cuedetatlite.view.state.OverlayState

class TableRenderer(private val paints: PaintCache) {

    private val POCKET_TO_BALL_RATIO = 1.8f
    private val DIAMOND_SIZE_FACTOR = 0.25f

    fun draw(canvas: Canvas, state: OverlayState) {
        val tableModel = state.screenState.tableModel ?: return
        val referenceRadius = state.screenState.actualCueBall?.radius ?: state.screenState.protractorUnit.targetBall.radius
        if (referenceRadius <= 0) return

        // Draw playing surface outline
        canvas.drawRect(tableModel.bounds, paints.tableOutlinePaint)

        // Draw pockets as stroked circles
        val pocketRadius = referenceRadius * POCKET_TO_BALL_RATIO
        val pocketPaint = paints.tableOutlinePaint

        tableModel.pockets.forEach { pocketCenter ->
            canvas.drawCircle(pocketCenter.x, pocketCenter.y, pocketRadius, pocketPaint)
        }

        // Draw Diamonds
        val diamondRadius = referenceRadius * DIAMOND_SIZE_FACTOR
        val diamondPaint = Paint(paints.tableOutlinePaint).apply {
            strokeWidth = (referenceRadius * DIAMOND_SIZE_FACTOR) / 2f
        }

        // Diamonds on the long rails (top and bottom)
        for (i in 1..3) {
            val xPos = tableModel.bounds.left + (tableModel.bounds.width() * (i / 4.0f))
            canvas.drawCircle(xPos, tableModel.bounds.top, diamondRadius, diamondPaint)
            canvas.drawCircle(xPos, tableModel.bounds.bottom, diamondRadius, diamondPaint)
        }

        // Diamonds on the short rails (left and right)
        canvas.drawCircle(tableModel.bounds.left, tableModel.bounds.centerY(), diamondRadius, diamondPaint)
        canvas.drawCircle(tableModel.bounds.right, tableModel.bounds.centerY(), diamondRadius, diamondPaint)
    }
}
