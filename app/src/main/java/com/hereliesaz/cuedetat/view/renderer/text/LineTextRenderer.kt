// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/text/LineTextRenderer.kt

package com.hereliesaz.cuedetat.view.renderer.text

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Typeface
import com.hereliesaz.cuedetat.ui.ZoomMapping
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.state.OverlayState
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

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
        draw(canvas, "Shot Guide Line", state.protractorUnit.ghostCueBallCenter, shotLineAngle, state.protractorUnit.radius * 2.5f * zoomFactor, 0f, textPaint)
    }


    fun drawAngleLabel(canvas: Canvas, center: PointF, referencePoint: PointF, angleDegrees: Float, paint: Paint, radius: Float) {
        val initialAngleRad = atan2(referencePoint.y - center.y, referencePoint.x - center.x)
        val labelAngleRad = initialAngleRad + Math.toRadians(angleDegrees.toDouble())
        val labelDistance = radius * 5.5f

        val text = "${angleDegrees.toInt()}°"
        val textX = center.x + (labelDistance * cos(labelAngleRad)).toFloat()
        val textY = center.y + (labelDistance * sin(labelAngleRad)).toFloat()

        canvas.drawText(text, textX, textY, paint)
    }

    fun drawDiamondLabel(canvas: Canvas, point: PointF, railType: RailType, state: OverlayState, paint: Paint) {
        val diamondNumberText = calculateDiamondNumber(point, railType, state) ?: return

        paint.textSize = getDynamicFontSize(60f, state.zoomSliderPosition)

        var textRotation = 0f
        when (railType) {
            RailType.TOP, RailType.BOTTOM -> textRotation = 0f
            RailType.LEFT -> textRotation = 90f
            RailType.RIGHT -> textRotation = -90f
        }

        // Keep text right-side up for the user
        val totalRotation = (state.tableRotationDegrees + textRotation) % 360
        val uprightCorrection = if (totalRotation > 90 && totalRotation < 270) 180f else 0f

        canvas.save()
        canvas.rotate(textRotation + uprightCorrection, point.x, point.y)

        // --- THE FIX: Offset the text "up" from the impact point, relative to the rotated canvas ---
        // 'ascent' is negative, so this lifts the text above the baseline. The padding adds extra space.
        val yOffset = paint.fontMetrics.ascent - 10f
        canvas.drawText(diamondNumberText, point.x, point.y + yOffset, paint)
        // --- END FIX ---

        canvas.restore()
    }


    private fun calculateDiamondNumber(point: PointF, railType: RailType, state: OverlayState): String? {
        val referenceRadius = state.onPlaneBall?.radius ?: state.protractorUnit.radius
        if (referenceRadius <= 0) return null

        val tableToBallRatioLong = state.tableSize.getTableToBallRatioLong()
        val tableToBallRatioShort = tableToBallRatioLong / state.tableSize.aspectRatio
        val tableWidth = tableToBallRatioLong * referenceRadius
        val tableHeight = tableToBallRatioShort * referenceRadius

        val halfW = tableWidth / 2f
        val halfH = tableHeight / 2f

        val logicalX = point.x - state.viewWidth / 2f
        val logicalY = point.y - state.viewHeight / 2f

        // End rails (short) have 4 diamond units, Side rails (long) have 8.
        val diamondValue = when (railType) {
            RailType.TOP -> ((logicalX + halfW) / tableWidth) * 8.0 // Left to Right: 0 -> 8
            RailType.RIGHT -> ((logicalY + halfH) / tableHeight) * 4.0 // Top to Bottom: 0 -> 4
            RailType.BOTTOM -> 8.0 - (((logicalX + halfW) / tableWidth) * 8.0) // Right to Left: 8 -> 0
            RailType.LEFT -> 4.0 - (((logicalY + halfH) / tableHeight) * 4.0) // Bottom to Top: 4 -> 0
        }

        return String.format("%.1f", diamondValue)
    }

    fun drawBankingLabels(canvas: Canvas, state: OverlayState, paints: PaintCache, typeface: Typeface?){
        // This function is deprecated as its logic has been moved to drawDiamondLabel and called from LineRenderer
    }
}