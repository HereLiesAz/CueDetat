package com.hereliesaz.cuedetat.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.abs

class AssignmentTest {

    /** 1-D positions: the distance between row r and column c. */
    private fun match(rows: DoubleArray, cols: DoubleArray, gate: Double) =
        Assignment.match(rows.size, cols.size, gate) { r, c -> abs(rows[r] - cols[c]) }

    @Test
    fun `an early item does not take the partner a later one needed`() {
        // Columns at 0 and 20, rows at 10 and -5, gate 25. Row 0 is 10 from both columns; taking
        // column 0 leaves row 1 (25 from column 20, not under the gate) with nothing. The
        // assignment that matches both is row 0 -> column 1 (10) and row 1 -> column 0 (5).
        assertArrayEquals(intArrayOf(1, 0), match(doubleArrayOf(10.0, -5.0), doubleArrayOf(0.0, 20.0), 25.0))
    }

    @Test
    fun `among full matchings the shorter total wins`() {
        // Rows 0 and 10, columns 1 and 11: 0->1, 10->11 totals 2; the crossed pairing totals 20.
        assertArrayEquals(intArrayOf(0, 1), match(doubleArrayOf(0.0, 10.0), doubleArrayOf(1.0, 11.0), 50.0))
    }

    @Test
    fun `a pair at or beyond the gate is never matched`() {
        assertArrayEquals(intArrayOf(-1), match(doubleArrayOf(0.0), doubleArrayOf(30.0), 30.0))
        assertArrayEquals(intArrayOf(0), match(doubleArrayOf(0.0), doubleArrayOf(29.9), 30.0))
    }

    @Test
    fun `surplus rows go unmatched and the closer row keeps the column`() {
        val m = match(doubleArrayOf(0.0, 4.0, 100.0), doubleArrayOf(3.0), 50.0)
        assertEquals(listOf(-1, 0, -1), m.toList())
    }

    @Test
    fun `surplus columns are simply left over`() {
        assertArrayEquals(intArrayOf(1), match(doubleArrayOf(10.0), doubleArrayOf(0.0, 9.0, 40.0), 50.0))
    }

    @Test
    fun `nothing in, nothing out`() {
        assertArrayEquals(IntArray(0), Assignment.match(0, 3, 10.0) { _, _ -> 0.0 })
        assertArrayEquals(intArrayOf(-1, -1), Assignment.match(2, 0, 10.0) { _, _ -> 0.0 })
    }

    @Test
    fun `minimize finds the optimum of a known matrix`() {
        // Independent reference by enumeration: permutations of 3 columns, the cheapest total of
        // [[4,1,3],[2,0,5],[3,2,2]] is 1 + 2 + 2 = 5 (rows 0,1,2 -> cols 1,0,2).
        val cost = arrayOf(doubleArrayOf(4.0, 1.0, 3.0), doubleArrayOf(2.0, 0.0, 5.0), doubleArrayOf(3.0, 2.0, 2.0))
        assertArrayEquals(intArrayOf(1, 0, 2), Assignment.minimize(cost))
    }
}
