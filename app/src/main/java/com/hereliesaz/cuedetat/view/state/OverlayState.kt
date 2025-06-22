package com.hereliesaz.cuedetat.view.state

import android.graphics.Matrix
import android.graphics.PointF
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme // For default initialization
import com.hereliesaz.cuedetat.view.model.ActualCueBall
import com.hereliesaz.cuedetat.view.model.ProtractorUnit

data class OverlayState(
    // View dimensions
    val viewWidth: Int = 0,
    val viewHeight: Int = 0,

    // Core logical model
    val protractorUnit: ProtractorUnit = ProtractorUnit(PointF(0f, 0f), 1f, 0f),
    val actualCueBall: ActualCueBall? = null, // Used for optional ball in protractor, and as banking ball in banking mode

    // UI control state
    val zoomSliderPosition: Float = 100f,
    val areHelpersVisible: Boolean = false,
    val isMoreHelpVisible: Boolean = false,
    val valuesChangedSinceReset: Boolean = false,

    // Banking mode specific state
    val isBankingMode: Boolean = false,
    val tableRotationDegrees: Float = 0f,
    val bankingAimTarget: PointF? = null,

    // Theme and Appearance FOR DRAWN ELEMENTS on ProtractorOverlayView
    val isForceLightMode: Boolean? = null, // null = system, true = light, false = dark (for PaintCache)
    val luminanceAdjustment: Float = 0f,   // Range -0.5f to 0.5f typically (for PaintCache)
    val showLuminanceDialog: Boolean = false,

    // Tutorial State
    val showTutorialOverlay: Boolean = false,
    val currentTutorialStep: Int = 0,
    // tutorialMessages will be a constant or resource, not in state directly

    // Sensor and perspective data
    val pitchAngle: Float = 0.0f,
    val pitchMatrix: Matrix = Matrix(),
    val railPitchMatrix: Matrix = Matrix(),
    val inversePitchMatrix: Matrix = Matrix(),
    val hasInverseMatrix: Boolean = false,

    // Derived state
    val isImpossibleShot: Boolean = false, // Relevant for protractor mode
    val warningText: String? = null,

    // Theming - This represents the UNMODIFIED Material Theme of the app's UI controls (sliders, menu, etc.)
    val appControlColorScheme: ColorScheme = darkColorScheme() // Base theme for UI controls, initialized to a default
)