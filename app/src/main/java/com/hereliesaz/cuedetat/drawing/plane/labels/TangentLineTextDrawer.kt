package com.hereliesaz.cuedetat.drawing.plane.labels

import android.graphics.Canvas
import android.graphics.PointF
import com.hereliesaz.cuedetat.config.AppConfig
import com.hereliesaz.cuedetat.state.AppPaints
import com.hereliesaz.cuedetat.state.AppState
import com.hereliesaz.cuedetat.drawing.utility.TextLayoutHelper
import com.hereliesaz.cuedetat.geometry.models.DeflectionLineParams
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class TangentLineTextDrawer(private val textLayoutHelper: TextLayoutHelper) {

    private val TEXT_STRING = "Tangent Line"

    fun draw(
        canvas: Canvas,
        appState: AppState,
        appPaints: AppPaints,
        config: AppConfig,
        deflectionParams: DeflectionLineParams
    ) {
        // Only draw if in AIMING mode and helper texts are visible
        if (!appState.isInitialized || !appState.areHelperTextsVisible || appState.currentMode != AppState.SelectionMode.AIMING) return
        if (deflectionParams.unitPerpendicularX == 0f && deflectionParams.unitPerpendicularY == 0f) return

        val paint = appPaints.tangentLineTextPaint // This is the AppPurple paint
        paint.textSize = getDynamicTextSizePPD(
            config.PLANE_LABEL_BASE_SIZE,
            appState.zoomFactor,
            config,
            true,
            config.TANGENT_LINE_TEXT_SIZE_FACTOR
        )

        val alphaDeg = appState.protractorRotationAngle
        val epsilon = 0.5f
        // isPositiveDeflectionVectorSolid means the line along (+unitPerpendicularX,Y) is drawn solid
        val isPositiveDeflectionVectorSolid = (alphaDeg > epsilon && alphaDeg < (180f - epsilon)) ||
                (alphaDeg <= epsilon || alphaDeg >= 360f - epsilon)

        val chosenVectorX: Float
        val chosenVectorY: Float

        if (isPositiveDeflectionVectorSolid) {
            chosenVectorX = deflectionParams.unitPerpendicularX
            chosenVectorY = deflectionParams.unitPerpendicularY
        } else {
            chosenVectorX = -deflectionParams.unitPerpendicularX
            chosenVectorY = -deflectionParams.unitPerpendicularY
        }

        val angleRad = atan2(chosenVectorY, chosenVectorX)
        val rotationDegrees = getReadableRotation(Math.toDegrees(angleRad.toDouble()).toFloat())
        val distance = (appState.logicalBallRadius * appState.zoomFactor) * 3.4f // Use logicalBallRadius and scale by zoomFactor

        val preferredX = appState.cueCircleCenter.x + cos(angleRad) * distance
        val preferredY = appState.cueCircleCenter.y + sin(angleRad) * distance

        textLayoutHelper.layoutAndDrawText(
            canvas, TEXT_STRING, preferredX, preferredY, paint, rotationDegrees, appState.cueCircleCenter
        )
    }

    private fun getReadableRotation(degrees: Float): Float {
        var normalizedAngle = degrees % 360f
        if (normalizedAngle < 0) normalizedAngle += 360f
        return if (normalizedAngle > 90f && normalizedAngle < 270f) degrees + 180f else degrees
    }

    private fun getDynamicTextSizePPD(
        baseSize: Float, zoomFactor: Float, config: AppConfig,
        isHelperLineLabel: Boolean = false, sizeMultiplier: Float = 1f
    ): Float {
        val effectiveZoom = if (isHelperLineLabel) zoomFactor.coerceIn(0.7f, 1.3f) else zoomFactor
        return (baseSize * sizeMultiplier * effectiveZoom.coerceIn(config.TEXT_MIN_SCALE_FACTOR, config.TEXT_MAX_SCALE_FACTOR))
            .coerceAtLeast(baseSize * sizeMultiplier * config.TEXT_MIN_SCALE_FACTOR * 0.7f)
    }
}