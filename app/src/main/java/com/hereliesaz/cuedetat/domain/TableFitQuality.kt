package com.hereliesaz.cuedetat.domain

/**
 * How far to trust a felt fit, as one number.
 *
 * Overlap with the felt (IoU) alone says the outline sits on green; it doesn't say the fit is
 * *steady*. A fit that jumps between neighbouring answers frame to frame is a guess that happens
 * to score well each time. So overlap is weighted by [stability]: how little the fitted outline
 * moved over the last few fits ([TableFitStabilizer]).
 */
object TableFitQuality {

    /** Share of the score that stability can take away: a fit with none keeps `1 - this`. */
    private const val STABILITY_WEIGHT = 0.4f

    /** 0..1. A perfectly steady fit scores its overlap; a jumpy one up to 40% less. */
    fun of(iou: Float, stability: Float): Float =
        (iou * ((1f - STABILITY_WEIGHT) + STABILITY_WEIGHT * stability.coerceIn(0f, 1f))).coerceIn(0f, 1f)
}
