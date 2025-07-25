package com.hereliesaz.cuedetat.ui.hatemode

import androidx.compose.ui.geometry.Offset

sealed class HaterEvent {
    data object ShowHater : HaterEvent()
    data object HideHater : HaterEvent()
    data class UpdateSensorOffset(val roll: Float, val pitch: Float) : HaterEvent()
    data object DragTriangleStart : HaterEvent()
    data class DragTriangle(val delta: Offset) : HaterEvent()
    data object DragTriangleEnd : HaterEvent()
}