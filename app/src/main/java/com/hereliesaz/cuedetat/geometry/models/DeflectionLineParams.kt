package com.hereliesaz.cuedetat.geometry.models

data class DeflectionLineParams(
    val cueToTargetDistance: Float,
    val unitPerpendicularX: Float, // Unit vector component
    val unitPerpendicularY: Float, // Unit vector component
    val visualDrawLength: Float
)