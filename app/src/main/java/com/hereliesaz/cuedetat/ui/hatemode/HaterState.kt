package com.hereliesaz.cuedetat.ui.hatemode

import androidx.annotation.DrawableRes

data class HaterState(
    val isHaterVisible: Boolean = false,
    @DrawableRes val currentAnswer: Int? = null,
    val recentlyUsedAnswers: List<Int> = emptyList(),
    val triggerNewAnswer: Boolean = false,
    val orientation: Int = 0,
    val isTriangleVisible: Boolean = false,
    val trianglePosition: Float = 0.5f,
    val isFirstReveal: Boolean = true
)