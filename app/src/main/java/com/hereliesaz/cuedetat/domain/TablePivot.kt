package com.hereliesaz.cuedetat.domain

import com.hereliesaz.cuedetat.domain.TableFrameHomography.Pt
import com.hereliesaz.cuedetat.domain.TableFrameHomography.Vec3
import kotlin.math.hypot

/**
 * Resizing the AR table from a corner instead of from its middle.
 *
 * Standing at a real table, the part of it in view is usually a corner or two and the rails that run
 * from them; the middle of the table (where a size change is normally centred) is off in the felt,
 * so scaling about it pushes the corner you have just lined up away. Scaling about the corner that
 * is in view and nearest keeps it where you put it, and the far edges move to meet the real ones.
 *
 * Pure Kotlin. The corners are the table's four, in the order the app keeps them (TL, TR, BR, BL).
 */
object TablePivot {

    /** A corner this close to the viewport's edge, as a fraction of its short side, counts as outside. */
    const val EDGE_MARGIN = 0.02f

    /**
     * The index of the corner to scale about: of the corners inside the viewport, the one that is
     * nearest the camera; when none is in view, the one nearest the viewport's centre.
     *
     * "Nearest the camera" is read off the screen quad: perspective makes the edges meeting at the
     * near corner the longest, so the sum of a corner's two edge lengths ranks them without needing
     * a depth. Corners are judged on the whole quad, off-screen parts included (the quad is the
     * virtual table, projected, wherever it falls).
     *
     * @param screenQuad the table's corners on screen, in view pixels.
     */
    fun choose(screenQuad: List<Pt>, viewportWidth: Int, viewportHeight: Int): Int {
        require(screenQuad.size == 4) { "a table has four corners" }
        if (viewportWidth <= 0 || viewportHeight <= 0 || screenQuad.any { !it.x.isFinite() || !it.y.isFinite() }) return 0
        val margin = EDGE_MARGIN * minOf(viewportWidth, viewportHeight)
        val inside = screenQuad.indices.filter { i ->
            val p = screenQuad[i]
            p.x >= margin && p.x <= viewportWidth - margin && p.y >= margin && p.y <= viewportHeight - margin
        }
        if (inside.isNotEmpty()) {
            return inside.maxByOrNull { reach(screenQuad, it) } ?: inside.first()
        }
        val cx = viewportWidth / 2f
        val cy = viewportHeight / 2f
        return screenQuad.indices.minByOrNull { hypot(screenQuad[it].x - cx, screenQuad[it].y - cy) } ?: 0
    }

    /** The sum of the lengths of the two edges meeting at corner [i]. */
    private fun reach(q: List<Pt>, i: Int): Float {
        val prev = q[(i + 3) % 4]
        val next = q[(i + 1) % 4]
        val p = q[i]
        return hypot(p.x - prev.x, p.y - prev.y) + hypot(p.x - next.x, p.y - next.y)
    }

    /**
     * How far the table's centre must move for a change of size to leave a corner where it is.
     *
     * Scaling a rectangle by [ratio] about its centre moves each corner by `(ratio - 1)` times its
     * offset from the centre; moving the centre by `-(ratio - 1)` times that offset puts the pivot
     * back. [centerToCorner] is the pivot corner's world offset from the centre at the old size.
     * The vertical part is left alone: a size change never lifts the table.
     */
    fun centerShift(centerToCorner: Vec3, ratio: Float): Vec3 {
        if (!ratio.isFinite() || ratio <= 0f) return Vec3(0f, 0f, 0f)
        val k = 1f - ratio
        return Vec3(centerToCorner.x * k, 0f, centerToCorner.z * k)
    }
}
