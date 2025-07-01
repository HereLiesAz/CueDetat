package com.hereliesaz.cuedetatlite.view.renderer

import android.graphics.Canvas
import com.hereliesaz.cuedetatlite.view.PaintCache
import com.hereliesaz.cuedetatlite.view.state.OverlayState

class TableRenderer(private val paintCache: PaintCache) {

    fun draw(canvas: Canvas, state: OverlayState) {
        val tableModel = state.tableModel ?: return

        canvas.save()
        canvas.concat(state.pitchMatrix)

        // Draw table surface
        canvas.drawRect(tableModel.surface, paintCache.whitePaint)

        // Draw pockets
        tableModel.pockets.forEach { pocket ->
            canvas.drawCircle(pocket.center.x, pocket.center.y, pocket.radius, paintCache.redPaint)
        }

        // Draw diamonds
        val diamondRadius = 5f
        tableModel.getDiamonds().forEach { diamond ->
            canvas.drawCircle(diamond.x, diamond.y, diamondRadius, paintCache.greenPaint)
        }

        canvas.restore()
    }
}