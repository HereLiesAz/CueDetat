package com.hereliesaz.cuedetat.view.renderer.table

import android.graphics.Canvas
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.state.OverlayState

class TableRenderer {

    private val tableToBallRatioLong = 88f
    private val tableToBallRatioShort = 44f

    fun draw(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        if (state.showTable || state.isBankingMode) {
            val referenceRadius = state.onPlaneBall?.radius ?: state.protractorUnit.radius
            if (referenceRadius <= 0) return

            val tablePlayingSurfaceHeight = tableToBallRatioShort * referenceRadius
            val tablePlayingSurfaceWidth = tableToBallRatioLong * referenceRadius
            val tableCenterX = state.viewWidth / 2f
            val tableCenterY = state.viewHeight / 2f

            val left = tableCenterX - tablePlayingSurfaceWidth / 2
            val top = tableCenterY - tablePlayingSurfaceHeight / 2
            val right = tableCenterX + tablePlayingSurfaceWidth / 2
            val bottom = tableCenterY + tablePlayingSurfaceHeight / 2

            // Draw Outline Only, no fill.
            canvas.drawRect(left, top, right, bottom, paints.tableOutlinePaint)

            // Draw Pockets
            val pocketRadius = referenceRadius * 1.8f
            canvas.drawCircle(left, top, pocketRadius, paints.tableOutlinePaint)
            canvas.drawCircle(right, top, pocketRadius, paints.tableOutlinePaint)
            canvas.drawCircle(left, bottom, pocketRadius, paints.tableOutlinePaint)
            canvas.drawCircle(right, bottom, pocketRadius, paints.tableOutlinePaint)
            canvas.drawCircle(tableCenterX, top, pocketRadius, paints.tableOutlinePaint)
            canvas.drawCircle(tableCenterX, bottom, pocketRadius, paints.tableOutlinePaint)
        }
    }
}