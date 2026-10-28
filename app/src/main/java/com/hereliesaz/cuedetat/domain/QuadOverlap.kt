package com.hereliesaz.cuedetat.domain

import com.hereliesaz.cuedetat.domain.TableFrameHomography.Pt

/**
 * Overlap of two convex polygons on screen (intersection over union), for asking whether the
 * table the AR session projects is where the felt fit says the real one is.
 *
 * Either polygon may be wound either way. Both must be convex (a table seen in perspective is).
 * Pure Kotlin.
 */
object QuadOverlap {

    fun iou(a: List<Pt>, b: List<Pt>): Float {
        if (a.size < 3 || b.size < 3) return 0f
        val pa = positive(a)
        val pb = positive(b)
        val areaA = area(pa)
        val areaB = area(pb)
        if (areaA <= 1e-6f || areaB <= 1e-6f) return 0f
        val inter = area(clip(pa, pb))
        val union = areaA + areaB - inter
        return if (union <= 1e-6f) 0f else (inter / union).coerceIn(0f, 1f)
    }

    /** Signed area; positive for one winding, negative for the other. */
    private fun signedArea(p: List<Pt>): Float {
        var s = 0f
        for (i in p.indices) {
            val q = p[(i + 1) % p.size]
            s += p[i].x * q.y - q.x * p[i].y
        }
        return s / 2f
    }

    private fun area(p: List<Pt>): Float = if (p.size < 3) 0f else kotlin.math.abs(signedArea(p))

    /** The same polygon wound so its signed area is positive. */
    private fun positive(p: List<Pt>): List<Pt> = if (signedArea(p) >= 0f) p else p.reversed()

    /** Sutherland-Hodgman: [subject] clipped to the convex [clip], both positively wound. */
    private fun clip(subject: List<Pt>, clip: List<Pt>): List<Pt> {
        var out = subject
        for (i in clip.indices) {
            if (out.isEmpty()) break
            val a = clip[i]
            val b = clip[(i + 1) % clip.size]
            val input = out
            val next = ArrayList<Pt>(input.size + 2)
            for (j in input.indices) {
                val cur = input[j]
                val prev = input[(j + input.size - 1) % input.size]
                val curIn = side(a, b, cur) >= 0f
                val prevIn = side(a, b, prev) >= 0f
                if (curIn) {
                    if (!prevIn) next.add(intersect(prev, cur, a, b))
                    next.add(cur)
                } else if (prevIn) {
                    next.add(intersect(prev, cur, a, b))
                }
            }
            out = next
        }
        return out
    }

    /** Positive when [p] is on the inside (left, for a positive winding) of the edge a->b. */
    private fun side(a: Pt, b: Pt, p: Pt): Float = (b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x)

    /** Where segment p->q crosses the infinite line a->b. */
    private fun intersect(p: Pt, q: Pt, a: Pt, b: Pt): Pt {
        val sp = side(a, b, p)
        val sq = side(a, b, q)
        val t = sp / (sp - sq)
        return Pt(p.x + (q.x - p.x) * t, p.y + (q.y - p.y) * t)
    }
}
