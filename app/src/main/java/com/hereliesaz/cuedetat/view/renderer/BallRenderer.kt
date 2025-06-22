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
            canvas.drawCircle(it.center.x, it.center.y, it.radius, paints.actualCueBallBasePaint)
            canvas.drawCircle(
                it.center.x,
                it.center.y,
                it.radius / 5f,
                paints.actualCueBallCenterMarkPaint
            )
        }

        if (state.isBankingMode) return

        canvas.save()
        canvas.translate(state.protractorUnit.center.x, state.protractorUnit.center.y)
        canvas.rotate(state.protractorUnit.rotationDegrees)

        canvas.drawCircle(0f, 0f, state.protractorUnit.radius, paints.targetCirclePaint)
        canvas.drawCircle(0f, 0f, state.protractorUnit.radius / 5f, paints.targetCenterMarkPaint)

        val protractorGhostCueLocalPos = state.protractorUnit.protractorCueBallCenter.let {
            val p = PointF(it.x, it.y)
            p.offset(-state.protractorUnit.center.x, -state.protractorUnit.center.y)
            p
        }
        val cuePaint =
            if (state.isImpossibleShot && !state.isBankingMode) paints.warningPaintRed1 else paints.cueCirclePaint
        canvas.drawCircle(
            protractorGhostCueLocalPos.x,
            protractorGhostCueLocalPos.y,
            state.protractorUnit.radius,
            cuePaint
        )
        canvas.drawCircle(
            protractorGhostCueLocalPos.x,
            protractorGhostCueLocalPos.y,
            state.protractorUnit.radius / 5f,
            paints.cueCenterMarkPaint
        )

        canvas.restore()
    }

    fun drawScreenSpaceBalls(
        canvas: Canvas,
        state: OverlayState,
        paints: PaintCache,
        typeface: Typeface?
    ) {
        paints.actualCueBallTextPaint.typeface =
            typeface // Set typeface for this specific text paint

        // Draw ActualCueBall (which is the BankingBall in banking mode, or optional cue ball in protractor)
        state.actualCueBall?.let { currentActualCueBall ->
            val screenProjectedCenter =
                DrawingUtils.mapPoint(currentActualCueBall.center, state.pitchMatrix)
            val visualRadiusOnScreen: Float
            val effectiveCenterY: Float

            if (state.isBankingMode) {
                effectiveCenterY = screenProjectedCenter.y
                val logicalCenter = currentActualCueBall.center
                val logicalRadius = currentActualCueBall.radius
                val p1Logical = PointF(logicalCenter.x - logicalRadius, logicalCenter.y)
                val p2Logical = PointF(logicalCenter.x + logicalRadius, logicalCenter.y)
                val screenP1 = DrawingUtils.mapPoint(p1Logical, state.pitchMatrix)
                val screenP2 = DrawingUtils.mapPoint(p2Logical, state.pitchMatrix)
                visualRadiusOnScreen = DrawingUtils.distance(screenP1, screenP2) / 2.0f

                // Logging for banking ball size debugging
                if (kotlin.math.abs(state.pitchAngle - lastLoggedPitchBanking) > 0.5f || kotlin.math.abs(
                        state.tableRotationDegrees % 360 - /* some lastTableRotation reference if needed */ 0f
                    ) > 1f
                ) {
                    Log.i(
                        "BallRenderer_Banking", "PITCH: ${"%.1f".format(state.pitchAngle)}, " +
                                "TableRot: ${"%.1f".format(state.tableRotationDegrees)}, " +
                                "LogRadius (from state): ${"%.2f".format(currentActualCueBall.radius)}, " +
                                "VISUAL ScreenRadius: ${"%.2f".format(visualRadiusOnScreen)}, " +
                                "CurrentZoomLevel: ${"%.3f".format(ZoomMapping.sliderToZoom(state.zoomSliderPosition))}"
                    )
                    lastLoggedPitchBanking = state.pitchAngle
                }

            } else { // Protractor mode - for the optional ActualCueBall
                val radiusInfo =
                    DrawingUtils.getPerspectiveRadiusAndLift(currentActualCueBall, state)
                visualRadiusOnScreen = radiusInfo.radius
                effectiveCenterY = screenProjectedCenter.y - radiusInfo.lift
            }

            canvas.drawCircle(
                screenProjectedCenter.x,
                effectiveCenterY,
                visualRadiusOnScreen,
                paints.actualCueBallGhostPaint
            )
            canvas.drawCircle(
                screenProjectedCenter.x,
                effectiveCenterY,
                visualRadiusOnScreen / 5f,
                paints.actualCueBallCenterMarkPaint
            )

            if (state.areHelpersVisible) {
                textRenderer.draw(
                    canvas,
                    paints.actualCueBallTextPaint,
                    state.zoomSliderPosition,
                    screenProjectedCenter.x,
                    effectiveCenterY,
                    visualRadiusOnScreen,
                    "Actual Cue Ball"
                )
            }
        }

        // Draw ProtractorUnit ghosts (Target Ball & Ghost Cue Ball) only if NOT in banking mode
        if (!state.isBankingMode) {
            paints.targetBallTextPaint.typeface = typeface // Set typeface for these text paints
            paints.cueBallTextPaint.typeface = typeface

            // 1. Protractor's Target Ball (Screen Ghost)
            // Its logical center IS state.protractorUnit.center
            val targetBallLogical =
                state.protractorUnit // ProtractorUnit itself is an ILogicalBall for its target part
            val targetRadiusInfo =
                DrawingUtils.getPerspectiveRadiusAndLift(targetBallLogical, state)
            val screenProjectedTargetCenter =
                DrawingUtils.mapPoint(targetBallLogical.center, state.pitchMatrix)
            val targetGhostVisualY = screenProjectedTargetCenter.y - targetRadiusInfo.lift

            canvas.drawCircle(
                screenProjectedTargetCenter.x,
                targetGhostVisualY,
                targetRadiusInfo.radius,
                paints.targetGhostBallOutlinePaint
            )
            if (state.areHelpersVisible) {
                textRenderer.draw(
                    canvas,
                    paints.targetBallTextPaint,
                    state.zoomSliderPosition,
                    screenProjectedTargetCenter.x,
                    targetGhostVisualY,
                    targetRadiusInfo.radius,
                    "Target Ball"
                )
            }

            // 2. Protractor's Ghost Cue Ball (Screen Ghost)
            // Its logical center IS state.protractorUnit.protractorCueBallCenter
            // Its logical radius IS state.protractorUnit.radius
            val protractorGhostCueLogical = object : ILogicalBall {
                override val center =
                    state.protractorUnit.protractorCueBallCenter // Use the absolute logical center
                override val radius = state.protractorUnit.radius
            }
            val cueRadiusInfo =
                DrawingUtils.getPerspectiveRadiusAndLift(protractorGhostCueLogical, state)
            val screenProjectedGhostCueCenter =
                DrawingUtils.mapPoint(protractorGhostCueLogical.center, state.pitchMatrix)
            val cueGhostVisualY = screenProjectedGhostCueCenter.y - cueRadiusInfo.lift

            val cueGhostPaint =
                if (state.isImpossibleShot) paints.warningPaintRed2 else paints.ghostCueOutlinePaint
            canvas.drawCircle(
                screenProjectedGhostCueCenter.x,
                cueGhostVisualY,
                cueRadiusInfo.radius,
                cueGhostPaint
            )
            if (state.areHelpersVisible) {
                textRenderer.draw(
                    canvas,
                    paints.cueBallTextPaint,
                    state.zoomSliderPosition,
                    screenProjectedGhostCueCenter.x,
                    cueGhostVisualY,
                    cueRadiusInfo.radius,
                    "Ghost Cue Ball"
                )
            }
        }
    }
}