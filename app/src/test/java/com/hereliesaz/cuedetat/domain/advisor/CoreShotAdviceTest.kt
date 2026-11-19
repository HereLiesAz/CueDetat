package com.hereliesaz.cuedetat.domain.advisor

import android.graphics.PointF
import com.hereliesaz.cuedetat.core.advisor.BallGroup
import com.hereliesaz.cuedetat.data.BallType
import com.hereliesaz.cuedetat.data.DetectedBall
import com.hereliesaz.cuedetat.data.VisionData
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.TargetType
import com.hereliesaz.cuedetat.view.model.OnPlaneBall
import com.hereliesaz.cuedetat.view.model.Table
import com.hereliesaz.cuedetat.view.state.TableSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun pf(x: Float, y: Float) = PointF().apply { this.x = x; this.y = y }
private fun ball(x: Float, y: Float) = OnPlaneBall(pf(x, y), 25f)

/**
 * The app-state side of the advisor: which balls go to the planner, with which group, and what the
 * best plan looks like back in logical coordinates. The planner itself is tested in `:core:advisor`;
 * the expected numbers here are worked by hand on an 8 ft table (logical long axis y, half-width
 * 488.9 units, a ball is 50 across).
 */
class CoreShotAdviceTest {

    private val state = CueDetatState().copy(table = Table(TableSize.EIGHT_FT, true))

    @Test
    fun `without a cue ball there is no layout`() {
        assertNull(CoreShotAdvice.layout(state.copy(obstacleBalls = listOf(ball(0f, 0f)))))
    }

    @Test
    fun `without a ball to shoot at there is no layout`() {
        assertNull(CoreShotAdvice.layout(state.copy(onPlaneBall = ball(0f, 300f))))
    }

    @Test
    fun `hand-placed balls are all the shooter's own`() {
        val layout = CoreShotAdvice.layout(
            state.copy(onPlaneBall = ball(0f, 300f), obstacleBalls = listOf(ball(100f, 0f), ball(-100f, 0f))),
        )!!
        assertEquals(2, layout.balls.size)
        assertTrue(layout.balls.all { it.group == BallGroup.MINE })
        assertEquals(setOf(0, 1), layout.balls.map { it.id }.toSet())
    }

    @Test
    fun `a ball hand-placed on top of a detected one is one ball`() {
        val detected = VisionData(balls = listOf(DetectedBall(pf(100f, 0f), BallType.SOLID)))
        val layout = CoreShotAdvice.layout(
            state.copy(
                hasInverseMatrix = true, visionData = detected, targetType = TargetType.SOLIDS,
                onPlaneBall = ball(0f, 300f), obstacleBalls = listOf(ball(110f, 5f)),
            ),
        )!!
        assertEquals(1, layout.balls.size)
    }

    @Test
    fun `detections are groups against the chosen target type, and unclassified ones are only avoided`() {
        val detected = VisionData(
            balls = listOf(
                DetectedBall(pf(100f, 0f), BallType.SOLID),
                DetectedBall(pf(-100f, 0f), BallType.STRIPE),
                DetectedBall(pf(0f, -100f), BallType.EIGHT),
                DetectedBall(pf(0f, 100f), BallType.UNKNOWN),
                DetectedBall(pf(0f, 400f), BallType.CUE),
            ),
        )
        val stripes = CoreShotAdvice.layout(state.copy(hasInverseMatrix = true, visionData = detected, targetType = TargetType.STRIPES))!!
        assertEquals(
            listOf(BallGroup.THEIRS, BallGroup.MINE, BallGroup.EIGHT, BallGroup.UNKNOWN),
            stripes.balls.map { it.group },
        )
        val solids = CoreShotAdvice.layout(state.copy(hasInverseMatrix = true, visionData = detected, targetType = TargetType.SOLIDS))!!
        assertEquals(BallGroup.MINE, solids.balls[0].group)
        assertEquals(BallGroup.THEIRS, solids.balls[1].group)
    }

    @Test
    fun `detections are ignored until the table pose is calibrated`() {
        val detected = VisionData(balls = listOf(DetectedBall(pf(100f, 0f), BallType.SOLID), DetectedBall(pf(0f, 400f), BallType.CUE)))
        assertNull(CoreShotAdvice.layout(state.copy(hasInverseMatrix = false, visionData = detected)))
    }

    @Test
    fun `the best shot at a ball in line with a side pocket is that straight pot`() {
        // Cue at (-200, 0), ball at (0, 0): the line runs along +x to the right-hand side pocket. The
        // ghost ball is two radii (50) behind the ball on that line; a straight pot is a 0 degree cut.
        val shot = CoreShotAdvice.recommend(
            state.copy(onPlaneBall = ball(-200f, 0f), obstacleBalls = listOf(ball(0f, 0f))),
        )!!
        assertEquals(ShotType.DIRECT, shot.type)
        assertEquals(0f, shot.targetPos.x, 0.5f)
        assertEquals(0f, shot.targetPos.y, 0.5f)
        assertEquals(-50f, shot.ghostCuePos.x, 1f)
        assertEquals(0f, shot.ghostCuePos.y, 1f)
        assertEquals(0f, shot.cutAngleDeg, 0.5f)
        assertTrue("pocket is on the right-hand rail: ${shot.pocketPos}", shot.pocketPos!!.x > 480f)
        assertEquals(0f, shot.pocketPos!!.y, 1f)
        // cue -> ghost -> ball -> pocket
        assertEquals(4, shot.shotPath.size)
        assertEquals(-200f, shot.shotPath.first().x, 0.5f)
        assertTrue(shot.makeProbability in 0.1f..1f)
    }

