// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/CueDetatState.kt
package com.hereliesaz.cuedetat.domain

import android.graphics.Matrix
import android.graphics.PointF
import androidx.annotation.Keep
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.data.FullOrientation
import com.hereliesaz.cuedetat.data.VisionData
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
import org.opencv.core.Mat

enum class CameraMode {
    OFF, CAMERA, AR_SETUP, AR_ACTIVE, CAMERA_ONLY, LITE_AR, META_GLASSES;
    fun next(): CameraMode {
        val nextOrdinal = (this.ordinal + 1) % values().size
        return values()[nextOrdinal]
    }
}

enum class ExperienceMode {
    EXPERT, BEGINNER, HATER;
    fun next(): ExperienceMode {
        val nextOrdinal = (this.ordinal + 1) % values().size
        return values()[nextOrdinal]
    }
}

enum class BallSelectionPhase {
    NONE, AWAITING_CUE, AWAITING_TARGET
}

enum class TutorialType {
    GENERAL, DYNAMIC_NON_AR, DYNAMIC_AR, BEGINNER_STATIC, BEGINNER_DYNAMIC
}

enum class TargetType {
    SOLIDS, STRIPES
}

@Keep
data class FeltSample(
    val id: String = java.util.UUID.randomUUID().toString(),
    val hsv: List<Float>
)

