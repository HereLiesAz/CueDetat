package com.hereliesaz.cuedetat.view.renderer.table

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.state.OverlayState

class TableRenderer {

    companion object {
        fun getLogicalPockets(state: OverlayState): List<PointF> {
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

            return listOf(
                PointF(left, top), PointF(right, top),
                PointF(left, bottom), PointF(right, bottom),
                PointF(canvasCenterX, top), PointF(canvasCenterX, bottom)
            )
        }
    }

    fun draw(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        if (state.showTable || state.isBankingMode) {
            val referenceRadius = state.onPlaneBall?.radius ?: state.protractorUnit.radius
            if (referenceRadius <= 0) return

            // Use the state's table size to determine proportions
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

            // Draw Outline Only, no fill.
            canvas.drawRect(left, top, right, bottom, paints.tableOutlinePaint)

            // Draw Diamond Grid
            val diamondGridPaint = paints.gridLinePaint
            // Vertical lines (3 on each side of center)
            for (i in 1..3) {
                val xOffset = tablePlayingSurfaceWidth * (i / 4.0f)
                canvas.drawLine(canvasCenterX - xOffset, top, canvasCenterX - xOffset, bottom, diamondGridPaint)
                canvas.drawLine(canvasCenterX + xOffset, top, canvasCenterX + xOffset, bottom, diamondGridPaint)
            }
            // Horizontal lines (1 on each side of center)
            for (i in 1..1) {
                val yOffset = tablePlayingSurfaceHeight * (i / 2.0f)
                canvas.drawLine(left, canvasCenterY - yOffset, right, canvasCenterY - yOffset, diamondGridPaint)
                canvas.drawLine(left, canvasCenterY + yOffset, right, canvasCenterY + yOffset, diamondGridPaint)
            }
            // Center lines
            canvas.drawLine(canvasCenterX, top, canvasCenterX, bottom, diamondGridPaint)
            canvas.drawLine(left, canvasCenterY, right, canvasCenterY, diamondGridPaint)


            // Draw Pockets
            val pocketRadius = referenceRadius * 1.8f
            val pockets = getLogicalPockets(state)

            // Prepare the white paint for pocketed shots
            val pocketedPaint = Paint(paints.pocketFillPaint).apply { color = android.graphics.Color.WHITE }

            pockets.forEachIndexed { index, pos ->
                val isAimedAt = state.aimedPocketIndex == index && !state.isBankingMode
                val isPocketedInBank = state.isBankingMode && index == state.pocketedBankShotPocketIndex

                val fillPaint = if (isAimedAt || isPocketedInBank) {
                    pocketedPaint
                } else {
                    paints.pocketFillPaint
                }
                // Fill the pocket first
                canvas.drawCircle(pos.x, pos.y, pocketRadius, fillPaint)
                // Then draw the outline
                canvas.drawCircle(pos.x, pos.y, pocketRadius, paints.tableOutlinePaint)
            }
        }
    }
}