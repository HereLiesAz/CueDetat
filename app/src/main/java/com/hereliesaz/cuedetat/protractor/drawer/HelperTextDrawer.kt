package com.hereliesaz.cuedetat.protractor.drawer

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import com.hereliesaz.cuedetat.protractor.ProtractorConfig
import com.hereliesaz.cuedetat.protractor.ProtractorPaints
import com.hereliesaz.cuedetat.protractor.ProtractorState
import com.hereliesaz.cuedetat.protractor.drawer.element.ProtractorPlaneTextDrawer
import com.hereliesaz.cuedetat.protractor.drawer.element.ScreenSpaceTextDrawer
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

class HelperTextDrawer(
    private val viewWidthProvider: () -> Int,
    private val viewHeightProvider: () -> Int
) {
    private val drawnTextBounds = mutableListOf<RectF>()
    private val MAX_NUDGE_ATTEMPTS = 5
    private val NUDGE_DISTANCE_STEP = 30.0f // Temporarily increased from 15.0f

    private val attemptTextDrawLambda: (canvas: Canvas, text: String, x: Float, y: Float, paint: Paint, rotationDegrees: Float, targetCenter: PointF) -> Boolean =
        { canvas, text, x, y, paint, rotationDegrees, targetCenter ->
            attemptToDrawTextWithNudging(canvas, text, x, y, paint, rotationDegrees, targetCenter)
        }

    private val planeTextDrawer = ProtractorPlaneTextDrawer(attemptTextDrawLambda, viewWidthProvider, viewHeightProvider)
    private val screenSpaceTextDrawer = ScreenSpaceTextDrawer(attemptTextDrawLambda, viewWidthProvider, viewHeightProvider)

    fun prepareForNewFrame() {
        drawnTextBounds.clear()
    }

    private fun calculateBounds(text: String, currentX: Float, currentY: Float, paint: Paint, rotationDegrees: Float): RectF {
        val lines = text.split('\n')
        val textWidth = lines.maxOfOrNull { paint.measureText(it) } ?: 0f
        val lineHeight = paint.descent() - paint.ascent()
        val textHeight = lineHeight * lines.size + (paint.fontSpacing - lineHeight) * (lines.size -1).coerceAtLeast(0)

        return if (rotationDegrees == 0f) {
            var top = currentY + paint.ascent()
            var bottom = currentY + paint.descent()
            if (lines.size > 1) {
                if (paint.textAlign == Paint.Align.CENTER) {
                    val blockTop = currentY - (textHeight / 2f)
                    top = blockTop
                    bottom = blockTop + textHeight
                } else {
                    top = currentY + paint.ascent()
                    bottom = currentY + (textHeight - lineHeight) + paint.descent()
                }
            }
            when (paint.textAlign) {
                Paint.Align.CENTER -> RectF(currentX - textWidth / 2, top, currentX + textWidth / 2, bottom)
                Paint.Align.LEFT -> RectF(currentX, top, currentX + textWidth, bottom)
                Paint.Align.RIGHT -> RectF(currentX - textWidth, top, currentX, bottom)
                else -> RectF(currentX, currentY, currentX, currentY)
            }
        } else {
            val maxDim = max(textWidth, textHeight) * 1.4f
            RectF(currentX - maxDim / 2, currentY - maxDim / 2, currentX + maxDim / 2, currentY + maxDim / 2)
        }
    }

    private fun drawTextAtPosition(canvas: Canvas, text: String, x: Float, y: Float, paint: Paint, rotationDegrees: Float) {
        val lines = text.split('\n')
        val lineHeight = paint.descent() - paint.ascent()
        val textHeight = lineHeight * lines.size + (paint.fontSpacing - lineHeight) * (lines.size -1).coerceAtLeast(0)

        if (rotationDegrees != 0f) {
            canvas.save()
            canvas.translate(x, y)
            canvas.rotate(rotationDegrees)
            var lineOffsetY = -(textHeight / 2f) + (lineHeight / 2f)
            lines.forEach { line ->
                val drawX = when (paint.textAlign) {
                    Paint.Align.CENTER -> -paint.measureText(line) / 2f
                    Paint.Align.LEFT -> 0f
                    Paint.Align.RIGHT -> -paint.measureText(line)
                    else -> 0f
                }
                canvas.drawText(line, drawX, lineOffsetY, paint)
                lineOffsetY += paint.fontSpacing
            }
            canvas.restore()
        } else {
            var currentYBaseline = y
            if (lines.size > 1 && paint.textAlign == Paint.Align.CENTER) {
                currentYBaseline = (y - textHeight / 2f) - paint.ascent()
            }
            lines.forEachIndexed { index, line ->
                val actualDrawY = if (index == 0) currentYBaseline else currentYBaseline + (index * paint.fontSpacing)
                canvas.drawText(line, x, actualDrawY, paint)
            }
        }
    }

    private fun attemptToDrawTextWithNudging(
        canvas: Canvas, text: String,
        preferredX: Float, preferredY: Float,
        paint: Paint, rotationDegrees: Float,
        targetCenter: PointF
    ): Boolean {
        var currentX = preferredX
        var currentY = preferredY
        var currentBounds: RectF

        for (attempt in 0..MAX_NUDGE_ATTEMPTS) {
            currentBounds = calculateBounds(text, currentX, currentY, paint, rotationDegrees)
            var collisionDetected = false
            for (existingBound in drawnTextBounds) {
                if (RectF.intersects(currentBounds, existingBound)) {
                    collisionDetected = true
                    break
                }
            }

            if (!collisionDetected) {
                drawTextAtPosition(canvas, text, currentX, currentY, paint, rotationDegrees)
                drawnTextBounds.add(currentBounds)
                return true
            }

            if (attempt < MAX_NUDGE_ATTEMPTS) {
                val dx = currentX - targetCenter.x
                val dy = currentY - targetCenter.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist > 0.01f) {
                    currentX += (dx / dist) * NUDGE_DISTANCE_STEP
                    currentY += (dy / dist) * NUDGE_DISTANCE_STEP
                } else {
                    currentY -= NUDGE_DISTANCE_STEP
                }
            }
        }
        currentBounds = calculateBounds(text, currentX, currentY, paint, rotationDegrees)
        drawTextAtPosition(canvas, text, currentX, currentY, paint, rotationDegrees)
        drawnTextBounds.add(currentBounds)
        return true
    }

    fun drawOnProtractorPlane(
        canvas: Canvas, state: ProtractorState, paints: ProtractorPaints, config: ProtractorConfig,
        aimLineStartX: Float, aimLineStartY: Float, aimLineCueX: Float, aimLineCueY: Float,
        aimLineNormDirX: Float, aimLineNormDirY: Float,
        deflectionVectorX: Float, deflectionVectorY: Float
    ) {
        planeTextDrawer.draw(canvas, state, paints, config, aimLineStartX, aimLineStartY, aimLineCueX, aimLineCueY, aimLineNormDirX, aimLineNormDirY, deflectionVectorX, deflectionVectorY)
    }

    fun drawScreenSpace(
        canvas: Canvas, state: ProtractorState, paints: ProtractorPaints, config: ProtractorConfig,
        targetGhostCenterX: Float, targetGhostCenterY: Float, targetGhostRadius: Float,
        cueGhostCenterX: Float, cueGhostCenterY: Float, cueGhostRadius: Float,
        isShotCurrentlyInvalid: Boolean,
        warningToDisplay: String?
    ) {
        screenSpaceTextDrawer.draw(
            canvas, state, paints, config,
            targetGhostCenterX, targetGhostCenterY, targetGhostRadius,
            cueGhostCenterX, cueGhostCenterY, cueGhostRadius,
            isShotCurrentlyInvalid, warningToDisplay
        )
    }
}