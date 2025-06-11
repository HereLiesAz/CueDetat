package com.hereliesaz.cuedetat.view.state

import android.graphics.Matrix
import android.graphics.PointF
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme

/**
 * An immutable data class representing the complete state of the protractor overlay.
 *
 * --- OFFICIAL GLOSSARY OF TERMS ---
 * - Protractor Unit: The main aiming tool, consisting of the "Target Ball" and the "Protractor Cue Ball".
 * - Target Ball: The center of the Protractor Unit. The user drags this to move the whole unit.
 * - Protractor Cue Ball: The ball that is always side-by-side with the Target Ball. Its position is derived from the Target Ball's position plus the rotation angle.
 * - Actual Cue Ball: A third, independent ball that can be toggled on/off. The user places this over the real-world cue ball on the table to act as a secondary aiming sight.
 * ---
 * - Shot Line: "the shot the user takes by hitting the cue ball". Extends from the screen bottom OR the Actual Cue Ball, through the Protractor Cue Ball.
 * - Aiming Line: "where the target ball will go upon impact by the cue ball". This is the 0-degree line extending from the Target Ball.
 */
data class OverlayState(
    // --- USER-CONTROLLED PROPERTIES ---
    val zoomFactor: Float = 0.4f,
    val rotationAngle: Float = 0.0f,
    val pitchAngle: Float = 0.0f,
    val areHelpersVisible: Boolean = true,
    val valuesChangedSinceReset: Boolean = false,
    val unitCenterPosition: PointF = PointF(),
    val isActualCueBallVisible: Boolean = false,
    /** The position of the Actual Cue Ball on the LOGICAL 2D plane. This is the source of truth. */
    val logicalActualCueBallPosition: PointF = PointF(),

    // --- VIEW-DERIVED & CALCULATED PROPERTIES ---
    val viewWidth: Int = 0,
    val viewHeight: Int = 0,
    val logicalRadius: Float = 1f,
    val zoomSliderPosition: Float = 50f,
    val targetCircleCenter: PointF = PointF(),
    val cueCircleCenter: PointF = PointF(),

    /** The single matrix for the overall scene perspective tilt, correctly pivoted around the view's center. */
    val pitchMatrix: Matrix = Matrix(),
    /** The inverse of the pitch matrix, used to map screen coordinates back to the logical 2D plane. */
    val inversePitchMatrix: Matrix = Matrix(),

    val hasInverseMatrix: Boolean = false,
    val dynamicColorScheme: ColorScheme = lightColorScheme(),
    val isImpossibleShot: Boolean = false
)