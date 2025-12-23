// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/UiModel.kt

package com.hereliesaz.cuedetat.domain

import android.graphics.Matrix
import android.graphics.PointF
import androidx.annotation.Keep
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.data.FullOrientation
import com.hereliesaz.cuedetat.data.VisionData
import com.hereliesaz.cuedetat.ui.hatemode.HaterEvent
import com.hereliesaz.cuedetat.ui.hatemode.HaterState
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
import kotlinx.coroutines.Job
import org.opencv.core.Mat


enum class ExperienceMode {
    EXPERT, BEGINNER, HATER;
    fun next(): ExperienceMode {
        val nextOrdinal = (this.ordinal + 1) % values().size
        return values()[nextOrdinal]
    }
}

@Keep
data class CueDetatState(
    val experienceMode: ExperienceMode? = null,
    val pendingExperienceMode: ExperienceMode? = null,
    val haterState: HaterState = HaterState(),
    val isMenuVisible: Boolean = false,
    val isNavigationRailExpanded: Boolean = false,
    val viewWidth: Int = 0,
    val viewHeight: Int = 0,
    val protractorUnit: ProtractorUnit = ProtractorUnit(PointF(0f, 0f), LOGICAL_BALL_RADIUS, 0f),
    val onPlaneBall: OnPlaneBall? = null,
    val obstacleBalls: List<OnPlaneBall> = emptyList(),
    val table: Table = Table(
        size = TableSize.EIGHT_FT,
        isVisible = false
    ),
    val zoomSliderPosition: Float = 0f,
    val worldRotationDegrees: Float = 0f,
    val areHelpersVisible: Boolean = LabelConfig.showLabelsByDefault,
    val isMoreHelpVisible: Boolean = false,
    val valuesChangedSinceReset: Boolean = false,
    val isCameraVisible: Boolean = true,
    val showCameraFeed: Boolean = false,
    val viewOffset: PointF = PointF(0f, 0f),
    val orientationLock: OrientationLock = OrientationLock.PORTRAIT,
    @Transient val pendingOrientationLock: OrientationLock? = null,
    val isBeginnerViewLocked: Boolean = false,
    val isBankingMode: Boolean = false,
    val bankingAimTarget: PointF? = null,
    @Transient val bankShotPath: List<PointF>? = null,
    @Transient val pocketedBankShotPocketIndex: Int? = null,
    val showTableSizeDialog: Boolean = false,
    val isForceLightMode: Boolean? = null,
    val luminanceAdjustment: Float = 0f,
    val showLuminanceDialog: Boolean = false,
    val glowStickValue: Float = 0f,
    val showGlowStickDialog: Boolean = false,
    val isSpinControlVisible: Boolean = false,
    val selectedSpinOffset: PointF? = null,
    @Transient val spinPaths: Map<Color, List<PointF>>? = null,
    val spinControlCenter: PointF? = null,
    val lingeringSpinOffset: PointF? = null,
    @Transient val spinPathsAlpha: Float = 1.0f,
    val showTutorialOverlay: Boolean = false,
    val currentTutorialStep: Int = 0,
    @Transient val tutorialHighlight: TutorialHighlightElement? = TutorialHighlightElement.NONE,
@Transient val flashingTutorialElement: TutorialHighlightElement? = null,
@Transient val highlightAlpha: Float = 0f,
    val currentOrientation: FullOrientation = FullOrientation(0f, 0f, 0f),
    @Transient val pitchMatrix: Matrix? = null,
    @Transient val railPitchMatrix: Matrix? = null,
    @Transient val sizeCalculationMatrix: Matrix? = null,
    @Transient val inversePitchMatrix: Matrix? = null,
    @Transient val flatMatrix: Matrix? = null,
    @Transient val logicalPlaneMatrix: Matrix? = null,
    @Transient val hasInverseMatrix: Boolean = false,
    @Transient val visionData: VisionData? = null,
    @Transient val snapCandidates: List<SnapCandidate>? = null,
    val lockedHsvColor: FloatArray? = null,
    val lockedHsvStdDev: FloatArray? = null,
    val showAdvancedOptionsDialog: Boolean = false,
    val showCalibrationScreen: Boolean = false,
    val showQuickAlignScreen: Boolean = false,
    val showArScreen: Boolean = false,
    val isArTableSnapping: Boolean = false,
    val isArBallSnapping: Boolean = false,
    val areArObstaclesEnabled: Boolean = false,
    val arTablePose: FloatArray? = null,
    val cvRefinementMethod: CvRefinementMethod = CvRefinementMethod.CONTOUR,
    val useCustomModel: Boolean = false,
    val isSnappingEnabled: Boolean = true,
    val hasTargetBallBeenMoved: Boolean = false,
    val hasCueBallBeenMoved: Boolean = false,
    val houghP1: Float = 100f,
    val houghP2: Float = 20f,
    val houghThreshold: Int = 50,
    val cannyThreshold1: Float = 50f,
    val cannyThreshold2: Float = 150f,
    val showCvMask: Boolean = false,
    val isTestingCvMask: Boolean = false,
    val isCalibratingColor: Boolean = false,
    val colorSamplePoint: Offset? = null,
    @Transient val cameraMatrix: Mat? = null,
    @Transient val distCoeffs: Mat? = null,
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
    @Transient val appControlColorScheme: ColorScheme? = null,
    val interactionMode: InteractionMode = InteractionMode.NONE,
    val movingObstacleBallIndex: Int? = null,
    val isMagnifierVisible: Boolean = false,
    @Transient val magnifierSourceCenter: Offset? = null,
    val isWorldLocked: Boolean = false,
    @Transient val preResetState: CueDetatState? = null,
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

sealed class MainScreenEvent {
    object ToggleExperienceModeSelection : MainScreenEvent()
    object ApplyPendingExperienceMode : MainScreenEvent()
    data class SetExperienceMode(val mode: ExperienceMode) : MainScreenEvent()
    data class HaterAction(val action: HaterEvent) : MainScreenEvent()
    object ToggleMenu : MainScreenEvent()
    object ToggleNavigationRail : MainScreenEvent()
    data class ScreenGestureStarted(val position: PointF) : MainScreenEvent()
    data class Drag(val previousPosition: PointF, val currentPosition: PointF) : MainScreenEvent()
    object GestureEnded : MainScreenEvent()
    data class SizeChanged(val width: Int, val height: Int) : MainScreenEvent()
    data class ZoomScaleChanged(val scaleFactor: Float) : MainScreenEvent()
    data class TableRotationApplied(val degrees: Float) : MainScreenEvent()
    data class ZoomSliderChanged(val position: Float) : MainScreenEvent()
    data class PanView(val delta: PointF) : MainScreenEvent()
    object ToggleSpinControl : MainScreenEvent()
    data class SpinApplied(val offset: PointF) : MainScreenEvent()
    object SpinSelectionEnded : MainScreenEvent()
    data class DragSpinControl(val delta: PointF) : MainScreenEvent()
    object ClearSpinState : MainScreenEvent()
    internal data class LogicalGestureStarted(val logicalPoint: PointF, val screenOffset: Offset) :
        MainScreenEvent()

    internal data class LogicalDragApplied(
        val previousLogicalPoint: PointF,
        val currentLogicalPoint: PointF,
        val screenDelta: Offset
    ) : MainScreenEvent()
    data class TableRotationChanged(val degrees: Float) : MainScreenEvent()
    data class FullOrientationChanged(val orientation: FullOrientation) : MainScreenEvent()
    data class ThemeChanged(val scheme: ColorScheme) : MainScreenEvent()
    object Reset : MainScreenEvent()
    object ToggleHelp : MainScreenEvent()
    object ToggleMoreHelp : MainScreenEvent()
    object ToggleBankingMode : MainScreenEvent()
    object CycleTableSize : MainScreenEvent()
    data class SetTableSize(val size: TableSize) : MainScreenEvent()
    object ToggleTableSizeDialog : MainScreenEvent()
    object ToggleForceTheme : MainScreenEvent()
    object ToggleCamera : MainScreenEvent()
    object ToggleLuminanceDialog : MainScreenEvent()
    data class AdjustLuminance(val adjustment: Float) : MainScreenEvent()
    object ToggleDistanceUnit : MainScreenEvent()
    object ToggleGlowStickDialog : MainScreenEvent()
    data class AdjustGlow(val value: Float) : MainScreenEvent()
    data class SetWarning(val warning: String?) : MainScreenEvent()
    object ToggleOrientationLock : MainScreenEvent()
    object ApplyPendingOrientationLock : MainScreenEvent()
    data class OrientationChanged(val orientationLock: CueDetatState.OrientationLock) :
        MainScreenEvent()

    object UnlockBeginnerView : MainScreenEvent()
    object LockBeginnerView : MainScreenEvent()
    object AddObstacleBall : MainScreenEvent()
    data class CvDataUpdated(val visionData: VisionData) : MainScreenEvent()
    object LockOrUnlockColor : MainScreenEvent()
    data class LockColor(val hsvMean: FloatArray, val hsvStdDev: FloatArray) : MainScreenEvent()
    object ClearSamplePoint : MainScreenEvent()
    object ToggleAdvancedOptionsDialog : MainScreenEvent()
    object ToggleCalibrationScreen : MainScreenEvent()
    object ToggleQuickAlignScreen : MainScreenEvent()
    object ToggleArScreen : MainScreenEvent()
    object ToggleArTableSnapping : MainScreenEvent()
    object ToggleArBallSnapping : MainScreenEvent()
    object ToggleArObstacles : MainScreenEvent()
    data class ArTap(val offset: Offset) : MainScreenEvent()
    data class UpdateArTablePose(val pose: FloatArray) : MainScreenEvent()
    data class ApplyQuickAlign(val translation: Offset, val rotation: Float, val scale: Float) :
        MainScreenEvent()

    object ToggleCvRefinementMethod : MainScreenEvent()
    data class UpdateHoughP1(val value: Float) : MainScreenEvent()
    data class UpdateHoughP2(val value: Float) : MainScreenEvent()
    data class UpdateHoughThreshold(val value: Float) : MainScreenEvent()
    data class UpdateCannyT1(val value: Float) : MainScreenEvent()
    data class UpdateCannyT2(val value: Float) : MainScreenEvent()
    object ToggleCvModel : MainScreenEvent()
    object ToggleSnapping : MainScreenEvent()
    object ToggleCvMask : MainScreenEvent()
    object EnterCvMaskTestMode : MainScreenEvent()
    object ExitCvMaskTestMode : MainScreenEvent()
    object EnterCalibrationMode : MainScreenEvent()
    data class SampleColorAt(val screenPosition: Offset) : MainScreenEvent()
    object StartTutorial : MainScreenEvent()
    object NextTutorialStep : MainScreenEvent()
    object EndTutorial : MainScreenEvent()
    data class UpdateHighlightAlpha(val alpha: Float) : MainScreenEvent()
    object CheckForUpdate : MainScreenEvent()
    object ViewArt : MainScreenEvent()
    object ViewAboutPage : MainScreenEvent()
    object SendFeedback : MainScreenEvent()
    object SingleEventConsumed : MainScreenEvent()
    object ToastShown : MainScreenEvent()
    data class RestoreState(val state: CueDetatState) : MainScreenEvent()
}