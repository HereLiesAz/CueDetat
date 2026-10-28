package com.hereliesaz.cuedetat.domain

/**
 * Decides when the world-anchored AR table has drifted from the real one far enough to put it
 * back, and only then. The felt fit is noisy frame to frame (that is why the preview is not
 * pulled by it); this acts only on a *sure* fit that keeps disagreeing with the projected table,
 * and gives up after [maxReseeds] so it can never fight the user or a table the fit can't see.
 *
 * One instance covers one suspect window: restart it ([reset]) when tracking returns after a
 * loss, a table is placed fresh, or a lock is dropped. Pure Kotlin; single owner (GL thread).
 *
 * @param sureQuality Fit quality ([TableFitQuality]) below which a fit says nothing.
 * @param verifiedAgreement Overlap with the projected table at or above which the table is right.
 * @param disagreeFits Consecutive sure, disagreeing fits needed before a reseed.
 * @param maxReseeds Reseeds allowed per window.
 */
class TableReseedPolicy(
    private val sureQuality: Float = 0.8f,
    private val verifiedAgreement: Float = 0.9f,
    private val disagreeFits: Int = 3,
    private val maxReseeds: Int = 2,
) {
    enum class Verdict { NONE, VERIFIED, RESEED }

    private var disagreeing = 0
    private var reseeds = 0

    fun reset() {
        disagreeing = 0
        reseeds = 0
    }

    /**
     * @param agreement Overlap (0..1) of the projected table with the fit's outline.
     * @param fitQuality [TableFitQuality.of] for this fit.
     * @param userBusy True while the user has just touched the table; nothing is judged then.
     */
    fun onFit(agreement: Float, fitQuality: Float, userBusy: Boolean): Verdict {
        if (fitQuality < sureQuality) return Verdict.NONE
        if (agreement >= verifiedAgreement) {
            disagreeing = 0
            return Verdict.VERIFIED
        }
        if (userBusy) {
            disagreeing = 0
            return Verdict.NONE
        }
        disagreeing++
        if (disagreeing >= disagreeFits && reseeds < maxReseeds) {
            reseeds++
            disagreeing = 0
            return Verdict.RESEED
        }
        return Verdict.NONE
    }
}
