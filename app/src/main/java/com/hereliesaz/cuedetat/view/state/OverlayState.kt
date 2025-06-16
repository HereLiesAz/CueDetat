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
    val protractorUnit: ProtractorUnit = ProtractorUnit(PointF(0f, 0f), 1f, 0f),
    val actualCueBall: ActualCueBall? = null,

    // UI control state
    val zoomSliderPosition: Float = 100f, // REVERTED to original value
    val areHelpersVisible: Boolean = false,
    val isMoreHelpVisible: Boolean = false,
    val valuesChangedSinceReset: Boolean = false,

    // New banking mode state
    val isBankingMode: Boolean = false,

    // Sensor and perspective data
    val pitchAngle: Float = 0.0f,
    val pitchMatrix: Matrix = Matrix(),
    val railPitchMatrix: Matrix = Matrix(), // New matrix for lifted rails
    val inversePitchMatrix: Matrix = Matrix(),
    val hasInverseMatrix: Boolean = false,

    // Derived state
    val isImpossibleShot: Boolean = false,
    val warningText: String? = null,

    // Theming
    val dynamicColorScheme: ColorScheme = darkColorScheme()
)