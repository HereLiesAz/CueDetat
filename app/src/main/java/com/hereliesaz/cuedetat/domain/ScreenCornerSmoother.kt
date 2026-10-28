package com.hereliesaz.cuedetat.domain

import com.hereliesaz.cuedetat.domain.TableFrameHomography.Pt
import kotlin.math.PI
import kotlin.math.abs

/**
 * Steadies the table's four projected corners frame to frame (a One Euro filter per coordinate).
 *
 * The AR table is pinned to the world, so its screen corners move only as fast as the camera
 * does; what is left at rest is tracking noise, a pixel or two shivering every frame. A fixed
 * low-pass would remove that and make the table swim behind every real movement. The One Euro
 * filter makes its cutoff follow speed instead: slow (or still) motion is filtered hard, fast
 * motion barely at all, so jitter goes and lag doesn't come.
 *
 * Pure Kotlin; no Android types, runs in plain JUnit. Not thread-safe: one owner (the GL thread).
 *
 * @param minCutoffHz Cutoff at rest. Lower is steadier and slower to settle.
 * @param beta How fast the cutoff opens with speed (Hz per pixel/second).
 * @param derivativeCutoffHz Cutoff of the speed estimate itself.
 * @param jumpPx A corner that moves further than this in one frame is a discontinuity (the table
 *   was placed, locked, unlocked or tracking came back), not motion: the filter restarts there
 *   instead of dragging the table across the screen.
 */
class ScreenCornerSmoother(
    private val minCutoffHz: Float = 1.2f,
    private val beta: Float = 0.02f,
    private val derivativeCutoffHz: Float = 1.0f,
    private val jumpPx: Float = 150f,
) {
    private var lastNanos = 0L
    private var x = FloatArray(0)
    private var dx = FloatArray(0)

    /** Forget the history; the next [filter] passes its input through. */
    fun reset() {
        lastNanos = 0L
        x = FloatArray(0)
        dx = FloatArray(0)
    }

    /** [corners] as of [nowNanos] (monotonic), smoothed. Any number of points, same count each call. */
    fun filter(corners: List<Pt>, nowNanos: Long): List<Pt> {
        val n = corners.size * 2
        val dt = (nowNanos - lastNanos) / 1e9f
        val restart = x.size != n || lastNanos == 0L || dt <= 0f || dt > 0.5f ||
            corners.indices.any { i ->
                abs(corners[i].x - x[i * 2]) > jumpPx || abs(corners[i].y - x[i * 2 + 1]) > jumpPx
            }
        lastNanos = nowNanos
        if (restart) {
            x = FloatArray(n) { corners[it / 2].let { p -> if (it % 2 == 0) p.x else p.y } }
            dx = FloatArray(n)
            return corners
        }
        val dAlpha = alpha(derivativeCutoffHz, dt)
        return List(corners.size) { i ->
            Pt(smooth(i * 2, corners[i].x, dt, dAlpha), smooth(i * 2 + 1, corners[i].y, dt, dAlpha))
        }
    }

    private fun smooth(i: Int, value: Float, dt: Float, dAlpha: Float): Float {
        val speed = (value - x[i]) / dt
        dx[i] += dAlpha * (speed - dx[i])
        val cutoff = minCutoffHz + beta * abs(dx[i])
        x[i] += alpha(cutoff, dt) * (value - x[i])
        return x[i]
    }

    /** Smoothing factor for a first-order low-pass with cutoff [cutoffHz] sampled every [dt] s. */
    private fun alpha(cutoffHz: Float, dt: Float): Float {
        val tau = 1f / (2f * PI.toFloat() * cutoffHz)
        return 1f / (1f + tau / dt)
    }
}
