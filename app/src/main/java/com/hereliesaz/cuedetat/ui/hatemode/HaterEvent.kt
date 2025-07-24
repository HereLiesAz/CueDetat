package com.hereliesaz.cuedetat.ui.hatemode

sealed class HaterEvent {
    data object ShowHater : HaterEvent()
    data object HideHater : HaterEvent()
    data class UpdateOrientation(val orientation: Int) : HaterEvent()
}