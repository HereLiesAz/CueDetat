// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/hatemode/HaterState.kt

package com.hereliesaz.cuedetat.ui.hatemode

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.chaffic.dynamics.Body

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
    val triangleWidth: Dp = 100.dp,
    val triangleHeight: Dp = 86.dp,
    val walls: List<Body> = emptyList()
)