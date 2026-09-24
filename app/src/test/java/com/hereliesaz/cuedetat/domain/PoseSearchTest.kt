package com.hereliesaz.cuedetat.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PoseSearchTest {

    @Test
    fun `finds the minimum of a bowl`() {
        // Minimum at (3, -2) by construction.
        val r = PoseSearch.minimize(
            start = doubleArrayOf(0.0, 0.0),
            steps = doubleArrayOf(4.0, 4.0),
            minSteps = doubleArrayOf(0.01, 0.01),
            maxEvaluations = 500,
        ) { p -> (p[0] - 3) * (p[0] - 3) + 2 * (p[1] + 2) * (p[1] + 2) }
        assertEquals(3.0, r.params[0], 0.02)
        assertEquals(-2.0, r.params[1], 0.02)
    }

    @Test
    fun `respects the evaluation budget`() {
        val r = PoseSearch.minimize(doubleArrayOf(0.0), doubleArrayOf(1.0), doubleArrayOf(1e-9), 10) { p -> abs(p[0] - 100) }
        assertTrue(r.evaluations <= 10)
    }

    @Test
    fun `a stepped cost like pixel overlap still converges near the answer`() {
        // Cost counts whole units off target 7: flat within each unit, like pixel counts.
        val r = PoseSearch.minimize(doubleArrayOf(0.0), doubleArrayOf(8.0), doubleArrayOf(0.1), 200) { p ->
            abs(kotlin.math.floor(p[0]) - 7.0)
        }
        assertEquals(7.0, kotlin.math.floor(r.params[0]), 0.0)
    }
}
