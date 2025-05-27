package com.hereliesaz.cuedetat.drawing.screen.elements

import android.graphics.Canvas
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.state.AppPaints
import com.hereliesaz.cuedetat.ui.theme.AppWhite // Default color

class GhostCueBallDrawer {
    fun draw(
        canvas: Canvas,
        appPaints: AppPaints,
        centerX: Float,
        centerY: Float,
        radius: Float,
        showErrorStyle: Boolean
    ) {
        if (radius <= 0.01f) return

        // Ghost Cue Ball Outline
        appPaints.ghostCueOutlinePaint.color = if (showErrorStyle) {
            appPaints.M3_COLOR_ERROR
        } else {
            AppWhite.toArgb()
        }
        canvas.drawCircle(centerX, centerY, radius, appPaints.ghostCueOutlinePaint)

        // Aiming Sight (Yellow) on Ghost Cue Ball
        // Sight color is usually fixed (e.g., yellow), not affected by error state of cue ball outline
        appPaints.ghostCueAimingSightPaint.color = appPaints.targetCirclePaint.color // Match target circle color (AppYellow)
        val sightArmLength = radius * 0.6f
        canvas.drawLine(centerX - sightArmLength, centerY, centerX + sightArmLength, centerY, appPaints.ghostCueAimingSightPaint)
        canvas.drawLine(centerX, centerY - sightArmLength, centerX, centerY + sightArmLength, appPaints.ghostCueAimingSightPaint)
        canvas.drawCircle(centerX, centerY, sightArmLength * 0.15f, appPaints.ghostCueAimingSightPaint)
    }
}