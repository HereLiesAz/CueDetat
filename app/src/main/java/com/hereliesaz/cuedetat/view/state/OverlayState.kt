package com.hereliesaz.cuedetat.view.state

import android.graphics.Matrix
import android.graphics.PointF

/**
 * An immutable data class representing the complete state of the protractor overlay.
 * This object contains all necessary information for the renderer to draw the scene.
 */
data class OverlayState(
    // User-controlled properties
    val zoomFactor: Float = 0.4f,
    val rotationAngle: Float = 0.0f,
    val pitchAngle: Float = 0.0f,
    val areHelpersVisible: Boolean = true,
    val valuesChangedSinceReset: Boolean = false,
    val isJumpingGhostBallActive: Boolean = false, // New state for the feature

    // View-derived properties
    val viewWidth: Int = 0,
    val viewHeight: Int = 0,
    val targetCircleCenter: PointF = PointF(),
    val cueCircleCenter: PointF = PointF(),
    val logicalRadius: Float = 1f,

    // Calculated matrices for rendering
    val pitchMatrix: Matrix = Matrix(),
    val inversePitchMatrix: Matrix = Matrix(),
    val hasInverseMatrix: Boolean = false,

    // Calculated condition flags for rendering
    val isImpossibleShot: Boolean = false
)