// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/hatemode/HaterEvent.kt

package com.hereliesaz.cuedetat.ui.hatemode

import androidx.compose.ui.geometry.Offset

sealed class HaterEvent {
    data object EnterHaterMode : HaterEvent()
    data object ShakeDetected : HaterEvent()
    data class Dragging(val delta: Offset) : HaterEvent()
    data object DragEnd : HaterEvent()
    data class SensorChanged(val roll: Float, val pitch: Float) : HaterEvent()
}