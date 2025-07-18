package com.hereliesaz.cuedetat.view.renderer.table

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.ui.theme.GunmetalFog
import com.hereliesaz.cuedetat.ui.theme.SlateGray
import com.hereliesaz.cuedetat.view.model.Table
import com.hereliesaz.cuedetat.view.state.OverlayState

object TableRenderer {
    private val tablePaint = Paint().apply {
        style = Paint.Style.FILL
        color = GunmetalFog.toArgb()
        isAntiAlias = true
    }

    private val railPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        color = SlateGray.toArgb()
        isAntiAlias = true
    }

    private val pocketPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        color = SlateGray.toArgb()
        isAntiAlias = true
    }

    private val pocketCutoutPaint = Paint().apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.BLACK
        isAntiAlias = true
    }

    private val diamondGridPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.0f
        color = SlateGray.toArgb()
        alpha = 100
        pathEffect = DashPathEffect(floatArrayOf(5f, 10f), 0f)
        isAntiAlias = true
    }

    fun drawTable(canvas: Canvas, state: OverlayState) {
        if (!state.table.isVisible) return

        canvas.save()
        canvas.concat(state.pitchMatrix)

        val table = state.table
        canvas.drawPath(table.surfacePath, tablePaint)
        drawDiamondGrid(canvas, table, diamondGridPaint)
        canvas.drawPath(table.railPath, railPaint)
        drawPockets(canvas, table, pocketPaint, pocketCutoutPaint)

        canvas.restore()
    }

    private fun drawPockets(canvas: Canvas, table: Table, rimPaint: Paint, cutoutPaint: Paint) {
        table.pockets.forEach { pocket ->
            canvas.drawCircle(pocket.x, pocket.y, table.pocketRadius, cutoutPaint)
            canvas.drawCircle(pocket.x, pocket.y, table.pocketRadius, rimPaint)
        }
    }

    private fun drawDiamondGrid(canvas: Canvas, table: Table, paint: Paint) {
        val path = Path()
        val diamonds = table.diamonds

        if (diamonds.size < 18) return

        for (i in 0..2) {
            val topStart = diamonds[i + 1]
            val bottomEnd = diamonds[15 - i]
            path.moveTo(topStart.x, topStart.y)
            path.lineTo(bottomEnd.x, bottomEnd.y)
        }

        path.moveTo(diamonds[0].x, diamonds[0].y)
        path.lineTo(diamonds[9].x, diamonds[9].y)

        path.moveTo(diamonds[4].x, diamonds[4].y)
        path.lineTo(diamonds[13].x, diamonds[13].y)

        val leftSidePocket = table.pockets[5]
        val rightSidePocket = table.pockets[2]
        path.moveTo(leftSidePocket.x, leftSidePocket.y)
        path.lineTo(rightSidePocket.x, rightSidePocket.y)

        canvas.drawPath(path, paint)
    }
}