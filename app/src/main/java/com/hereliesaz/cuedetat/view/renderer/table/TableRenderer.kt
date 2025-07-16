// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/table/TableRenderer.kt
package com.hereliesaz.cuedetat.view.renderer.table

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.ui.theme.RebelYellow
import com.hereliesaz.cuedetat.ui.theme.WarningRed
import com.hereliesaz.cuedetat.view.config.table.Holes
import com.hereliesaz.cuedetat.view.config.table.Table
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject

class TableRenderer @Inject constructor() {

    private val tableConfig = Table()
    private val holesConfig = Holes()

    fun drawSurface(canvas: Canvas, state: OverlayState, typeface: Typeface?) {
        val geometry = state.table.geometry
        if (!geometry.isValid) return

        val tableOutlinePaint = tableConfig.getStrokePaint(state.luminanceAdjustment)
        val diamondGridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(15f, 15f), 0f)
            color = tableConfig.strokeColor.toArgb()
            alpha = 75
        }

        val corners = geometry.unrotatedCorners
        val left = corners[0].x
        val top = corners[0].y
        val right = corners[2].x
        val bottom = corners[2].y

        canvas.drawRect(left, top, right, bottom, tableOutlinePaint)

        val halfWidth = geometry.width / 2
        val halfHeight = geometry.height / 2
        val tableCenterX = 0f
        val tableCenterY = 0f

        for (i in 1..3) {
            val xOffset = halfWidth * (i / 4.0f)
            canvas.drawLine(tableCenterX - xOffset, top, tableCenterX - xOffset, bottom, diamondGridPaint)
            canvas.drawLine(tableCenterX + xOffset, top, tableCenterX + xOffset, bottom, diamondGridPaint)
        }
        val shortRailYOffsets = listOf(-halfHeight / 2, 0f, halfHeight / 2)
        for (yOffset in shortRailYOffsets) {
            canvas.drawLine(left, tableCenterY + yOffset, right, tableCenterY + yOffset, diamondGridPaint)
        }
        canvas.drawLine(tableCenterX, top, tableCenterX, bottom, diamondGridPaint)
        canvas.drawLine(left, tableCenterY, right, tableCenterY, diamondGridPaint)
    }

    fun drawPockets(canvas: Canvas, state: OverlayState) {
        val geometry = state.table.geometry
        if (!geometry.isValid) return

        val referenceRadius = state.onPlaneBall?.radius ?: state.protractorUnit.radius
        val pockets = state.table.getLogicalPockets(referenceRadius)
        val pocketRadius = referenceRadius * 1.8f

        val pocketedPaintWhite = holesConfig.getFillPaint(state.luminanceAdjustment).apply { color = android.graphics.Color.WHITE }
        val pocketedPaintRed = holesConfig.getFillPaint(state.luminanceAdjustment).apply { color = WarningRed.toArgb() }
        val pocketOutlinePaint = holesConfig.getStrokePaint(state.luminanceAdjustment)
        val pocketFillPaint = holesConfig.getFillPaint(state.luminanceAdjustment)

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