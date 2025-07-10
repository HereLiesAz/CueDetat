package com.hereliesaz.cuedetat.view.renderer.table

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.config.table.Holes
import com.hereliesaz.cuedetat.view.config.table.Table
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

            // Load configs
            val tableConfig = Table()
            val holesConfig = Holes()

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

            // Configure paint from config
            val tableOutlinePaint = Paint(paints.tableOutlinePaint).apply {
                color = tableConfig.strokeColor.toArgb()
                strokeWidth = tableConfig.strokeWidth
            }

            // Draw Outline Only, no fill.
            canvas.drawRect(left, top, right, bottom, tableOutlinePaint)

            // Draw Diamond Grid
            val diamondGridPaint = paints.gridLinePaint
            val halfWidth = tablePlayingSurfaceWidth / 2
            val halfHeight = tablePlayingSurfaceHeight / 2

            // Vertical lines (connecting long rail diamonds)
            for (i in 1..3) {
                val xOffset = halfWidth * (i / 4.0f)
                canvas.drawLine(canvasCenterX - xOffset, top, canvasCenterX - xOffset, bottom, diamondGridPaint)
                canvas.drawLine(canvasCenterX + xOffset, top, canvasCenterX + xOffset, bottom, diamondGridPaint)
            }
            // Horizontal lines (connecting short rail diamonds)
            val shortRailYOffsets = listOf(-halfHeight / 2, 0f, halfHeight / 2)
            for (yOffset in shortRailYOffsets) {
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
            val pocketOutlinePaint = Paint(paints.tableOutlinePaint).apply {
                color = holesConfig.strokeColor.toArgb()
                strokeWidth = holesConfig.strokeWidth
            }
            val pocketFillPaint = Paint(paints.pocketFillPaint).apply {
                color = holesConfig.fillColor.toArgb()
            }


            pockets.forEachIndexed { index, pos ->
                val isAimedAt = state.aimedPocketIndex == index && !state.isBankingMode
                val isPocketedInBank = state.isBankingMode && index == state.pocketedBankShotPocketIndex

                val fillPaint = if (isAimedAt || isPocketedInBank) {
                    pocketedPaint
                } else {
                    pocketFillPaint
                }
                // Fill the pocket first
                canvas.drawCircle(pos.x, pos.y, pocketRadius, fillPaint)
                // Then draw the outline
                canvas.drawCircle(pos.x, pos.y, pocketRadius, pocketOutlinePaint)
            }
        }
    }
}