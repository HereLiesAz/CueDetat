package com.hereliesaz.cuedetat.protractor.drawer

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.hereliesaz.cuedetat.protractor.ProtractorConfig
import com.hereliesaz.cuedetat.protractor.ProtractorPaints
import com.hereliesaz.cuedetat.protractor.ProtractorState
import com.hereliesaz.cuedetat.protractor.drawer.element.ProtractorPlaneTextDrawer
import com.hereliesaz.cuedetat.protractor.drawer.element.ScreenSpaceTextDrawer
import kotlin.math.max

class HelperTextDrawer(
    private val viewWidthProvider: () -> Int,
    private val viewHeightProvider: () -> Int
) {
    private val drawnTextBounds = mutableListOf<RectF>()

    private val attemptTextDrawLambda: (Canvas, String, Float, Float, Paint, Float) -> Boolean =
        { canvas, text, x, y, paint, rotationDegrees ->
            attemptToDrawTextInternal(canvas, text, x, y, paint, rotationDegrees)
        }

    private val planeTextDrawer = ProtractorPlaneTextDrawer(attemptTextDrawLambda, viewWidthProvider, viewHeightProvider)
    private val screenSpaceTextDrawer = ScreenSpaceTextDrawer(attemptTextDrawLambda, viewWidthProvider, viewHeightProvider)

    fun prepareForNewFrame() {
        drawnTextBounds.clear()
    }

    private fun attemptToDrawTextInternal(canvas: Canvas, text: String, x: Float, y: Float, paint: Paint, rotationDegrees: Float = 0f): Boolean {
        val lines = text.split('\n')
        val textWidth = lines.maxOfOrNull { paint.measureText(it) } ?: 0f
        val lineHeight = paint.descent() - paint.ascent()
        val textHeight = lineHeight * lines.size + (paint.fontSpacing - lineHeight) * (lines.size -1).coerceAtLeast(0)

        val bounds: RectF
        if (rotationDegrees == 0f) {
            var top = y + paint.ascent()
            var bottom = y + paint.descent()
            if (lines.size > 1) {
                if (paint.textAlign == Paint.Align.CENTER) {
                    val blockTop = y - (textHeight / 2f) + lineHeight - paint.descent()
                    top = blockTop + paint.ascent()
                    bottom = blockTop + textHeight - lineHeight + paint.descent()
                } else {
                    top = y + paint.ascent()
                    bottom = y + textHeight - lineHeight + paint.descent()
                }
            }
            bounds = when (paint.textAlign) {
                Paint.Align.CENTER -> RectF(x - textWidth / 2, top, x + textWidth / 2, bottom)
                Paint.Align.LEFT -> RectF(x, top, x + textWidth, bottom)
                Paint.Align.RIGHT -> RectF(x - textWidth, top, x, bottom)
                else -> RectF(x,y,x,y)
            }
        } else {
            val maxDim = max(textWidth, textHeight) * 1.3f
            bounds = RectF(x - maxDim / 2, y - maxDim / 2, x + maxDim / 2, y + maxDim / 2)
        }

        for (existingBound in drawnTextBounds) {
            if (RectF.intersects(bounds, existingBound)) { return false }
        }

        if (rotationDegrees != 0f) {
            canvas.save()
            canvas.translate(x,y)
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
                currentYBaseline = y - (textHeight / 2f) + (lineHeight / 2f) - paint.ascent()
            }
            lines.forEachIndexed { index, line ->
                canvas.drawText(line, x, currentYBaseline + index * paint.fontSpacing, paint)
            }
        }
        drawnTextBounds.add(bounds)
        return true
    }

    fun drawOnProtractorPlane(
        canvas: Canvas, state: ProtractorState, paints: ProtractorPaints, config: ProtractorConfig,
        aimLineStartX: Float, aimLineStartY: Float, aimLineCueX: Float, aimLineCueY: Float,
        aimLineNormDirX: Float, aimLineNormDirY: Float,
        deflectionVectorX: Float, deflectionVectorY: Float
    ) {
        if (!state.areTextLabelsVisible) return // This guard is now primarily for non-warning plane text
        planeTextDrawer.draw(canvas, state, paints, config, aimLineStartX, aimLineStartY, aimLineCueX, aimLineCueY, aimLineNormDirX, aimLineNormDirY, deflectionVectorX, deflectionVectorY)
    }

    // Update signature here
    fun drawScreenSpace(
        canvas: Canvas, state: ProtractorState, paints: ProtractorPaints, config: ProtractorConfig,
        targetGhostCenterX: Float, targetGhostCenterY: Float, targetGhostRadius: Float,
        cueGhostCenterX: Float, cueGhostCenterY: Float, cueGhostRadius: Float,
        isShotCurrentlyInvalid: Boolean,
        warningToDisplay: String? // Added parameter
    ) {
        // The ScreenSpaceTextDrawer.draw method itself will check state.areTextLabelsVisible
        // for its non-warning components, and warningToDisplay for the warning.
        screenSpaceTextDrawer.draw(
            canvas, state, paints, config,
            targetGhostCenterX, targetGhostCenterY, targetGhostRadius,
            cueGhostCenterX, cueGhostCenterY, cueGhostRadius,
            isShotCurrentlyInvalid, warningToDisplay
        )
    }
}