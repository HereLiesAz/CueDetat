// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/hatemode/HaterEvent.kt

package com.hereliesaz.cuedetat.ui.hatemode

import androidx.compose.ui.geometry.Offset

/**
 * Sealed class defining all events that can occur within the "Hater Mode" feature.
 *
 * Hater Mode is a Magic-8-Ball style toy physics simulation.
 */
sealed class HaterEvent {
    /** Initialize the mode (reset state, shuffle answers). */
    data object EnterHaterMode : HaterEvent()
    /** Device shake detected, trigger the answer reveal animation. */
    data object ShakeDetected : HaterEvent()
    /** User is dragging finger on screen, apply force to the virtual die. */
    data class Dragging(val delta: Offset) : HaterEvent()
    /** User released drag. */
    data object DragEnd : HaterEvent()
    /** Device orientation changed, update gravity vector for physics. */
    data class SensorChanged(val roll: Float, val pitch: Float) : HaterEvent()
}
