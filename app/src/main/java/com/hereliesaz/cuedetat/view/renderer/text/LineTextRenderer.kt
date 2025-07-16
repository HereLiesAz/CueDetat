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
        val diamondNumberText = calculateDiamondNumber(point, state) ?: return

        val ballRadiusOnRailPlane = DrawingUtils.getPerspectiveRadiusAndLift(
            logicalCenter = point,
            logicalRadius = state.protractorUnit.radius,
            state = state,
            matrix = state.railPitchMatrix
        ).radius

        paint.textSize = ballRadiusOnRailPlane * 2f

        val padding = 15f

        var textRotation = 0f
        var textX = point.x
        var textY = point.y
        val textHeight = paint.descent() - paint.ascent()
        val textOffset = textHeight / 2 - paint.descent()

        when (railType) {
            RailType.TOP -> textY += padding + textHeight/2
            RailType.BOTTOM -> textY -= padding
            RailType.LEFT -> { textX += padding + textHeight/2; textRotation = 90f }
            RailType.RIGHT -> { textX -= padding; textRotation = -90f }
        }

        val uprightCorrection = if (state.tableRotationDegrees > 0 && state.tableRotationDegrees < 180) 180f else 0f

        canvas.save()
        canvas.rotate(textRotation + uprightCorrection, textX, textY)
        canvas.drawText(diamondNumberText, textX, textY + textOffset, paint)
        canvas.restore()
    }


    private fun calculateDiamondNumber(point: PointF, state: OverlayState): String? {
        val referenceRadius = state.onPlaneBall?.radius ?: state.protractorUnit.radius
        if (referenceRadius <= 0) return null

        val ballRealDiameter = 2.25f
        val ballLogicalDiameter = referenceRadius * 2
        val scale = ballLogicalDiameter / ballRealDiameter
        val tableWidth = state.tableSize.longSideInches * scale
        val tableHeight = state.tableSize.shortSideInches * scale
        val halfW = tableWidth / 2f
        val halfH = tableHeight / 2f

        // The 'point' is in the rotated logical space. We must apply an inverse rotation
        // to get its position relative to the table's un-rotated coordinate system.
        val angleRad = Math.toRadians(-state.tableRotationDegrees.toDouble()) // Negative angle for inverse
        val cosA = cos(angleRad).toFloat()
        val sinA = sin(angleRad).toFloat()

        val unrotatedX = point.x * cosA - point.y * sinA
        val unrotatedY = point.x * sinA + point.y * cosA

        val tolerance = 5f // A small tolerance in logical units

        // Use the un-rotated coordinates to determine the diamond number
        val diamondNumber = when {
            abs(unrotatedY - (-halfH)) < tolerance -> 2 + (unrotatedX / halfW) * 2
            abs(unrotatedY - halfH) < tolerance -> 6 + (-unrotatedX / halfW) * 2
            abs(unrotatedX - (-halfW)) < tolerance -> 4 + (-unrotatedY / halfH)
            abs(unrotatedX - halfW) < tolerance -> 8 + (unrotatedY / halfH)
            else -> return null // Should not happen if railType is correct
        }

        return String.format("%.1f", diamondNumber)
    }


    fun drawBankingLabels(canvas: Canvas, state: OverlayState, paints: PaintCache, typeface: Typeface?){
        // This function is deprecated as its logic has been moved to LineRenderer, which has access to the necessary helper functions.
    }
}