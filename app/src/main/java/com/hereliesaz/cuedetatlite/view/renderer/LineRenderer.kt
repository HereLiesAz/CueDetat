// hereliesaz/cuedetat/CueDetat-CueDetatLite/app/src/main/java/com/hereliesaz/cuedetatlite/view/renderer/LineRenderer.kt
package com.hereliesaz.cuedetatlite.view.renderer

import android.graphics.Canvas
import android.graphics.PointF
import com.hereliesaz.cuedetatlite.view.PaintCache
import com.hereliesaz.cuedetatlite.view.model.TableModel
import com.hereliesaz.cuedetatlite.view.renderer.text.LineTextRenderer
import com.hereliesaz.cuedetatlite.view.state.OverlayState
import com.hereliesaz.cuedetatlite.view.state.ScreenState
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class LineRenderer(
    private val paintCache: PaintCache,
    private val lineTextRenderer: LineTextRenderer
) {

    fun draw(canvas: Canvas, screenState: ScreenState, overlayState: OverlayState) {
        canvas.save()
        canvas.concat(overlayState.pitchMatrix)

        if (screenState.isProtractorMode) {
            drawProtractorLines(canvas, screenState)
        } else {
            drawBankingLines(canvas, screenState)
        }

        canvas.restore()
    }

    private fun drawProtractorLines(canvas: Canvas, screenState: ScreenState) {
        val protractorUnit = screenState.protractorUnit

        // Aiming line
        canvas.drawLine(
            protractorUnit.targetBall.logicalPosition.x,
            protractorUnit.targetBall.logicalPosition.y,
            protractorUnit.cueBall.logicalPosition.x,
            protractorUnit.cueBall.logicalPosition.y,
            paintCache.greenPaint
        )

        // Shot line
        screenState.actualCueBall?.let { actualCueBall ->
            if (screenState.showActualCueBall) {
                val dx = protractorUnit.cueBall.logicalPosition.x - actualCueBall.logicalPosition.x
                val dy = protractorUnit.cueBall.logicalPosition.y - actualCueBall.logicalPosition.y
                canvas.drawLine(
                    actualCueBall.logicalPosition.x,
                    actualCueBall.logicalPosition.y,
                    actualCueBall.logicalPosition.x + dx * 10,
                    actualCueBall.logicalPosition.y + dy * 10,
                    paintCache.redPaint
                )
            }
        }
    }

    private fun drawBankingLines(canvas: Canvas, screenState: ScreenState) {
        val path = screenState.bankingPath
        val tableModel = screenState.tableModel ?: return
        if (path.size < 2) return

        for (i in 0 until path.size - 1) {
            val start = path[i]
            val end = path[i + 1]

            // Check if the end of the path is a pocket
            val isLastSegmentPocketed = (i == path.size - 2) && tableModel.pockets.any { pocket: TableModel.Pocket ->
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
                lineTextRenderer.draw(canvas, text, point.x, textY, paintCache.lineTextPaint)
            }
        }
    }

    private fun distance(p1: PointF, p2: PointF): Float {
        return sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))
    }
}
