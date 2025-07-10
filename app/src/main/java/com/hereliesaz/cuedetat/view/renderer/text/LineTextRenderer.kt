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

        paint.textSize = getDynamicFontSize(24f, state.zoomSliderPosition) // A bit smaller for numbers
        val textOffset = 25f * (ZoomMapping.sliderToZoom(state.zoomSliderPosition) / ZoomMapping.DEFAULT_ZOOM).coerceAtLeast(0.7f) // Dynamic offset

        var textX = point.x
        var textY = point.y
        val rotation: Float

        when (railType) {
            RailType.TOP -> { textY -= textOffset; rotation = 0f }
            RailType.BOTTOM -> { textY += textOffset; rotation = 0f }
            RailType.LEFT -> { textX -= textOffset; rotation = 90f }
            RailType.RIGHT -> { textX += textOffset; rotation = 90f }
        }

        canvas.save()
        canvas.rotate(rotation, point.x, point.y) // Rotate around the impact point
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

        // Short rails have 4 diamond units, Long rails have 8
        val diamondValue = when (railType) {
            RailType.TOP -> ((logicalX + halfW) / tableWidth) * 4
            RailType.BOTTOM -> ((logicalX + halfW) / tableWidth) * 4
            RailType.LEFT -> ((logicalY + halfH) / tableHeight) * 8
            RailType.RIGHT -> ((logicalY + halfH) / tableHeight) * 8
        }

        return String.format("%.1f", diamondValue)
    }

    fun drawBankingLabels(canvas: Canvas, state: OverlayState, paints: PaintCache, typeface: Typeface?){
        // Banking label logic would be implemented here
    }
}