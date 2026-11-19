package com.hereliesaz.cuedetat.core.advisor

import com.hereliesaz.cuedetat.core.geometry.BallSpec
import com.hereliesaz.cuedetat.core.units.Angle
import com.hereliesaz.cuedetat.core.units.Length
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * The advisor's make-probability, in one place so every kind of shot uses the same model.
 *
 * The *structure* is derived: a pocket of effective width `w`, with the object ball's path `d` long,
 * accepts a spread of departure directions of `(w - ball) / (2d)` either side; an aiming error at the
 * cue is magnified at each ball-to-ball contact (`distance / 2R`, and `1 / cos(cut)` for how fast the
 * contact point sweeps across the ball as the cut opens), so the precision the shot demands of the cue
 * is the acceptance divided by the product of those magnifications; run through a normal distribution
 * of the player's aiming error ([aimingSigma]) that is a probability.
 *
 * What is *not* derived, and is a placeholder: [aimingSigma] (a plausible club-level spread, not fitted
 * to any recorded outcome), the thin-cut cap of 12.5x, and [CUSHION_PRECISION], the price of each
 * cushion. The output is a model probability, not a measured one.
 */
internal object PrecisionModel {

    /**
     * One ball-to-ball contact in the chain from the cue to the ball that drops: how far the striking
     * ball travelled to reach it (centre to centre) and the cut angle there. A direct pot has one; a
     * combination has two.
     */
    data class Leg(val distance: Length, val cutAngle: Angle)

    /**
     * Each cushion on the way multiplies the precision the shot demands by this (so a bank needs
     * twice the precision of the same path unbanked). Uncalibrated placeholder.
     */
    const val CUSHION_PRECISION = 0.5

    /** Cut beyond which the thin-cut penalty stops growing: `cos(cut) >= 0.08`, about 85 degrees. */
    private const val MIN_COS_CUT = 0.08

    /**
     * @param pocketEffectiveWidth the width the pocket presents to the final ball's approach.
     * @param finalPathLength how far the ball that drops travels from its last contact to the pocket,
     *   across any cushions on the way (unfolded).
     * @param legs the contacts, cue-first.
     * @param cushions how many cushions the shot uses.
     */
    fun makeProbability(
        ball: BallSpec,
        aimingSigma: Angle,
        pocketEffectiveWidth: Length,
        finalPathLength: Length,
        legs: List<Leg>,
        cushions: Int = 0,
    ): Double {
        val clearance = pocketEffectiveWidth.meters - ball.diameter.meters
        if (clearance <= 0.0 || finalPathLength.meters <= 0.0) return 0.0

        // Angular spread of directions the pocket still accepts.
        val acceptance = clearance / (2.0 * finalPathLength.meters)

        var magnification = 1.0
        for (leg in legs) {
            val amplification = leg.distance.meters / (2.0 * ball.radius.meters)
            val thinCutPenalty = 1.0 / cos(leg.cutAngle.radians).coerceAtLeast(MIN_COS_CUT)
            magnification *= amplification * thinCutPenalty
        }

        val requiredPrecision = acceptance / magnification * CUSHION_PRECISION.pow(cushions)
        return normalCdfTwoSided(requiredPrecision / aimingSigma.radians)
    }

    /** `P(|Z| < z)` for a standard normal: the fraction of attempts that land. */
    private fun normalCdfTwoSided(z: Double): Double {
        if (z <= 0.0) return 0.0
        return erf(z / sqrt(2.0)).coerceIn(0.0, 1.0)
    }

    /** Abramowitz & Stegun 7.1.26. Max absolute error 1.5e-7. */
    private fun erf(x: Double): Double {
        val sign = if (x < 0) -1.0 else 1.0
        val a = abs(x)
        val t = 1.0 / (1.0 + 0.3275911 * a)
        val y = 1.0 - (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t - 0.284496736) * t + 0.254829592) * t * exp(-a * a)
        return sign * y
    }
}
