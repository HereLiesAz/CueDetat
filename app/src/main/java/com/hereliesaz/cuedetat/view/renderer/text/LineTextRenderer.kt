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

        // Aiming Line Label
        draw(canvas, "Aiming Line", state.protractorUnit.center, state.protractorUnit.rotationDegrees, state.protractorUnit.radius * 2.5f, 0f, textPaint)

        // Shot Line Label
        state.onPlaneBall?.let {
            val shotLineAngle = Math.toDegrees(atan2((state.protractorUnit.ghostCueBallCenter.y - it.center.y).toDouble(), (state.protractorUnit.ghostCueBallCenter.x - it.center.x).toDouble()).toDouble()).toFloat()
            draw(canvas, "Shot Line", it.center, shotLineAngle, 150f, 0f, textPaint)
        }
    }

    fun drawAngleLabel(canvas: Canvas, center: PointF, referencePoint: PointF, angleDegrees: Float, paint: Paint, radius: Float) {
        val initialAngleRad = atan2(referencePoint.y - center.y, referencePoint.x - center.x)
        val labelAngleRad = initialAngleRad + Math.toRadians(angleDegrees.toDouble())
        val labelDistance = radius * 3.5f

        val text = "${angleDegrees.toInt()}°"
        val textX = center.x + (labelDistance * cos(labelAngleRad)).toFloat()
        val textY = center.y + (labelDistance * sin(labelAngleRad)).toFloat()

        canvas.drawText(text, textX, textY, paint)
    }

    fun drawDiamondLabel(canvas: Canvas, point: PointF, railType: RailType, state: OverlayState, paint: Paint) {
        val diamondNumberText = calculateDiamondNumber(point, railType, state) ?: return

        paint.textSize = getDynamicFontSize(50f, state.zoomSliderPosition)
        val textOffset = 80f * (ZoomMapping.sliderToZoom(state.zoomSliderPosition) / ZoomMapping.DEFAULT_ZOOM).coerceAtLeast(0.7f) // Dynamic offset

        var textX = point.x
        var textY = point.y
        var textRotation = 0f

        when (railType) {
            RailType.TOP -> { textY -= textOffset; textRotation = 0f }
            RailType.BOTTOM -> { textY += textOffset; textRotation = 0f }
            RailType.LEFT -> { textX -= textOffset; textRotation = 90f }
            RailType.RIGHT -> { textX += textOffset; textRotation = -90f }
        }

        // Keep text right-side up for the user
        val totalRotation = (state.tableRotationDegrees + textRotation) % 360
        val uprightCorrection = if (totalRotation > 90 && totalRotation < 270) 180f else 0f

        canvas.save()
        canvas.rotate(textRotation + uprightCorrection, point.x, point.y)
        canvas.drawText(diamondNumberText, textX, textY, paint)
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

        // Short rails have 4 diamond units (0-4), Long rails have 8 (0-8).
        val diamondValue = when (railType) {
            RailType.TOP -> ((logicalX + halfW) / tableWidth) * 4.0 // 0 at left, 4 at right
            RailType.RIGHT -> ((logicalY + halfH) / tableHeight) * 8.0 // 0 at top, 8 at bottom
            RailType.BOTTOM -> 4.0 - (((logicalX + halfW) / tableWidth) * 4.0) // 4 at right, 0 at left
            RailType.LEFT -> 8.0 - (((logicalY + halfH) / tableHeight) * 8.0) // 8 at bottom, 0 at top
        }

        return String.format("%.1f", diamondValue)
    }

    fun drawBankingLabels(canvas: Canvas, state: OverlayState, paints: PaintCache, typeface: Typeface?){
        // This function is deprecated as its logic has been moved to drawDiamondLabel and called from LineRenderer
    }
}