package com.hereliesaz.cuedetat.drawing.utility

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.sqrt

class TextLayoutHelper(
    private val viewWidthProvider: () -> Int,
    private val viewHeightProvider: () -> Int
) {
    private val drawnTextBoundsThisFrame = mutableListOf<RectF>()
    private val MAX_NUDGE_ATTEMPTS = 5
    private val NUDGE_DISTANCE_STEP = 20.0f

    fun prepareForNewFrame() {
        drawnTextBoundsThisFrame.clear()
    }

    private fun calculateTextBounds(
        text: String,
        currentX: Float,
        currentY: Float,
        paint: Paint,
        rotationDegrees: Float
    ): RectF {
        val lines = text.split('\n')
        val textMetrics = paint.fontMetrics
        val singleLineHeight = textMetrics.descent - textMetrics.ascent
        val totalTextHeight = if (lines.size > 1) {
            (singleLineHeight * lines.size) + (paint.fontSpacing - singleLineHeight) * (lines.size - 1)
        } else {
            singleLineHeight
        }
        val maxLineWidth = lines.maxOfOrNull { paint.measureText(it) } ?: 0f

        if (rotationDegrees == 0f) {
            var top: Float
            var bottom: Float
            var left: Float
            var right: Float

            when (paint.textAlign) {
                Paint.Align.CENTER -> {
                    val blockTopY = currentY - totalTextHeight / 2f
                    top = blockTopY
                    bottom = blockTopY + totalTextHeight
                    left = currentX - maxLineWidth / 2f
                    right = currentX + maxLineWidth / 2f
                }
                Paint.Align.LEFT -> {
                    top = currentY + textMetrics.ascent
                    bottom = currentY + (totalTextHeight - singleLineHeight) + textMetrics.descent
                    left = currentX
                    right = currentX + maxLineWidth
                }
                Paint.Align.RIGHT -> {
                    top = currentY + textMetrics.ascent
                    bottom = currentY + (totalTextHeight - singleLineHeight) + textMetrics.descent
                    left = currentX - maxLineWidth
                    right = currentX
                }
                else -> {
                    top = currentY; bottom = currentY; left = currentX; right = currentX
                }
            }
            return RectF(left, top, right, bottom)
        } else {
            val maxDim = max(maxLineWidth, totalTextHeight) * 1.5f
            return RectF(currentX - maxDim / 2, currentY - maxDim / 2, currentX + maxDim / 2, currentY + maxDim / 2)
        }
    }

    private fun drawTextAtPosition(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        paint: Paint,
        rotationDegrees: Float
    ) {
        val lines = text.split('\n')
        val textMetrics = paint.fontMetrics
        val singleLineHeight = textMetrics.descent - textMetrics.ascent
        val totalTextHeight = if (lines.size > 1) {
            (singleLineHeight * lines.size) + (paint.fontSpacing - singleLineHeight) * (lines.size - 1)
        } else {
            singleLineHeight
        }

        if (rotationDegrees != 0f) {
            canvas.save()
            canvas.translate(x, y)
            canvas.rotate(rotationDegrees)
            var lineOffsetY = -(totalTextHeight / 2f) - textMetrics.ascent
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
            if (paint.textAlign == Paint.Align.CENTER && lines.size > 1) {
                currentYBaseline = y - (totalTextHeight / 2f) - textMetrics.ascent
            } else if (paint.textAlign == Paint.Align.CENTER && lines.size == 1) {
                currentYBaseline = y - (textMetrics.ascent + textMetrics.descent) / 2f
            }
            lines.forEachIndexed { index, line ->
                val actualDrawY = if (index == 0) currentYBaseline else currentYBaseline + (index * paint.fontSpacing)
                canvas.drawText(line, x, actualDrawY, paint)
            }
        }
    }

    fun layoutAndDrawText(
        canvas: Canvas,
        text: String,
        preferredX: Float,
        preferredY: Float,
        paint: Paint,
        rotationDegrees: Float,
        radialNudgeCenter: PointF
    ): Boolean {
        var currentX = preferredX
        var currentY = preferredY
        var currentBounds: RectF

        for (attempt in 0..MAX_NUDGE_ATTEMPTS) {
            currentBounds = calculateTextBounds(text, currentX, currentY, paint, rotationDegrees)
            var collisionDetected = false
            for (existingBound in drawnTextBoundsThisFrame) {
                if (RectF.intersects(currentBounds, existingBound)) {
                    collisionDetected = true
                    break
                }
            }

            if (!collisionDetected) {
                drawTextAtPosition(canvas, text, currentX, currentY, paint, rotationDegrees)
                drawnTextBoundsThisFrame.add(currentBounds)
                return true
            }

            if (attempt < MAX_NUDGE_ATTEMPTS) {
                val dx = currentX - radialNudgeCenter.x
                val dy = currentY - radialNudgeCenter.y
                val distFromNudgeCenter = sqrt(dx * dx + dy * dy)
                if (distFromNudgeCenter > 0.01f) {
                    currentX += (dx / distFromNudgeCenter) * NUDGE_DISTANCE_STEP
                    currentY += (dy / distFromNudgeCenter) * NUDGE_DISTANCE_STEP
                } else {
                    currentY -= NUDGE_DISTANCE_STEP
                }
            }
        }
        currentBounds = calculateTextBounds(text, currentX, currentY, paint, rotationDegrees)
        drawTextAtPosition(canvas, text, currentX, currentY, paint, rotationDegrees)
        drawnTextBoundsThisFrame.add(currentBounds)
        return true
    }
}