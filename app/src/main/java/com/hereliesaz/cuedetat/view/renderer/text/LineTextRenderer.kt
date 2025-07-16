// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/text/LineTextRenderer.kt
package com.hereliesaz.cuedetat.view.renderer.text

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.ui.ZoomMapping
import com.hereliesaz.cuedetat.view.model.Table
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class LineTextRenderer @Inject constructor() {

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

    fun drawProtractorLabels(canvas: Canvas, state: OverlayState, typeface: Typeface?) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textAlign = Paint.Align.CENTER
            color = state.appControlColorScheme.onSurface.toArgb()
            textSize = getDynamicFontSize(38f, state.zoomSliderPosition)
        }
        val zoomFactor = ZoomMapping.sliderToZoom(state.zoomSliderPosition) / ZoomMapping.DEFAULT_ZOOM

        draw(canvas, "Aiming Line", state.protractorUnit.center, state.protractorUnit.rotationDegrees, state.protractorUnit.radius * 5.0f * zoomFactor, 0f, textPaint)

        val shotLineAngle = Math.toDegrees(atan2((state.protractorUnit.ghostCueBallCenter.y - state.shotLineAnchor.y).toDouble(), (state.protractorUnit.ghostCueBallCenter.x - state.shotLineAnchor.x).toDouble()).toDouble()).toFloat()
        draw(canvas, "Shot Guide Line", state.protractorUnit.ghostCueBallCenter, shotLineAngle, state.protractorUnit.radius * 4.0f * zoomFactor, 0f, textPaint)

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
        val diamondNumberText = calculateDiamondNumber(point, railType, state.table) ?: return

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

        val uprightCorrection = if (state.table.rotationDegrees > 90 && state.table.rotationDegrees < 270) 180f else 0f

        canvas.save()
        canvas.rotate(textRotation + uprightCorrection, textX, textY)
        canvas.drawText(diamondNumberText, textX, textY + textOffset, paint)
        canvas.restore()
    }

    fun getRailForPoint(point: PointF, state: OverlayState): RailType? {
        val geometry = state.table.geometry
        if (!geometry.isValid) return null

        val angleRad = Math.toRadians(-state.table.rotationDegrees.toDouble())
        val cosA = cos(angleRad).toFloat()
        val sinA = sin(angleRad).toFloat()

        val unrotatedPoint = PointF(
            point.x * cosA - point.y * sinA,
            point.x * sinA + point.y * cosA
        )

        val halfW = geometry.width / 2f
        val halfH = geometry.height / 2f
        val tolerance = 5f

        return when {
            abs(unrotatedPoint.y - (-halfH)) < tolerance -> RailType.TOP
            abs(unrotatedPoint.y - halfH) < tolerance -> RailType.BOTTOM
            abs(unrotatedPoint.x - (-halfW)) < tolerance -> RailType.LEFT
            abs(unrotatedPoint.x - halfW) < tolerance -> RailType.RIGHT
            else -> null
        }
    }

    private fun calculateDiamondNumber(point: PointF, railType: RailType, table: Table): String? {
        val geometry = table.geometry
        if (!geometry.isValid) return null
        val halfW = geometry.width / 2f
        val halfH = geometry.height / 2f

        val angleRad = Math.toRadians(-table.rotationDegrees.toDouble())
        val cosA = cos(angleRad).toFloat()
        val sinA = sin(angleRad).toFloat()

        val unrotatedX = point.x * cosA - point.y * sinA
        val unrotatedY = point.x * sinA + point.y * cosA

        val diamondNumber = when (railType) {
            RailType.TOP -> 2.0 + (unrotatedX / halfW) * 2.0
            RailType.BOTTOM -> 6.0 + (-unrotatedX / halfW) * 2.0
            RailType.LEFT -> 4.0 + (-unrotatedY / halfH)
            RailType.RIGHT -> 8.0 + (unrotatedY / halfH)
        }

        return String.format("%.1f", diamondNumber)
    }
}