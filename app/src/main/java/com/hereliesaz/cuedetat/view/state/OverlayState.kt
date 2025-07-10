// app/src/main/java/com/hereliesaz/cuedetat/view/state/OverlayState.kt
package com.hereliesaz.cuedetat.view.state

import android.graphics.Matrix
import android.graphics.PointF
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import com.hereliesaz.cuedetat.data.FullOrientation
import com.hereliesaz.cuedetat.view.model.OnPlaneBall
import com.hereliesaz.cuedetat.view.model.ProtractorUnit

enum class InteractionMode {
    NONE,
    SCALING,
    ROTATING_PROTRACTOR,
    MOVING_PROTRACTOR_UNIT,
    MOVING_ACTUAL_CUE_BALL,
    AIMING_BANK_SHOT
}

data class OverlayState(
    // View dimensions
    val viewWidth: Int = 0,
    val viewHeight: Int = 0,

    // Core logical model
    val protractorUnit: ProtractorUnit = ProtractorUnit(PointF(0f, 0f), 1f, 0f),
    val onPlaneBall: OnPlaneBall? = null,

    // UI control state
    val zoomSliderPosition: Float = 0f, // Centered default
    val areHelpersVisible: Boolean = false,
    val isMoreHelpVisible: Boolean = false,
    val valuesChangedSinceReset: Boolean = false,

    // Banking mode specific state
    val isBankingMode: Boolean = false,
    val showTable: Boolean = false,
    val tableRotationDegrees: Float = 0f,
    val bankingAimTarget: PointF? = null,

    // Theme and Appearance
    val isForceLightMode: Boolean? = null,
    val luminanceAdjustment: Float = 0f,
    val showLuminanceDialog: Boolean = false,

    // Tutorial State
    val showTutorialOverlay: Boolean = false,
    val currentTutorialStep: Int = 0,

    // Sensor and perspective data
    val currentOrientation: FullOrientation = FullOrientation(0f, 0f, 0f),
    val pitchMatrix: Matrix = Matrix(),
    val railPitchMatrix: Matrix = Matrix(),
    val inversePitchMatrix: Matrix = Matrix(),
    val hasInverseMatrix: Boolean = false,

    // Derived state
    val shotLineAnchor: PointF = PointF(0f, 0f),
    val tangentDirection: Float = 1.0f, // 1.0f for one side, -1.0f for the other
    val isImpossibleShot: Boolean = false,
    val isTiltBeyondLimit: Boolean = false,
    val warningText: String? = null,

    // Theming
    val appControlColorScheme: ColorScheme = darkColorScheme(),

    // Gesture State
    val interactionMode: InteractionMode = InteractionMode.NONE,

    // State for Reset/Revert functionality
    val preResetState: OverlayState? = null,

    // Version Info
    val latestVersionName: String? = null
) {
    val pitchAngle: Float
        get() = currentOrientation.pitch
}