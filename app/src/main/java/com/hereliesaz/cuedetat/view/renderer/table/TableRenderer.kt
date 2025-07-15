// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/table/TableRenderer.kt

package com.hereliesaz.cuedetat.view.renderer.table

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.ui.theme.WarningRed
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.config.table.Holes
import com.hereliesaz.cuedetat.view.config.table.Table
import com.hereliesaz.cuedetat.view.state.OverlayState

class TableRenderer {

    companion object {
        fun getLogicalPockets(state: OverlayState): List<PointF> {
            val referenceRadius = state.onPlaneBall?.radius ?: state.protractorUnit.radius
            if (referenceRadius <= 0) return emptyList()

            val ballRealDiameter = 2.25f
            val ballLogicalDiameter = referenceRadius * 2
            val scale = ballLogicalDiameter / ballRealDiameter

            val tablePlayingSurfaceWidth = state.tableSize.longSideInches * scale
            val tablePlayingSurfaceHeight = state.tableSize.shortSideInches * scale

            // Corrected: Use logical origin (0,0) instead of screen center
            val tableCenterX = 0f
            val tableCenterY = 0f
            val left = tableCenterX - tablePlayingSurfaceWidth / 2
            val top = tableCenterY - tablePlayingSurfaceHeight / 2
            val right = tableCenterX + tablePlayingSurfaceWidth / 2
            val bottom = tableCenterY + tablePlayingSurfaceHeight / 2

            // Move side pockets outward by half a ball radius.
            val sidePocketOffset = referenceRadius * 0.5f

            return listOf(
                PointF(left, top), PointF(right, top), // Top corners
                PointF(left, bottom), PointF(right, bottom),   // Bottom corners
                PointF(tableCenterX, top - sidePocketOffset), PointF(tableCenterX, bottom + sidePocketOffset) // Side pockets
            )
        }
    }

    fun drawSurface(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        if (!state.showTable && !state.isBankingMode) return

        val referenceRadius = state.onPlaneBall?.radius ?: state.protractorUnit.radius
        if (referenceRadius <= 0) return

        val tableConfig = Table()

        val ballRealDiameter = 2.25f
        val ballLogicalDiameter = referenceRadius * 2
        val scale = ballLogicalDiameter / ballRealDiameter
        val tablePlayingSurfaceWidth = state.tableSize.longSideInches * scale
        val tablePlayingSurfaceHeight = state.tableSize.shortSideInches * scale

        // Corrected: Use logical origin (0,0) instead of screen center
        val tableCenterX = 0f
        val tableCenterY = 0f

        val left = tableCenterX - tablePlayingSurfaceWidth / 2
        val top = tableCenterY - tablePlayingSurfaceHeight / 2
        val right = tableCenterX + tablePlayingSurfaceWidth / 2
        val bottom = tableCenterY + tablePlayingSurfaceHeight / 2

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
            canvas.drawLine(tableCenterX - xOffset, top, tableCenterX - xOffset, bottom, diamondGridPaint)
            canvas.drawLine(tableCenterX + xOffset, top, tableCenterX + xOffset, bottom, diamondGridPaint)
        }
        // Horizontal lines (connecting short rail diamonds)
        val shortRailYOffsets = listOf(-halfHeight / 2, 0f, halfHeight / 2)
        for (yOffset in shortRailYOffsets) {
            canvas.drawLine(left, tableCenterY + yOffset, right, tableCenterY + yOffset, diamondGridPaint)
        }
        // Center lines
        canvas.drawLine(tableCenterX, top, tableCenterX, bottom, diamondGridPaint)
        canvas.drawLine(left, tableCenterY, right, tableCenterY, diamondGridPaint)
    }

    fun drawPockets(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        if (!state.showTable && !state.isBankingMode) return
        val referenceRadius = state.onPlaneBall?.radius ?: state.protractorUnit.radius
        if (referenceRadius <= 0) return

        val holesConfig = Holes()
        val pockets = getLogicalPockets(state)
        val pocketRadius = referenceRadius * 1.8f

        val pocketedPaintWhite = Paint(paints.pocketFillPaint).apply { color = android.graphics.Color.WHITE }
        val pocketedPaintRed = Paint(paints.pocketFillPaint).apply { color = WarningRed.toArgb() }
        val pocketOutlinePaint = Paint(paints.tableOutlinePaint).apply {
            color = holesConfig.strokeColor.toArgb()
            strokeWidth = holesConfig.strokeWidth
        }
        val pocketFillPaint = Paint(paints.pocketFillPaint).apply {
            color = holesConfig.fillColor.toArgb()
        }


        pockets.forEachIndexed { index, pos ->
            val isAimedAtByAimingLine = state.aimedPocketIndex == index && !state.isBankingMode
            val isAimedAtByTangentLine = state.tangentAimedPocketIndex == index && !state.isBankingMode
            val isPocketedInBank = state.isBankingMode && index == state.pocketedBankShotPocketIndex

            val fillPaint = when {
                isAimedAtByAimingLine || isPocketedInBank -> pocketedPaintWhite
                isAimedAtByTangentLine -> pocketedPaintRed
                else -> pocketFillPaint
            }
            canvas.drawCircle(pos.x, pos.y, pocketRadius, fillPaint)
            canvas.drawCircle(pos.x, pos.y, pocketRadius, pocketOutlinePaint)
        }
    }
}