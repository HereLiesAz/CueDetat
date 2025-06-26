// app/src/main/java/com/hereliesaz/cuedetat/view/renderer/LineRenderer.kt
package com.hereliesaz.cuedetat.view.renderer

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.PointF
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetat.view.state.OverlayState
import kotlin.math.cos
import kotlin.math.sin

object LineRenderer {

    fun drawShotLine(
        canvas: Canvas,
        state: OverlayState,
        paints: PaintCache
    ) {
        val unit = state.protractorUnit
        // The start point should be the direct screen center of the unit, ignoring perspective lift.
        val startPoint = unit.screenCenter
        val angleRad = Math.toRadians(unit.rotationDegrees.toDouble() - 90)
        val lineLength = (canvas.width + canvas.height).toFloat()
        val endPoint = PointF(
            startPoint.x + (lineLength * cos(angleRad)).toFloat(),
            startPoint.y + (lineLength * sin(angleRad)).toFloat()
        )
        canvas.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y, paints.shotLinePaint)
    }

    fun drawGhostBallLine(
        canvas: Canvas,
        state: OverlayState,
        paints: PaintCache
    ) {
        state.actualCueBall?.let { cueBall ->
            val unit = state.protractorUnit
            val cueBallRadiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(cueBall, state)
            val unitRadiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(unit, state)

            val startPoint = PointF(cueBall.screenCenter.x, cueBall.screenCenter.y + cueBallRadiusInfo.lift)
            val endPoint = PointF(unit.screenCenter.x, unit.screenCenter.y + unitRadiusInfo.lift)
            paints.ghostLinePaint.pathEffect = DashPathEffect(floatArrayOf(20f, 15f), 0f)
            canvas.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y, paints.ghostLinePaint)
        }
    }
}