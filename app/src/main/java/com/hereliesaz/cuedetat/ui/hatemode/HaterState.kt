// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/hatemode/HaterState.kt

package com.hereliesaz.cuedetat.ui.hatemode

import androidx.annotation.DrawableRes
import androidx.compose.ui.geometry.Offset

data class HaterState(
    val isHaterVisible: Boolean = false,
    @DrawableRes val currentAnswer: Int? = null,
    val recentlyUsedAnswers: List<Int> = emptyList(),
    val isFirstReveal: Boolean = true,
    val isCooldownActive: Boolean = false,

    // Physics Properties - The engine is the source of truth. These are for rendering.
    val position: Offset = Offset.Zero,
    val angle: Float = 0f,

    // User Interaction
    val isUserDragging: Boolean = false,
    val dragDelta: Offset = Offset.Zero, // Temporary storage for the drag gesture

    // Appearance Properties
    val randomRotation: Float = 0f,
    val gradientStart: Offset = Offset.Zero,
    val gradientEnd: Offset = Offset.Infinite,
)