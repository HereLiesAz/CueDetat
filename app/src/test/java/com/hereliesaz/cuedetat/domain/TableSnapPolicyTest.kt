package com.hereliesaz.cuedetat.domain

import com.hereliesaz.cuedetat.domain.TableSnapPolicy.Pose
import com.hereliesaz.cuedetat.domain.TableSnapPolicy.TablePull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

class TableSnapPolicyTest {

    private val here = Pose(0f, 0f, 10f, 1f)
    private val there = Pose(100f, -40f, 30f, 2f)
    private val quiet = 10_000L

    private fun offset(x: Float) = Pose(x, 0f, 0f, 1f)

    @Test
    fun `no pull while the user is handling the table`() {
        assertNull(TablePull().step(here, there, iou = 0.95f, msSinceUserAdjust = 500L, nowMs = 0L))
    }

    @Test
    fun `no pull on an unsure fit`() {
        assertNull(TablePull().step(here, there, iou = 0.7f, msSinceUserAdjust = quiet, nowMs = 0L))
    }

    @Test
    fun `the first pull moves 15 percent of the way`() {
        val p = TablePull().step(here, there, iou = 0.95f, msSinceUserAdjust = quiet, nowMs = 1_000L)!!
        assertEquals(15f, p.offsetX, 1e-3f)          // 0.15 * 100
        assertEquals(-6f, p.offsetY, 1e-3f)          // 0.15 * -40
        assertEquals(13f, p.rotationDeg, 1e-3f)      // 10 + 0.15 * 20
        assertEquals(1.1096f, p.zoom, 1e-3f)         // 2^0.15
    }

    @Test
    fun `rotation takes the short way across the half-turn`() {
        // 85 to -85 is 10 degrees modulo 180, not 170 back the long way.
        val p = TablePull().step(Pose(0f, 0f, 85f, 1f), Pose(0f, 0f, -85f, 1f), 0.95f, quiet, 1_000L)!!
        assertEquals(86.5f, p.rotationDeg, 1e-3f)    // 85 + 0.15 * 10
    }

    @Test
    fun `already there, no step`() {
        assertNull(TablePull().step(here, here, 0.95f, quiet, 1_000L))
    }

    @Test
    fun `once started it carries on through the middle band and stops below it, and does not restart there`() {
        val pull = TablePull()
        var now = 1_000L
        // A 10 px error starts it.
        assertNotNull(pull.step(offset(0f), offset(10f), 0.95f, quiet, now))
        // An error of 1.0 px is between the exit (0.5) and the enter (1.5) thresholds: still going.
        now += 300
        assertNotNull(pull.step(offset(9f), offset(10f), 0.95f, quiet, now))
        // Below 0.5 px it stops...
        now += 300
        assertNull(pull.step(offset(9.7f), offset(10f), 0.95f, quiet, now))
        // ...and 1.0 px no longer starts it: that needs 1.5.
        now += 300
        assertNull(pull.step(offset(9f), offset(10f), 0.95f, quiet, now))
        now += 300
        assertNotNull(pull.step(offset(8f), offset(10f), 0.95f, quiet, now))
    }

    @Test
    fun `an unsure fit or a hand on the table forgets the target`() {
        val pull = TablePull()
        // Smoothed toward 100 for a few fits...
        var now = 1_000L
        repeat(5) { pull.step(offset(0f), offset(100f), 0.95f, quiet, now); now += 300 }
        // ...then a hand. The next sure fit is the target outright, not an average with the old one.
        assertNull(pull.step(offset(0f), offset(0f), 0.95f, 0L, now))
        now += 300
        val p = pull.step(offset(0f), offset(20f), 0.95f, quiet, now)!!
        assertEquals(3f, p.offsetX, 1e-3f)           // 15% of 20: the target is the new fit alone
    }

    @Test
    fun `a late fit moves the table by the same amount of time as one on schedule`() {
        // Settling from 50 px to within 5 (90%) takes about 4.25 s however often the fits arrive.
        fun secondsToClose(intervalMs: Long): Float {
            val pull = TablePull()
            var current = 0f
            var now = 1_000L
            val start = now
            while (now - start < 20_000L) {
                pull.step(offset(current), offset(50f), 0.95f, quiet, now)?.let { current = it.offsetX }
                if (50f - current < 5f) return (now - start) / 1000f
                now += intervalMs
            }
            return Float.MAX_VALUE
        }
        val fast = secondsToClose(150)
        val onTime = secondsToClose(300)
        val slow = secondsToClose(600)
        assertEquals("150 ms vs 300 ms", onTime, fast, 0.8f)
        assertEquals("600 ms vs 300 ms", onTime, slow, 0.8f)
        assertTrue("settles in about four seconds: $onTime", onTime in 3f..6f)
    }

    private fun gaussian(random: Random, sigma: Float): Float {
        val u1 = random.nextDouble().coerceAtLeast(1e-12)
        return (sqrt(-2.0 * ln(u1)) * cos(2.0 * PI * random.nextDouble())).toFloat() * sigma
    }

    @Test
    fun `against a fit that is 3 px noisy the table settles as fast and wanders less, stepping far less often`() {
        // 50 px away, 300 fits, fit noise 3 px. Compared with the pull this replaced (15% of the raw
        // error past a 1 px deadband), worked out on the same signals: 5.8 s to settle either way,
        // 0.80 px of wander in place against 0.62, and steps on 75% of fits against 37%.
        var wander = 0f
        var stepFraction = 0f
        var settle = 0f
        val seeds = 40
        for (seed in 1..seeds) {
            val random = Random(seed)
            val pull = TablePull()
            var current = 0f
            var now = 1_000L
            val trail = ArrayList<Float>()
            var steps = 0
            var settled: Float? = null
            for (k in 0 until 300) {
                val step = pull.step(offset(current), offset(50f + gaussian(random, 3f)), 0.95f, quiet, now)
                if (step != null) { if (k >= 150) steps++; current = step.offsetX }
                if (settled == null && abs(current - 50f) < 2f) settled = k * 0.3f
                if (k >= 150) trail += current
                now += 300
            }
            val mean = trail.sum() / trail.size
            wander += sqrt(trail.fold(0f) { a, v -> a + (v - mean) * (v - mean) } / trail.size)
            stepFraction += steps / 150f
            settle += settled ?: 90f
        }
        wander /= seeds; stepFraction /= seeds; settle /= seeds
        assertTrue("wander $wander px", wander < 0.75f)
        assertTrue("fraction of fits that step $stepFraction", stepFraction < 0.5f)
        assertTrue("seconds to settle $settle", settle < 8f)
    }
}
