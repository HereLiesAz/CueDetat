package com.hereliesaz.cuedetat.domain

import com.hereliesaz.cuedetat.domain.ArMatrixHold.Decision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The policy is generic in the matrix type, so strings stand in for matrices. Times are in ms. */
class ArMatrixHoldTest {

    private val hold = ArMatrixHold<String>(holdNanos = 250L)

    @Test
    fun `a live matrix passes straight through`() {
        assertEquals(Decision.Publish("A", held = false), hold.decide("A", 0L))
    }

    @Test
    fun `a hole within the hold is bridged with the last matrix, marked held`() {
        hold.decide("A", 0L)
        assertEquals(Decision.Publish("A", held = true), hold.decide(null, 100L))
        assertEquals(Decision.Publish("A", held = true), hold.decide(null, 250L))
    }

    @Test
    fun `a hole longer than the hold clears the table`() {
        hold.decide("A", 0L)
        assertEquals(Decision.Clear, hold.decide(null, 251L))
        // And stays cleared: the old matrix is not brought back.
        assertEquals(Decision.Clear, hold.decide(null, 260L))
    }

    @Test
    fun `the hold is measured from the last good matrix, not from the last hole`() {
        hold.decide("A", 0L)
        hold.decide(null, 100L)
        hold.decide(null, 200L)
        assertEquals(Decision.Clear, hold.decide(null, 300L))
    }

    @Test
    fun `a matrix that comes back is live, and restarts the hold`() {
        hold.decide("A", 0L)
        hold.decide(null, 100L)
        assertEquals(Decision.Publish("B", held = false), hold.decide("B", 120L))
        assertEquals(Decision.Publish("B", held = true), hold.decide(null, 360L))
        assertEquals(Decision.Clear, hold.decide(null, 371L))
    }

    @Test
    fun `reset forgets the held matrix, as a tracking loss must`() {
        hold.decide("A", 0L)
        hold.reset()
        assertEquals(Decision.Clear, hold.decide(null, 10L))
    }

    @Test
    fun `with nothing ever shown the hole is simply a hole`() {
        assertTrue(hold.decide(null, 0L) is Decision.Clear)
    }

    @Test
    fun `a one-frame blink in a 30 fps stream never reaches the screen`() {
        // 90 frames, with frames 30, 31 and 60 missing: no frame is cleared.
        val frame = 33L
        var clears = 0
        for (i in 0 until 90) {
            val matrix = if (i == 30 || i == 31 || i == 60) null else "M$i"
            if (hold.decide(matrix, i * frame) is Decision.Clear) clears++
        }
        assertEquals(0, clears)
    }
}
