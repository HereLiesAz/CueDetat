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
import com.hereliesaz.cuedetat.view.config.table.Table as TableConfig
import com.hereliesaz.cuedetat.view.state.OverlayState

class TableRenderer {

    companion object {
        fun getLogicalPockets(state: OverlayState): List<PointF> {
            return state.table.pockets
        }
    }

    fun drawSurface(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        if (!state.table.isVisible) return

        val tableConfig = TableConfig()
        val corners = state.table.corners
        if (corners.size < 4) return

        val tableOutlinePaint = Paint(paints.tableOutlinePaint).apply {
            color = tableConfig.strokeColor.toArgb()
            strokeWidth = tableConfig.strokeWidth
        }

        // Draw Rotated Outline
        val path = Path()
        path.moveTo(corners[0].x, corners[0].y)
        path.lineTo(corners[1].x, corners[1].y)
        path.lineTo(corners[2].x, corners[2].y)
        path.lineTo(corners[3].x, corners[3].y)
        path.close()
        canvas.drawPath(path, tableOutlinePaint)

        // Draw Diamond Grid
        val diamondGridPaint = paints.gridLinePaint
        val topMid = interpolate(corners[0], corners[1], 0.5f)
        val bottomMid = interpolate(corners[3], corners[2], 0.5f)
        canvas.drawLine(topMid.x, topMid.y, bottomMid.x, bottomMid.y, diamondGridPaint)

        val leftMid = interpolate(corners[0], corners[3], 0.5f)
        val rightMid = interpolate(corners[1], corners[2], 0.5f)
        canvas.drawLine(leftMid.x, leftMid.y, rightMid.x, rightMid.y, diamondGridPaint)

        // Vertical lines
        val top1_4 = interpolate(corners[0], corners[1], 0.25f)
        val bottom1_4 = interpolate(corners[3], corners[2], 0.25f)
        canvas.drawLine(top1_4.x, top1_4.y, bottom1_4.x, bottom1_4.y, diamondGridPaint)

        val top3_4 = interpolate(corners[0], corners[1], 0.75f)
        val bottom3_4 = interpolate(corners[3], corners[2], 0.75f)
        canvas.drawLine(top3_4.x, top3_4.y, bottom3_4.x, bottom3_4.y, diamondGridPaint)

        // Horizontal lines
        val left1_4 = interpolate(corners[0], corners[3], 0.25f)
        val right1_4 = interpolate(corners[1], corners[2], 0.25f)
        canvas.drawLine(left1_4.x, left1_4.y, right1_4.x, right1_4.y, diamondGridPaint)

        val left3_4 = interpolate(corners[0], corners[3], 0.75f)
        val right3_4 = interpolate(corners[1], corners[2], 0.75f)
        canvas.drawLine(left3_4.x, left3_4.y, right3_4.x, right3_4.y, diamondGridPaint)
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