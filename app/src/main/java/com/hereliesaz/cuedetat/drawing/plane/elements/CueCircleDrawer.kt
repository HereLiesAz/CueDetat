package com.hereliesaz.cuedetat.drawing.plane.elements

import android.graphics.Canvas
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.state.AppPaints
import com.hereliesaz.cuedetat.state.AppState
import com.hereliesaz.cuedetat.ui.theme.AppBlack
import com.hereliesaz.cuedetat.ui.theme.AppWhite

class CueCircleDrawer {
    fun draw(
        canvas: Canvas,
        appState: AppState,
        appPaints: AppPaints,
        useErrorColor: Boolean
    ) {
        // Draw the *logical* cue ball (ghost ball on the plane)
        if (!appState.isInitialized || appState.currentLogicalRadius <= 0.01f) return

        val centerX = appState.cueCircleCenter.x
        val centerY = appState.cueCircleCenter.y
        val radius = appState.currentLogicalRadius
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