package com.hereliesaz.cuedetat.domain

import android.graphics.Matrix
import android.graphics.PointF
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.data.FullOrientation
import com.hereliesaz.cuedetat.data.VisionData
import com.hereliesaz.cuedetat.ui.hatemode.HaterState
import com.hereliesaz.cuedetat.ui.hatemode.HaterViewModel
import com.hereliesaz.cuedetat.view.config.ui.LabelConfig
import com.hereliesaz.cuedetat.view.model.OnPlaneBall
import com.hereliesaz.cuedetat.view.model.ProtractorUnit
import com.hereliesaz.cuedetat.view.model.Table
import com.hereliesaz.cuedetat.view.state.CvRefinementMethod
import com.hereliesaz.cuedetat.view.state.DistanceUnit
import com.hereliesaz.cuedetat.view.state.InteractionMode
import com.hereliesaz.cuedetat.view.state.SnapCandidate
import com.hereliesaz.cuedetat.view.state.TableSize
import com.hereliesaz.cuedetat.view.state.TutorialHighlightElement
import org.opencv.core.Mat

// Represents the different modes the application can be in.
enum class ExperienceMode {
    EXPERT, BEGINNER, HATER;

    fun next(): ExperienceMode {
        val nextOrdinal = (this.ordinal + 1) % values().size
        return values()[nextOrdinal]
    }
}

// Defines the state of any overlay that might be shown over the main content.
sealed class OverlayState {
    // No overlay is visible.
    object None : OverlayState()

    // The experience mode selection overlay is visible.
    data class ExperienceModeSelection(val currentMode: ExperienceMode) : OverlayState()
}

