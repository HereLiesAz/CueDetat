package com.hereliesaz.cuedetatlite.view.renderer

import android.graphics.Canvas
import android.graphics.PointF
import com.hereliesaz.cuedetatlite.view.PaintCache
import com.hereliesaz.cuedetatlite.view.model.TableModel
import com.hereliesaz.cuedetatlite.view.renderer.text.LineTextRenderer
import com.hereliesaz.cuedetatlite.view.state.OverlayState
import com.hereliesaz.cuedetatlite.view.state.ScreenState
import kotlin.math.*

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

        val angleRad = Math.toRadians(protractorUnit.aimingAngleDegrees.toDouble()).toFloat()
        val totalRadius = targetBall.radius * 2
        val ghostBallX = targetBall.logicalPosition.x - cos(angleRad) * totalRadius
        val ghostBallY = targetBall.logicalPosition.y - sin(angleRad) * totalRadius
        val ghostBallPos = PointF(ghostBallX, ghostBallY)

        // Aiming line from Ghost Ball to Target Ball
        canvas.drawLine(ghostBallX, ghostBallY, targetBall.logicalPosition.x, targetBall.logicalPosition.y, paintCache.aimingLinePaint)

        // Shot line (from actual cue ball, if present)
        screenState.actualCueBall?.let { actualCueBall ->
            if (screenState.showActualCueBall) {
                val dx = ghostBallX - actualCueBall.logicalPosition.x
                val dy = ghostBallY - actualCueBall.logicalPosition.y
                canvas.drawLine(
                    actualCueBall.logicalPosition.x,
                    actualCueBall.logicalPosition.y,
                    actualCueBall.logicalPosition.x + dx * 10,
                    actualCueBall.logicalPosition.y + dy * 10,
                    paintCache.shotLinePaint
                )
            }
        }

        // Tangent Lines
        drawTangentLines(canvas, targetBall.logicalPosition, ghostBallPos, paintCache)

        // Protractor lines
        drawProtractorAngleLines(canvas, targetBall.logicalPosition, ghostBallPos, paintCache)
    }

    private fun drawTangentLines(canvas: Canvas, targetCenter: PointF, ghostCenter: PointF, paints: PaintCache) {
        val dx = targetCenter.x - ghostCenter.x
        val dy = targetCenter.y - ghostCenter.y
        val mag = sqrt(dx*dx + dy*dy)
        if (mag == 0f) return

        val tangentDx = -dy / mag
        val tangentDy = dx / mag
        val extend = 2000f

        canvas.drawLine(ghostCenter.x, ghostCenter.y, ghostCenter.x + tangentDx * extend, ghostCenter.y + tangentDy * extend, paints.tangentLineDottedPaint)
        canvas.drawLine(ghostCenter.x, ghostCenter.y, ghostCenter.x - tangentDx * extend, ghostCenter.y - tangentDy * extend, paints.tangentLineDottedPaint)
    }

    private fun drawProtractorAngleLines(canvas: Canvas, targetCenter: PointF, ghostCenter: PointF, paints: PaintCache) {
        val baseAngleRad = atan2(ghostCenter.y - targetCenter.y, ghostCenter.x - targetCenter.x)
        val extend = 2000f

        listOf(15f, 30f, 45f).forEach { angleDeg ->
            val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()

            val pX = targetCenter.x + extend * cos(baseAngleRad + angleRad)
            val pY = targetCenter.y + extend * sin(baseAngleRad + angleRad)
            canvas.drawLine(targetCenter.x, targetCenter.y, pX, pY, paints.protractorLinePaint)

            val nX = targetCenter.x + extend * cos(baseAngleRad - angleRad)
            val nY = targetCenter.y + extend * sin(baseAngleRad - angleRad)
            canvas.drawLine(targetCenter.x, targetCenter.y, nX, nY, paints.protractorLinePaint)
        }
    }

    private fun drawBankingLines(canvas: Canvas, screenState: ScreenState) {
        val path = screenState.bankingPath
        val tableModel = screenState.tableModel ?: return
        if (path.size < 2) return

        val bankLinePaints = listOf(paintCache.bankShotLinePaint1, paintCache.bankShotLinePaint2, paintCache.bankShotLinePaint3)

        for (i in 0 until path.size - 1) {
            val start = path[i]
            val end = path[i + 1]

            val paint = bankLinePaints.getOrElse(i) { bankLinePaints.last() }
            canvas.drawLine(start.x, start.y, end.x, end.y, paint)
        }
    }
}