// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/state/OverlayState.kt
package com.hereliesaz.cuedetat.view.state

import android.graphics.Matrix
import android.graphics.PointF
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.data.FullOrientation
import com.hereliesaz.cuedetat.data.VisionData
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.view.config.ui.LabelConfig
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
        isVisible = false
    ),

    // UI control state
    val zoomSliderPosition: Float = 0f,
    val worldRotationDegrees: Float = 0f,
    val areHelpersVisible: Boolean = LabelConfig.showLabelsByDefault,
    val isMoreHelpVisible: Boolean = false,
    val valuesChangedSinceReset: Boolean = false,
    val isCameraVisible: Boolean = true,
    val viewOffset: PointF = PointF(0f, 0f), // Pan state
    val orientationLock: OrientationLock = OrientationLock.AUTOMATIC,

    // Banking mode specific state
    val isBankingMode: Boolean = false,
    val bankingAimTarget: PointF? = null,
    @Transient val bankShotPath: List<PointF>? = null,
    @Transient val pocketedBankShotPocketIndex: Int? = null,
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
    @Transient val spinPaths: Map<Color, List<PointF>>? = null,
    val spinControlCenter: PointF? = null,
    val lingeringSpinOffset: PointF? = null,
    @Transient val spinPathsAlpha: Float = 1.0f,

    // Tutorial State
    val showTutorialOverlay: Boolean = false,
    val currentTutorialStep: Int = 0,

    // Sensor and perspective data
    val currentOrientation: FullOrientation = FullOrientation(0f, 0f, 0f),
    @Transient val pitchMatrix: Matrix? = null,
    @Transient val railPitchMatrix: Matrix? = null,
    @Transient val sizeCalculationMatrix: Matrix? = null,
    @Transient val inversePitchMatrix: Matrix? = null,
    @Transient val flatMatrix: Matrix? = null,
    @Transient val hasInverseMatrix: Boolean = false,

    // CV Data
    @Transient val visionData: VisionData? = null,
    @Transient val snapCandidates: List<SnapCandidate>? = null,
    val lockedHsvColor: FloatArray? = null,
    val lockedHsvStdDev: FloatArray? = null,
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
    val showCvMask: Boolean = false,
    val isTestingCvMask: Boolean = false,
    val isCalibratingColor: Boolean = false,
    val colorSamplePoint: Offset? = null,

    // Derived state
    @Transient val shotLineAnchor: PointF? = null,
    @Transient val tangentDirection: Float = 1.0f,
    @Transient val isGeometricallyImpossible: Boolean = false,
    @Transient val isStraightShot: Boolean = false,
    @Transient val isObstructed: Boolean = false,
    @Transient val isTiltBeyondLimit: Boolean = false,
    @Transient val warningText: String? = null,
    @Transient val shotGuideImpactPoint: PointF? = null,
    @Transient val aimingLineBankPath: List<PointF>? = null,
    @Transient val tangentLineBankPath: List<PointF>? = null,
    @Transient val inactiveTangentLineBankPath: List<PointF>? = null,
    @Transient val aimedPocketIndex: Int? = null,
    @Transient val tangentAimedPocketIndex: Int? = null,
    @Transient val aimingLineEndPoint: PointF? = null,

    // Theming
    @Transient val appControlColorScheme: ColorScheme? = null,

    // Gesture State
    val interactionMode: InteractionMode = InteractionMode.NONE,
    val movingObstacleBallIndex: Int? = null,
    val isMagnifierVisible: Boolean = false,
    @Transient val magnifierSourceCenter: Offset? = null,

    // State for Reset/Revert functionality
    @Transient val preResetState: OverlayState? = null,

    // Version Info
    @Transient val latestVersionName: String? = null,
    val distanceUnit: DistanceUnit = DistanceUnit.IMPERIAL,
    @Transient val targetBallDistance: Float = 0f
) {
    val pitchAngle: Float
        get() = currentOrientation.pitch

    enum class OrientationLock {
        AUTOMATIC, PORTRAIT, LANDSCAPE;

        fun next(): OrientationLock = when (this) {
            AUTOMATIC -> PORTRAIT
            PORTRAIT -> LANDSCAPE
            LANDSCAPE -> AUTOMATIC
        }
    }
}