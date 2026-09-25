// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/text/LineTextRenderer.kt

package com.hereliesaz.cuedetat.view.renderer.text

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.ui.ZoomMapping
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.config.ui.LabelConfig
import com.hereliesaz.cuedetat.view.config.ui.LabelProperties
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class LineTextRenderer {

    private val minFontSize = 18f
    private val maxFontSize = 70f
    enum class RailType { TOP, BOTTOM, LEFT, RIGHT }

    private fun getDynamicFontSize(baseSize: Float, state: CueDetatState): Float {
        val (minZoom, maxZoom) = ZoomMapping.getZoomRange(
            state.experienceMode,
            state.isBeginnerViewLocked
        )
        val zoomFactor = ZoomMapping.sliderToZoom(
            state.zoomSliderPosition,
            minZoom,
            maxZoom
        ) / ZoomMapping.DEFAULT_ZOOM
        return (baseSize * zoomFactor).coerceIn(minFontSize, maxFontSize)
    }

    /**
     * Draws [text] along a line on the table: [distanceFromOrigin] logical units from [origin]
     * in the logical direction [directionRadians] (atan2 convention). Position and angle are
     * both taken from the projected line, so the label sits on the line as drawn and turns
     * with it under pitch, table rotation and lens distortion; it is flipped when needed so
     * it never reads upside down.
     */
    private fun draw(
        canvas: Canvas,
        text: String,
        origin: PointF,
        directionRadians: Double,
        distanceFromOrigin: Float,
        config: LabelProperties,
        paint: Paint,
        state: CueDetatState,
        matrix: Matrix,
        camArray: DoubleArray?,
        distArray: DoubleArray?
    ) {
        val dirX = cos(directionRadians).toFloat()
        val dirY = sin(directionRadians).toFloat()

        val logicalX = origin.x + distanceFromOrigin * dirX
        val logicalY = origin.y + distanceFromOrigin * dirY
        val step = state.protractorUnit.radius
        val screen = project(logicalX, logicalY, matrix, camArray, distArray) ?: return
        val ahead = project(logicalX + dirX * step, logicalY + dirY * step, matrix, camArray, distArray) ?: return

        var angle = Math.toDegrees(atan2((ahead.y - screen.y).toDouble(), (ahead.x - screen.x).toDouble())).toFloat()
        if (angle > 90f) angle -= 180f else if (angle < -90f) angle += 180f

        val finalX = screen.x + config.xOffset
        val finalY = screen.y + config.yOffset

        canvas.save()
        canvas.rotate(angle + config.rotationDegrees, finalX, finalY)
        paint.color = config.color.copy(alpha = config.opacity).toArgb()
        // Baseline just above the line, so the text rides on it rather than across it.
        canvas.drawText(text, finalX, finalY - paint.descent() - LINE_GAP, paint)
        canvas.restore()
    }

    /** Logical point → screen, the way [DrawingUtils.buildDistortedLinePath] draws lines; null behind the camera. */
    private fun project(x: Float, y: Float, matrix: Matrix, camArray: DoubleArray?, distArray: DoubleArray?): PointF? {
        val v = FloatArray(9).also { matrix.getValues(it) }
        if (v[Matrix.MPERSP_0] * x + v[Matrix.MPERSP_1] * y + v[Matrix.MPERSP_2] <= 0f) return null
        val pts = floatArrayOf(x, y)
        matrix.mapPoints(pts)
        return if (camArray != null && distArray != null && camArray.size == 9) {
            DrawingUtils.applyBarrelDistortion(pts[0], pts[1], camArray, distArray)
        } else {
            PointF(pts[0], pts[1])
        }
    }

    fun drawProtractorLabels(
        canvas: Canvas,
        state: CueDetatState,
        paints: PaintCache,
        typeface: Typeface?,
        matrix: Matrix,
        camArray: DoubleArray?,
        distArray: DoubleArray?
    ) {
        val textPaint = paints.textPaint.apply { this.typeface = typeface }
        textPaint.textSize = getDynamicFontSize(38f, state)
        val (minZoom, maxZoom) = ZoomMapping.getZoomRange(
            state.experienceMode,
            state.isBeginnerViewLocked
        )
        val zoomFactor = ZoomMapping.sliderToZoom(
            state.zoomSliderPosition,
            minZoom,
            maxZoom
        ) / ZoomMapping.DEFAULT_ZOOM

        val ghost = state.protractorUnit.ghostCueBallCenter
        val target = state.protractorUnit.center
        val aimDirection = atan2((target.y - ghost.y).toDouble(), (target.x - ghost.x).toDouble())

        // Aiming Line Label - beyond the target ball, along the aiming line
        draw(
            canvas,
            "Aiming Line",
            target,
            aimDirection,
            state.protractorUnit.radius * 5.0f * zoomFactor,
            LabelConfig.aimingLine,
            textPaint,
            state, matrix, camArray, distArray
        )

        // Shot Guide Line Label - between the ghost ball and the cue ball, along the shot line
        state.shotLineAnchor?.let { anchor ->
            draw(
                canvas,
                "Shot Guide Line",
                ghost,
                atan2((anchor.y - ghost.y).toDouble(), (anchor.x - ghost.x).toDouble()),
                state.protractorUnit.radius * 4.0f * zoomFactor,
                LabelConfig.shotGuideLine,
                textPaint,
                state, matrix, camArray, distArray
            )
        }

        // Tangent Line Labels - both sides of the ghost ball, along the tangent
        val tangentDistance = state.protractorUnit.radius * 3.0f * zoomFactor
        for (side in listOf(1.0, -1.0)) {
            draw(
                canvas,
                "Tangent Line",
                ghost,
                aimDirection + side * Math.PI / 2,
                tangentDistance,
                LabelConfig.tangentLine,
                textPaint,
                state, matrix, camArray, distArray
            )
        }
    }

    fun drawAngleLabel(canvas: Canvas, center: PointF, referencePoint: PointF, angleDegrees: Float, paint: Paint, radius: Float) {
        val config = LabelConfig.angleGuide
        if (!config.isPersistentlyVisible) return // Assuming this is a helper label

        val initialAngleRad = atan2(referencePoint.y - center.y, referencePoint.x - center.x)
        val labelAngleRad = initialAngleRad + Math.toRadians(angleDegrees.toDouble())
        val labelDistance = radius * 16.5f

        val text = "${angleDegrees.toInt()}°"
        val textX = center.x + (labelDistance * cos(labelAngleRad)).toFloat()
        val textY = center.y + (labelDistance * sin(labelAngleRad)).toFloat()

        paint.color = config.color.copy(alpha = config.opacity).toArgb()
        canvas.drawText(text, textX, textY, paint)
    }

    fun drawDiamondLabel(
        canvas: Canvas,
        point: PointF,
        railType: RailType,
        state: CueDetatState,
        paint: Paint,
        padding: Float
    ) {
        val config = LabelConfig.diamondSystem
        if (!state.areHelpersVisible && !config.isPersistentlyVisible) return

        val diamondNumberText = calculateDiamondNumber(point, railType, state) ?: return

        // --- HERESY CORRECTED: Work in the logical coordinate space of the pre-transformed canvas. ---
        // The canvas is already transformed by railPitchMatrix. We draw at the logical point.
        // We must convert screen-space padding to logical-space padding.
        val (minZoom, maxZoom) = ZoomMapping.getZoomRange(
            state.experienceMode,
            state.isBeginnerViewLocked
        )
        val zoomFactor = ZoomMapping.sliderToZoom(state.zoomSliderPosition, minZoom, maxZoom)
        val logicalPadding = if (zoomFactor > 0) padding / zoomFactor else padding

        val textHeight = paint.descent() - paint.ascent()
        val textOffset = textHeight / 2 - paint.descent()

        var textX = point.x
        var textY = point.y

        when (railType) {
            RailType.TOP -> textY += logicalPadding
            RailType.BOTTOM -> textY -= logicalPadding
            RailType.LEFT -> textX += logicalPadding
            RailType.RIGHT -> textX -= logicalPadding
        }

        // Apply config offset, also scaled to logical space
        textX += if (zoomFactor > 0) config.xOffset / zoomFactor else config.xOffset
        textY += if (zoomFactor > 0) config.yOffset / zoomFactor else config.yOffset

        // Counter-rotate the text relative to the world rotation to keep it screen-aligned
        canvas.save()
        canvas.translate(textX, textY)
        canvas.rotate(-state.worldRotationDegrees)
        paint.color = config.color.copy(alpha = config.opacity).toArgb()
        canvas.drawText(diamondNumberText, 0f, textOffset, paint)
        canvas.restore()
        // --- END CORRECTION ---
    }


    private fun calculateDiamondNumber(
        point: PointF,
        railType: RailType,
        state: CueDetatState
    ): String? {
        val table = state.table
        if (!table.isVisible || table.logicalWidth <= 0 || table.logicalHeight <= 0) return null

        // De-rotate the impact point to align with the un-rotated table for easier calculation
        val deRotatedPoint = PointF()
        val angleRad = Math.toRadians(-state.worldRotationDegrees.toDouble())
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

    private companion object {
        /** Screen gap in pixels between a line and the label riding on it. */
        const val LINE_GAP = 12f
    }
}
