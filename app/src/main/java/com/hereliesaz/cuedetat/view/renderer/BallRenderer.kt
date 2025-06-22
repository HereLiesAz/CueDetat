// app/src/main/java/com/hereliesaz/cuedetat/view/renderer/BallRenderer.kt
package com.hereliesaz.cuedetat.view.renderer

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.Typeface
import android.util.Log
import com.hereliesaz.cuedetat.ui.ZoomMapping // Import if needed for direct zoom factor
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.model.ILogicalBall
import com.hereliesaz.cuedetat.view.renderer.text.BallTextRenderer
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetat.view.state.OverlayState
import kotlin.math.min // For base radius calculation if used

class BallRenderer {

    private val textRenderer = BallTextRenderer()
    private var lastLoggedPitchBanking = -9999f

    // ... drawLogicalBalls method (no changes from previous correct version) ...
    fun drawLogicalBalls(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        state.actualCueBall?.let { // This is the single ActualCueBall, used as banking ball in banking mode
            canvas.drawCircle(it.center.x, it.center.y, it.radius, paints.actualCueBallBasePaint)
            canvas.drawCircle(
                it.center.x,
                it.center.y,
                it.radius / 5f,
                paints.actualCueBallCenterMarkPaint
            )
        }

        if (state.isBankingMode) return // Protractor unit not drawn in banking mode

        // Protractor unit drawing (only if not in banking mode)
        canvas.save()
        canvas.translate(state.protractorUnit.center.x, state.protractorUnit.center.y)
        canvas.rotate(state.protractorUnit.rotationDegrees)
        // Target Ball of Protractor
        canvas.drawCircle(0f, 0f, state.protractorUnit.radius, paints.targetCirclePaint)
        canvas.drawCircle(0f, 0f, state.protractorUnit.radius / 5f, paints.targetCenterMarkPaint)
        // Ghost Cue Ball of Protractor
        val protractorCueBallLocalCenter = state.protractorUnit.protractorCueBallCenter.let {
            val p = PointF(it.x, it.y); p.offset(
            -state.protractorUnit.center.x,
            -state.protractorUnit.center.y
        ); p
        }
        val rotationInvertedMatrix =
            Matrix().apply { setRotate(-state.protractorUnit.rotationDegrees) }
        val cueBallRelativePosition =
            floatArrayOf(protractorCueBallLocalCenter.x, protractorCueBallLocalCenter.y)
        rotationInvertedMatrix.mapPoints(cueBallRelativePosition)
        val relativeCuePos = PointF(cueBallRelativePosition[0], cueBallRelativePosition[1])
        val cuePaint =
            if (state.isImpossibleShot) paints.warningPaintRed1 else paints.cueCirclePaint
        canvas.drawCircle(relativeCuePos.x, relativeCuePos.y, state.protractorUnit.radius, cuePaint)
        canvas.drawCircle(
            relativeCuePos.x,
            relativeCuePos.y,
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
        paints.actualCueBallTextPaint.typeface = typeface

        state.actualCueBall?.let { currentActualCueBall ->
            val screenProjectedCenter =
                DrawingUtils.mapPoint(currentActualCueBall.center, state.pitchMatrix)
            val visualRadiusOnScreen: Float
            val effectiveCenterY: Float

            if (state.isBankingMode) {
                effectiveCenterY = screenProjectedCenter.y

                // For banking mode, the visual radius should primarily reflect the zoom level,
                // and then be uniformly scaled by the perspective at the ball's depth.
                // The ball's logicalRadius is already scaled by zoom.
                // We need a factor representing how 1 logical unit *at the ball's depth* scales to screen pixels.

                // 1. Get the current zoom multiplier based on the slider position.
                //    The logical radius already incorporates this.
                val logicalRadius = currentActualCueBall.radius

                // 2. Determine the perspective scale factor at the ball's center depth.
                //    Project two points very close together vertically in logical space at the ball's center,
                //    and see how their screen distance scales. This gives a local Z-depth scale.
                //    This method attempts to get a scale factor that is less sensitive to XY rotation of the measurement axis.
                val pCenterLogical = currentActualCueBall.center
                val pSlightlyAboveLogical =
                    PointF(pCenterLogical.x, pCenterLogical.y - 1f) // 1 logical unit offset

                val pCenterScreen = screenProjectedCenter // Already have this
                val pSlightlyAboveScreen =
                    DrawingUtils.mapPoint(pSlightlyAboveLogical, state.pitchMatrix)

                val logicalUnitDistance = 1f
                var screenDistanceForLogicalUnit =
                    DrawingUtils.distance(pCenterScreen, pSlightlyAboveScreen)

                if (screenDistanceForLogicalUnit < 0.001f) { // Avoid division by zero or extreme scaling
                    // Fallback: project a horizontal radius if vertical is too small (e.g. extreme side view)
                    val pSlightlyRightLogical = PointF(pCenterLogical.x + 1f, pCenterLogical.y)
                    val pSlightlyRightScreen =
                        DrawingUtils.mapPoint(pSlightlyRightLogical, state.pitchMatrix)
                    screenDistanceForLogicalUnit =
                        DrawingUtils.distance(pCenterScreen, pSlightlyRightScreen)
                    if (screenDistanceForLogicalUnit < 0.001f) {
                        // If still too small, use a default based on initial zoom setup (very rough)
                        val baseScreenRadiusAtDefaultZoom =
                            (min(state.viewWidth, state.viewHeight) * 0.30f / 2f)
                        val defaultLogicalRadiusAtDefaultZoom =
                            baseScreenRadiusAtDefaultZoom * ZoomMapping.DEFAULT_ZOOM
                        if (defaultLogicalRadiusAtDefaultZoom > 0) {
                            screenDistanceForLogicalUnit =
                                (baseScreenRadiusAtDefaultZoom / defaultLogicalRadiusAtDefaultZoom)
                        } else {
                            screenDistanceForLogicalUnit = 1f
                        }
                    }
                }

                val perspectiveScaleAtBallDepth = screenDistanceForLogicalUnit / logicalUnitDistance
                visualRadiusOnScreen = logicalRadius * perspectiveScaleAtBallDepth


                if (kotlin.math.abs(state.pitchAngle - lastLoggedPitchBanking) > 0.5f || kotlin.math.abs(
                        state.tableRotationDegrees % 360 - /* some lastTableRotation */ 0f
                    ) > 1f
                ) {
                    Log.i(
                        "BallRenderer_Banking", "Pitch: ${"%.1f".format(state.pitchAngle)}, " +
                                "TableRot: ${"%.1f".format(state.tableRotationDegrees)}, " +
                                "LogRadius: ${"%.2f".format(logicalRadius)}, " +
                                "PerspScale: ${"%.3f".format(perspectiveScaleAtBallDepth)}, " +
                                "VISUAL ScreenRadius: ${"%.2f".format(visualRadiusOnScreen)}"
                    )
                    lastLoggedPitchBanking = state.pitchAngle
                }

            } else { // Protractor mode
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

        // Draw ProtractorUnit ghosts only if NOT in banking mode
        if (!state.isBankingMode) {
            // ... (rest of protractor ghost ball drawing, no changes needed here from previous version)
            paints.targetBallTextPaint.typeface = typeface
            paints.cueBallTextPaint.typeface = typeface

            val targetRadiusInfo =
                DrawingUtils.getPerspectiveRadiusAndLift(state.protractorUnit, state)
            val pTGC = DrawingUtils.mapPoint(state.protractorUnit.center, state.pitchMatrix)
            val targetGhostCenterY = pTGC.y - targetRadiusInfo.lift
            canvas.drawCircle(
                pTGC.x,
                targetGhostCenterY,
                targetRadiusInfo.radius,
                paints.targetGhostBallOutlinePaint
            )
            if (state.areHelpersVisible) {
                textRenderer.draw(
                    canvas, paints.targetBallTextPaint, state.zoomSliderPosition,
                    pTGC.x, targetGhostCenterY, targetRadiusInfo.radius, "Target Ball"
                )
            }

            val protractorCueBall = object : ILogicalBall {
                override val center = state.protractorUnit.protractorCueBallCenter
                override val radius = state.protractorUnit.radius
            }
            val cueRadiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(protractorCueBall, state)
            val pCGC = DrawingUtils.mapPoint(
                state.protractorUnit.protractorCueBallCenter,
                state.pitchMatrix
            )
            val cueGhostCenterY = pCGC.y - cueRadiusInfo.lift
            val cueGhostPaint =
                if (state.isImpossibleShot) paints.warningPaintRed2 else paints.ghostCueOutlinePaint
            canvas.drawCircle(pCGC.x, cueGhostCenterY, cueRadiusInfo.radius, cueGhostPaint)
            if (state.areHelpersVisible) {
                textRenderer.draw(
                    canvas, paints.cueBallTextPaint, state.zoomSliderPosition,
                    pCGC.x, cueGhostCenterY, cueRadiusInfo.radius, "Ghost Cue Ball"
                )
            }
        }
    }
}