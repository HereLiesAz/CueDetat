// hereliesaz/cuedetat/CueDetat-CueDetatLite/app/src/main/java/com/hereliesaz/cuedetatlite/view/model/Perspective.kt
package com.hereliesaz.cuedetatlite.view.model

import android.graphics.Matrix

/**
 * Holds the transformation matrices for rendering the scene.
 * Making this a data class automatically provides componentN() functions for destructuring.
 */
data class Perspective(
    val pitchMatrix: Matrix,
    val railPitchMatrix: Matrix,
    val inverseMatrix: Matrix,
    val hasInverse: Boolean
)
