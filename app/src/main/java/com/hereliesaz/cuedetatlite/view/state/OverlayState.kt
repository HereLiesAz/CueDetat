package com.hereliesaz.cuedetatlite.view.state

import android.graphics.Matrix
import android.graphics.PointF
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import com.hereliesaz.cuedetatlite.data.FullOrientation

data class OverlayState(
    // View dimensions
    val viewWidth: Int = 0,
    val viewHeight: Int = 0,

    // Combined screen state
    val screenState: ScreenState = ScreenState(),

    // UI control state
    val zoomSliderPosition: Float = 50f, // MODIFIED: Default to halfway point
    val areHelpersVisible: Boolean = false,
    val isMoreHelpVisible: Boolean = false,
    val valuesChangedSinceReset: Boolean = false,

    // Banking mode specific state
    val isBankingMode: Boolean = false,
    val tableRotationDegrees: Float = 0f,
    val bankingAimTarget: PointF? = null,

    // Theme and Appearance FOR DRAWN ELEMENTS on ProtractorOverlayView
    val isForceLightMode: Boolean? = null,
    val luminanceAdjustment: Float = 0f,
    val showLuminanceDialog: Boolean = false,

    // Tutorial State
    val showTutorialOverlay: Boolean = false,
    val currentTutorialStep: Int = 0,

    // Sensor and perspective data
    val currentOrientation: FullOrientation = FullOrientation(0f, 0f, 0f), // Current real-time orientation
    val anchorOrientation: FullOrientation? = null, // Stored orientation when locked

    val pitchMatrix: Matrix = Matrix(),
    val railPitchMatrix: Matrix = Matrix(),
    val inversePitchMatrix: Matrix = Matrix(),
    val hasInverseMatrix: Boolean = false,

    // Theming
    val appControlColorScheme: ColorScheme = darkColorScheme(),

    // Spatial Lock State
    val isSpatiallyLocked: Boolean = false
) {
    val pitchAngle: Float
        get() = currentOrientation.pitch
}