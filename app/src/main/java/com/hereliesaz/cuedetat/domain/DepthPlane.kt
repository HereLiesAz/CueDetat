package com.hereliesaz.cuedetat.domain

/**
 * Represents a detected table surface plane from the ARCore Depth API.
 *
 * @param distanceMeters  Median distance from the camera to the table surface (metres).
 * @param confidence      0..1. High confidence means a tight depth distribution → flat surface.
 * @param capability      Which depth source produced this result.
 */
data class DepthPlane(
    val distanceMeters: Float,
    val confidence: Float,
    val capability: DepthCapability,
)
