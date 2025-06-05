package com.hereliesaz.cuedetat.geometry.models

import android.graphics.PointF

/**
 * Represents the screen coordinates and radius for the visual overlays drawn
 * *on top of the actual detected/manual balls* visible through the camera feed.
 * The radii here are intended to be `appState.currentLogicalRadius` for consistent sizing.
 */
data class ActualBallOverlayCoords(
    val actualTargetOverlayPosition: PointF,
    val actualCueOverlayPosition: PointF,
    val actualTargetOverlayRadius: Float,
    val actualCueOverlayRadius: Float
)