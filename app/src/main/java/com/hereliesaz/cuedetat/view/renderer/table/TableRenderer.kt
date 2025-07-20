// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/table/TableRenderer.kt

package com.hereliesaz.cuedetat.view.renderer.table

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.ui.theme.WarningRed
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.config.table.Holes
import com.hereliesaz.cuedetat.view.state.OverlayState

class TableRenderer {

    companion object {
        fun getLogicalPockets(state: OverlayState): List<PointF> {
            return state.table.pockets
        }
    }

    fun drawSurface(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        if (!state.table.isVisible) return

        val corners = state.table.corners
        if (corners.size < 4) return

        val tableOutlinePaint = paints.tableOutlinePaint
        val diamondGridPaint = paints.gridLinePaint

        // Draw Rotated Outline
        val path = Path()
        path.moveTo(corners[0].x, corners[0].y)
        path.lineTo(corners[1].x, corners[1].y)
        path.lineTo(corners[2].x, corners[2].y)
        path.lineTo(corners[3].x, corners[3].y)
        path.close()
        canvas.drawPath(path, tableOutlinePaint)

        // --- HERESY CORRECTED: Draw the proper 3x7 diamond system grid. ---
        // Vertical lines (3 lines)
        for (i in 1..3) {
            val fraction = i / 4.0f
            val top = interpolate(corners[0], corners[1], fraction)
            val bottom = interpolate(corners[3], corners[2], fraction)
            canvas.drawLine(top.x, top.y, bottom.x, bottom.y, diamondGridPaint)
        }

        // Horizontal lines (7 lines)
        for (i in 1..7) {
            val fraction = i / 8.0f
            val left = interpolate(corners[0], corners[3], fraction)
            val right = interpolate(corners[1], corners[2], fraction)
            canvas.drawLine(left.x, left.y, right.x, right.y, diamondGridPaint)
        }
        // --- END CORRECTION ---
    }

    private fun interpolate(p1: PointF, p2: PointF, fraction: Float): PointF {
        return PointF(p1.x + (p2.x - p1.x) * fraction, p1.y + (p2.y - p1.y) * fraction)
    }

    fun drawPockets(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        if (!state.table.isVisible) return
        val referenceRadius = state.onPlaneBall?.radius ?: state.protractorUnit.radius
        if (referenceRadius <= 0) return

        val holesConfig = Holes()
        val pockets = getLogicalPockets(state)
        val pocketRadius = referenceRadius * 1.8f

        val pocketedPaintWhite =
            Paint(paints.pocketFillPaint).apply { color = android.graphics.Color.WHITE }
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
            val isAimedAtByTangentLine =
                state.tangentAimedPocketIndex == index && !state.isBankingMode
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