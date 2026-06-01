package com.hereliesaz.cuedetat.domain.advisor

import android.graphics.PointF

/** How the cue reaches the object ball. Phase 1 implements DIRECT; the rest layer on later. */
enum class ShotType { DIRECT, BANK, COMBO, KICK, SAFETY }

/** Coarse strike strength, surfaced to the user as a word. */
enum class Hardness { SOFT, MEDIUM, FIRM, BREAK }

/**
 * A fully-evaluated recommended shot, in logical-plane coordinates (table centered at origin).
 *
 * Phase 1 fills the make-probability fields and a stun (no-spin) recommendation; later phases
 * add position-aware spin, banks/combos/kicks, and safety scoring.
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
)

/**
 * Decoupled, test-friendly input to [ShotAdvisor] — plain geometry, no app state.
 *
 * @param cue cue-ball center (logical).
 * @param targetBalls the player's pottable balls (logical).
 * @param allBalls every ball on the table incl. cue/targets/others (for obstruction).
 * @param pockets pocket centers (logical).
 * @param ballRadius logical ball radius (LOGICAL_BALL_RADIUS).
 * @param tableDiagonal logical diagonal, used to normalize distance penalties.
 */
data class AdvisorInput(
    val cue: PointF,
    val targetBalls: List<PointF>,
    val opponentBalls: List<PointF> = emptyList(), // the other group, for safety scoring
    val allBalls: List<PointF>,
    val pockets: List<PointF>,
    val ballRadius: Float,
    val tableDiagonal: Float,
    /** Table geometry for the cue-leave simulation. When null, spin/position is skipped (stun). */
    val table: com.hereliesaz.cuedetat.view.model.Table? = null,
)
