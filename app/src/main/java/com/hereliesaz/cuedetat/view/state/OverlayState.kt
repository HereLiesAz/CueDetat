// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/state/OverlayState.kt
package com.hereliesaz.cuedetat.view.state

import android.graphics.Matrix
import android.graphics.PointF
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.data.FullOrientation
import com.hereliesaz.cuedetat.data.VisionData
import com.hereliesaz.cuedetat.view.model.*

data class OverlayState(
    val viewWidth: Int = 0,
    val viewHeight: Int = 0,
    val protractorUnit: ProtractorUnit = ProtractorUnit(),
    val onPlaneBall: LogicalCircular? = null,
    val obstacleBalls: List<LogicalCircular> = emptyList(),
    val isGeometricallyImpossible: Boolean = false,
    val isObstructed: Boolean = false,
    val isTiltBeyondLimit: Boolean = false,
    val tangentDirection: Float = 1.0f,
    val pitchMatrix: Matrix = Matrix(),
    val railPitchMatrix: Matrix = Matrix(),
    val inversePitchMatrix: Matrix = Matrix(),
    val hasInverseMatrix: Boolean = false,
    val currentOrientation: FullOrientation = FullOrientation(),
    val pitchAngle: Float = 0f,
    val isCameraVisible: Boolean = true,
    val zoomSliderPosition: Float = 0.5f,
    val targetBallDistance: Float = 0f,
    val shotLineAnchor: PointF = PointF(0f, 10000f),
    val aimedPocketIndex: Int? = null,
    val tangentAimedPocketIndex: Int? = null,
    val aimingLineEndPoint: PointF? = null,
    val shotGuideImpactPoint: PointF? = null,
    val aimingLineBankPath: List<PointF> = emptyList(),
    val tangentLineBankPath: List<PointF> = emptyList(),
    val table: Table = Table(),
    val visionData: VisionData = VisionData(),
    val isBankingMode: Boolean = false,
    val bankingAimTarget: PointF = PointF(0f, -100f),
    val bankShotPath: List<PointF> = emptyList(),
    val pocketedBankShotPocketIndex: Int? = null,
    val appControlColorScheme: ColorScheme = darkColorScheme(),
    val luminanceAdjustment: Float = 0f,
    val glowStickValue: Float = 0f,
    val isLuminanceDialogVisible: Boolean = false,
    val isGlowStickDialogVisible: Boolean = false,
    val isTableSizeDialogVisible: Boolean = false,
    val isAdvancedOptionsDialogVisible: Boolean = false,
    val isForceLightMode: Boolean? = null,
    val isMagnifierVisible: Boolean = false,
    val magnifierSourceCenter: Offset? = null,
    val isTutorialVisible: Boolean = true,
    val tutorialStep: Int = 0,
    val warningText: String? = null,
    val isSpinControlVisible: Boolean = false,
    val spinControlCenter: PointF? = null,
    val selectedSpinOffset: Offset? = null,
    val lingeringSpinOffset: Offset? = null,
    val spinPathsAlpha: Float = 1.0f,
    val spinPaths: Map<Color, List<PointF>> = emptyMap(),
    val lockedHsvColor: FloatArray? = null,
    val areHelpersVisible: Boolean = true,
    val valuesChangedSinceReset: Boolean = false,
    val toastMessage: ToastMessage? = null, // Restored for proper toast handling
    val isSnappingEnabled: Boolean = true,
    val cvModel: String = "Generic", // Example, adjust as needed
    val cvRefinement: CvRefinementMethod = CvRefinementMethod.HOUGH,
    val houghP1: Float = 100f,
    val houghP2: Float = 30f,
    val cannyThreshold1: Float = 50f,
    val cannyThreshold2: Float = 100f,
    val distanceUnit: DistanceUnit = DistanceUnit.IMPERIAL,
    val hasTargetBallBeenMoved: Boolean = false,
    val hasCueBallBeenMoved: Boolean = false,
    val snapCandidates: List<SnapCandidate> = emptyList()
)