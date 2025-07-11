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
import com.hereliesaz.cuedetat.view.model.OnPlaneBall
import com.hereliesaz.cuedetat.view.model.ProtractorUnit

enum class InteractionMode {
    NONE,
    SCALING,
    ROTATING_PROTRACTOR,
    MOVING_PROTRACTOR_UNIT,
    MOVING_ACTUAL_CUE_BALL,
    AIMING_BANK_SHOT,
    MOVING_SPIN_CONTROL,
    MOVING_OBSTACLE_BALL
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
    val obstacleBalls: List<OnPlaneBall> = emptyList(),

    // UI control state
    val zoomSliderPosition: Float = 0f,
    val areHelpersVisible: Boolean = false,
    val isMoreHelpVisible: Boolean = false,
    val valuesChangedSinceReset: Boolean = false,
    val isCameraVisible: Boolean = true,

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
    val railPitchMatrix: Matrix = Matrix(),
    val inversePitchMatrix: Matrix = Matrix(),
    val flatMatrix: Matrix = Matrix(),
    val hasInverseMatrix: Boolean = false,

    // CV Data
    val visionData: VisionData = VisionData(),
    val lockedHsvColor: FloatArray? = null,

    // Derived state
    val shotLineAnchor: PointF = PointF(0f, 0f),
    val tangentDirection: Float = 1.0f,
    val isImpossibleShot: Boolean = false,
    val isTiltBeyondLimit: Boolean = false,
    val warningText: String? = null,
    val shotGuideImpactPoint: PointF? = null,
    val aimingLineBankPath: List<PointF> = emptyList(),
    val tangentLineBankPath: List<PointF> = emptyList(),
    val aimedPocketIndex: Int? = null,
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
    val tableWasLastOnWithBall: Boolean = false,

