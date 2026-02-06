package com.hereliesaz.cuedetat.view.config.ball

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.ui.theme.Mariner
import com.hereliesaz.cuedetat.view.config.base.BallsConfig
import com.hereliesaz.cuedetat.view.config.base.CenterShape

/**
 * Configuration for the visual appearance of the "Actual Cue Ball".
 *
 * This represents the physical cue ball detected by the camera or placed by the user.
 */
data class ActualCueBall(
    /** Label used for identification. */
    override val label: String = "Actual Cue Ball",
    /** Fully opaque. */
    override val opacity: Float = 1.0f,
    /** Width of the glow effect. */
    override val glowWidth: Float = 12f,
    /** Glow color (Mariner Blue). */
    override val glowColor: Color = Mariner,
    /** Stroke width for the ball outline. */
    override val strokeWidth: Float = 6f,
    /** Stroke color (Mariner Blue). */
    override val strokeColor: Color = Mariner,
    /** No additional 2D offset. */
    override val additionalOffset: Float = 0f,
    /** Center marker shape is a simple DOT. */
    override val centerShape: CenterShape = CenterShape.DOT,
    /** Center dot size is 20% of the radius. */
    override val centerSize: Float = 0.2f,
    /** Center dot is White. */
    override val centerColor: Color = Color.White,
    /** No fill color (wireframe/transparent). */
    override val fillColor: Color = Color.Transparent,
    /** No 3D lift. */
    override val additionalOffset3d: Float = 0f,
) : BallsConfig
