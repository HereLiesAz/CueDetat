package com.hereliesaz.cuedetat.domain

import com.hereliesaz.cuedetat.domain.TableFrameHomography.Pt
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Steadies the table's four projected corners frame to frame (a One Euro filter on the quad).
 *
 * The AR table is pinned to the world, so its screen corners move only as fast as the camera
 * does; what is left at rest is tracking noise, a pixel or two shivering every frame. A fixed
 * low-pass would remove that and make the table swim behind every real movement. The One Euro
 * filter makes its cutoff follow speed instead: slow (or still) motion is filtered hard, fast
 * motion barely at all, so jitter goes and lag doesn't come.
 *
 * **What changed from the per-coordinate filter.** The speed that opens the cutoff was estimated from
 * the noisy frame-to-frame difference, so at rest the estimate itself was a few tens of pixels a
 * second of pure noise, which opened the cutoff to about 1.4 Hz and let a third of the noise through
 * (0.36 px for 1 px in). Now:
 *  - One speed for the whole quad (the mean of the four corners'), and so one cutoff: the four
 *    corners are filtered alike and the table keeps its shape, where eight independent cutoffs
 *    could pull a corner ahead of its neighbours.
 *  - A noise floor: the first [noiseFloorPxPerSec] of estimated speed count as nothing, so a still
 *    camera really gets the [minCutoffHz] filter (0.21 px left instead of 0.37), and a moving one
 *    opens up from there at [beta].
 *  - The published quad holds still while every corner is within [holdPx] of the last published one.
 *    The homography built from it is then equal frame to frame, and the pipeline downstream can see
 *    that nothing moved.
 *
 * Measured on a synthetic quad at 30 fps with 1 px of noise per coordinate (`tools/jitter_sweeps.py`),
 * against the filter this replaced: still, 0.22 px residual against 0.36; onset of a 1000 px/s pan, 4 px
 * worst lag against 12.5; steady lag at 1000 px/s, 2.7 px against 6.4; a 30 px/s creep, 2.1 px against
 * 1.7 (the one place it is slightly slower). The lag in motion is the same at 15, 30, 60 and 120 fps;
 * the residual at rest falls with the frame rate (0.33 at 15 fps, 0.14 at 120), as it must, because
 * more samples of white noise are averaged in the same time.
 *
 * Pure Kotlin; no Android types, runs in plain JUnit. Not thread-safe: one owner (the GL thread).
 *
 * @param minCutoffHz Cutoff at rest. Lower is steadier and slower to settle.
 * @param beta How fast the cutoff opens with speed beyond the floor (Hz per pixel/second).
 * @param derivativeCutoffHz Cutoff of the speed estimate itself.
 * @param noiseFloorPxPerSec Estimated speed below this is taken as noise.
 * @param holdPx The published quad does not move until some corner has moved this far from it.
 * @param maxCutoffHz The cutoff never opens past this (a flick still passes with a few ms of lag).
 * @param jumpPx A corner that moves further than this in one frame is a discontinuity (the table
 *   was placed, locked, unlocked or tracking came back), not motion: the filter restarts there
 *   instead of dragging the table across the screen.
 */
class ScreenCornerSmoother(
    private val minCutoffHz: Float = 0.4f,
    private val beta: Float = 0.1f,
    private val derivativeCutoffHz: Float = 1.5f,
    private val noiseFloorPxPerSec: Float = 12f,
    private val holdPx: Float = 0.15f,
    private val maxCutoffHz: Float = 60f,
    private val jumpPx: Float = 150f,
) {
    private var lastNanos = 0L
    private var x = FloatArray(0)
    private var dx = FloatArray(0)
    private var previousRaw = FloatArray(0)
    private var published: List<Pt> = emptyList()

    /** Forget the history; the next [filter] passes its input through. */
    fun reset() {
        lastNanos = 0L
        x = FloatArray(0)
        dx = FloatArray(0)
        previousRaw = FloatArray(0)
        published = emptyList()
    }

    /** [corners] as of [nowNanos] (monotonic), smoothed. Any number of points, same count each call. */
    fun filter(corners: List<Pt>, nowNanos: Long): List<Pt> {
        val n = corners.size * 2
        val dt = (nowNanos - lastNanos) / 1e9f
        // The same frame again (ARCore hands one back when the display runs faster than the camera) is
        // not a new sample: nothing moved, and restarting here would publish its raw noise every other call.
        if (x.size == n && lastNanos != 0L && dt <= 0f) return published
        val restart = x.size != n || lastNanos == 0L || dt > 0.5f ||
            corners.indices.any { i ->
                abs(corners[i].x - x[i * 2]) > jumpPx || abs(corners[i].y - x[i * 2 + 1]) > jumpPx
            }
        lastNanos = nowNanos
        if (restart) {
            x = FloatArray(n) { corners[it / 2].let { p -> if (it % 2 == 0) p.x else p.y } }
            previousRaw = x.copyOf()
            dx = FloatArray(n)
            published = corners
            return corners
        }

        val dAlpha = alpha(derivativeCutoffHz, dt)
        for (i in 0 until n) {
            val value = if (i % 2 == 0) corners[i / 2].x else corners[i / 2].y
            // Speed is the frame-to-frame change of the *raw* corner. (It used to be the filter's lag
            // over dt, which is a different number at every frame rate: the same creep read faster at
            // 15 fps than at 120, and the noise floor meant something different on each.)
            dx[i] += dAlpha * ((value - previousRaw[i]) / dt - dx[i])
            previousRaw[i] = value
        }

        // One speed, and so one cutoff, for the whole quad.
        var speed = 0f
        for (c in corners.indices) speed += hypot(dx[c * 2], dx[c * 2 + 1])
        speed /= corners.size
        val cutoff = (minCutoffHz + beta * (speed - noiseFloorPxPerSec).coerceAtLeast(0f))
            .coerceAtMost(maxCutoffHz)
        val a = alpha(cutoff, dt)
        for (i in 0 until n) {
            val value = if (i % 2 == 0) corners[i / 2].x else corners[i / 2].y
            x[i] += a * (value - x[i])
        }

        val smoothed = List(corners.size) { Pt(x[it * 2], x[it * 2 + 1]) }
        val moved = smoothed.indices.any { i ->
            hypot(smoothed[i].x - published[i].x, smoothed[i].y - published[i].y) >= holdPx
        }
        if (moved) published = smoothed
        return published
    }

    /** Smoothing factor for a first-order low-pass with cutoff [cutoffHz] sampled every [dt] s. */
    private fun alpha(cutoffHz: Float, dt: Float): Float {
        val tau = 1f / (2f * PI.toFloat() * cutoffHz)
        return 1f / (1f + tau / dt)
    }
}
