package com.hereliesaz.cuedetat.view.config.ball

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.MutedGray
import com.hereliesaz.cuedetat.view.config.base.BallsConfig
import com.hereliesaz.cuedetat.view.config.base.CenterShape

/**
 * Configuration for the visual appearance of the "Ghost Cue Ball".
 *
 * This represents the predicted position of the cue ball at the moment of impact with the target ball.
 */
data class GhostCueBall(
    /** Label used for identification. */
    override val label: String = "Ghost Cue Ball",
    /** Fully opaque. */
    override val opacity: Float = 1.0f,
    /** Width of the glow effect. */
    override val glowWidth: Float = 12f,
    /** Glow color (Gray). */
    override val glowColor: Color = MutedGray,
    /** Stroke width for the ball outline. */
    override val strokeWidth: Float = 3f,
    /** Stroke color (Gray). */
    override val strokeColor: Color = MutedGray,
    /** No additional 2D offset. */
    override val additionalOffset: Float = 0f,
    /** Center marker shape is a CROSSHAIR. */
    override val centerShape: CenterShape = CenterShape.CROSSHAIR,
    /** Center marker size is 30% of the radius. */
    override val centerSize: Float = 0.3f,
    /** Center marker is White. */
    override val centerColor: Color = Color.White,
    /** No fill color. */
    override val fillColor: Color = Color.Transparent,
    /** Lifted 4 inches (simulated units) above the table to avoid visual z-fighting and indicate it's a projection. */
    override val additionalOffset3d: Float = 4f,
) : BallsConfig
