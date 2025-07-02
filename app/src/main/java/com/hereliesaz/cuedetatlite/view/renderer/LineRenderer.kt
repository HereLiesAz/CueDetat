package com.hereliesaz.cuedetatlite.view.renderer

import android.graphics.Canvas
import android.graphics.PointF
import com.hereliesaz.cuedetatlite.view.PaintCache
import com.hereliesaz.cuedetatlite.view.model.TableModel
import com.hereliesaz.cuedetatlite.view.renderer.text.LineTextRenderer
import com.hereliesaz.cuedetatlite.view.state.OverlayState
import com.hereliesaz.cuedetatlite.view.state.ScreenState
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
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
        val targetBall = protractorUnit.targetBall

        // --- Calculate Ghost Ball position to draw line ---
        val angleRad = Math.toRadians(protractorUnit.aimingAngleDegrees.toDouble()).toFloat()
        val totalRadius = targetBall.radius * 2
        val ghostBallX = targetBall.logicalPosition.x - cos(angleRad) * totalRadius
        val ghostBallY = targetBall.logicalPosition.y - sin(angleRad) * totalRadius

        // Aiming line from Ghost Ball to Target Ball
        canvas.drawLine(
            ghostBallX,
            ghostBallY,
            targetBall.logicalPosition.x,
            targetBall.logicalPosition.y,
            paintCache.greenPaint
        )

        // Shot line (from actual cue ball, if present)
        screenState.actualCueBall?.let { actualCueBall ->
            if (screenState.showActualCueBall) {
                // The shot line direction is from the actual cue ball towards the ghost ball
                val dx = ghostBallX - actualCueBall.logicalPosition.x
                val dy = ghostBallY - actualCueBall.logicalPosition.y
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

            val isLastSegmentPocketed = (i == path.size - 2) && tableModel.pockets.any { pocket: TableModel.Pocket ->
                distance(end, pocket.center) < pocket.radius
            }

            val paint = if (isLastSegmentPocketed) {
                paintCache.whitePaint
            } else {
                paintCache.bluePaint
            }
            canvas.drawLine(start.x, start.y, end.x, end.y, paint)
        }

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