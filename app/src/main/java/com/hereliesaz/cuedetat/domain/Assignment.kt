package com.hereliesaz.cuedetat.domain

/**
 * Optimal one-to-one matching (the Hungarian / Kuhn-Munkres algorithm), for matching detections
 * to tracks and anchors to detections.
 *
 * Matching each item to its nearest partner in turn is order-dependent: an early item can take a
 * partner a later one needed, leaving that one with nothing even though an assignment existed
 * that gave everyone a partner. This finds the assignment that matches as many as the gate
 * allows, and among those the one with the smallest total distance.
 *
 * Pure Kotlin. O(n^2 m) for n rows and m >= n columns; the inputs here are a few dozen at most.
 */
object Assignment {

    /** Cost of a pair that must not be matched; dominates every real cost. */
    private const val FORBIDDEN = 1e9

    /**
     * Matches rows to columns, pairing only those within [gate] of each other.
     *
     * @param distance Distance between row r and column c; a pair is allowed only when it is
     *   finite and strictly less than [gate].
     * @return for each row, its column, or -1 when it has none (nothing allowed was left for it).
     */
    fun match(rows: Int, cols: Int, gate: Double, distance: (row: Int, col: Int) -> Double): IntArray {
        val result = IntArray(rows) { -1 }
        if (rows == 0 || cols == 0) return result
        val cost = Array(rows) { r ->
            DoubleArray(cols) { c ->
                val d = distance(r, c)
                if (d.isFinite() && d < gate) d else FORBIDDEN
            }
        }
        val assigned = minimize(cost)
        for (r in 0 until rows) {
            val c = assigned[r]
            if (c >= 0 && cost[r][c] < FORBIDDEN) result[r] = c
        }
        return result
    }

    /**
     * The assignment of rows to distinct columns with the least total cost, every row assigned
     * when there are at least as many columns as rows (otherwise every column is used and the
     * surplus rows get -1).
     */
    fun minimize(cost: Array<DoubleArray>): IntArray {
        val rows = cost.size
        if (rows == 0) return IntArray(0)
        val cols = cost[0].size
        if (cols == 0) return IntArray(rows) { -1 }
        if (rows > cols) {
            // Solve the transpose, then read the answer back.
            val transposed = Array(cols) { c -> DoubleArray(rows) { r -> cost[r][c] } }
            val byCol = minimize(transposed)
            val result = IntArray(rows) { -1 }
            for (c in 0 until cols) if (byCol[c] >= 0) result[byCol[c]] = c
            return result
        }
        // Shortest augmenting paths with row/column potentials; 1-based, index 0 is a sentinel.
        val u = DoubleArray(rows + 1)
        val v = DoubleArray(cols + 1)
        val p = IntArray(cols + 1)
        val way = IntArray(cols + 1)
        for (i in 1..rows) {
            p[0] = i
            var j0 = 0
            val minv = DoubleArray(cols + 1) { Double.MAX_VALUE }
            val used = BooleanArray(cols + 1)
            do {
                used[j0] = true
                val i0 = p[j0]
                var delta = Double.MAX_VALUE
                var j1 = 0
                for (j in 1..cols) {
                    if (used[j]) continue
                    val cur = cost[i0 - 1][j - 1] - u[i0] - v[j]
                    if (cur < minv[j]) { minv[j] = cur; way[j] = j0 }
                    if (minv[j] < delta) { delta = minv[j]; j1 = j }
                }
                for (j in 0..cols) {
                    if (used[j]) { u[p[j]] += delta; v[j] -= delta } else minv[j] -= delta
                }
                j0 = j1
            } while (p[j0] != 0)
            do {
                val j1 = way[j0]
                p[j0] = p[j1]
                j0 = j1
            } while (j0 != 0)
        }
        val result = IntArray(rows) { -1 }
        for (j in 1..cols) if (p[j] != 0) result[p[j] - 1] = j - 1
        return result
    }
}