// The single source of truth for the application's entire state.
data class CueDetatState(
    // High-level state
    val experienceMode: ExperienceMode? = null,
    val overlay: OverlayState = OverlayState.None,
    val haterState: HaterState = HaterState(),

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
    val orientationLock: OrientationLock = OrientationLock.PORTRAIT,
    @Transient val pendingOrientationLock: OrientationLock? = null,
    val isBeginnerViewLocked: Boolean = false,

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
    @Transient val tutorialHighlight: TutorialHighlightElement? = TutorialHighlightElement.NONE,

    // Sensor and perspective data
    val currentOrientation: FullOrientation = FullOrientation(0f, 0f, 0f),
    @Transient val pitchMatrix: Matrix? = null,
    @Transient val railPitchMatrix: Matrix? = null,
    @Transient val sizeCalculationMatrix: Matrix? = null,
    @Transient val inversePitchMatrix: Matrix? = null,
    @Transient val flatMatrix: Matrix? = null,
    @Transient val logicalPlaneMatrix: Matrix? = null,
    @Transient val hasInverseMatrix: Boolean = false,

    // CV Data
    @Transient val visionData: VisionData? = null,
    @Transient val snapCandidates: List<SnapCandidate>? = null,
    val lockedHsvColor: FloatArray? = null,
    val lockedHsvStdDev: FloatArray? = null,
    val showAdvancedOptionsDialog: Boolean = false,
    val showCalibrationScreen: Boolean = false,
    val showQuickAlignScreen: Boolean = false,
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
    @Transient val cameraMatrix: Mat? = null,
    @Transient val distCoeffs: Mat? = null,

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
    val isWorldLocked: Boolean = false,

    // State for Reset/Revert functionality
    @Transient val preResetState: CueDetatState? = null,

    // Version Info
    @Transient val latestVersionName: String? = null,
    val distanceUnit: DistanceUnit = DistanceUnit.IMPERIAL,
    @Transient val targetBallDistance: Float = 0f,
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

const val LOGICAL_BALL_RADIUS = 25f

// A sealed class for all possible actions/events that can be dispatched to the reducer.
sealed class CueDetatAction {
    // High-level Actions
    object ToggleExperienceMode : CueDetatAction()
    data class ApplyPendingExperienceMode(val mode: ExperienceMode) : CueDetatAction()
    data class HaterAction(val action: HaterViewModel.Action) : CueDetatAction()

    // UI-Originated Events
    data class ScreenGestureStarted(val position: PointF) : CueDetatAction()
    data class Drag(val previousPosition: PointF, val currentPosition: PointF) : CueDetatAction()
    object GestureEnded : CueDetatAction()
    data class SizeChanged(val width: Int, val height: Int) : CueDetatAction()
    data class ZoomScaleChanged(val scaleFactor: Float) : CueDetatAction()
    data class TableRotationApplied(val degrees: Float) : CueDetatAction()
    data class ZoomSliderChanged(val position: Float) : CueDetatAction()
    data class PanView(val delta: PointF) : CueDetatAction()

    // Spin Control Events
    object ToggleSpinControl : CueDetatAction()
    data class SpinApplied(val offset: PointF) : CueDetatAction()
    object SpinSelectionEnded : CueDetatAction()
    data class DragSpinControl(val delta: PointF) : CueDetatAction()
    object ClearSpinState : CueDetatAction()

    // Logical Events (dispatched by ViewModel)
    internal data class LogicalGestureStarted(val logicalPoint: PointF, val screenOffset: Offset) :
        CueDetatAction()

    internal data class LogicalDragApplied(
        val previousLogicalPoint: PointF,
        val currentLogicalPoint: PointF,
        val screenDelta: Offset
    ) : CueDetatAction()

    // Direct State Change Events
    data class TableRotationChanged(val degrees: Float) : CueDetatAction()
    data class FullOrientationChanged(val orientation: FullOrientation) : CueDetatAction()
    data class ThemeChanged(val scheme: ColorScheme) : CueDetatAction()
    object Reset : CueDetatAction()
    object ToggleHelp : CueDetatAction()
    object ToggleMoreHelp : CueDetatAction()
    object ToggleBankingMode : CueDetatAction()
    object CycleTableSize : CueDetatAction()
    data class SetTableSize(val size: TableSize) : CueDetatAction()
    object ToggleTableSizeDialog : CueDetatAction()
    object ToggleForceTheme : CueDetatAction()
    object ToggleCamera : CueDetatAction()
    object ToggleLuminanceDialog : CueDetatAction()
    data class AdjustLuminance(val adjustment: Float) : CueDetatAction()
    object ToggleDistanceUnit : CueDetatAction()
    object ToggleGlowStickDialog : CueDetatAction()
    data class AdjustGlow(val value: Float) : CueDetatAction()
    data class SetWarning(val warning: String?) : CueDetatAction()
    object ToggleOrientationLock : CueDetatAction()
    object ApplyPendingOrientationLock : CueDetatAction()
    data class OrientationChanged(val orientationLock: CueDetatState.OrientationLock) :
        CueDetatAction()

    data class SetExperienceMode(val mode: ExperienceMode) : CueDetatAction()
    object UnlockBeginnerView : CueDetatAction()
    object LockBeginnerView : CueDetatAction()

    // Obstacle Events
    object AddObstacleBall : CueDetatAction()

    // CV Events
    data class CvDataUpdated(val data: VisionData) : CueDetatAction()
    object LockOrUnlockColor : CueDetatAction()
    data class LockColor(val hsvMean: FloatArray, val hsvStdDev: FloatArray) : CueDetatAction()
    object ClearSamplePoint : CueDetatAction()
    object ToggleAdvancedOptionsDialog : CueDetatAction()
    object ToggleCalibrationScreen : CueDetatAction()
    object ToggleQuickAlignScreen : CueDetatAction()
    data class ApplyQuickAlign(val translation: Offset, val rotation: Float, val scale: Float) :
        CueDetatAction()

    object ToggleCvRefinementMethod : CueDetatAction()
    data class UpdateHoughP1(val value: Float) : CueDetatAction()
    data class UpdateHoughP2(val value: Float) : CueDetatAction()
    data class UpdateCannyT1(val value: Float) : CueDetatAction()
    data class UpdateCannyT2(val value: Float) : CueDetatAction()
    object ToggleCvModel : CueDetatAction()
    object ToggleSnapping : CueDetatAction()
    object ToggleCvMask : CueDetatAction()
    object EnterCvMaskTestMode : CueDetatAction()
    object ExitCvMaskTestMode : CueDetatAction()
    object EnterCalibrationMode : CueDetatAction()
    data class SampleColorAt(val screenPosition: Offset) : CueDetatAction()

    // Tutorial Events
    object StartTutorial : CueDetatAction()
    object NextTutorialStep : CueDetatAction()
    object EndTutorial : CueDetatAction()

    // Meta/Single Events
    object CheckForUpdate : CueDetatAction()
    object ViewArt : CueDetatAction()
    object ViewAboutPage : CueDetatAction()
    object SendFeedback : CueDetatAction()
    object SingleEventConsumed : CueDetatAction()
    object ToastShown : CueDetatAction()
    data class RestoreState(val state: CueDetatState) : CueDetatAction()
    object MenuClosed : CueDetatAction()
}