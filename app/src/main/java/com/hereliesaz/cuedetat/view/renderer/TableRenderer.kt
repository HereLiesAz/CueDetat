package com.hereliesaz.cuedetat.view.renderer

import android.graphics.Canvas
import android.graphics.Paint
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.state.OverlayState

class TableRenderer {

    private val tableToBallRatioLong = 88f
    private val tableToBallRatioShort = 44f

    fun draw(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        // In banking mode, the table's scale is relative to the ActualCueBall's radius (which is the BankingBall)
        // This radius already incorporates the overall zoom level.
        val referenceRadius = state.actualCueBall?.radius
            ?: state.protractorUnit.radius // Fallback, but should always have actualCueBall in banking

        if (referenceRadius <= 0) return

        // Logical dimensions of the playing surface
        val tablePlayingSurfaceHeight = tableToBallRatioShort * referenceRadius
        val tablePlayingSurfaceWidth = tableToBallRatioLong * referenceRadius

        // Table is always logically centered on the view's logical center
        val tableCenterX = state.viewWidth / 2f
        val tableCenterY = state.viewHeight / 2f

        val left = tableCenterX - tablePlayingSurfaceWidth / 2
        val top = tableCenterY - tablePlayingSurfaceHeight / 2
        val right = tableCenterX + tablePlayingSurfaceWidth / 2
        val bottom = tableCenterY + tablePlayingSurfaceHeight / 2

        // Draw playing surface outline
        canvas.drawRect(left, top, right, bottom, paints.tableOutlinePaint)

        // Draw pockets as stroked circles
        val pocketRadius = referenceRadius * 1.8f // Pockets are roughly 1.8x ball radius
        val pocketPaint = paints.tableOutlinePaint // Use same stroke style as table outline

        // Corner Pockets (centers are at the corners of the playing surface)
        canvas.drawCircle(left, top, pocketRadius, pocketPaint)
        canvas.drawCircle(right, top, pocketRadius, pocketPaint)
        canvas.drawCircle(left, bottom, pocketRadius, pocketPaint)
        canvas.drawCircle(right, bottom, pocketRadius, pocketPaint)

        // Side Pockets (centers are at the midpoint of the long sides)
        canvas.drawCircle(tableCenterX, top, pocketRadius, pocketPaint)
        canvas.drawCircle(tableCenterX, bottom, pocketRadius, pocketPaint)
    }
}