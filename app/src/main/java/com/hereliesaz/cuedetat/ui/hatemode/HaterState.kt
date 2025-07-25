package com.hereliesaz.cuedetat.ui.hatemode

import androidx.annotation.DrawableRes
import androidx.compose.ui.geometry.Offset

data class HaterState(
    val isHaterVisible: Boolean = false,
    @DrawableRes val currentAnswer: Int? = null,
    val recentlyUsedAnswers: List<Int> = emptyList(),
    val isFirstReveal: Boolean = true,
    val isCooldownActive: Boolean = false,
    val randomRotation: Float = 0f,
    val randomOffset: Offset = Offset.Zero,
    val gradientStart: Offset = Offset.Zero,
    val gradientEnd: Offset = Offset.Infinite,
    val gravityTargetOffset: Offset = Offset.Zero,
    val touchDrivenOffset: Offset = Offset.Zero,
    val isUserDragging: Boolean = false
)