// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/state/StateEnums.kt

package com.hereliesaz.cuedetat.view.state

import android.graphics.PointF

enum class TableSize(val feet: Int, val longSideInches: Float, val shortSideInches: Float) {
    SEVEN_FT(7, 78f, 39f),
    EIGHT_FT(8, 88f, 44f),
    NINE_FT(9, 100f, 50f);

    fun next(): TableSize {
        val nextOrdinal = (this.ordinal + 1) % entries.size
        return entries[nextOrdinal]
    }
}

enum class DistanceUnit {
    METRIC, IMPERIAL
}

enum class CvRefinementMethod {
    CONTOUR, HOUGH;

    fun next(): CvRefinementMethod {
        return if (this == CONTOUR) HOUGH else CONTOUR
    }
}

enum class TutorialHighlightElement {
    NONE, TARGET_BALL, GHOST_BALL, CUE_BALL, ZOOM_SLIDER, BANK_BUTTON
}

enum class InteractionMode {
    NONE,
    MOVING_PROTRACTOR_UNIT,
    ROTATING_PROTRACTOR,
    MOVING_ACTUAL_CUE_BALL,
    MOVING_SPIN_CONTROL,
    MOVING_OBSTACLE_BALL,
    AIMING_BANK_SHOT
}

data class SnapCandidate(
    val detectedPoint: PointF,
    val firstSeenTimestamp: Long,
    val isConfirmed: Boolean = false
)