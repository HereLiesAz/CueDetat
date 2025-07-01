// hereliesaz/cuedetat/CueDetat-CueDetatLite/app/src/main/java/com/hereliesaz/cuedetatlite/view/renderer/LineRenderer.kt
package com.hereliesaz.cuedetatlite.view.renderer

import android.graphics.Canvas
import android.graphics.PointF
import com.hereliesaz.cuedetatlite.view.PaintCache
import com.hereliesaz.cuedetatlite.view.renderer.text.LineTextRenderer
import com.hereliesaz.cuedetatlite.view.state.OverlayState
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class LineRenderer(
    private val paintCache: PaintCache,
    private val lineTextRenderer: LineTextRenderer
) {

    fun draw(canvas: Canvas, state: OverlayState) {
        canvas.save()
        canvas.concat(state.pitchMatrix)

        if (state.screenState.isProtractorMode) {
            drawProtractorLines(canvas, state)
        } else {
            drawBankingLines(canvas, state)
        }

        canvas.restore()
    }

    private fun drawProtractorLines(canvas: Canvas, state: OverlayState) {
        val protractorUnit = state.screenState.protractorUnit

        // Aiming line
        canvas.drawLine(
            protractorUnit.targetBall.center.x,
            protractorUnit.targetBall.center.y,
            protractorUnit.cueBall.center.x,
            protractorUnit.cueBall.center.y,
            paintCache.greenPaint
        )

        // Shot line
        state.screenState.actualCueBall?.let { actualCueBall ->
            if (state.screenState.showActualCueBall) {
                val dx = protractorUnit.cueBall.center.x - actualCueBall.center.x
                val dy = protractorUnit.cueBall.center.y - actualCueBall.center.y
                canvas.drawLine(
                    actualCueBall.center.x,
                    actualCueBall.center.y,
                    actualCueBall.center.x + dx * 10,
                    actualCueBall.center.y + dy * 10,
                    paintCache.redPaint
                )
            }
        }
    }

    private fun drawBankingLines(canvas: Canvas, state: OverlayState) {
        val path = state.screenState.bankingPath
        val tableModel = state.screenState.tableModel ?: return
        if (path.size < 2) return

        for (i in 0 until path.size - 1) {
            val start = path[i]
            val end = path[i + 1]

            // Check if the end of the path is a pocket
            val isLastSegmentPocketed = (i == path.size - 2) && tableModel.pockets.any { pocket ->
                distance(end, pocket.center) < pocket.radius
            }

            val paint = if (isLastSegmentPocketed) {
                paintCache.whitePaint // Use white paint for the final segment into a pocket
            } else {
                paintCache.bluePaint
            }
            canvas.drawLine(start.x, start.y, end.x, end.y, paint)
        }

        // Draw diamond numbers at reflection points
        val reflectionPoints = path.drop(1).dropLast(if (path.size > 1) 1 else 0)
        reflectionPoints.forEach { point ->
            val diamondValue = tableModel.getDiamondValue(point)
            if (diamondValue != null) {
                val text = "%.1f".format(diamondValue)
                val textY = if (abs(point.y - tableModel.surface.top) < 5) point.y - 20 else point.y + 40
                lineTextRenderer.draw(canvas, text, point.x, textY)
            }
        }
    }

    private fun distance(p1: PointF, p2: PointF): Float {
        return sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))
    }
}
