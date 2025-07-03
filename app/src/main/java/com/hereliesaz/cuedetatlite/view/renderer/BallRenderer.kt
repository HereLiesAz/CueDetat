package com.hereliesaz.cuedetatlite.view.renderer

import android.graphics.Canvas
import android.graphics.Typeface
import com.hereliesaz.cuedetatlite.view.PaintCache
import com.hereliesaz.cuedetatlite.view.renderer.text.BallTextRenderer
import com.hereliesaz.cuedetatlite.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetatlite.view.state.OverlayState

class BallRenderer(private val paints: PaintCache, private val textRenderer: BallTextRenderer) {

    fun drawLogicalBalls(canvas: Canvas, state: OverlayState) {
        state.screenState.actualCueBall?.let {
            canvas.drawCircle(it.logicalPosition.x, it.logicalPosition.y, it.radius, paints.actualCueBallBasePaint)
            canvas.drawCircle(it.logicalPosition.x, it.logicalPosition.y, it.radius / 5f, paints.actualCueBallCenterMarkPaint)
        }

        if (state.screenState.isBankingMode) return

        val protractorUnit = state.screenState.protractorUnit
        canvas.drawCircle(protractorUnit.targetBall.logicalPosition.x, protractorUnit.targetBall.logicalPosition.y, protractorUnit.targetBall.radius, paints.targetCirclePaint)
        canvas.drawCircle(protractorUnit.targetBall.logicalPosition.x, protractorUnit.targetBall.logicalPosition.y, protractorUnit.targetBall.radius / 5f, paints.targetCenterMarkPaint)

        val ghostCueBall = protractorUnit.ghostCueBall
        val cuePaint = if (state.screenState.isImpossibleShot) paints.warningPaintRed1 else paints.cueCirclePaint
        canvas.drawCircle(ghostCueBall.logicalPosition.x, ghostCueBall.logicalPosition.y, ghostCueBall.radius, cuePaint)
        canvas.drawCircle(ghostCueBall.logicalPosition.x, ghostCueBall.logicalPosition.y, ghostCueBall.radius / 5f, paints.cueCenterMarkPaint)
    }

    fun drawScreenSpaceBalls(canvas: Canvas, state: OverlayState, typeface: Typeface?) {
        state.screenState.actualCueBall?.let { currentActualCueBall ->
            val radiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(currentActualCueBall, state)
            val screenProjectedCenter = DrawingUtils.mapPoint(currentActualCueBall.logicalPosition, state.pitchMatrix)
            val visualY = if (state.screenState.isBankingMode) screenProjectedCenter.y else screenProjectedCenter.y - radiusInfo.lift
            canvas.drawCircle(screenProjectedCenter.x, visualY, radiusInfo.radius, paints.actualCueBallGhostPaint)
        }

        if (!state.screenState.isBankingMode) {
            val targetBallLogical = state.screenState.protractorUnit.targetBall
            val targetRadiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(targetBallLogical, state)
            val screenProjectedTargetCenter = DrawingUtils.mapPoint(targetBallLogical.logicalPosition, state.pitchMatrix)
            val targetGhostVisualY = screenProjectedTargetCenter.y - targetRadiusInfo.lift
            canvas.drawCircle(screenProjectedTargetCenter.x, targetGhostVisualY, targetRadiusInfo.radius, paints.targetGhostBallOutlinePaint)

            val ghostCueBall = state.screenState.protractorUnit.ghostCueBall
            val cueRadiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(ghostCueBall, state)
            val screenProjectedGhostCueCenter = DrawingUtils.mapPoint(ghostCueBall.logicalPosition, state.pitchMatrix)
            val cueGhostVisualY = screenProjectedGhostCueCenter.y - cueRadiusInfo.lift
            val cueGhostPaint = if (state.screenState.isImpossibleShot) paints.warningPaintRed2 else paints.ghostCueOutlinePaint
            canvas.drawCircle(screenProjectedGhostCueCenter.x, cueGhostVisualY, cueRadiusInfo.radius, cueGhostPaint)
        }
    }
}
