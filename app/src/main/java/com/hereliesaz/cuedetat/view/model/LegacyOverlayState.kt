package com.hereliesaz.cuedetat.view.model

// Renamed from OverlayState to LegacyOverlayState to prevent class name collision.
data class LegacyOverlayState(
    val cueBall: Vector3? = null,
    val objectBalls: List<Vector3> = emptyList(),
    val predictedPath: List<Vector3> = emptyList()
)

data class Vector3(val x: Float, val y: Float, val z: Float)
