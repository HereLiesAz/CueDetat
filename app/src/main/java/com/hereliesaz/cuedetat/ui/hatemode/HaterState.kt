// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/hatemode/HaterState.kt

package com.hereliesaz.cuedetat.ui.hatemode

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path

enum class TriangleState {
    IDLE,
    SUBMERGING,
    EMERGING,
    SETTLING
}

data class HaterState(
    val diePosition: Offset = Offset.Zero,
    val dieAngle: Float = 0f,
    val dieAnimationProgress: Float = 1f,
    val answer: String = "Haters gonna eight.",
    val triangleState: TriangleState = TriangleState.IDLE,
    val diePath: Path? = null,
)