    @Test
    fun `the signature follows what the planner reads, and nothing finer than the tolerance`() {
        val a = state.copy(onPlaneBall = ball(-200f, 0f), obstacleBalls = listOf(ball(0f, 0f)))
        val sig = CoreShotAdvice.signature(a)
        assertTrue(sig.sameAs(CoreShotAdvice.signature(a.copy(obstacleBalls = listOf(ball(0.4f, 0.4f))))))
        assertTrue("1.5 units of jitter is not a change", sig.sameAs(CoreShotAdvice.signature(a.copy(obstacleBalls = listOf(ball(1.5f, 0f))))))
        assertFalse("2.5 units is", sig.sameAs(CoreShotAdvice.signature(a.copy(obstacleBalls = listOf(ball(2.5f, 0f))))))
        assertFalse(sig.sameAs(CoreShotAdvice.signature(a.copy(obstacleBalls = listOf(ball(30f, 0f))))))
        assertFalse(sig.sameAs(CoreShotAdvice.signature(a.copy(table = Table(TableSize.NINE_FT, true)))))
        assertFalse(sig.sameAs(CoreShotAdvice.signature(a.copy(targetType = TargetType.STRIPES))))
        assertFalse("a ball more or fewer", sig.sameAs(CoreShotAdvice.signature(a.copy(obstacleBalls = emptyList()))))
        assertFalse("off is not on", sig.sameAs(CoreShotAdvice.OFF))
        assertTrue(CoreShotAdvice.OFF.sameAs(CoreShotAdvice.OFF))
    }

    @Test
    fun `a ball on a whole-unit boundary no longer flips the signature`() {
        // The old key truncated to whole units, so 99.99 and 100.01 were different answers.
        val a = state.copy(onPlaneBall = ball(-200f, 0f), obstacleBalls = listOf(ball(99.99f, 0f)))
        val b = a.copy(obstacleBalls = listOf(ball(100.01f, 0f)))
        assertTrue(CoreShotAdvice.signature(a).sameAs(CoreShotAdvice.signature(b)))
    }

    @Test
    fun `the order of the balls does not matter, their kinds do`() {
        val two = state.copy(onPlaneBall = ball(-200f, 0f), obstacleBalls = listOf(ball(0f, 0f), ball(100f, 100f)))
        val swapped = two.copy(obstacleBalls = listOf(ball(100f, 100f), ball(0f, 0f)))
        assertTrue(CoreShotAdvice.signature(two).sameAs(CoreShotAdvice.signature(swapped)))

        val detected = VisionData(balls = listOf(DetectedBall(pf(100f, 0f), BallType.SOLID), DetectedBall(pf(-100f, 0f), BallType.STRIPE)))
        val flipped = VisionData(balls = listOf(DetectedBall(pf(100f, 0f), BallType.STRIPE), DetectedBall(pf(-100f, 0f), BallType.SOLID)))
        val base = state.copy(onPlaneBall = ball(0f, 400f), hasInverseMatrix = true)
        assertTrue(CoreShotAdvice.signature(base.copy(visionData = detected))
            .sameAs(CoreShotAdvice.signature(base.copy(visionData = detected.copy(balls = detected.balls.reversed())))))
        assertFalse("a ball that changes kind is a change",
            CoreShotAdvice.signature(base.copy(visionData = detected)).sameAs(CoreShotAdvice.signature(base.copy(visionData = flipped))))
    }

    @Test
    fun `against the last signature acted on, slow drift adds up to a recompute`() {
        // The ball creeps 0.5 units a frame. Compared with the last signature acted on (as the
        // driver's distinctUntilChanged does), it triggers every few frames and never lets the
        // plan trail the ball by more than the tolerance.
        var actedOn = CoreShotAdvice.signature(state.copy(onPlaneBall = ball(-200f, 0f), obstacleBalls = listOf(ball(0f, 0f))))
        var recomputes = 0
        for (i in 1..40) {
            val sig = CoreShotAdvice.signature(state.copy(onPlaneBall = ball(-200f, 0f), obstacleBalls = listOf(ball(i * 0.5f, 0f))))
            if (!sig.sameAs(actedOn)) { actedOn = sig; recomputes++ }
        }
        // 20 units of creep in steps of just over 2: ten recomputes, not forty.
        assertTrue("recomputes: $recomputes", recomputes in 8..10)
    }

    @Test
    fun `a bank or a combination is still drawn from the cue to the pocket`() {
        // Two balls on the cue's line toward the right side pocket: cue (-300, 0), A (-100, 0), B (100, 0).
        // Both are the shooter's own; B can be hit directly only through A, so a combination or a plain
        // pot of A must come back, and whichever it is the path starts at the cue and ends at a pocket.
        val shot = CoreShotAdvice.recommend(
            state.copy(onPlaneBall = ball(-300f, 0f), obstacleBalls = listOf(ball(-100f, 0f), ball(100f, 0f))),
        )
        assertNotNull(shot)
        assertEquals(-300f, shot!!.shotPath.first().x, 0.5f)
        assertEquals(shot.pocketPos!!.x, shot.shotPath.last().x, 0.5f)
    }
}