    // Version Info
    val latestVersionName: String? = null,
    val distanceUnit: DistanceUnit = DistanceUnit.IMPERIAL,
    val targetBallDistance: Float = 0f
) {
    val pitchAngle: Float
        get() = currentOrientation.pitch

    // Custom equals/hashCode needed for FloatArray property
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OverlayState

        if (viewWidth != other.viewWidth) return false
        if (viewHeight != other.viewHeight) return false
        if (protractorUnit != other.protractorUnit) return false
        if (onPlaneBall != other.onPlaneBall) return false
        if (obstacleBalls != other.obstacleBalls) return false
        if (zoomSliderPosition != other.zoomSliderPosition) return false
        if (areHelpersVisible != other.areHelpersVisible) return false
        if (isMoreHelpVisible != other.isMoreHelpVisible) return false
        if (valuesChangedSinceReset != other.valuesChangedSinceReset) return false
        if (isCameraVisible != other.isCameraVisible) return false
        if (isBankingMode != other.isBankingMode) return false
        if (showTable != other.showTable) return false
        if (tableRotationDegrees != other.tableRotationDegrees) return false
        if (bankingAimTarget != other.bankingAimTarget) return false
        if (bankShotPath != other.bankShotPath) return false
        if (pocketedBankShotPocketIndex != other.pocketedBankShotPocketIndex) return false
        if (tableSize != other.tableSize) return false
        if (showTableSizeDialog != other.showTableSizeDialog) return false
        if (isForceLightMode != other.isForceLightMode) return false
        if (luminanceAdjustment != other.luminanceAdjustment) return false
        if (showLuminanceDialog != other.showLuminanceDialog) return false
        if (glowStickValue != other.glowStickValue) return false
        if (showGlowStickDialog != other.showGlowStickDialog) return false
        if (isSpinControlVisible != other.isSpinControlVisible) return false
        if (selectedSpinOffset != other.selectedSpinOffset) return false
        if (spinPaths != other.spinPaths) return false
        if (spinControlCenter != other.spinControlCenter) return false
        if (lingeringSpinOffset != other.lingeringSpinOffset) return false
        if (spinPathsAlpha != other.spinPathsAlpha) return false
        if (showTutorialOverlay != other.showTutorialOverlay) return false
        if (currentTutorialStep != other.currentTutorialStep) return false
        if (currentOrientation != other.currentOrientation) return false
        if (pitchMatrix != other.pitchMatrix) return false
        if (railPitchMatrix != other.railPitchMatrix) return false
        if (inversePitchMatrix != other.inversePitchMatrix) return false
        if (flatMatrix != other.flatMatrix) return false
        if (hasInverseMatrix != other.hasInverseMatrix) return false
        if (visionData != other.visionData) return false
        if (lockedHsvColor != null) {
            if (other.lockedHsvColor == null) return false
            if (!lockedHsvColor.contentEquals(other.lockedHsvColor)) return false
        } else if (other.lockedHsvColor != null) return false
        if (shotLineAnchor != other.shotLineAnchor) return false
        if (tangentDirection != other.tangentDirection) return false
        if (isImpossibleShot != other.isImpossibleShot) return false
        if (isTiltBeyondLimit != other.isTiltBeyondLimit) return false
        if (warningText != other.warningText) return false
        if (shotGuideImpactPoint != other.shotGuideImpactPoint) return false
        if (aimingLineBankPath != other.aimingLineBankPath) return false
        if (tangentLineBankPath != other.tangentLineBankPath) return false
        if (aimedPocketIndex != other.aimedPocketIndex) return false
        if (aimingLineEndPoint != other.aimingLineEndPoint) return false
        if (appControlColorScheme != other.appControlColorScheme) return false
        if (interactionMode != other.interactionMode) return false
        if (movingObstacleBallIndex != other.movingObstacleBallIndex) return false
        if (isMagnifierVisible != other.isMagnifierVisible) return false
        if (magnifierSourceCenter != other.magnifierSourceCenter) return false
        if (preResetState != other.preResetState) return false
        if (tableWasLastOnWithBall != other.tableWasLastOnWithBall) return false
        if (latestVersionName != other.latestVersionName) return false
        if (distanceUnit != other.distanceUnit) return false
        if (targetBallDistance != other.targetBallDistance) return false

        return true
    }

    override fun hashCode(): Int {
        var result = viewWidth
        result = 31 * result + viewHeight
        result = 31 * result + protractorUnit.hashCode()
        result = 31 * result + (onPlaneBall?.hashCode() ?: 0)
        result = 31 * result + obstacleBalls.hashCode()
        result = 31 * result + zoomSliderPosition.hashCode()
        result = 31 * result + areHelpersVisible.hashCode()
        result = 31 * result + isMoreHelpVisible.hashCode()
        result = 31 * result + valuesChangedSinceReset.hashCode()
        result = 31 * result + isCameraVisible.hashCode()
        result = 31 * result + isBankingMode.hashCode()
        result = 31 * result + showTable.hashCode()
        result = 31 * result + tableRotationDegrees.hashCode()
        result = 31 * result + (bankingAimTarget?.hashCode() ?: 0)
        result = 31 * result + bankShotPath.hashCode()
        result = 31 * result + (pocketedBankShotPocketIndex ?: 0)
        result = 31 * result + tableSize.hashCode()
        result = 31 * result + showTableSizeDialog.hashCode()
        result = 31 * result + (isForceLightMode?.hashCode() ?: 0)
        result = 31 * result + luminanceAdjustment.hashCode()
        result = 31 * result + showLuminanceDialog.hashCode()
        result = 31 * result + glowStickValue.hashCode()
        result = 31 * result + showGlowStickDialog.hashCode()
        result = 31 * result + isSpinControlVisible.hashCode()
        result = 31 * result + (selectedSpinOffset?.hashCode() ?: 0)
        result = 31 * result + spinPaths.hashCode()
        result = 31 * result + (spinControlCenter?.hashCode() ?: 0)
        result = 31 * result + (lingeringSpinOffset?.hashCode() ?: 0)
        result = 31 * result + spinPathsAlpha.hashCode()
        result = 31 * result + showTutorialOverlay.hashCode()
        result = 31 * result + currentTutorialStep
        result = 31 * result + currentOrientation.hashCode()
        result = 31 * result + pitchMatrix.hashCode()
        result = 31 * result + railPitchMatrix.hashCode()
        result = 31 * result + inversePitchMatrix.hashCode()
        result = 31 * result + flatMatrix.hashCode()
        result = 31 * result + hasInverseMatrix.hashCode()
        result = 31 * result + visionData.hashCode()
        result = 31 * result + (lockedHsvColor?.contentHashCode() ?: 0)
        result = 31 * result + shotLineAnchor.hashCode()
        result = 31 * result + tangentDirection.hashCode()
        result = 31 * result + isImpossibleShot.hashCode()
        result = 31 * result + isTiltBeyondLimit.hashCode()
        result = 31 * result + (warningText?.hashCode() ?: 0)
        result = 31 * result + (shotGuideImpactPoint?.hashCode() ?: 0)
        result = 31 * result + aimingLineBankPath.hashCode()
        result = 31 * result + tangentLineBankPath.hashCode()
        result = 31 * result + (aimedPocketIndex ?: 0)
        result = 31 * result + (aimingLineEndPoint?.hashCode() ?: 0)
        result = 31 * result + appControlColorScheme.hashCode()
        result = 31 * result + interactionMode.hashCode()
        result = 31 * result + (movingObstacleBallIndex ?: 0)
        result = 31 * result + isMagnifierVisible.hashCode()
        result = 31 * result + (magnifierSourceCenter?.hashCode() ?: 0)
        result = 31 * result + (preResetState?.hashCode() ?: 0)
        result = 31 * result + tableWasLastOnWithBall.hashCode()
        result = 31 * result + (latestVersionName?.hashCode() ?: 0)
        result = 31 * result + distanceUnit.hashCode()
        result = 31 * result + targetBallDistance.hashCode()
        return result
    }
}