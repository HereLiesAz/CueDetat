package com.hereliesaz.cuedetat.view.config.ball

import androidx.compose.ui.graphics.Color
import com.hereliesaz.cuedetat.view.config.base.BallsConfig
import com.hereliesaz.cuedetat.view.config.base.CenterShape

/**
 * Configuration for the visual appearance of "Obstacle Balls".
 *
 * These represent other balls on the table that need to be avoided.
 */
data class ObstacleBall(
    /** Label used for identification. */
    override val label: String = "Obstacle Ball",
    /** Semi-transparent (40% opacity) to distinguish from active balls. */
    override val opacity: Float = 0.4f,
    /** No glow effect. */
    override val glowWidth: Float = 0f,
    /** Transparent glow color. */
    override val glowColor: Color = Color.Transparent,
    /** Stroke width for the ball outline. */
    override val strokeWidth: Float = 4f,
    /** Stroke color (White). */
    override val strokeColor: Color = Color.White,
    /** No additional 2D offset. */
    override val additionalOffset: Float = 0f,
    /** No center marker. */
    override val centerShape: CenterShape = CenterShape.NONE,
    /** Center marker size (irrelevant). */
    override val centerSize: Float = 0f,
    /** Center marker color (irrelevant). */
    override val centerColor: Color = Color.Transparent,
    /** Filled with semi-transparent black to dim the area behind them. */
    override val fillColor: Color = Color.Black.copy(alpha = 0.5f),
    /** No 3D lift. */
    override val additionalOffset3d: Float = 0f,
) : BallsConfig