@Keep
data class CueDetatState(
    val experienceMode: ExperienceMode? = null,
    val pendingExperienceMode: ExperienceMode? = null,
    val haterState: HaterState = HaterState(),
    val viewWidth: Int = 0,
    val viewHeight: Int = 0,
    val screenDensity: Float = 1.0f,
    val protractorUnit: ProtractorUnit = ProtractorUnit(PointF(0f, 0f), LOGICAL_BALL_RADIUS, 0f),
    val onPlaneBall: OnPlaneBall? = null,
    val obstacleBalls: List<OnPlaneBall> = emptyList(),
    val savedFeltSamples: List<FeltSample> = emptyList(),
    val table: Table = Table(
        size = TableSize.EIGHT_FT,
        isVisible = false
    ),
    val zoomSliderPosition: Float = 0f,
    val worldRotationDegrees: Float = 0f,
    val areHelpersVisible: Boolean = LabelConfig.showLabelsByDefault,
    val valuesChangedSinceReset: Boolean = false,
    val cameraMode: CameraMode = CameraMode.OFF,
    val viewOffset: PointF = PointF(0f, 0f),
    val tableZOffset: Float = 0f,
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
    val isMasseModeActive: Boolean = false,
    val masseShotAngleDeg: Float = 0f,
    /** Normalized spin offset: x = English (side), y = Vertical (draw/follow). Range -1.0..1.0. */
    val selectedSpinOffset: PointF? = null,
    @Transient val spinPaths: Map<Color, List<PointF>>? = null,
    @Transient val masseImpactPoints: List<PointF> = emptyList(),
    @Transient val masseConnectsTarget: Boolean = false,
    @Transient val masseGhostBallCenter: PointF? = null,
    val spinControlCenter: PointF? = null,
    /** Normalized spin offset: x = English (side), y = Vertical (draw/follow). Range -1.0..1.0. */
    val lingeringSpinOffset: PointF? = null,
    @Transient val spinPathsAlpha: Float = 1.0f,
    val showTutorialOverlay: Boolean = false,
    val hasSeenBeginnerTutorial: Boolean = false,
    val hasSeenDynamicBeginnerTutorial: Boolean = false,
    val hasSeenExpertTutorial: Boolean = false,
    val tutorialType: TutorialType = TutorialType.GENERAL,
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
    @Transient val arConfidenceHistory: List<Float> = emptyList(),
    @Transient val arLowConfidenceFrameCount: Int = 0,
    @Transient val relocaliserDeltaQ: FloatArray? = null,
    @Transient val relocaliserAttemptFrames: Int = 0,
    @Transient val snapCandidates: List<SnapCandidate>? = null,
    @Transient val tableScanModel: TableScanModel? = null,
    @Transient val depthPlane: DepthPlane? = null,
    @Transient val arDerivedPitch: Float? = null,
    @Transient val arMeasuredHeightM: Float? = null,
    // World-anchored table (ARCore). arTableMatrix maps logical table space -> screen pixels,
    // recomputed every AR frame from the camera pose + corner anchors, so the overlay tracks in
    // full 6DoF as the user walks around. Null until 4 corners are captured / tracking is valid.
    @Transient val arTableMatrix: Matrix? = null,
    // Live screen positions of the captured corner anchors, projected each AR frame. Drives the
    // capture-feedback line from the last captured pocket to the centre reticle.
    @Transient val arCapturedCorners: List<PointF> = emptyList(),
    val depthCapability: DepthCapability = DepthCapability.NONE,
    // Download/load lifecycle of the on-demand Expert-AR module. Transient: it is
    // runtime-only and must not survive process death (a LOADING snapshot would
    // be stale on restart).
    @Transient val arModuleState: ArModuleState = ArModuleState.IDLE,
    val lockedHsvColor: FloatArray? = null,
    val lockedHsvStdDev: FloatArray? = null,
    val showAdvancedOptionsDialog: Boolean = false,
    val showBillingDebugDialog: Boolean = false,
    val showCalibrationScreen: Boolean = false,
    val showTableScanScreen: Boolean = false,
    val cvRefinementMethod: CvRefinementMethod = CvRefinementMethod.CONTOUR,
    val useCustomModel: Boolean = false,
    val isSnappingEnabled: Boolean = true,
    val hasTargetBallBeenMoved: Boolean = false,
    val hasCueBallBeenMoved: Boolean = false,
    val cannyThreshold1: Float = 40f,
    val cannyThreshold2: Float = 120f,
    val isAutoCalibrating: Boolean = false,
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
    @Transient val aimedPocketIndex: Int? = null,
    @Transient val aimingLineBankPath: List<PointF>? = null,
    @Transient val tangentLineBankPath: List<PointF>? = null,
    @Transient val inactiveTangentLineBankPath: List<PointF>? = null,
    @Transient val tangentAimedPocketIndex: Int? = null,
    @Transient val aimingLineEndPoint: PointF? = null,
    @Transient val appControlColorScheme: ColorScheme? = null,
    val interactionMode: InteractionMode = InteractionMode.NONE,
    val movingObstacleBallIndex: Int? = null,
    val isMagnifierVisible: Boolean = false,
    @Transient val magnifierSourceCenter: Offset? = null,
    val isWorldLocked: Boolean = false,
    @Transient val preResetState: CueDetatState? = null,
    @Transient val postResetState: CueDetatState? = null,
    @Transient val ballSelectionPhase: BallSelectionPhase = BallSelectionPhase.NONE,
    @Transient val cueBallCvAnchor: PointF? = null,
    @Transient val targetCvAnchor: PointF? = null,
    @Transient val obstacleCvAnchors: List<PointF?> = emptyList(),
    @Transient val latestVersionName: String? = null,
    val distanceUnit: DistanceUnit = DistanceUnit.IMPERIAL,
    @Transient val targetBallDistance: Float = 0f,
    val lensWarpTps: TpsWarpData? = null,
    val targetType: TargetType = TargetType.SOLIDS,
    val isTopDownViewActive: Boolean = false,
    @Transient val topDownBitmap: android.graphics.Bitmap? = null,
    val topDownTransitionProgress: Float = 0f,
    val isExpertEntitled: Boolean = false,
    val isAdvisorEnabled: Boolean = false,
    @Transient val recommendedShot: com.hereliesaz.cuedetat.domain.advisor.RecommendedShot? = null,
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
    data class EntitlementChanged(val entitlement: com.hereliesaz.cuedetat.billing.Entitlement) : MainScreenEvent()
    data class ShowPaywall(val trigger: com.hereliesaz.cuedetat.billing.PaywallTrigger) : MainScreenEvent()
    data class ScreenGestureStarted(val position: PointF) : MainScreenEvent()
    data class Drag(val previousPosition: PointF, val currentPosition: PointF) : MainScreenEvent()
    object GestureEnded : MainScreenEvent()
    data class SizeChanged(val width: Int, val height: Int, val density: Float) : MainScreenEvent()
    data class ZoomScaleChanged(val scaleFactor: Float) : MainScreenEvent()
    data class TableRotationApplied(val degrees: Float) : MainScreenEvent()
    data class ZoomSliderChanged(val position: Float) : MainScreenEvent()
    data class PanView(val delta: PointF) : MainScreenEvent()
    data class MoveTableZ(val delta: Float) : MainScreenEvent()
    object ToggleSpinControl : MainScreenEvent()
    object ToggleMasseMode : MainScreenEvent()
    data class SpinApplied(val offset: PointF) : MainScreenEvent()
    object SpinSelectionEnded : MainScreenEvent()
    data class DragSpinControl(val delta: PointF) : MainScreenEvent()
    object ClearSpinState : MainScreenEvent()
    object SpinPathTick : MainScreenEvent()
    internal data class LogicalGestureStarted(val logicalPoint: PointF, val screenOffset: Offset, val isDoubleTap: Boolean = false) :
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
    object ToggleBankingMode : MainScreenEvent()
    object CycleTableSize : MainScreenEvent()
    data class SetTableSize(val size: TableSize) : MainScreenEvent()
    object ToggleTableSizeDialog : MainScreenEvent()
    object ToggleForceTheme : MainScreenEvent()
    object CycleCameraMode : MainScreenEvent()
    data class SetCameraMode(val mode: CameraMode) : MainScreenEvent()
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
    object AutoCalibrateCv : MainScreenEvent()
    object LockOrUnlockColor : MainScreenEvent()
    data class LockColor(val hsvMean: FloatArray, val hsvStdDev: FloatArray) : MainScreenEvent()
    data class AddFeltSample(val hsv: List<Float>) : MainScreenEvent()
    data class DeleteFeltSamples(val ids: Set<String>) : MainScreenEvent()
    data class MoveFeltSample(val fromIndex: Int, val toIndex: Int) : MainScreenEvent()
    object StartArTracking : MainScreenEvent()
    object ClearSamplePoint : MainScreenEvent()
    object ToggleAdvancedOptionsDialog : MainScreenEvent()
    object ToggleBillingDebugDialog : MainScreenEvent()
    object ToggleCalibrationScreen : MainScreenEvent()
    data class ApplyQuickAlign(
        val translation: Offset,
        val rotation: Float,
        val scale: Float,
        val tpsWarpData: TpsWarpData
    ) : MainScreenEvent()

    object ToggleCvRefinementMethod : MainScreenEvent()
    data class UpdateCannyT1(val value: Float) : MainScreenEvent()
    data class UpdateCannyT2(val value: Float) : MainScreenEvent()
    object ToggleCvModel : MainScreenEvent()
    object ToggleSnapping : MainScreenEvent()
    object ToggleCvMask : MainScreenEvent()
    object EnterCvMaskTestMode : MainScreenEvent()
    object ExitCvMaskTestMode : MainScreenEvent()
    object StartCalibrationMode : MainScreenEvent()
    data class SampleColorAt(val screenPosition: Offset) : MainScreenEvent()
    data class StartTutorial(val type: TutorialType = TutorialType.GENERAL) : MainScreenEvent()
    object NextTutorialStep : MainScreenEvent()
    object EndTutorial : MainScreenEvent()
    object TutorialBack : MainScreenEvent()
    data class UpdateHighlightAlpha(val alpha: Float) : MainScreenEvent()
    object CheckForUpdate : MainScreenEvent()
    object ViewArt : MainScreenEvent()
    object ViewAboutPage : MainScreenEvent()
    object SendFeedback : MainScreenEvent()
    object SingleEventConsumed : MainScreenEvent()
    object Shake : MainScreenEvent()
    object ExitToSplash : MainScreenEvent()
    data class RestoreState(val state: CueDetatState) : MainScreenEvent()

    // Table scan events
    data class LoadTableScan(val model: TableScanModel) : MainScreenEvent()
    object ClearTableScan : MainScreenEvent()
    object StartManualHoleCapture : MainScreenEvent()
    data class UpdateArPose(
        val translation: Offset,
        val rotation: Float,
        val scale: Float
    ) : MainScreenEvent()
    data class UpdateTableScanClusters(
        val updatedClusters: List<PocketCluster>
    ) : MainScreenEvent()
    object ToggleTableScanScreen : MainScreenEvent()

    // On-demand Expert-AR module delivery (see ArControllerFacade / MainViewModel).
    object ArModuleLoadStarted : MainScreenEvent()
    object ArModuleLoadSucceeded : MainScreenEvent()
    object ArModuleLoadFailed : MainScreenEvent()
    /** User tapped "Retry" on the download-failed overlay. */
    object RetryArModuleLoad : MainScreenEvent()

    // Depth / ARCore events
    data class DepthPlaneUpdated(val plane: DepthPlane) : MainScreenEvent()
    data class DepthCapabilityDetected(val capability: DepthCapability) : MainScreenEvent()
    data class ArCameraPoseUpdated(
        val pitchDegrees: Float,
        val heightAboveSurfaceM: Float
    ) : MainScreenEvent()

    // World-anchored table events (emitted from the ARCore GL thread each frame).
    // [matrix] is the logical->screen homography (null until 4 corners are tracked);
    // [capturedCorners] are the live projected screen positions of the captured anchors.
    data class ArTableMatrixUpdated(
        val matrix: Matrix?,
        val capturedCorners: List<PointF>
    ) : MainScreenEvent()
    // Result of a corner-capture hit-test. [hit] is false when the centre reticle did not land on
    // a tracked plane (the user is shown guidance and the capture is a no-op). [count] is the new
    // number of captured corners.
    data class ArCornerCaptured(val hit: Boolean, val count: Int) : MainScreenEvent()

    // AR setup / lifecycle events
    object CancelArSetup : MainScreenEvent()
    object TurnCameraOff : MainScreenEvent()
    object ArTrackingLost : MainScreenEvent()
    data class ArSurfaceTapped(val screenPoint: PointF) : MainScreenEvent()
    object ToggleTargetType : MainScreenEvent()
    object ToggleTopDownView : MainScreenEvent()
    object ClearTopDownView : MainScreenEvent()
    data class SetTopDownBitmap(val bitmap: android.graphics.Bitmap?) : MainScreenEvent()

    // Shot advisor
    object ToggleAdvisor : MainScreenEvent()
    data class RecommendationComputed(val shot: com.hereliesaz.cuedetat.domain.advisor.RecommendedShot?) : MainScreenEvent()

    // Relocalisation events
    object ForceArActive : MainScreenEvent()
    data class SeedRelocaliser(val deltaQ: FloatArray?) : MainScreenEvent()
}