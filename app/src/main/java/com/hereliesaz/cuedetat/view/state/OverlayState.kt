// hereliesaz/cued8at/CueD8at-66142b655f7e247d83b8004a442ad41e04dd6348/app/src/main/java/com/hereliesaz/cuedetat/view/state/OverlayState.kt
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
    // ProtractorUnit is primarily for non-banking mode.
    val protractorUnit: ProtractorUnit = ProtractorUnit(PointF(0f, 0f), 1f, 0f),
    // ActualCueBall is the user-draggable cue ball.
    // In protractor mode, it's optional (for jump shot / advanced aiming).
    // In banking mode, it's the primary "banking ball" and is always present.
    val actualCueBall: ActualCueBall? = null,

    // UI control state
    val zoomSliderPosition: Float = 100f,
    val areHelpersVisible: Boolean = false,
    val isMoreHelpVisible: Boolean = false,
    val valuesChangedSinceReset: Boolean = false,

    // Banking mode specific state
    val isBankingMode: Boolean = false,
    val tableRotationDegrees: Float = 0f,
    val bankingAimTarget: PointF? = null, // Logical target for banking mode aiming

    // Sensor and perspective data
    val pitchAngle: Float = 0.0f,
    val pitchMatrix: Matrix = Matrix(),       // Primary matrix for protractor or table plane
    val railPitchMatrix: Matrix = Matrix(),   // For lifted rails in banking mode
    val inversePitchMatrix: Matrix = Matrix(),
    val hasInverseMatrix: Boolean = false,

    // Derived state
    val isImpossibleShot: Boolean = false, // Relevant for protractor mode
    val warningText: String? = null,

    // Theming
    val dynamicColorScheme: ColorScheme = darkColorScheme()
)