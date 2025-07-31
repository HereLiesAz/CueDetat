
package com.hereliesaz.cuedetat.ui.hatemode

import androidx.compose.ui.geometry.Offset

enum class TriangleState {
    IDLE,
    SUBMERGING,
    EMERGING,
    SETTLING
}

data class HaterState(
    val diePosition: Offset = Offset.Zero,
    val dieAngle: Float = 0f,
    val particles: List<Offset> = emptyList(),
    val answer: String = "Haters gonna eight.",
    val triangleState: TriangleState = TriangleState.IDLE,
)