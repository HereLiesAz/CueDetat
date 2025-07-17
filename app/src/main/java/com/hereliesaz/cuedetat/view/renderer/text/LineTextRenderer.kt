// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/text/LineTextRenderer.kt

package com.hereliesaz.cuedetat.view.renderer.text

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Typeface
import com.hereliesaz.cuedetat.ui.ZoomMapping
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetat.view.state.OverlayState
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class LineTextRenderer {

    private val minFontSize = 18f
    private val maxFontSize = 70f
    enum class RailType { TOP, BOTTOM, LEFT, RIGHT }

    private fun getDynamicFontSize(baseSize: Float, zoomSliderPosition: Float): Float {
        val zoomFactor = ZoomMapping.sliderToZoom(zoomSliderPosition) / ZoomMapping.DEFAULT_ZOOM
        return (baseSize * zoomFactor).coerceIn(minFontSize, maxFontSize)
    }

    private fun draw(
        canvas: Canvas,
        text: String,
        origin: PointF,
        angleDegrees: Float,
        distanceFromOrigin: Float,
        rotationOffset: Float,
        paint: Paint
    ) {
        val textAngleRadians = Math.toRadians((angleDegrees - 90).toDouble())
        val textX = origin.x + (distanceFromOrigin * cos(textAngleRadians)).toFloat()
        val textY = origin.y + (distanceFromOrigin * sin(textAngleRadians)).toFloat()

        canvas.save()
        canvas.rotate(angleDegrees + rotationOffset, textX, textY)
        canvas.drawText(text, textX, textY, paint)
        canvas.restore()
    }

    fun drawProtractorLabels(canvas: Canvas, state: OverlayState, paints: PaintCache, typeface: Typeface?) {
        val textPaint = paints.textPaint.apply { this.typeface = typeface }
        textPaint.textSize = getDynamicFontSize(38f, state.zoomSliderPosition)
        val zoomFactor = ZoomMapping.sliderToZoom(state.zoomSliderPosition) / ZoomMapping.DEFAULT_ZOOM

        // Aiming Line Label
        draw(canvas, "Aiming Line", state.protractorUnit.center, state.protractorUnit.rotationDegrees, state.protractorUnit.radius * 5.0f * zoomFactor, 0f, textPaint)

        // Shot Guide Line Label - Anchored to Ghost Ball
        val shotLineAngle = Math.toDegrees(atan2((state.protractorUnit.ghostCueBallCenter.y - state.shotLineAnchor.y).toDouble(), (state.protractorUnit.ghostCueBallCenter.x - state.shotLineAnchor.x).toDouble()).toDouble()).toFloat()
        draw(canvas, "Shot Guide Line", state.protractorUnit.ghostCueBallCenter, shotLineAngle, state.protractorUnit.radius * 4.0f * zoomFactor, 0f, textPaint)

        // Tangent Line Labels - Drawn on both sides
        val tangentBaseAngle = shotLineAngle + 90f
        val tangentDistance = state.protractorUnit.radius * 3.0f * zoomFactor
        draw(canvas, "Tangent Line", state.protractorUnit.ghostCueBallCenter, tangentBaseAngle + (90 * state.tangentDirection), tangentDistance, 0f, textPaint)
        draw(canvas, "Tangent Line", state.protractorUnit.ghostCueBallCenter, tangentBaseAngle - (90 * state.tangentDirection), tangentDistance, 0f, textPaint)
    }


    fun drawAngleLabel(canvas: Canvas, center: PointF, referencePoint: PointF, angleDegrees: Float, paint: Paint, radius: Float) {
        val initialAngleRad = atan2(referencePoint.y - center.y, referencePoint.x - center.x)
        val labelAngleRad = initialAngleRad + Math.toRadians(angleDegrees.toDouble())
        val labelDistance = radius * 16.5f

        val text = "${angleDegrees.toInt()}°"
        val textX = center.x + (labelDistance * cos(labelAngleRad)).toFloat()
        val textY = center.y + (labelDistance * sin(labelAngleRad)).toFloat()

        canvas.drawText(text, textX, textY, paint)
    }

    fun drawDiamondLabel(
        canvas: Canvas,
        point: PointF,
        railType: RailType,
        state: OverlayState,
        paint: Paint,
        padding: Float
    ) {
        val diamondNumberText = calculateDiamondNumber(point, railType, state) ?: return

        val textHeight = paint.descent() - paint.ascent()
        val textOffset = textHeight / 2 - paint.descent()

        var textX = point.x
        var textY = point.y

        when (railType) {
            RailType.TOP -> textY += padding
            RailType.BOTTOM -> textY -= padding
            RailType.LEFT -> textX += padding
            RailType.RIGHT -> textX -= padding
        }

        // Counter-rotate the text to keep it upright relative to the screen
        canvas.save()
        canvas.translate(textX, textY)
        canvas.rotate(-state.table.rotationDegrees)
        canvas.drawText(diamondNumberText, 0f, textOffset, paint)
        canvas.restore()
    }


    private fun calculateDiamondNumber(point: PointF, railType: RailType, state: OverlayState): String? {
        val table = state.table
        if (!table.isVisible || table.logicalWidth <= 0 || table.logicalHeight <= 0) return null

        // De-rotate the impact point to align with the un-rotated table for easier calculation
        val deRotatedPoint = PointF()
        val angleRad = Math.toRadians(-state.table.rotationDegrees.toDouble())
        val cosA = cos(angleRad).toFloat()
        val sinA = sin(angleRad).toFloat()
        deRotatedPoint.x = point.x * cosA - point.y * sinA
        deRotatedPoint.y = point.x * sinA + point.y * cosA


        val halfW = table.logicalWidth / 2f
        val halfH = table.logicalHeight / 2f

        val logicalX = deRotatedPoint.x
        val logicalY = deRotatedPoint.y

        val diamondValue = when (railType) {
            RailType.TOP -> if (table.logicalWidth > 0) ((logicalX + halfW) / table.logicalWidth) * 4.0 else 0.0
            RailType.BOTTOM -> if (table.logicalWidth > 0) 4.0 - (((logicalX + halfW) / table.logicalWidth) * 4.0) else 0.0
            RailType.LEFT -> if (table.logicalHeight > 0) ((logicalY + halfH) / table.logicalHeight) * 8.0 else 0.0
            RailType.RIGHT -> if (table.logicalHeight > 0) 8.0 - (((logicalY + halfH) / table.logicalHeight) * 8.0) else 0.0
        }

        return String.format("%.1f", diamondValue)
    }

    fun drawBankingLabels(canvas: Canvas, state: OverlayState, paints: PaintCache, typeface: Typeface?){
        // This function is deprecated as its logic has been moved to LineRenderer, which has access to the necessary helper functions.
    }
}