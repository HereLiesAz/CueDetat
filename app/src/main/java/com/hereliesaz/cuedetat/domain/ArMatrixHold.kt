package com.hereliesaz.cuedetat.domain

/**
 * Bridges a momentary hole in the AR table matrix so the table does not blink.
 *
 * While ARCore is tracking the camera but the table's anchor is not tracked for a frame or two,
 * the table session has no matrix to give and the overlay used to vanish for exactly that long.
 * The last matrix is held for [holdNanos] instead; past that the table really has gone and the
 * hole passes through, as before.
 *
 * This is not for a camera tracking loss: there the projection is stale while the camera keeps
 * moving, and it is hidden at once (see `ArCoreBackground`). Call [reset] then, so a held matrix is
 * never brought back after a loss.
 *
 * Generic in the matrix type so the policy runs in a JVM test without Android's `Matrix`.
 */
class ArMatrixHold<M : Any>(private val holdNanos: Long = DEFAULT_HOLD_NANOS) {

    sealed interface Decision<out M> {
        /** Send this matrix: the live one, or (with [held]) the last good one standing in for it. */
        data class Publish<M>(val matrix: M, val held: Boolean) : Decision<M>

        /** Nothing to show: send the null that hides the table. */
        data object Clear : Decision<Nothing>
    }

    private var last: M? = null
    private var lastGoodAtNanos = 0L

    /** The decision for this frame's [matrix] (null when the table had none), at time [nowNanos]. */
    fun decide(matrix: M?, nowNanos: Long): Decision<M> {
        if (matrix != null) {
            last = matrix
            lastGoodAtNanos = nowNanos
            return Decision.Publish(matrix, held = false)
        }
        val held = last
        if (held != null && nowNanos - lastGoodAtNanos <= holdNanos) {
            return Decision.Publish(held, held = true)
        }
        last = null
        return Decision.Clear
    }

    /** Forget the held matrix (the camera lost tracking, or the session ended). */
    fun reset() {
        last = null
    }

    companion object {
        /** A quarter of a second: a few frames at 30 fps, too short to show a stale table as if it were live. */
        const val DEFAULT_HOLD_NANOS = 250_000_000L
    }
}
