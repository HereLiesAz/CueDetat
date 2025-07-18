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
interface VisualProperties {
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
interface BallsConfig : VisualProperties {
    val centerShape: CenterShape
    val centerSize: Float
    val centerColor: Color
    val fillColor: Color
    val additionalOffset3d: Float // For the lifted "ghost" circle
}

/**
 * An interface for visual configurations specific to line-like objects.
 */
interface LinesConfig : VisualProperties {
    // New properties for the wide obstacle path, with default implementations.
    val obstaclePathColor: Color get() = Color.Transparent
    val obstaclePathOpacity: Float get() = 0f
}

/**
 * An interface for visual configurations specific to table components.
 */
interface TableComponentConfig : VisualProperties {
    val fillColor: Color
}