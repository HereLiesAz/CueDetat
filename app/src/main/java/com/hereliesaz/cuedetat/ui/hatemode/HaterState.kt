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

    // Physics Properties
    val position: Offset = Offset.Zero,
    val velocity: Offset = Offset.Zero,
    val angle: Float = 0f,
    val angularVelocity: Float = 0f,
    val gravity: Offset = Offset.Zero,
    val touchForce: Offset = Offset.Zero,
    val isUserDragging: Boolean = false,


    // Appearance Properties
    val randomRotation: Float = 0f,
    val gradientStart: Offset = Offset.Zero,
    val gradientEnd: Offset = Offset.Infinite,
)