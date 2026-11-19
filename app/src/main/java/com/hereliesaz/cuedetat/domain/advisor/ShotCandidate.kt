package com.hereliesaz.cuedetat.domain.advisor

import android.graphics.PointF

/** How the cue reaches the object ball. [CoreShotAdvice] produces all but SAFETY (not yet planned by the core). */
enum class ShotType { DIRECT, BANK, COMBO, KICK, SAFETY }

/** Coarse strike strength, surfaced to the user as a word. */
enum class Hardness { SOFT, MEDIUM, FIRM, BREAK }

/**
 * A fully-evaluated recommended shot, in logical-plane coordinates (table centered at origin,
 * logical units — not inches; ≈22.2 units per inch).
 *
 * Make-probability is always filled. [CoreShotAdvice] leaves spin at centre ball and [positionScore] and
 * [cueLeavePath] empty: the core has no post-contact cue path yet (no cue-leave, no safeties).
 */
data class RecommendedShot(
    val type: ShotType,
    val targetPos: PointF,
    val pocketPos: PointF?,        // null only for SAFETY
    val ghostCuePos: PointF,       // where to aim the cue (ghost-ball center)
    val cutAngleDeg: Float,
    val hardness: Hardness,
    val spin: PointF,              // recommended english/draw offset, -1..1; (0,0) = center/stun
    val makeProbability: Float,    // 0..1
    val positionScore: Float = 0f, // 0..1, quality of the resulting cue leave (Phase 2)
    val confidence: Float,         // 0..1, gates whether we surface anything
    val shotPath: List<PointF>,    // polyline for rendering: cue → ghost → target → pocket (+ banks)
    val cueLeavePath: List<PointF> = emptyList(), // predicted cue path after contact (Phase 2)
    /**
     * For KICK shots only: the rail bank point the cue caroms off immediately before reaching
     * [ghostCuePos]. The final approach into contact runs `approachOrigin → ghostCuePos`, not
     * `cue → ghostCuePos` (which is blocked — that's why a kick was needed at all). Null for every
     * other shot type, where the cue's approach is the straight line from the cue ball itself.
     */
    val approachOrigin: PointF? = null,
)
