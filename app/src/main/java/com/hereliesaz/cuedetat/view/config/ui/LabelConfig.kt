// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/config/ui/LabelConfig.kt

package com.hereliesaz.cuedetat.view.config.ui

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.Mariner
import com.hereliesaz.cuedetat.ui.theme.MutedGray
import com.hereliesaz.cuedetat.ui.theme.SulfurDust

/** Interface defining the common properties for all text labels. */
interface LabelProperties {
    val color: Color
    val xOffset: Float
    val yOffset: Float
    val opacity: Float
    val rotationDegrees: Float
    val isPersistentlyVisible: Boolean
}

// --- Data Classes for each individual label ---

data class TargetBallLabelProperties(
    override val color: Color = SulfurDust,
    override val xOffset: Float = 0f,
    override val yOffset: Float = 0f,
    override val opacity: Float = 1.0f,
    override val rotationDegrees: Float = 0f,
    override val isPersistentlyVisible: Boolean = false
) : LabelProperties

data class GhostCueBallLabelProperties(
    override val color: Color = MutedGray,
    override val xOffset: Float = 0f,
    override val yOffset: Float = 0f,
    override val opacity: Float = 1.0f,
    override val rotationDegrees: Float = 0f,
    override val isPersistentlyVisible: Boolean = false
) : LabelProperties

data class ActualCueBallLabelProperties(
    override val color: Color = Mariner,
    override val xOffset: Float = 0f,
    override val yOffset: Float = 0f,
    override val opacity: Float = 1.0f,
    override val rotationDegrees: Float = 0f,
    override val isPersistentlyVisible: Boolean = false
) : LabelProperties

data class BankingBallLabelProperties(
    override val color: Color = SulfurDust,
    override val xOffset: Float = 0f,
    override val yOffset: Float = 0f,
    override val opacity: Float = 1.0f,
    override val rotationDegrees: Float = 0f,
    override val isPersistentlyVisible: Boolean = false
) : LabelProperties

data class ObstacleBallLabelProperties(
    override val color: Color = Color.White,
    override val xOffset: Float = 0f,
    override val yOffset: Float = 0f,
    override val opacity: Float = 1.0f,
    override val rotationDegrees: Float = 0f,
    override val isPersistentlyVisible: Boolean = false
) : LabelProperties

data class AimingLineLabelProperties(
    override val color: Color = SulfurDust,
    override val xOffset: Float = 5f,
    override val yOffset: Float = -4f,
    override val opacity: Float = 1.0f,
    override val rotationDegrees: Float = 270f,
    override val isPersistentlyVisible: Boolean = false
) : LabelProperties

data class ShotGuideLineLabelProperties(
    override val color: Color = Mariner,
    override val xOffset: Float = 5f,
    override val yOffset: Float = 0f,
    override val opacity: Float = 1.0f,
    override val rotationDegrees: Float = 0f,
    override val isPersistentlyVisible: Boolean = false
) : LabelProperties

data class TangentLineLabelProperties(
    override val color: Color = MutedGray,
    override val xOffset: Float = 10f,
    override val yOffset: Float = 0f,
    override val opacity: Float = 1.0f,
    override val rotationDegrees: Float = 0f,
    override val isPersistentlyVisible: Boolean = false
) : LabelProperties

data class AngleGuideLabelProperties(
    override val color: Color = Color.White,
    override val xOffset: Float = 0f,
    override val yOffset: Float = 0f,
    override val opacity: Float = 0.4f,
    override val rotationDegrees: Float = 0f,
    override val isPersistentlyVisible: Boolean = true
) : LabelProperties

data class DiamondSystemLabelProperties(
    override val color: Color = Mariner,
    override val xOffset: Float = 0f,
    override val yOffset: Float = 0f,
    override val opacity: Float = 1.0f,
    override val rotationDegrees: Float = 0f,
    override val isPersistentlyVisible: Boolean = true
) : LabelProperties

data class ProtractorLabelProperties(
    override val color: Color = MutedGray,
    override val xOffset: Float = 0f,
    override val yOffset: Float = 60f,
    override val opacity: Float = 1.0f,
    override val rotationDegrees: Float = 0f,
    override val isPersistentlyVisible: Boolean = true
) : LabelProperties


/**
 * A centralized configuration object holding an instance of each specific
 * label property class.
 */
object LabelConfig {
    /** Master toggle for the default visibility of helper labels. */
    const val showLabelsByDefault: Boolean = false

    val targetBall = TargetBallLabelProperties()
    val ghostCueBall = GhostCueBallLabelProperties()
    val actualCueBall = ActualCueBallLabelProperties()
    val bankingBall = BankingBallLabelProperties()
    val obstacleBall = ObstacleBallLabelProperties()

    val aimingLine = AimingLineLabelProperties()
    val shotGuideLine = ShotGuideLineLabelProperties()
    val tangentLine = TangentLineLabelProperties()
    val angleGuide = AngleGuideLabelProperties()
    val diamondSystem = DiamondSystemLabelProperties()
    val protractor = ProtractorLabelProperties()
}