// app/src/main/java/com/hereliesaz/cuedetat/view/renderer/BallRenderer.kt
package com.hereliesaz.cuedetat.view.renderer

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.Typeface
import android.util.Log
import com.hereliesaz.cuedetat.ui.ZoomMapping
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.model.ILogicalBall
import com.hereliesaz.cuedetat.view.renderer.text.BallTextRenderer
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetat.view.state.OverlayState
import kotlin.math.min

class BallRenderer {

    private val textRenderer = BallTextRenderer()
    private var lastLoggedPitchBanking = -9999f

    fun drawLogicalBalls(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        state.actualCueBall?.let {
            // Drawing logical balls, so use logicalPosition
            canvas.drawCircle(it.logicalPosition.x, it.logicalPosition.y, it.radius, paints.actualCueBallBasePaint)
            canvas.drawCircle(it.logicalPosition.x, it.logicalPosition.y, it.radius / 5f, paints.actualCueBallCenterMarkPaint)
        }

        if (state.isBankingMode) return

        canvas.save()
        // Logical drawing, so translate by logicalPosition of protractorUnit
        canvas.translate(state.protractorUnit.logicalPosition.x, state.protractorUnit.logicalPosition.y)
        canvas.rotate(state.protractorUnit.rotationDegrees)

        canvas.drawCircle(0f, 0f, state.protractorUnit.radius, paints.targetCirclePaint)
        canvas.drawCircle(0f, 0f, state.protractorUnit.radius / 5f, paints.targetCenterMarkPaint)

        val distanceBetweenProtractorCenters = 2 * state.protractorUnit.radius
        val unrotatedGhostCueLocalPos = PointF(0f, distanceBetweenProtractorCenters) // This is relative to protractor logical center

        val cuePaint = if (state.isImpossibleShot && !state.isBankingMode) paints.warningPaintRed1 else paints.cueCirclePaint
        canvas.drawCircle(unrotatedGhostCueLocalPos.x, unrotatedGhostCueLocalPos.y, state.protractorUnit.radius, cuePaint)
        canvas.drawCircle(unrotatedGhostCueLocalPos.x, unrotatedGhostCueLocalPos.y, state.protractorUnit.radius / 5f, paints.cueCenterMarkPaint)

        canvas.restore()
    }

    fun drawScreenSpaceBalls(
        canvas: Canvas,
        state: OverlayState,
        paints: PaintCache,
        typeface: Typeface?
    ) {
        paints.actualCueBallTextPaint.typeface = typeface

        state.actualCueBall?.let { currentActualCueBall ->
            // Drawing screen space, so project the logicalPosition through the matrix
            val screenProjectedCenter = DrawingUtils.mapPoint(currentActualCueBall.logicalPosition, state.pitchMatrix)
            val visualRadiusOnScreen: Float
            val effectiveCenterY: Float
            val labelText: String

            if (state.isBankingMode) {
                effectiveCenterY = screenProjectedCenter.y
                labelText = "Ball to Bank"

                val unitZoomScreenRadius = (min(state.viewWidth, state.viewHeight) * 0.30f / 2f)
                val currentZoomLevel = ZoomMapping.sliderToZoom(state.zoomSliderPosition)
                visualRadiusOnScreen = unitZoomScreenRadius * currentZoomLevel

                if (kotlin.math.abs(state.pitchAngle - lastLoggedPitchBanking) > 0.5f || kotlin.math.abs(state.tableRotationDegrees % 360 - 0f) > 1f) {
                    Log.i("BallRenderer_Banking", "PITCH: ${"%.1f".format(state.pitchAngle)}, " +
                            "TableRot: ${"%.1f".format(state.tableRotationDegrees)}, " +
                            "LogRadius (from state): ${"%.2f".format(currentActualCueBall.radius)}, " +
                            "VISUAL ScreenRadius: ${"%.2f".format(visualRadiusOnScreen)}, " +
                            "CurrentZoomLevel: ${"%.3f".format(currentZoomLevel)}")
                    lastLoggedPitchBanking = state.pitchAngle
                }

            } else {
                labelText = "Actual Cue Ball"
                val radiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(currentActualCueBall, state)
                visualRadiusOnScreen = radiusInfo.radius
                effectiveCenterY = screenProjectedCenter.y - radiusInfo.lift
            }

            canvas.drawCircle(screenProjectedCenter.x, effectiveCenterY, visualRadiusOnScreen, paints.actualCueBallGhostPaint)
            canvas.drawCircle(screenProjectedCenter.x, effectiveCenterY, visualRadiusOnScreen / 5f, paints.actualCueBallCenterMarkPaint)

            if (state.areHelpersVisible) {
                textRenderer.draw(canvas, paints.actualCueBallTextPaint, state.zoomSliderPosition,
                    screenProjectedCenter.x, effectiveCenterY, visualRadiusOnScreen, labelText)
            }
        }

        if (!state.isBankingMode) {
            paints.targetBallTextPaint.typeface = typeface
            paints.cueBallTextPaint.typeface = typeface

            val targetBallLogical = state.protractorUnit // This is an ILogicalBall
            val targetRadiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(targetBallLogical, state)
            val screenProjectedTargetCenter = DrawingUtils.mapPoint(targetBallLogical.logicalPosition, state.pitchMatrix) // Use logicalPosition
            val targetGhostVisualY = screenProjectedTargetCenter.y - targetRadiusInfo.lift
            canvas.drawCircle(screenProjectedTargetCenter.x, targetGhostVisualY, targetRadiusInfo.radius, paints.targetGhostBallOutlinePaint)
            if (state.areHelpersVisible) {
                textRenderer.draw(canvas, paints.targetBallTextPaint, state.zoomSliderPosition,
                    screenProjectedTargetCenter.x, targetGhostVisualY, targetRadiusInfo.radius, "Target Ball")
            }

            // Corrected: Provide logicalPosition for anonymous ILogicalBall implementation
            val protractorGhostCueLogical = object : ILogicalBall {
                override val logicalPosition = state.protractorUnit.protractorCueBallLogicalCenter // Use the new logical center for ghost cue
                override val radius = state.protractorUnit.radius
            }
            val cueRadiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(protractorGhostCueLogical, state)
            val screenProjectedGhostCueCenter = DrawingUtils.mapPoint(protractorGhostCueLogical.logicalPosition, state.pitchMatrix) // Use logicalPosition
            val cueGhostVisualY = screenProjectedGhostCueCenter.y - cueRadiusInfo.lift
            val cueGhostPaint = if (state.isImpossibleShot) paints.warningPaintRed2 else paints.ghostCueOutlinePaint
            canvas.drawCircle(screenProjectedGhostCueCenter.x, cueGhostVisualY, cueRadiusInfo.radius, cueGhostPaint)
            if (state.areHelpersVisible) {
                textRenderer.draw(canvas, paints.cueBallTextPaint, state.zoomSliderPosition,
                    screenProjectedGhostCueCenter.x, cueGhostVisualY, cueRadiusInfo.radius, "Ghost Cue Ball")
            }
        }
    }
}