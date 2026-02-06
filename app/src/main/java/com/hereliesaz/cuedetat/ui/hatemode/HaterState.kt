// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/hatemode/HaterState.kt

package com.hereliesaz.cuedetat.ui.hatemode

import androidx.compose.ui.geometry.Offset

/**
 * State representing the animation phase of the 8-ball reveal.
 */
enum class TriangleState {
    IDLE,
    SUBMERGING, // Die fading into the darkness.
    EMERGING,   // Die appearing from the darkness.
    SETTLING    // Die floating but stable.
}

/**
 * Data class holding the visual state of the Hater Mode screen.
 *
 * @property diePosition Current (x,y) offset of the die from the screen center.
 * @property dieAngle Current rotation of the die in degrees.
 * @property particles List of particle positions for the fluid effect.
 * @property answer The text currently displayed on the die.
 * @property triangleState The current phase of the reveal animation.
 */
data class HaterState(
    val diePosition: Offset = Offset.Zero,
    val dieAngle: Float = 0f,
    val particles: List<Offset> = emptyList(),
    val answer: String = "Haters gonna eight.",
    val triangleState: TriangleState = TriangleState.IDLE,
)
