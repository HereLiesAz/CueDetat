// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/hatemode/HaterState.kt

package com.hereliesaz.cuedetat.ui.hatemode

import androidx.annotation.DrawableRes
import de.chaffic.dynamics.Body

data class HaterState(
    val bodies: List<Body> = emptyList(),
    @DrawableRes val currentAnswer: Int? = null,
    val isAnswerVisible: Boolean = false
)