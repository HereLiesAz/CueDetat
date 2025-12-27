// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/table/TableRenderer.kt

package com.hereliesaz.cuedetat.view.renderer.table

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.view.renderer.util.PaintCache

class TableRenderer {
    fun drawSurface(canvas: Canvas, state: CueDetatState, paints: PaintCache) {
        val corners = state.table.corners
        if (corners.size < 4) return

        val path = Path()
        path.moveTo(corners[0].x, corners[0].y)
        path.lineTo(corners[1].x, corners[1].y)
        path.lineTo(corners[2].x, corners[2].y)
        path.lineTo(corners[3].x, corners[3].y)
        path.close()

        val matrix = state.pitchMatrix ?: return
        val gridLinePaint = paints.gridLinePaint
        val outlinePaint = paints.tableOutlinePaint

        // Transform canvas for 3D perspective
        canvas.save()
        canvas.concat(matrix)

        // Draw surface? Currently we rely on AR camera, so we might just draw grid or outline.
        // If we want a solid surface (e.g. debugging or non-AR), we'd draw it here.
        // For AR, we often just draw the table outline.

        canvas.drawPath(path, outlinePaint)

        // Draw grid if needed (e.g. for debugging alignment)
        if (state.areHelpersVisible) {
            drawGrid(canvas, state, gridLinePaint)
        }

        canvas.restore()
    }

    private fun drawGrid(canvas: Canvas, state: CueDetatState, paint: Paint) {
        val width = state.table.logicalWidth
        val height = state.table.logicalHeight
        val step = 10f // Grid step in logical units

        val halfW = width / 2f
        val halfH = height / 2f

        // Vertical lines
        var x = -halfW
        while (x <= halfW) {
            canvas.drawLine(x, -halfH, x, halfH, paint)
            x += step
        }

        // Horizontal lines
        var y = -halfH
        while (y <= halfH) {
            canvas.drawLine(-halfW, y, halfW, y, paint)
            y += step
        }
    }

    fun drawPockets(canvas: Canvas, state: CueDetatState, paints: PaintCache) {
        val pockets = state.table.pockets
        if (pockets.isEmpty()) return

        val matrix = state.pitchMatrix ?: return
        val pocketPaint = paints.pocketFillPaint.apply {
            color = android.graphics.Color.BLACK
        }
        val outlinePaint = paints.tableOutlinePaint.apply {
            color = android.graphics.Color.BLACK
            strokeWidth = 2f
        }
        val pocketRadius = 4.5f // Logical units

        canvas.save()
        canvas.concat(matrix)

        pockets.forEach { pocket ->
             canvas.drawCircle(pocket.x, pocket.y, pocketRadius, pocketPaint)
             canvas.drawCircle(pocket.x, pocket.y, pocketRadius, outlinePaint)
        }

        canvas.restore()
    }
}
