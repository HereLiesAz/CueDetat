package com.hereliesaz.cuedetat.view.renderer

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.Typeface
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.model.ILogicalBall
import com.hereliesaz.cuedetat.view.renderer.text.BallTextRenderer
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetat.view.state.OverlayState

class BallRenderer {

    private val textRenderer = BallTextRenderer()

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

        if (state.isBankingMode) return // Don't draw the protractor unit in banking mode

        canvas.save()
        canvas.translate(state.protractorUnit.center.x, state.protractorUnit.center.y)
        canvas.rotate(state.protractorUnit.rotationDegrees)

        canvas.drawCircle(0f, 0f, state.protractorUnit.radius, paints.targetCirclePaint)
        canvas.drawCircle(0f, 0f, state.protractorUnit.radius / 5f, paints.targetCenterMarkPaint)

        val cueBallLocalCenter = state.protractorUnit.protractorCueBallCenter.let {
            val p = PointF(it.x, it.y)
            p.offset(-state.protractorUnit.center.x, -state.protractorUnit.center.y)
            p
        }
        val rotationInvertedMatrix =
            Matrix().apply { setRotate(-state.protractorUnit.rotationDegrees) }
        val cueBallRelativePosition = floatArrayOf(cueBallLocalCenter.x, cueBallLocalCenter.y)
        rotationInvertedMatrix.mapPoints(cueBallRelativePosition)
        val cuePos = PointF(cueBallRelativePosition[0], cueBallRelativePosition[1])

        val cuePaint =
            if (state.isImpossibleShot) paints.warningPaintRed1 else paints.cueCirclePaint
        canvas.drawCircle(cuePos.x, cuePos.y, state.protractorUnit.radius, cuePaint)
        canvas.drawCircle(
            cuePos.x,
            cuePos.y,
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
        paints.targetBallTextPaint.typeface = typeface
        paints.cueBallTextPaint.typeface = typeface
        paints.ghostBallTextPaint.typeface = typeface
        paints.actualCueBallTextPaint.typeface = typeface

        state.actualCueBall?.let {
            val radiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(it, state)
            val screenBasePos = DrawingUtils.mapPoint(it.center, state.pitchMatrix)
            val ghostCenterY = screenBasePos.y - radiusInfo.lift
            canvas.drawCircle(
                screenBasePos.x,
                ghostCenterY,
                radiusInfo.radius,
                paints.actualCueBallGhostPaint
            )
            if (state.areHelpersVisible) {
                textRenderer.draw(
                    canvas,
                    paints.actualCueBallTextPaint,
                    state.zoomSliderPosition,
                    screenBasePos.x,
                    ghostCenterY,
                    radiusInfo.radius,
                    "Actual Cue Ball"
                )
            }
        }

        if (state.isBankingMode) return // Don't draw ghost balls for the protractor unit

        val targetRadiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(state.protractorUnit, state)
        val cueRadiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(object : ILogicalBall {
            override val center = state.protractorUnit.protractorCueBallCenter
            override val radius = state.protractorUnit.radius
        }, state)

        val pTGC = DrawingUtils.mapPoint(state.protractorUnit.center, state.pitchMatrix)
        val pCGC =
            DrawingUtils.mapPoint(state.protractorUnit.protractorCueBallCenter, state.pitchMatrix)

        val targetGhostCenterY = pTGC.y - targetRadiusInfo.lift
        val cueGhostCenterY = pCGC.y - cueRadiusInfo.lift

        canvas.drawCircle(
            pTGC.x,
            targetGhostCenterY,
            targetRadiusInfo.radius,
            paints.targetGhostBallOutlinePaint
        )
        val cueGhostPaint =
            if (state.isImpossibleShot) paints.warningPaintRed2 else paints.ghostCueOutlinePaint
        canvas.drawCircle(pCGC.x, cueGhostCenterY, cueRadiusInfo.radius, cueGhostPaint)

        if (state.areHelpersVisible) {
            textRenderer.draw(
                canvas,
                paints.targetBallTextPaint,
                state.zoomSliderPosition,
                pTGC.x,
                targetGhostCenterY,
                targetRadiusInfo.radius,
                "Target Ball"
            )
            textRenderer.draw(
                canvas,
                paints.cueBallTextPaint,
                state.zoomSliderPosition,
                pCGC.x,
                cueGhostCenterY,
                cueRadiusInfo.radius,
                "Ghost Cue Ball"
            )
        }
    }
}