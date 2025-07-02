package com.hereliesaz.cuedetatlite.view.renderer

import android.graphics.Canvas
import com.hereliesaz.cuedetatlite.view.PaintCache
import com.hereliesaz.cuedetatlite.view.state.OverlayState
import com.hereliesaz.cuedetatlite.view.state.ScreenState

class RailRenderer(private val paints: PaintCache) {

    fun draw(canvas: Canvas, screenState: ScreenState, overlayState: OverlayState) {
        canvas.save()
        canvas.concat(overlayState.railPitchMatrix)

        val tableModel = screenState.tableModel ?: return
        val railWidth = 20f

        val railPaint = screenState.actualCueBall?.let { actualCueBall ->
            val targetBall = screenState.protractorUnit.targetBall
            if (actualCueBall.logicalPosition.y > targetBall.logicalPosition.y) {
                paints.bankShotLinePaint3
            } else {
                paints.bankShotLinePaint1
            }
        } ?: paints.bankShotLinePaint1

        // Top rail
        canvas.drawRect(tableModel.surface.left - railWidth, tableModel.surface.top - railWidth, tableModel.surface.right + railWidth, tableModel.surface.top, railPaint)
        // Bottom rail
        canvas.drawRect(tableModel.surface.left - railWidth, tableModel.surface.bottom, tableModel.surface.right + railWidth, tableModel.surface.bottom + railWidth, railPaint)
        // Left rail
        canvas.drawRect(tableModel.surface.left - railWidth, tableModel.surface.top, tableModel.surface.left, tableModel.surface.bottom, railPaint)
        // Right rail
        canvas.drawRect(tableModel.surface.right, tableModel.surface.top, tableModel.surface.right + railWidth, tableModel.surface.bottom, railPaint)

        canvas.restore()
    }
}