package com.hereliesaz.cuedetat.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.layout

/**
 * A modifier that rotates a composable 90 degrees counter-clockwise to make it vertical.
 *
 * It adjusts the layout constraints so that the rotated component occupies the correct
 * amount of space (swapping width and height).
 * This is commonly used for vertical sliders.
 */
fun Modifier.vertical() = this
    .rotate(-90f)
    .layout { measurable, constraints ->
        // Swap width and height constraints.
        val placeable = measurable.measure(
            constraints.copy(
                minWidth = constraints.minHeight,
                maxWidth = constraints.maxHeight,
                minHeight = constraints.minWidth,
                maxHeight = constraints.maxWidth,
            )
        )
        // Position the rotated content.
        layout(placeable.height, placeable.width) {
            placeable.placeRelative(
                x = -(placeable.width / 2 - placeable.height / 2),
                y = -(placeable.height / 2 - placeable.width / 2)
            )
        }
    }
