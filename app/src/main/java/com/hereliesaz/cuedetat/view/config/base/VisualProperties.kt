package com.hereliesaz.cuedetat.view.config.base

import androidx.compose.ui.graphics.Color

/**
 * Defines the shape of the center marker for a ball.
 *
 * Used to differentiate between different types of balls visually.
 */
enum class CenterShape {
    /** No center marker is drawn. */
    NONE,
    /** A simple solid dot is drawn at the center. */
    DOT,
    /** A crosshair (plus sign) is drawn at the center. */
    CROSSHAIR
}

/**
 * Base interface for all visual configuration objects.
 *
 * Provides common properties for styling rendered elements such as opacity,
 * glow effects, stroke styles, and labeling.
 */
interface VisualProperties {
    /** The human-readable label for this element (used in debug or UI). */
    val label: String
    /** The opacity (alpha) of the element, from 0.0 to 1.0. */
    val opacity: Float
    /** The width of the outer glow effect in pixels. Set to 0 to disable. */
    val glowWidth: Float
    /** The color of the outer glow effect. */
    val glowColor: Color
    /** The width of the main stroke line in pixels. */
    val strokeWidth: Float
    /** The color of the main stroke. */
    val strokeColor: Color
    /** An additional 2D offset applied to the element's position on the logical plane. */
    val additionalOffset: Float
}

/**
 * An interface for visual configurations specific to ball-like objects.
 *
 * Extends [VisualProperties] with properties unique to rendering spheres/circles,
 * such as center markers and fill colors.
 */
interface BallsConfig : VisualProperties {
    /** The shape of the marker drawn at the ball's center. */
    val centerShape: CenterShape
    /** The size of the center marker relative to the ball's radius (0.0 to 1.0). */
    val centerSize: Float
    /** The color of the center marker. */
    val centerColor: Color
    /** The fill color of the ball. Use Transparent for wireframe style. */
    val fillColor: Color
    /** An additional 3D offset (Z-axis) to "lift" the ghost representation above the table. */
    val additionalOffset3d: Float
}

/**
 * An interface for visual configurations specific to line-like objects.
 *
 * Currently acts as a marker interface extending [VisualProperties] for type safety
 * and future expansion of line-specific features (e.g., dash patterns).
 */
interface LinesConfig : VisualProperties {
    // Currently no line-specific properties beyond the base VisualProperties.
    // This interface exists for future expansion and type safety.
}

/**
 * An interface for visual configurations specific to table components.
 *
 * Extends [VisualProperties] to include fill properties for table surfaces or rails.
 */
interface TableComponentConfig : VisualProperties {
    /** The fill color of the table component. */
    val fillColor: Color
}
