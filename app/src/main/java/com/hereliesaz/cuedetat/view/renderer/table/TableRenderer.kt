package com.hereliesaz.cuedetat.view.renderer.table

import android.graphics.Canvas
import android.graphics.Paint
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.state.OverlayState

class TableRenderer {

    private val tableToBallRatioLong = 88f
    private val tableToBallRatioShort = 44f

    fun draw(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        if (state.showTable || state.isBankingMode) {
            val referenceRadius = state.onPlaneBall?.radius ?: state.protractorUnit.radius
            if (referenceRadius <= 0) return

            val tablePlayingSurfaceWidth = tableToBallRatioLong * referenceRadius
            val tablePlayingSurfaceHeight = tableToBallRatioShort * referenceRadius

            val canvasCenterX = state.viewWidth / 2f
            val canvasCenterY = state.viewHeight / 2f

            val left = canvasCenterX - tablePlayingSurfaceWidth / 2
            val top = canvasCenterY - tablePlayingSurfaceHeight / 2
            val right = canvasCenterX + tablePlayingSurfaceWidth / 2
            val bottom = canvasCenterY + tablePlayingSurfaceHeight / 2

            // Draw Outline Only, no fill.
            canvas.drawRect(left, top, right, bottom, paints.tableOutlinePaint)

            // Draw Pockets
            val pocketRadius = referenceRadius * 1.8f
            val pockets = listOf(
                Pair(left, top), Pair(right, top), Pair(left, bottom), Pair(right, bottom),
                Pair(canvasCenterX, top), Pair(canvasCenterX, bottom)
            )

            // Prepare the white paint for pocketed shots
            val pocketedPaint = Paint(paints.pocketFillPaint).apply { color = android.graphics.Color.WHITE }

            pockets.forEachIndexed { index, pos ->
                val fillPaint = if (state.isBankingMode && index == state.pocketedBankShotPocketIndex) {
                    pocketedPaint
                } else {
                    paints.pocketFillPaint
                }
                // Fill the pocket first
                canvas.drawCircle(pos.first, pos.second, pocketRadius, fillPaint)
                // Then draw the outline
                canvas.drawCircle(pos.first, pos.second, pocketRadius, paints.tableOutlinePaint)
            }
        }
    }
}