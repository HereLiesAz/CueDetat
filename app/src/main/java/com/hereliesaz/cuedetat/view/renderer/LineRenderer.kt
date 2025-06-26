// app/src/main/java/com/hereliesaz/cuedetat/view/renderer/LineRenderer.kt
package com.hereliesaz.cuedetat.view.renderer

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Typeface
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.renderer.text.LineTextRenderer
import com.hereliesaz.cuedetat.view.state.OverlayState
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object LineRenderer {

    private val lineTextRenderer = LineTextRenderer()
    private const val LINE_LENGTH_MULTIPLIER = 2.5f

    fun drawLogicalLines(canvas: Canvas, state: OverlayState, paints: PaintCache, typeface: Typeface?) {
        if (state.isBankingMode) {
            drawBankingLines(canvas, state, paints)
        } else {
            drawProtractorLines(canvas, state, paints, typeface)
        }
    }

    private fun drawBankingLines(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        val cueBall = state.actualCueBall ?: return
        val aimTarget = state.bankingAimTarget ?: return

        // For now, just draw a direct line. Reflection logic is complex.
        canvas.drawLine(cueBall.logicalPosition.x, cueBall.logicalPosition.y, aimTarget.x, aimTarget.y, paints.bankShotLinePaint1)
    }

    private fun drawProtractorLines(canvas: Canvas, state: OverlayState, paints: PaintCache, typeface: Typeface?) {
        val unit = state.protractorUnit
        val radius = unit.radius

        // --- Draw elements relative to the protractor's logical position and rotation ---
        canvas.save()
        canvas.translate(unit.logicalPosition.x, unit.logicalPosition.y)
        canvas.rotate(unit.rotationDegrees)

        // Draw aiming line (from ghost cue to target)
        val ghostCueLocalY = 2 * radius
        canvas.drawLine(0f, 0f, 0f, ghostCueLocalY, paints.aimingLinePaint)

        // Draw tangent line for cue ball deflection
        val tangentStart = PointF(-radius, ghostCueLocalY)
        val tangentEnd = PointF(radius, ghostCueLocalY)
        canvas.drawLine(tangentStart.x, tangentStart.y, tangentEnd.x, tangentEnd.y, paints.tangentLineSolidPaint)

        // Draw protractor angle markings
        drawProtractorMarkings(canvas, radius, paints, state, typeface)

        canvas.restore()

        // --- Draw absolute logical lines ---
        drawAbsoluteShotLine(canvas, state, paints)
    }

    private fun drawAbsoluteShotLine(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        val cueBall = state.actualCueBall ?: return // Only draw if actual cue ball is present
        val unit = state.protractorUnit

        // Calculate the absolute logical position of the ghost cue ball
        val angleRad = Math.toRadians((unit.rotationDegrees - 90).toDouble())
        val ghostCueOffsetX = (2 * unit.radius * cos(angleRad)).toFloat()
        val ghostCueOffsetY = (2 * unit.radius * sin(angleRad)).toFloat()
        val ghostCueLogicalCenter = PointF(unit.logicalPosition.x + ghostCueOffsetX, unit.logicalPosition.y + ghostCueOffsetY)

        // Extend the line from the actual cue ball through the ghost ball
        val dx = ghostCueLogicalCenter.x - cueBall.logicalPosition.x
        val dy = ghostCueLogicalCenter.y - cueBall.logicalPosition.y
        val lineLength = (state.viewWidth + state.viewHeight) * LINE_LENGTH_MULTIPLIER

        val endPointX = cueBall.logicalPosition.x + dx * lineLength
        val endPointY = cueBall.logicalPosition.y + dy * lineLength

        val shotPaint = if (state.isImpossibleShot) paints.warningPaintRed3 else paints.shotLinePaint
        canvas.drawLine(
            cueBall.logicalPosition.x,
            cueBall.logicalPosition.y,
            endPointX,
            endPointY,
            shotPaint
        )
    }

    private fun drawProtractorMarkings(canvas: Canvas, radius: Float, paints: PaintCache, state: OverlayState, typeface: Typeface?) {
        paints.lineTextPaint.typeface = typeface
        val lineLength = radius * 2
        val longMark = radius / 8f
        val shortMark = radius / 15f

        for (angle in -60..60 step 5) {
            val angleRad = Math.toRadians(angle.toDouble())
            val startX = (lineLength * sin(angleRad)).toFloat()
            val startY = (lineLength * cos(angleRad)).toFloat()
            val markLength = if (angle % 15 == 0) longMark else shortMark
            val endX = ((lineLength - markLength) * sin(angleRad)).toFloat()
            val endY = ((lineLength - markLength) * cos(angleRad)).toFloat()

            canvas.drawLine(startX, startY, endX, endY, paints.protractorLinePaint)

            if (angle % 15 == 0 && state.areHelpersVisible) {
                val text = "${kotlin.math.abs(angle)}°"
                val textDistance = lineLength + longMark
                lineTextRenderer.draw(
                    canvas, text, PointF(0f, 0f), angle.toFloat(),
                    textDistance, 90f, 0f,
                    paints.lineTextPaint, 28f, state.zoomSliderPosition
                )
            }
        }
    }
}