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

enum class DistanceUnit {
    METRIC, IMPERIAL
}

enum class TableSize(val feet: Int, val aspectRatio: Float) {
    SIX_FT(6, 2.0f),
    SEVEN_FT(7, 2.0f),
    EIGHT_FT(8, 2.0f),
    NINE_FT(9, 2.0f),
    TEN_FT(10, 2.0f);

    fun next(): TableSize {
        val nextOrdinal = (this.ordinal + 1) % entries.size
        return entries[nextOrdinal]
    }

    // Ratio of long side to cue ball diameter (standard is 2.25 inches)
    // Example: 9ft (100") table / 2.25" ball = ~44.44
    // We will use this to scale the logical table size based on the ball's logical radius
    fun getTableToBallRatioLong(): Float {
        return when (this) {
            SIX_FT -> 33f
            SEVEN_FT -> 38f
            EIGHT_FT -> 44f
            NINE_FT -> 50f
            TEN_FT -> 55f
        }
    }
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
    val bankShotPath: List<PointF> = emptyList(),
    val pocketedBankShotPocketIndex: Int? = null,
    val tableSize: TableSize = TableSize.EIGHT_FT,
    val showTableSizeDialog: Boolean = false,

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
    val flatMatrix: Matrix = Matrix(),
    val hasInverseMatrix: Boolean = false,

    // Derived state
    val shotLineAnchor: PointF = PointF(0f, 0f),
    val tangentDirection: Float = 1.0f, // 1.0f for one side, -1.0f for the other
    val isImpossibleShot: Boolean = false,
    val isTiltBeyondLimit: Boolean = false,
    val warningText: String? = null,
    val shotGuideImpactPoint: PointF? = null,

    // Pocket aiming state
    val aimedPocketIndex: Int? = null,
    val aimingLineEndPoint: PointF? = null,


    // Theming
    val appControlColorScheme: ColorScheme = darkColorScheme(),

    // Gesture State
    val interactionMode: InteractionMode = InteractionMode.NONE,

    // State for Reset/Revert functionality
    val preResetState: OverlayState? = null,

    // Version Info
    val latestVersionName: String? = null,

    // New properties for units and calculated distance
    val distanceUnit: DistanceUnit = DistanceUnit.IMPERIAL,
    val targetBallDistance: Float = 0f
) {
    val pitchAngle: Float
        get() = currentOrientation.pitch
}