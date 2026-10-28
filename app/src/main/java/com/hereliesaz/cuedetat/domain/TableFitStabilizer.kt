package com.hereliesaz.cuedetat.domain

import com.hereliesaz.cuedetat.domain.TableFrameHomography.Pt
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

/**
 * Steadies the felt fit over time and says how steady it is.
 *
 * The outline reported is the per-coordinate median of the last [window] fits, so a single bad
 * frame (a hand over the felt, a flash of lamp glare) cannot move it. [Stable.stability] is high
 * when those fits agree and low when they scatter. A new fit far from the recent ones is a table
 * that really moved (or a different one): the history restarts rather than averaging the two.
 *
 * Pure Kotlin; single owner.
 *
 * @param window Fits kept.
 * @param moveFraction A fit whose corner is further than this fraction of the outline's diagonal
 *   from the median restarts the history.
 * @param scatterFraction Corner scatter, as a fraction of the diagonal, at which stability is 0.
 */
class TableFitStabilizer(
    private val window: Int = 5,
    private val moveFraction: Float = 0.25f,
    private val scatterFraction: Float = 0.06f,
) {
    /** [quad] is the median outline; [stability] 0..1; [count] fits behind it. */
    data class Stable(val quad: List<Pt>, val stability: Float, val count: Int)

    private val history = ArrayDeque<List<Pt>>()

    fun reset() = history.clear()

    fun add(quad: List<Pt>): Stable {
        if (history.isNotEmpty() && (history.first().size != quad.size || movedFar(quad))) history.clear()
        history.addLast(quad)
        while (history.size > window) history.removeFirst()
        val median = median()
        val diagonal = diagonal(median)
        val scatter = history.maxOf { fit -> fit.indices.maxOf { i -> hypot(fit[i].x - median[i].x, fit[i].y - median[i].y) } }
        val steady = if (diagonal <= 1e-3f) 0f else (1f - scatter / (scatterFraction * diagonal)).coerceIn(0f, 1f)
        // Too few fits to call steady: a lone fit has nothing to agree with.
        val seen = (history.size - 1).toFloat() / (window - 1).coerceAtLeast(1)
        return Stable(median, steady * seen, history.size)
    }

    private fun movedFar(quad: List<Pt>): Boolean {
        val median = median()
        val limit = moveFraction * diagonal(median)
        return quad.indices.any { i -> hypot(quad[i].x - median[i].x, quad[i].y - median[i].y) > limit }
    }

    private fun median(): List<Pt> {
        val n = history.first().size
        return List(n) { i ->
            Pt(
                medianOf(history.map { it[i].x }),
                medianOf(history.map { it[i].y }),
            )
        }
    }

    private fun medianOf(values: List<Float>): Float {
        val s = values.sorted()
        return if (s.size % 2 == 1) s[s.size / 2] else (s[s.size / 2 - 1] + s[s.size / 2]) / 2f
    }

    private fun diagonal(q: List<Pt>): Float {
        var d = 0f
        for (i in q.indices) for (j in i + 1 until q.size) d = max(d, hypot(abs(q[i].x - q[j].x), abs(q[i].y - q[j].y)))
        return d
    }
}
