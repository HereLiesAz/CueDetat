package com.hereliesaz.cuedetat.ui.state

import androidx.compose.ui.geometry.Offset
import androidx.xr.arcore.Anchor
import com.google.ar.core.Anchor
import com.google.ar.core.Pose

/**
 * Represents the complete, immutable state of the UI at a single point in time.
 */
data class UiState(
    val statusText: String = "Searching for planes...",
    val anchors: List<AppAnchor> = emptyList(),
    val tablePlaced: Boolean = false,
    val shotType: ShotType = ShotType.FOLLOW,
    val spinOffset: Offset = Offset.Zero,
    val cueElevation: Float = 0f,
    val selectedBallId: String? = null
)

/**
 * A wrapper for ARCore Anchors to associate them with application-specific data.
 */
data class AppAnchor(val arAnchor: Anchor, val isTable: Boolean = false)

/**
 * Defines the types of shots the user can select.
 */
enum class ShotType {
    FOLLOW,
    DRAW,
    JUMP,
    BANK
}