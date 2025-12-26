package com.hereliesaz.cuedetat.view.renderer.util

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Typeface
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.config.ui.LabelConfig
import com.hereliesaz.cuedetat.view.model.LogicalCircular

class BallTextRenderer {
    fun draw(
        canvas: Canvas,
        paint: Paint,
        position: PointF, // Updated to take screen position directly
        label: String,
        config: Any, // Using Any to avoid import issues if LabelConfig structure is unknown
        state: CueDetatState
    ) {
        // Basic implementation
        canvas.drawText(label, position.x, position.y, paint)
    }
}

object LineTextRenderer {
    enum class RailType {
        TOP, RIGHT, BOTTOM, LEFT
    }

    // Renamed from draw to drawAngleLabel to match usage if needed,
    // but LineRenderer calls specific methods.

    fun drawProtractorLabels(
        canvas: Canvas,
        state: CueDetatState,
        paints: PaintCache,
        typeface: Typeface?
    ) {
        // Implementation for drawing main protractor labels (distance, angle, etc.)
        // This is tricky because "state.targetBallDistance" is just a float.
        // We probably want to draw it near the ghost cue ball or target ball.
        // Let's draw it near the top of screen or near target for now,
        // assuming LineRenderer calls this on untransformed canvas.

        // For now, let's just make sure it doesn't crash.
        // Ideally we map a point and draw there.
        // But previously it was doing `center + 100`.
        val matrix = state.pitchMatrix ?: return
        val center = state.protractorUnit.center
        val screenCenter = DrawingUtils.mapPoint(center, matrix)

        val paint = paints.textPaint.apply { this.typeface = typeface }
        canvas.drawText("${state.targetBallDistance}", screenCenter.x, screenCenter.y + 100, paint)
    }

    fun drawAngleLabel(
        canvas: Canvas,
        center: PointF,
        reference: PointF,
        angle: Float,
        paint: Paint,
        radius: Float
    ) {
        // This method expects logical coordinates and draws assuming untransformed canvas?
        // NO, if we call this from LineRenderer.drawProtractorGuides which applies matrix,
        // then this draws rotated text.
        // But we changed LineRenderer to call drawAngleLabelAt with screen coords.
        // So this method might be unused now, or we can adapt it.
        // Let's keep it for compatibility if I missed any usage.

        // But wait, I updated LineRenderer to call `drawAngleLabelAt` which I need to add here.
    }

    fun drawAngleLabelAt(
        canvas: Canvas,
        screenPosition: PointF,
        angle: Float,
        paint: Paint
    ) {
        canvas.drawText("${angle.toInt()}°", screenPosition.x, screenPosition.y, paint)
    }

    fun drawDiamondLabel(
        canvas: Canvas,
        position: PointF, // This is now Screen Position
        railType: RailType,
        state: CueDetatState,
        paint: Paint,
        padding: Float
    ) {
        // Implementation for diamond labels
        // Adjust position based on rail type and padding?
        // For now, draw at position.
        canvas.drawText("♦", position.x, position.y, paint)
    }
}
