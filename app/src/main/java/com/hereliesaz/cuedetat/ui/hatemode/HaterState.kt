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
 * @property dieScale Scale multiplier of the die.
 * @property answerIndex Index into [HaterResponses.allAnswers] for the text displayed on the die.
 * @property triangleState The current phase of the reveal animation.
 */
data class HaterState(
    val diePosition: Offset = Offset.Zero,
    val dieAngle: Float = 0f,
    val dieScale: Float = 0f,
    val rockAngleX: Float = 0f,   // 3-D perspective tilt around X axis (degrees)
    val rockAngleY: Float = 0f,   // 3-D perspective tilt around Y axis (degrees)
    val answerIndex: Int = 0,
    val triangleState: TriangleState = TriangleState.IDLE,
)
