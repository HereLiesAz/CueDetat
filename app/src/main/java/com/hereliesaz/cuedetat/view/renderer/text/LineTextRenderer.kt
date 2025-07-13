// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/text/LineTextRenderer.kt

package com.hereliesaz.cuedetat.view.renderer.text

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Typeface
import com.hereliesaz.cuedetat.ui.ZoomMapping
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.state.OverlayState
import kotlin.math.abs
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

    fun drawDiamondLabel(canvas: Canvas, point: PointF, railType: RailType, state: OverlayState, paint: Paint) {
        val diamondNumberText = calculateDiamondNumber(point, railType, state) ?: return

        paint.textSize = getDynamicFontSize(30f, state.zoomSliderPosition) // Reduced size slightly for clarity
        val padding = 15f // Distance from the rail line

        var textRotation = 0f
        var textX = point.x
        var textY = point.y

        when (railType) {
            RailType.TOP -> { textY += padding; textRotation = 0f }
            RailType.BOTTOM -> { textY -= padding; textRotation = 0f }
            RailType.LEFT -> { textX += padding; textRotation = 90f }
            RailType.RIGHT -> { textX -= padding; textRotation = -90f }
        }

        val totalRotation = (state.tableRotationDegrees + textRotation) % 360
        val uprightCorrection = if (totalRotation > 90 && totalRotation < 270) 180f else 0f

        canvas.save()
        canvas.rotate(textRotation + uprightCorrection, textX, textY)
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

        val diamondValue = when (railType) {
            RailType.TOP -> ((logicalX + halfW) / tableWidth) * 8.0
            RailType.RIGHT -> ((logicalY + halfH) / tableHeight) * 4.0
            RailType.BOTTOM -> 8.0 - (((logicalX + halfW) / tableWidth) * 8.0)
            RailType.LEFT -> 4.0 - (((logicalY + halfH) / tableHeight) * 4.0)
        }

        return String.format("%.1f", diamondValue)
    }

    fun drawBankingLabels(canvas: Canvas, state: OverlayState, paints: PaintCache, typeface: Typeface?){
        if (state.bankShotPath.size < 2) return

        for (i in 0 until state.bankShotPath.size - 1) {
            val end = state.bankShotPath[i+1]
            getRailForPoint(end, state)?.let { railType ->
                val textPaint = paints.textPaint.apply { this.typeface = typeface }
                drawDiamondLabel(canvas, end, railType, state, textPaint)
            }
        }
    }
}