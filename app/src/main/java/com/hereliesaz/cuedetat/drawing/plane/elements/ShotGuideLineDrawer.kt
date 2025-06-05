package com.hereliesaz.cuedetat.drawing.plane.elements

import android.graphics.Canvas
import android.graphics.PointF // Import PointF
import com.hereliesaz.cuedetat.state.AppPaints
import com.hereliesaz.cuedetat.geometry.models.AimingLineLogicalCoords

class ShotGuideLineDrawer {
    fun draw(
        canvas: Canvas,
        appPaints: AppPaints,
        aimingLineCoords: AimingLineLogicalCoords,
        actualCueBallScreenCenter: PointF? // New parameter for actual cue ball screen position
    ) {
        val startX: Float
        val startY: Float

        if (actualCueBallScreenCenter != null) {
            // If actual cue ball is selected, the line starts from its screen position.
            startX = actualCueBallScreenCenter.x
            startY = actualCueBallScreenCenter.y
        } else {
            // Fallback: If no actual cue ball selected, start from the logical starting point
            // from the calculator (which used to be screen bottom, now still that if no actual cue selected)
            startX = aimingLineCoords.startX
            startY = aimingLineCoords.startY
        }

        val cueX = aimingLineCoords.cueX
        val cueY = aimingLineCoords.cueY
        val endX = aimingLineCoords.endX
        val endY = aimingLineCoords.endY

        val hasNearSegment = (startX != cueX || startY != cueY)

        if (!hasNearSegment && aimingLineCoords.normDirX == 0f && aimingLineCoords.normDirY == 0f) {
            return
        }

        // Draw the near segment of the shot guide (from actual cue ball to ghost cue ball)
        if (hasNearSegment || (aimingLineCoords.normDirX == 0f && aimingLineCoords.normDirY == 0f)) {
            canvas.drawLine(startX, startY, cueX, cueY, appPaints.shotGuideNearPaint)
        }

        // Draw the far segment of the shot guide (from ghost cue ball extended)
        if (aimingLineCoords.normDirX != 0f || aimingLineCoords.normDirY != 0f) {
            canvas.drawLine(cueX, cueY, endX, endY, appPaints.shotGuideFarPaint)
        }
    }
}