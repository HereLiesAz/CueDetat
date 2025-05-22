package com.hereliesaz.cuedetat.protractor.drawer // Corrected package

import android.graphics.Canvas
import androidx.compose.ui.graphics.toArgb
// ... other imports ...
import com.hereliesaz.cuedetat.protractor.ProtractorPaints
import com.hereliesaz.cuedetat.protractor.ProtractorState
import com.hereliesaz.cuedetat.ui.theme.AppWhite
import com.hereliesaz.cuedetat.ui.theme.AppYellow


class GhostBallDrawer {
    fun draw(
        canvas: Canvas,
        paints: ProtractorPaints,
        // Screen-space coordinates and radii, already pitch-adjusted
        targetGhostCenterX: Float, targetGhostCenterY: Float, targetGhostRadius: Float,
        cueGhostCenterX: Float, cueGhostCenterY: Float, cueGhostRadius: Float,
        showWarningStyleForGhosts: Boolean
    ) {
        // Ghost Target Ball (Yellow Outline)
        paints.targetGhostBallOutlinePaint.color = AppYellow.toArgb() // Ensure color
        canvas.drawCircle(targetGhostCenterX, targetGhostCenterY, targetGhostRadius, paints.targetGhostBallOutlinePaint)

        // Ghost Cue Ball (White or Red Outline)
        paints.ghostCueOutlinePaint.color = if (showWarningStyleForGhosts) paints.M3_COLOR_ERROR else AppWhite.toArgb()
        canvas.drawCircle(cueGhostCenterX, cueGhostCenterY, cueGhostRadius, paints.ghostCueOutlinePaint)

        // Aiming Sight (Yellow) on Ghost Cue Ball
        val sightArmLength = cueGhostRadius * 0.6f
        canvas.drawLine(cueGhostCenterX - sightArmLength, cueGhostCenterY, cueGhostCenterX + sightArmLength, cueGhostCenterY, paints.aimingSightPaint)
        canvas.drawLine(cueGhostCenterX, cueGhostCenterY - sightArmLength, cueGhostCenterX, cueGhostCenterY + sightArmLength, paints.aimingSightPaint)
        canvas.drawCircle(cueGhostCenterX, cueGhostCenterY, sightArmLength * 0.15f, paints.aimingSightPaint)
    }
}