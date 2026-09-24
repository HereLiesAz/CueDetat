package com.hereliesaz.cuedetat.domain

/**
 * How strongly the table snaps to the felt, by how sure the fit is. Three degrees:
 *
 * - **Suggest** ([GHOST_MIN_IOU]): a ghost outline of the fit is drawn; nothing moves.
 * - **Lock** ([LOCK_MIN_IOU]): tapping Lock uses the fit rather than the user's placement.
 * - **Pull** ([PULL_MIN_IOU]): the table drifts toward the fit by [PULL_ALPHA] per fit, but only
 *   once the user has left it alone for [PULL_QUIET_MS], so it never fights a hand.
 *
 * Plus two uses of memory: a remembered orientation trusted to [PRIOR_APPLY_CONFIDENCE] seeds the
 * table when the camera comes on, and a very good fit ([RECORD_MIN_IOU]) is remembered at most
 * every [RECORD_INTERVAL_MS].
 *
 * Pure Kotlin; runs in plain JUnit.
 */
object TableSnapPolicy {

    const val GHOST_MIN_IOU = 0.6f
    const val LOCK_MIN_IOU = 0.7f
    const val PULL_MIN_IOU = 0.85f
    const val PULL_ALPHA = 0.15f
    const val PULL_QUIET_MS = 1500L
    const val PRIOR_APPLY_CONFIDENCE = 0.3f
    const val RECORD_MIN_IOU = 0.9f
    const val RECORD_INTERVAL_MS = 60_000L

    /** Pose as the state stores it. */
    data class Pose(val offsetX: Float, val offsetY: Float, val rotationDeg: Float, val zoom: Float)

    /**
     * One step of the pull from [current] toward [target], or null when the pull should not act
     * (fit not sure enough, the user touched the table recently, or already there).
     */
    fun pullStep(current: Pose, target: Pose, iou: Float, msSinceUserAdjust: Long): Pose? {
        if (iou < PULL_MIN_IOU || msSinceUserAdjust < PULL_QUIET_MS) return null
        val dRot = TableOrientationLearner.normalize180(target.rotationDeg - current.rotationDeg)
        val dx = target.offsetX - current.offsetX
        val dy = target.offsetY - current.offsetY
        val dz = target.zoom / current.zoom
        if (kotlin.math.abs(dRot) < 0.2f && kotlin.math.abs(dx) < 1f && kotlin.math.abs(dy) < 1f &&
            kotlin.math.abs(dz - 1f) < 0.002f
        ) return null
        return Pose(
            current.offsetX + dx * PULL_ALPHA,
            current.offsetY + dy * PULL_ALPHA,
            // Shortest way round, modulo the table's half-turn symmetry.
            current.rotationDeg + dRot * PULL_ALPHA,
            // Geometric step: zoom is a ratio.
            current.zoom * Math.pow(dz.toDouble(), PULL_ALPHA.toDouble()).toFloat(),
        )
    }
}
