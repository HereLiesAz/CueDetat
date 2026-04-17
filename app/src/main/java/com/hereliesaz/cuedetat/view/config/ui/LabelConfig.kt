// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/config/ui/LabelConfig.kt

package com.hereliesaz.cuedetat.view.config.ui

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.Mariner
import com.hereliesaz.cuedetat.ui.theme.MutedGray
import com.hereliesaz.cuedetat.ui.theme.SulfurDust

/** Interface defining the common properties for all text labels. */
interface LabelProperties {
    /** The color of the text. */
    val color: Color
    /** The X offset in pixels from the anchor point. */
    val xOffset: Float
    /** The Y offset in pixels from the anchor point. */
    val yOffset: Float
    /** The opacity of the text (0-1). */
    val opacity: Float
    /** The rotation in degrees. */
    val rotationDegrees: Float
    /** Whether the label should be shown even when "helpers" are toggled off (e.g. key guides). */
    val isPersistentlyVisible: Boolean
}

// --- Data Classes for each individual label ---

/** Properties for the label attached to the Target Ball. */
data class TargetBallLabelProperties(
    override val color: Color = SulfurDust,
    override val xOffset: Float = 0f,
    override val yOffset: Float = 0f,
    override val opacity: Float = 1.0f,
    override val rotationDegrees: Float = 0f,
    override val isPersistentlyVisible: Boolean = false
) : LabelProperties

/** Properties for the label attached to the Ghost Cue Ball. */
data class GhostCueBallLabelProperties(
    override val color: Color = MutedGray,
    override val xOffset: Float = 0f,
    override val yOffset: Float = 0f,
    override val opacity: Float = 1.0f,
    override val rotationDegrees: Float = 0f,
    override val isPersistentlyVisible: Boolean = false
) : LabelProperties

/** Properties for the label attached to the Actual Cue Ball. */
data class ActualCueBallLabelProperties(
    override val color: Color = Mariner,
    override val xOffset: Float = 0f,
    override val yOffset: Float = 0f,
    override val opacity: Float = 1.0f,
    override val rotationDegrees: Float = 0f,
    override val isPersistentlyVisible: Boolean = false
) : LabelProperties

/** Properties for the label attached to the Banking Ball. */
data class BankingBallLabelProperties(
    override val color: Color = SulfurDust,
    override val xOffset: Float = 0f,
    override val yOffset: Float = 0f,
    override val opacity: Float = 1.0f,
    override val rotationDegrees: Float = 0f,
    override val isPersistentlyVisible: Boolean = false
) : LabelProperties

/** Properties for the label attached to Obstacle Balls. */
data class ObstacleBallLabelProperties(
    override val color: Color = Color.White,
    override val xOffset: Float = 0f,
    override val yOffset: Float = 0f,
    override val opacity: Float = 1.0f,
    override val rotationDegrees: Float = 0f,
    override val isPersistentlyVisible: Boolean = false
) : LabelProperties

/** Properties for the label along the Aiming Line. */
data class AimingLineLabelProperties(
    override val color: Color = SulfurDust,
    override val xOffset: Float = 5f,
    override val yOffset: Float = -4f,
    override val opacity: Float = 1.0f,
    override val rotationDegrees: Float = 270f,
    override val isPersistentlyVisible: Boolean = false
) : LabelProperties

/** Properties for the label along the Shot Guide Line. */
data class ShotGuideLineLabelProperties(
    override val color: Color = Mariner,
    override val xOffset: Float = 5f,
    override val yOffset: Float = 0f,
    override val opacity: Float = 1.0f,
    override val rotationDegrees: Float = 0f,
    override val isPersistentlyVisible: Boolean = false
) : LabelProperties

/** Properties for the label along the Tangent Line. */
data class TangentLineLabelProperties(
    override val color: Color = MutedGray,
    override val xOffset: Float = 10f,
    override val yOffset: Float = 0f,
    override val opacity: Float = 1.0f,
    override val rotationDegrees: Float = 0f,
    override val isPersistentlyVisible: Boolean = false
) : LabelProperties

/** Properties for general angle guides. */
data class AngleGuideLabelProperties(
    override val color: Color = Color.White,
    override val xOffset: Float = 0f,
    override val yOffset: Float = 0f,
    override val opacity: Float = 0.4f,
    override val rotationDegrees: Float = 0f,
    override val isPersistentlyVisible: Boolean = true
) : LabelProperties

/** Properties for Diamond System guides. */
data class DiamondSystemLabelProperties(
    override val color: Color = Mariner,
    override val xOffset: Float = 0f,
    override val yOffset: Float = 0f,
    override val opacity: Float = 1.0f,
    override val rotationDegrees: Float = 0f,
    override val isPersistentlyVisible: Boolean = true
) : LabelProperties

/** Properties for Protractor guides. */
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
 *
 * This allows easy access to style definitions across the application.
 */
object LabelConfig {
    /** Master toggle for the default visibility of helper labels. */
    const val showLabelsByDefault: Boolean = false

    /** Instance of [TargetBallLabelProperties]. */
    val targetBall = TargetBallLabelProperties()
    /** Instance of [GhostCueBallLabelProperties]. */
    val ghostCueBall = GhostCueBallLabelProperties()
    /** Instance of [ActualCueBallLabelProperties]. */
    val actualCueBall = ActualCueBallLabelProperties()
    /** Instance of [BankingBallLabelProperties]. */
    val bankingBall = BankingBallLabelProperties()
    /** Instance of [ObstacleBallLabelProperties]. */
    val obstacleBall = ObstacleBallLabelProperties()

    /** Instance of [AimingLineLabelProperties]. */
    val aimingLine = AimingLineLabelProperties()
    /** Instance of [ShotGuideLineLabelProperties]. */
    val shotGuideLine = ShotGuideLineLabelProperties()
    /** Instance of [TangentLineLabelProperties]. */
    val tangentLine = TangentLineLabelProperties()
    /** Instance of [AngleGuideLabelProperties]. */
    val angleGuide = AngleGuideLabelProperties()
    /** Instance of [DiamondSystemLabelProperties]. */
    val diamondSystem = DiamondSystemLabelProperties()
    /** Instance of [ProtractorLabelProperties]. */
    val protractor = ProtractorLabelProperties()
}
