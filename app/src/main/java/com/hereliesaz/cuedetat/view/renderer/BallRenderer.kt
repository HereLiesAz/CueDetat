package com.hereliesaz.cuedetat.view.renderer

import android.graphics.Canvas
// import android.graphics.Matrix // Not needed for this simplified approach
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
        // This canvas is ALREADY transformed by state.pitchMatrix by the OverlayRenderer

        // Draw ActualCueBall (which is the BankingBall in banking mode, or optional cue ball in protractor)
        // Its state.actualCueBall.center IS its absolute logical coordinate.
        state.actualCueBall?.let {
            canvas.drawCircle(it.center.x, it.center.y, it.radius, paints.actualCueBallBasePaint)
            canvas.drawCircle(
                it.center.x,
                it.center.y,
                it.radius / 5f,
                paints.actualCueBallCenterMarkPaint
            )
        }

        if (state.isBankingMode) return // Protractor unit not drawn in banking mode

        // --- Protractor unit drawing (only if not in banking mode) ---
        // Draw using their ABSOLUTE LOGICAL coordinates on the already pitch-transformed canvas.

        // 1. Draw Target Ball of Protractor
        // Its absolute logical center is state.protractorUnit.center
        canvas.drawCircle(
            state.protractorUnit.center.x,
            state.protractorUnit.center.y,
            state.protractorUnit.radius,
            paints.targetCirclePaint
        )
        canvas.drawCircle(
            state.protractorUnit.center.x,
            state.protractorUnit.center.y,
            state.protractorUnit.radius / 5f,
            paints.targetCenterMarkPaint
        )

        // 2. Draw Ghost Cue Ball of Protractor
        // Its absolute logical center is state.protractorUnit.protractorCueBallCenter (already has rotation applied)
        val ghostCueAbsoluteLogicalCenter = state.protractorUnit.protractorCueBallCenter
        val cuePaint =
            if (state.isImpossibleShot && !state.isBankingMode) paints.warningPaintRed1 else paints.cueCirclePaint
        canvas.drawCircle(
            ghostCueAbsoluteLogicalCenter.x,
            ghostCueAbsoluteLogicalCenter.y,
            state.protractorUnit.radius,
            cuePaint
        )
        canvas.drawCircle(
            ghostCueAbsoluteLogicalCenter.x,
            ghostCueAbsoluteLogicalCenter.y,
            state.protractorUnit.radius / 5f,
            paints.cueCenterMarkPaint
        )
    }

    fun drawScreenSpaceBalls( /* ... (no changes from previous version, assumed correct for banking ball sizing and protractor 3D ghosts) ... */
                              canvas: Canvas,
                              state: OverlayState,
                              paints: PaintCache,
                              typeface: Typeface?
    ) {
        paints.actualCueBallTextPaint.typeface = typeface

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

                if (kotlin.math.abs(state.pitchAngle - lastLoggedPitchBanking) > 0.5f || kotlin.math.abs(
                        state.tableRotationDegrees % 360 - 0f
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

            } else {
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

        if (!state.isBankingMode) {
            paints.targetBallTextPaint.typeface = typeface
            paints.cueBallTextPaint.typeface = typeface

            val targetBallLogical = state.protractorUnit
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

            val protractorGhostCueLogical = object : ILogicalBall {
                override val center = state.protractorUnit.protractorCueBallCenter
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