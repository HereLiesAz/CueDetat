package com.hereliesaz.cuedetat.view.config.ball

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.SulfurDust
import com.hereliesaz.cuedetat.view.config.base.BallsConfig
import com.hereliesaz.cuedetat.view.config.base.CenterShape

/**
 * Configuration for the visual appearance of the "Target Ball" (Protractor Unit).
 *
 * This is the object ball the user intends to hit.
 */
data class TargetBall(
    /** Label used for identification. */
    override val label: String = "Target Ball",
    /** Fully opaque. */
    override val opacity: Float = 1.0f,
    /** Width of the glow effect. */
    override val glowWidth: Float = 12f,
    /** Glow color (Sulfur/Yellow). */
    override val glowColor: Color = SulfurDust,
    /** Stroke width for the ball outline. */
    override val strokeWidth: Float = 6f,
    /** Stroke color (Sulfur/Yellow). */
    override val strokeColor: Color = SulfurDust,
    /** No additional 2D offset. */
    override val additionalOffset: Float = 0f,
    /** Center marker shape is a DOT. */
    override val centerShape: CenterShape = CenterShape.DOT,
    /** Center marker size is 20% of the radius. */
    override val centerSize: Float = 0.2f,
    /** Center marker is White. */
    override val centerColor: Color = Color.White,
    /** No fill color. */
    override val fillColor: Color = Color.Transparent,
    /** No 3D lift. */
    override val additionalOffset3d: Float = 0f,
) : BallsConfig
