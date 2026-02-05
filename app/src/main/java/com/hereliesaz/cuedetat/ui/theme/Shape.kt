package com.hereliesaz.cuedetat.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 Shape definitions for the application.
 * Defines the corner radius for cards, dialogs, buttons, etc.
 */
val Shapes = Shapes(
    // A subtle, almost imperceptible rounding. Like the edges of a well-worn tombstone.
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp)
)
