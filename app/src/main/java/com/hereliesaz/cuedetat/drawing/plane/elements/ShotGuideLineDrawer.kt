package com.hereliesaz.cuedetat.drawing.plane.elements

import android.graphics.Canvas
import com.hereliesaz.cuedetat.state.AppPaints
import com.hereliesaz.cuedetat.geometry.models.AimingLineLogicalCoords

class ShotGuideLineDrawer {
    fun draw(
        canvas: Canvas,
        appPaints: AppPaints,
        aimingLineCoords: AimingLineLogicalCoords
    ) {
        val startX = aimingLineCoords.startX
        val startY = aimingLineCoords.startY
        val cueX = aimingLineCoords.cueX
        val cueY = aimingLineCoords.cueY
        val endX = aimingLineCoords.endX
        val endY = aimingLineCoords.endY

        val hasNearSegment = (startX != cueX || startY != cueY)
        // val hasFarSegment = (cueX != endX || cueY != endY) // Not used for condition

        if (!hasNearSegment && aimingLineCoords.normDirX == 0f && aimingLineCoords.normDirY == 0f) {
            return
        }

        if (hasNearSegment || (aimingLineCoords.normDirX == 0f && aimingLineCoords.normDirY == 0f)) {
            canvas.drawLine(startX, startY, cueX, cueY, appPaints.shotGuideNearPaint)
        }

        if (aimingLineCoords.normDirX != 0f || aimingLineCoords.normDirY != 0f) {
            canvas.drawLine(cueX, cueY, endX, endY, appPaints.shotGuideFarPaint)
        }
    }
}