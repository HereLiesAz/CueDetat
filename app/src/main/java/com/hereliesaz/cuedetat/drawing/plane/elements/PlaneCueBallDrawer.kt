package com.hereliesaz.cuedetat.drawing.plane.elements

import android.graphics.Canvas
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.state.AppPaints
import com.hereliesaz.cuedetat.state.AppState
import com.hereliesaz.cuedetat.ui.theme.AppBlack
import com.hereliesaz.cuedetat.ui.theme.AppWhite

/**
 * Draws the *logical* cue ball (ghost ball) on the protractor plane.
 * This element is fixed relative to the target ball on the plane.
 * It does NOT draw the aiming sight, as that is part of the actual cue ball overlay.
 */
class PlaneCueBallDrawer {
    fun draw(
        canvas: Canvas,
        appState: AppState,
        appPaints: AppPaints,
        useErrorColor: Boolean
    ) {
        // Draw the *logical* cue ball (ghost ball on the plane)
        if (!appState.isInitialized || appState.logicalBallRadius <= 0.01f) return

        val centerX = appState.cueCircleCenter.x
        val centerY = appState.cueCircleCenter.y
        val radius = appState.logicalBallRadius * appState.zoomFactor // Scale by zoomFactor
        val centerMarkRadius = radius / 5f

        appPaints.cueCirclePaint.color = if (useErrorColor) {
            appPaints.M3_COLOR_ERROR
        } else {
            AppWhite.toArgb()
        }
        canvas.drawCircle(centerX, centerY, radius, appPaints.cueCirclePaint)

        val originalCenterMarkPaintColor = appPaints.centerMarkPaint.color
        appPaints.centerMarkPaint.color = if (useErrorColor) {
            AppWhite.toArgb()
        } else {
            AppBlack.toArgb()
        }
        canvas.drawCircle(centerX, centerY, centerMarkRadius, appPaints.centerMarkPaint)
        appPaints.centerMarkPaint.color = originalCenterMarkPaintColor
    }
}