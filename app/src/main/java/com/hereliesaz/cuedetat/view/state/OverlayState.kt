// app/src/main/java/com/hereliesaz/cuedetat/view/state/OverlayState.kt
package com.hereliesaz.cuedetat.view.state

import android.graphics.Matrix
import android.graphics.PointF
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import com.hereliesaz.cuedetat.view.model.ActualCueBall
import com.hereliesaz.cuedetat.view.model.ProtractorUnit

data class OverlayState(
    // View dimensions
    val viewWidth: Int = 0,
    val viewHeight: Int = 0,

    // Core logical model
    val protractorUnit: ProtractorUnit = ProtractorUnit(PointF(0f, 0f), 1f, 0f),
    val actualCueBall: ActualCueBall? = null,

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
    val isForceLightMode: Boolean? = null,
    val luminanceAdjustment: Float = 0f,
    val showLuminanceDialog: Boolean = false,

    // Tutorial State
    val showTutorialOverlay: Boolean = false,
    val currentTutorialStep: Int = 0,

    // Sensor and perspective data
    val pitchAngle: Float = 0.0f,
    // Add new sensor data fields for full orientation when locked
    // val lockedPitch: Float? = null, // Example for future
    // val lockedRoll: Float? = null,  // Example for future
    // val lockedYaw: Float? = null,   // Example for future
    val pitchMatrix: Matrix = Matrix(),
    val railPitchMatrix: Matrix = Matrix(),
    val inversePitchMatrix: Matrix = Matrix(),
    val hasInverseMatrix: Boolean = false,

    // Derived state
    val isImpossibleShot: Boolean = false,
    val warningText: String? = null,

    // Theming
    val appControlColorScheme: ColorScheme = darkColorScheme(),

    // New Spatial Lock State
    val isSpatiallyLocked: Boolean = false
)