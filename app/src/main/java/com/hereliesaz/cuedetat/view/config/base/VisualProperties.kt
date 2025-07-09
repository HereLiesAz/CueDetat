package com.hereliesaz.cuedetat.view.config.base

import androidx.compose.ui.graphics.Color

/**
 * Defines the shape of the center marker for a ball.
 */
enum class CenterShape {
    NONE, DOT, CROSSHAIR
}

/**
 * Base interface for all visual configuration objects.
 * Provides common properties for styling rendered elements.
 */
interface VisualConfig {
    val label: String
    val opacity: Float
    val glowWidth: Float
    val glowColor: Color
    val strokeWidth: Float
    val strokeColor: Color
    val additionalOffset: Float // For 2D elements on the logical plane
}

/**
 * An interface for visual configurations specific to ball-like objects.
 */
interface BallConfig : VisualConfig {
    val centerShape: CenterShape
    val centerSize: Float
    val centerColor: Color
    val fillColor: Color
    val additionalOffset3d: Float // For the lifted "ghost" circle
}

/**
 * An interface for visual configurations specific to line-like objects.
 */
interface LineConfig : VisualConfig {
    // Currently no line-specific properties beyond the base VisualConfig.
    // This interface exists for future expansion and type safety.
}

/**
 * An interface for visual configurations specific to table components.
 */
interface TableComponentConfig : VisualConfig {
    val fillColor: Color
}
