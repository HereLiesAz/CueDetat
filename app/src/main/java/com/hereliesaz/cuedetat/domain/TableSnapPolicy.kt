package com.hereliesaz.cuedetat.domain

/**
 * How strongly the table snaps to the felt, by how sure the fit is. Three degrees:
 *
 * - **Suggest** ([GHOST_MIN_IOU]): a ghost outline of the fit is drawn; nothing moves.
 * - **Lock** ([LOCK_MIN_IOU]): tapping Lock uses the fit rather than the user's placement.
 * - **Pull** ([PULL_MIN_IOU], see [TablePull]): the table drifts toward the (smoothed) fit by
 *   [PULL_ALPHA] per [REFERENCE_INTERVAL_MS], but only once the user has left it alone for
 *   [PULL_QUIET_MS], so it never fights a hand.
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

    /** The interval the per-fit constants below were chosen for: `TABLE_FIT_INTERVAL_MS` in VisionRepository. */
    const val REFERENCE_INTERVAL_MS = 300L

    /** Share of the way a fit's pose is taken into the smoothed target, per reference interval. */
    const val TARGET_ALPHA = 0.3f

    /** A gap between fits longer than this means the pull was not running: start the target afresh. */
    const val MAX_FIT_GAP_MS = 2_000L

    /** The pull starts when the smoothed target is further than this from the table: degrees, pixels, ratio. */
    const val ENTER_ROTATION_DEG = 0.3f
    const val ENTER_OFFSET_PX = 1.5f
    const val ENTER_ZOOM = 0.003f

    /** ...and stops when it is nearer than this on every axis, so it cannot chatter at one boundary. */
    const val EXIT_ROTATION_DEG = 0.1f
    const val EXIT_OFFSET_PX = 0.5f
    const val EXIT_ZOOM = 0.001f

    /**
     * The pull of the table toward the felt fit, one instance per camera session. It is stateful
     * because three things the old stateless step could not do need a memory:
     *
     *  - **A smoothed target.** The fit is raw (the quad is steadied by [TableFitStabilizer], the pose
     *    is not), a few pixels of noise from fit to fit; the pull chased every one of them. The target
     *    is an exponential average of the fits, so noise is averaged before the table moves toward it.
     *  - **Hysteresis.** Start at [ENTER_OFFSET_PX] and friends, stop at [EXIT_OFFSET_PX] and friends. A
     *    single threshold leaves the table taking a step every other fit when the error sits on it.
     *  - **Time, not count.** Both the target's average and the pull's share are taken from the time
     *    since the last fit (`1 - exp(-dt / tau)`), with the constants unchanged at the reference
     *    interval, so a fit that arrives late (frames skipped, a slow phone) moves the table by the
     *    same amount of *time* as one that arrives on schedule.
     *
     * On a synthetic fit with 3 px of noise: the same 5.8 s to settle, 0.62 px of wander in place of 0.80,
     * and the table steps on 37% of fits where it stepped on 75% (each step is a full recompute).
     *
     * Pure Kotlin; runs in plain JUnit.
     */
    class TablePull {
        private var target: Pose? = null
        private var engaged = false
        private var lastFitMs = 0L

        /** Forget the target and stop pulling (the user touched the table, or the fit is no longer sure). */
        fun release() {
            target = null
            engaged = false
            lastFitMs = 0L
        }

        /**
         * One step of the pull from [current] toward [fit] (taken as the pose of the felt at [nowMs]), or
         * null when it should not act: the fit is not sure enough, the user touched the table within
         * [PULL_QUIET_MS], or the table is already as close as the hysteresis asks.
         */
        fun step(current: Pose, fit: Pose, iou: Float, msSinceUserAdjust: Long, nowMs: Long): Pose? {
            if (iou < PULL_MIN_IOU || msSinceUserAdjust < PULL_QUIET_MS) {
                release()
                return null
            }
            val gap = nowMs - lastFitMs
            val previous = target
            val dt = if (previous == null || lastFitMs == 0L || gap <= 0L || gap > MAX_FIT_GAP_MS) {
                REFERENCE_INTERVAL_MS
            } else gap
            lastFitMs = nowMs

            val smoothed = if (previous == null || gap > MAX_FIT_GAP_MS) fit else blend(previous, fit, share(TARGET_ALPHA, dt))
            target = smoothed

            val dRot = TableOrientationLearner.normalize180(smoothed.rotationDeg - current.rotationDeg)
            val dx = smoothed.offsetX - current.offsetX
            val dy = smoothed.offsetY - current.offsetY
            val dz = smoothed.zoom / current.zoom
            val offsetError = kotlin.math.max(kotlin.math.abs(dx), kotlin.math.abs(dy))
            val zoomError = kotlin.math.abs(dz - 1f)

            if (!engaged) {
                if (kotlin.math.abs(dRot) > ENTER_ROTATION_DEG || offsetError > ENTER_OFFSET_PX || zoomError > ENTER_ZOOM) {
                    engaged = true
                } else return null
            } else if (kotlin.math.abs(dRot) < EXIT_ROTATION_DEG && offsetError < EXIT_OFFSET_PX && zoomError < EXIT_ZOOM) {
                engaged = false
                return null
            }

            val a = share(PULL_ALPHA, dt).coerceAtMost(MAX_PULL_SHARE)
            return Pose(
                current.offsetX + dx * a,
                current.offsetY + dy * a,
                // Shortest way round, modulo the table's half-turn symmetry.
                current.rotationDeg + dRot * a,
                // Geometric step: zoom is a ratio.
                current.zoom * Math.pow(dz.toDouble(), a.toDouble()).toFloat(),
            )
        }

        /** [from] moved the share [a] of the way to [to]: offsets linearly, rotation the short way, zoom as a ratio. */
        private fun blend(from: Pose, to: Pose, a: Float) = Pose(
            from.offsetX + (to.offsetX - from.offsetX) * a,
            from.offsetY + (to.offsetY - from.offsetY) * a,
            from.rotationDeg + TableOrientationLearner.normalize180(to.rotationDeg - from.rotationDeg) * a,
            from.zoom * Math.pow((to.zoom / from.zoom).toDouble(), a.toDouble()).toFloat(),
        )

        /**
         * The share taken in [dtMs] by a first-order filter that takes [perReference] in
         * [REFERENCE_INTERVAL_MS]: `1 - (1 - perReference) ^ (dt / reference)`, which is `1 - exp(-dt / tau)`
         * with `tau = -reference / ln(1 - perReference)`.
         */
        private fun share(perReference: Float, dtMs: Long): Float =
            (1.0 - Math.pow((1.0 - perReference).toDouble(), dtMs.toDouble() / REFERENCE_INTERVAL_MS)).toFloat()
    }

    /** However late a fit is, one pull step takes no more than this of the error. */
    const val MAX_PULL_SHARE = 0.5f
}
