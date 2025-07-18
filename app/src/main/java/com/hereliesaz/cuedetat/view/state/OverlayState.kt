package com.hereliesaz.cuedetat.view.state

import android.graphics.Matrix
import android.graphics.PointF
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.data.FullOrientation
import com.hereliesaz.cuedetat.data.VisionData
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.view.model.OnPlaneBall
import com.hereliesaz.cuedetat.view.model.ProtractorUnit
import com.hereliesaz.cuedetat.view.model.Table

data class SnapCandidate(
    val detectedPoint: PointF,
    val firstSeenTimestamp: Long, // Time in millis
    val isConfirmed: Boolean = false
)

enum class CvRefinementMethod {
    HOUGH, CONTOUR;
    fun next(): CvRefinementMethod = if (this == HOUGH) CONTOUR else HOUGH
}

enum class InteractionMode {
    NONE,
    SCALING,
    ROTATING_PROTRACTOR,
    MOVING_PROTRACTOR_UNIT,
    MOVING_ACTUAL_CUE_BALL,
    AIMING_BANK_SHOT,
    MOVING_SPIN_CONTROL,
    MOVING_OBSTACLE_BALL,
    PANNING_VIEW
}

enum class DistanceUnit {
    METRIC, IMPERIAL
}

enum class TableSize(
    val feet: Int,
    val longSideInches: Float,
    val shortSideInches: Float
) {
    SIX_FT(6, 74f, 41f),
    SEVEN_FT(7, 78f, 39f),
    EIGHT_FT(8, 88f, 44f),
    NINE_FT(9, 100f, 50f),
    TEN_FT(10, 112f, 56f);

    fun next(): TableSize {
        val nextOrdinal = (this.ordinal + 1) % entries.size
        return entries[nextOrdinal]
    }
}

data class OverlayState(
    // View dimensions
    val viewWidth: Int = 0,
    val viewHeight: Int = 0,

    // Core logical model
    val protractorUnit: ProtractorUnit = ProtractorUnit(PointF(0f, 0f), LOGICAL_BALL_RADIUS, 0f),
    val onPlaneBall: OnPlaneBall? = null,
    val obstacleBalls: List<OnPlaneBall> = emptyList(),

    // Table State Object
    val table: Table = Table(
        size = TableSize.EIGHT_FT,
        rotationDegrees = 0f,
        isVisible = false
    ),

    // UI control state
    val zoomSliderPosition: Float = 0f,
    val areHelpersVisible: Boolean = false,
    val isMoreHelpVisible: Boolean = false,
    val valuesChangedSinceReset: Boolean = false,
    val isCameraVisible: Boolean = true,
    val viewOffset: PointF = PointF(0f, 0f), // Pan state

    // Banking mode specific state
    val isBankingMode: Boolean = false,
    val bankingAimTarget: PointF? = null,
    val bankShotPath: List<PointF> = emptyList(),
    val pocketedBankShotPocketIndex: Int? = null,
    val showTableSizeDialog: Boolean = false,

    // Theme and Appearance
    val isForceLightMode: Boolean? = null,
    val luminanceAdjustment: Float = 0f,
    val showLuminanceDialog: Boolean = false,
    val glowStickValue: Float = 0f,
    val showGlowStickDialog: Boolean = false,

    // Spin State
    val isSpinControlVisible: Boolean = false,
    val selectedSpinOffset: PointF? = null,
    val spinPaths: Map<Color, List<PointF>> = emptyMap(),
    val spinControlCenter: PointF? = null,
    val lingeringSpinOffset: PointF? = null,
    val spinPathsAlpha: Float = 1.0f,

    // Tutorial State
    val showTutorialOverlay: Boolean = false,
    val currentTutorialStep: Int = 0,

    // Sensor and perspective data
    val currentOrientation: FullOrientation = FullOrientation(0f, 0f, 0f),
    val pitchMatrix: Matrix = Matrix(),
    val inversePitchMatrix: Matrix = Matrix(),
    val flatMatrix: Matrix = Matrix(),
    val hasInverseMatrix: Boolean = false,

    // CV Data
    val visionData: VisionData = VisionData(),
    val snapCandidates: List<SnapCandidate> = emptyList(),
    val lockedHsvColor: FloatArray? = null,
    val showAdvancedOptionsDialog: Boolean = false,
    val cvRefinementMethod: CvRefinementMethod = CvRefinementMethod.CONTOUR,
    val useCustomModel: Boolean = false,
    val isSnappingEnabled: Boolean = true,
    val hasTargetBallBeenMoved: Boolean = false,
    val hasCueBallBeenMoved: Boolean = false,
    val houghP1: Float = 100f,
    val houghP2: Float = 20f,
    val cannyThreshold1: Float = 50f,
    val cannyThreshold2: Float = 150f,
    val cvHsvRangeMultiplier: Float = 1.5f,
    val showCvMask: Boolean = false,
    val isTestingCvMask: Boolean = false,
    val isCalibratingColor: Boolean = false,
    val colorSamplePoint: Offset? = null,

    // Derived state
    val shotLineAnchor: PointF = PointF(0f, 0f),
    val tangentDirection: Float = 1.0f,
    val isGeometricallyImpossible: Boolean = false,
    val isStraightShot: Boolean = false,
    val isObstructed: Boolean = false,
    val isTiltBeyondLimit: Boolean = false,
    val warningText: String? = null,
    val shotGuideImpactPoint: PointF? = null,
    val aimingLineBankPath: List<PointF> = emptyList(),
    val tangentLineBankPath: List<PointF> = emptyList(),
    val inactiveTangentLineBankPath: List<PointF> = emptyList(),
    val aimedPocketIndex: Int? = null,
    val tangentAimedPocketIndex: Int? = null,
    val aimingLineEndPoint: PointF? = null,

    // Theming
    val appControlColorScheme: ColorScheme = darkColorScheme(),

    // Gesture State
    val interactionMode: InteractionMode = InteractionMode.NONE,
    val movingObstacleBallIndex: Int? = null,
    val isMagnifierVisible: Boolean = false,
    val magnifierSourceCenter: Offset? = null,

    // State for Reset/Revert functionality
    val preResetState: OverlayState? = null,

    // Version Info
    val latestVersionName: String? = null,
    val distanceUnit: DistanceUnit = DistanceUnit.IMPERIAL,
    val targetBallDistance: Float = 0f
) {
    val pitchAngle: Float
        get() = currentOrientation.pitch
}